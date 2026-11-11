package com.algoviz;

import com.algoviz.engine.Algorithm;
import com.algoviz.engine.AlgorithmRegistry;
import com.algoviz.engine.Step;
import com.algoviz.engine.ds.ListView;
import com.algoviz.engine.math.MathView;
import com.algoviz.engine.trace.Trace;
import com.algoviz.ui.CodePanel;
import com.algoviz.ui.ComplexityPanel;
import com.algoviz.app.DictationDiff;
import com.algoviz.app.DictationJudge;
import com.algoviz.ui.DiffHighlighter;
import com.algoviz.app.NotesStore;
import com.algoviz.ui.DictationPanel;


import com.algoviz.ui.IntentBar;
import com.algoviz.ui.BTreeRenderer;
import com.algoviz.ui.TabContents;
import com.algoviz.ui.CanvasDispatch;
import com.algoviz.ui.HashRenderer;
import com.algoviz.ui.MarkdownView;
import com.algoviz.ui.PlaybackController;
import com.algoviz.ui.PredictionController;
import com.algoviz.ui.PredictionPanel;
import com.algoviz.ui.Theme;
import com.algoviz.ui.VarsPanel;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.Tooltip;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * M2：左目录 / 中动画 / 右 Tab（代码·说明·预测）/ 底控制 + 现场面板（蓝图 §6.1）。
 * 预测模式 v0（蓝图 §2.5-A）：在当前帧猜「下一帧哪些格子会变」，提交揭晓 + diff。
 */
public class MainApp extends Application {


    private final PlaybackController playback = new PlaybackController();

    private BorderPane rootPane;
    private Pane canvasPane;
    private Timeline timer;
    private boolean playing;   // 播放状态的唯一真源（避免依赖 Timeline.getStatus 的瞬时值）
    private Algorithm currentAlgorithm;
    private TreeView<String> tree;
    private CodePanel codePanel;
    private String currentCodeResource;
    private Tab codeTab;
    private MarkdownView markdownView;
    private PredictionPanel predictPanel;
    private DictationPanel dictationPanel;
    private Tab complexityTab;
    private Tab compareTab;
    private VarsPanel varsPanel;
    private IntentBar intentBar;
    private Canvas canvas;
    private Slider slider;
    private Slider dataNSlider;
    private Button randBtn;
    private Label stepLabel;
    private Button playBtn;

    // 画布视图变换：拖动平移 + 缩放（滚轮/按钮），围绕画布中心
    private double viewZoom = 1, viewPanX = 0, viewPanY = 0;
    private double lastDragX, lastDragY;
    private boolean draggedView;

    // 预测模式状态机
    private PredictionController prediction;

    @Override
    public void start(Stage stage) {
        playback.addListener(this::render);

        BorderPane root = new BorderPane();
        rootPane = root;
        root.setStyle(Theme.rootStyle());

        // 左：目录
        tree = buildTree();
        tree.setPrefWidth(170);

        // 中：动画区（Pane 不参与子节点布局，画布尺寸手动跟随且钳制下限——
        // Canvas 被压到 0 会触发 NGCanvas 渲染缓冲 NPE，整个画布从此白板）
        canvas = new Canvas(600, 400);
        canvas.setOnMouseClicked(e -> {
            if (draggedView) {          // 拖动后的松手不算点选
                draggedView = false;
                return;
            }
            onCanvasClick(
                    CanvasDispatch.toWorldX(e.getX(), canvas.getWidth(), viewZoom, viewPanX),
                    CanvasDispatch.toWorldY(e.getY(), canvas.getHeight(), viewZoom, viewPanY));
        });
        canvas.setOnMousePressed(e -> {
            lastDragX = e.getX();
            lastDragY = e.getY();
            draggedView = false;
        });
        canvas.setOnMouseDragged(e -> {
            double dx = e.getX() - lastDragX;
            double dy = e.getY() - lastDragY;
            if (Math.abs(dx) + Math.abs(dy) > 3) {
                draggedView = true;
            }
            viewPanX += dx;
            viewPanY += dy;
            lastDragX = e.getX();
            lastDragY = e.getY();
            render();
        });
        canvas.setOnScroll(e -> {
            double f = e.getDeltaY() > 0 ? 1.1 : 1 / 1.1;
            viewZoom = Math.max(0.4, Math.min(6, viewZoom * f));
            render();
            e.consume();
        });
        canvasPane = new Pane(canvas);
        canvasPane.getChildren().add(buildViewControls());
        canvasPane.setStyle(Theme.canvasPaneStyle());
        canvasPane.setMinSize(240, 200);
        canvasPane.widthProperty().addListener((o, ov, nv) -> {
            if (nv != null) {
                canvas.setWidth(Math.max(60, nv.doubleValue()));
                render();
            }
        });
        canvasPane.heightProperty().addListener((o, ov, nv) -> {
            if (nv != null) {
                canvas.setHeight(Math.max(60, nv.doubleValue()));
                render();
            }
        });

        // 右：Tab（代码 / 说明 / 预测）
        currentCodeResource = "/code/BubbleSort.java";
        codePanel = new CodePanel(currentCodeResource);
        codeTab = new Tab("代码", codePanel);
        codeTab.setClosable(false);
        markdownView = new MarkdownView();
        Tab docTab = new Tab("说明", markdownView);
        docTab.setClosable(false);
        predictPanel = new PredictionPanel();
        prediction = new PredictionController(playback, predictPanel, () -> currentAlgorithm, this::render);
        predictPanel.onSubmit = prediction::submit;
        predictPanel.onReveal = prediction::reveal;
        Tab predictTab = new Tab("🎯 预测", predictPanel);
        predictTab.setClosable(false);
        dictationPanel = new DictationPanel();
        dictationPanel.onCompare = this::onDictationCompare;
        dictationPanel.onBehaviorJudge = this::onDictationBehavior;
        Tab dictationTab = new Tab("📝 默写", dictationPanel);
        dictationTab.setClosable(false);

        // M7 复杂度 Tab：增长曲线 + 体感表（画布复用中间区域）
        complexityTab = new Tab("📈 复杂度", TabContents.complexity());
        complexityTab.setClosable(false);
        complexityTab.setOnSelectionChanged(e -> {
            if (complexityTab.isSelected()) {
                stopPlayback();
                ComplexityPanel.draw(canvas, 50);
            }
        });
        compareTab = new Tab("⚖ 对比", TabContents.compare());
        compareTab.setClosable(false);
        compareTab.setOnSelectionChanged(e -> {
            if (compareTab.isSelected()) {
                stopPlayback();
                ComplexityPanel.drawEmpirical(canvas, 50);
            }
        });
        TabPane tabs = new TabPane(codeTab, docTab, predictTab, dictationTab, complexityTab, compareTab);
        tabs.setPrefWidth(500);

        // 底：意图栏 + 控制栏 + 现场面板
        intentBar = new IntentBar();
        HBox controls = buildControls();
        varsPanel = new VarsPanel();
        VBox bottom = new VBox(6, intentBar, controls, varsPanel);
        bottom.setPadding(new Insets(8));
        bottom.setStyle(Theme.rootStyle());

        // 三段可拖拽：左目录 / 中动画 / 右面板（代码/说明/预测）——分隔条随意拉
        SplitPane split = new SplitPane(tree, canvasPane, tabs);
        split.setDividerPositions(0.13, 0.60);
        SplitPane.setResizableWithParent(tree, Boolean.FALSE);

        root.setCenter(split);
        root.setBottom(bottom);

        stage.setTitle("AlgoViz — 看得见的执行现场");
        // 窗口尺寸按屏幕自适应（小屏/高缩放比下 1280×780 会把右/下挤没）
        var vb = Screen.getPrimary().getVisualBounds();
        stage.setScene(new Scene(root,
                Math.min(1280, vb.getWidth() * 0.92),
                Math.min(800, vb.getHeight() * 0.92)));
        stage.setMinWidth(980);
        stage.setMinHeight(640);
        stage.show();

        setupKeys(stage);
        selectDefault();
    }

    private TreeView<String> buildTree() {
        TreeItem<String> root = new TreeItem<>("AlgoViz");
        root.setExpanded(true);

        var all = AlgorithmRegistry.all();
        Map<String, Long> catCounts = all.stream()
                .collect(java.util.stream.Collectors.groupingBy(AlgorithmRegistry.Entry::category,
                        java.util.stream.Collectors.counting()));
        Map<String, Long> subCounts = all.stream()
                .filter(e -> e.sub() != null)
                .collect(java.util.stream.Collectors.groupingBy(e -> e.category() + "/" + e.sub(),
                        java.util.stream.Collectors.counting()));

        Map<String, TreeItem<String>> categories = new LinkedHashMap<>();
        Map<String, TreeItem<String>> subs = new LinkedHashMap<>();
        for (AlgorithmRegistry.Entry e : all) {
            TreeItem<String> cat = categories.computeIfAbsent(e.category(), k -> {
                TreeItem<String> t = new TreeItem<>(k + "(" + catCounts.get(k) + ")");
                t.setExpanded(true);
                root.getChildren().add(t);
                return t;
            });
            TreeItem<String> parent = cat;
            if (e.sub() != null) {
                String key = e.category() + "/" + e.sub();
                parent = subs.computeIfAbsent(key, k -> {
                    TreeItem<String> t = new TreeItem<>(e.sub() + "(" + subCounts.get(k) + ")");
                    t.setExpanded(true);
                    cat.getChildren().add(t);
                    return t;
                });
            }
            parent.getChildren().add(new TreeItem<>(e.algorithm().title()));
        }

        TreeView<String> tv = new TreeView<>(root);
        tv.getSelectionModel().selectedItemProperty().addListener((o, ov, nv) -> {
            if (nv == null || nv.getValue() == null) {
                return;
            }
            AlgorithmRegistry.all().stream()
                    .filter(e -> e.algorithm().title().equals(nv.getValue()))
                    .findFirst()
                    .ifPresent(e -> load(e.algorithm()));
        });
        return tv;
    }

    private void placeholder(TreeItem<String> root, String category, String leaf) {
        TreeItem<String> cat = new TreeItem<>(category);
        cat.getChildren().add(new TreeItem<>(leaf));
        root.getChildren().add(cat);
    }

    private HBox buildControls() {
        Button first = new Button("⏮");
        Button prev = new Button("◀");
        playBtn = new Button("▶");
        Button next = new Button("▶|");
        Button last = new Button("⏭");
        Button themeBtn = new Button(Theme.isDark() ? "☀" : "🌙");
        themeBtn.setOnAction(e -> {
            boolean dark = Theme.toggle();
            themeBtn.setText(dark ? "☀" : "🌙");
            applyTheme();
        });
        randBtn = new Button("🎲");
        Tooltip.install(randBtn, new Tooltip("换一批随机数据（灰=该演示是固定剧本）"));
        randBtn.setOnAction(e -> {
            if (currentAlgorithm == null) {
                return;
            }
            Algorithm fresh = AlgorithmRegistry.randomVariant(currentAlgorithm);
            if (fresh != null) {
                load(fresh);
            }
        });

        // 数据量滑杆：n = 5..12，松手才触发（valueChanging 防止连发）
        Label nLabel = new Label("n=8");
        nLabel.setStyle("-fx-font-size:11px; -fx-text-fill:#6b7280;");
        Slider nSlider = new Slider(5, 12, 8);
        nSlider.setPrefWidth(120);
        nSlider.setMajorTickUnit(1);
        nSlider.setSnapToTicks(true);
        nSlider.setShowTickMarks(true);
        nSlider.valueChangingProperty().addListener((o, ov, nv) -> {
            if (!nv) {
                regenerateWithN((int) nSlider.getValue());
            }
        });
        nSlider.valueProperty().addListener((o, ov, nv) -> nLabel.setText("n=" + nv.intValue()));
        nLabel.setGraphic(nSlider);
        nLabel.setContentDisplay(javafx.scene.control.ContentDisplay.RIGHT);
        dataNSlider = nSlider;

        first.setOnAction(e -> playback.go(0));
        prev.setOnAction(e -> playback.prev());
        next.setOnAction(e -> playback.next());
        last.setOnAction(e -> playback.go(playback.size() - 1));
        playBtn.setOnAction(e -> togglePlay());

        slider = new Slider(0, 1, 0);
        HBox.setHgrow(slider, Priority.ALWAYS);
        slider.valueProperty().addListener((o, ov, nv) -> playback.go((int) Math.round(nv.doubleValue())));

        stepLabel = new Label("0 / 0");
        HBox bar = new HBox(8, first, prev, playBtn, next, last, randBtn, nLabel, themeBtn, slider, stepLabel);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(4, 0, 4, 0));
        return bar;
    }

    private void togglePlay() {
        if (playing) {
            stopPlayback();
            return;
        }
        playing = true;
        playBtn.setText("⏸");
        if (timer == null) {
            // 到尾即停：直接停表，不递归 togglePlay（避免与按钮/空格的重入竞态）
            timer = new Timeline(new KeyFrame(Duration.millis(350), e -> {
                if (!playing) {
                    return;   // 已被外部停止：本次 tick 作废
                }
                if (playback.index() >= playback.size() - 1) {
                    stopPlayback();   // 播完：停在最后一帧
                } else {
                    playback.next();
                }
            }));
            timer.setCycleCount(Timeline.INDEFINITE);
        }
        if (playback.index() >= playback.size() - 1) {
            playback.go(0);   // 从头重播
        }
        timer.play();
    }

    private void stopPlayback() {
        playing = false;
        playBtn.setText("▶");
        if (timer != null) {
            timer.stop();
        }
    }

    /** 主题切换后统一重刷各面板。 */
    private void applyTheme() {
        rootPane.setStyle(Theme.rootStyle());
        canvasPane.setStyle(Theme.canvasPaneStyle());
        codePanel.applyTheme();
        varsPanel.applyTheme();
        intentBar.applyTheme();
        predictPanel.applyTheme();
        markdownView.applyTheme();
        render();
    }

    /** 渲染管线：UI 各组件分别取自己订阅的图层 + 上帧做 diff（蓝图 §2.2 视图 C）。
     *  try/catch 保证单个算法渲染异常不会把整个播放器打死成白板。 */
    private void render() {
        try {
            if (canvas.getWidth() < 10 || canvas.getHeight() < 10) {
                return; // 画布还没就绪，画了也白画
            }
            Step cur = playback.current();
            Step prev = playback.index() > 0 ? playback.trace().steps().get(playback.index() - 1) : null;
            Set<Integer> pickedNow = prediction.armed() ? prediction.picked() : null;
            CanvasDispatch.paint(canvas, prev, cur, pickedNow, viewZoom, viewPanX, viewPanY);
            if (cur != null) {
                codePanel.highlight(cur.loc().startLine());
            }
            varsPanel.render(cur, prev);
            intentBar.render(cur);
            slider.setMax(Math.max(0, playback.size() - 1));
            slider.setValue(playback.index());
            stepLabel.setText((playback.index() + 1) + " / " + playback.size());
            prediction.onNavChanged();
        } catch (Exception e) {
            System.err.println(">>> render 失败 @step " + playback.index() + "：");
            e.printStackTrace();
        }
    }

    /** 数据量滑杆：按 n 重新生成当前算法（支持随机变体的才生效）。 */
    private void regenerateWithN(int n) {
        if (currentAlgorithm == null) {
            return;
        }
        Algorithm fresh = AlgorithmRegistry.randomVariant(currentAlgorithm, n);
        if (fresh != null) {
            load(fresh);
        }
    }

    private void load(Algorithm alg) {
        stopPlayback();   // 换算法：播放立刻停，避免 timer 在新 Trace 上继续走
        viewZoom = 1;     // 视图复位：新演示从默认视角开始
        viewPanX = 0;
        viewPanY = 0;
        currentAlgorithm = alg;
        Trace trace = alg.produce();
        // 代码页每次重建：🎲 随机输入后，main() 里的数据行要与画面同步
        codePanel = new CodePanel(alg.codeResource(), alg.dataLines(), alg.complexity());
        codeTab.setContent(codePanel);
        markdownView.loadResource(alg.docResource());
        // 默写面板：设置参考答案 + 清空白纸
        var refLines = new java.util.ArrayList<String>();
        try (var is = getClass().getResourceAsStream(alg.codeResource())) {
            if (is != null) {
                new String(is.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8)
                        .lines().forEach(refLines::add);
            }
        } catch (java.io.IOException e) { /* ignore */ }
        dictationPanel.setReferenceCode(String.join("\n", refLines));
        dictationPanel.clear();
        prediction.onAlgorithmLoaded();
        playback.load(trace);
        // 🎲/滑杆：固定剧本类没有「换一批数据」——置灰 + 提示，不再静默无反应
        boolean canRoll = AlgorithmRegistry.hasVariant(alg);
        randBtn.setDisable(!canRoll);
        randBtn.setText(canRoll ? "🎲" : "🎲🚫");
        dataNSlider.setDisable(!canRoll);
    }

    // ---------- 预测模式（蓝图 §2.5-A） ----------

    private void onCanvasClick(double x, double y) {
        Step cur = playback.current();
        if (cur == null) {
            return;
        }
        prediction.onCanvasClick(CanvasDispatch.pick(canvas, cur, x, y));
    }

    /** 画布右上角的视图控件：放大 / 缩小 / 还原（配合拖拽平移与滚轮缩放）。 */
    private javafx.scene.Node buildViewControls() {
        Button zoomIn = new Button("＋");
        Button zoomOut = new Button("－");
        Button reset = new Button("⟲");
        zoomIn.setOnAction(e -> {
            viewZoom = Math.min(6, viewZoom * 1.25);
            render();
        });
        zoomOut.setOnAction(e -> {
            viewZoom = Math.max(0.4, viewZoom / 1.25);
            render();
        });
        reset.setOnAction(e -> {
            viewZoom = 1;
            viewPanX = 0;
            viewPanY = 0;
            render();
        });
        for (Button b : new Button[]{zoomIn, zoomOut, reset}) {
            b.setStyle("-fx-background-color: rgba(255,255,255,0.9); -fx-border-color: #cbd5e1;"
                    + "-fx-border-radius: 6; -fx-background-radius: 6; -fx-padding: 1 7 1 7; -fx-cursor: hand;");
        }
        javafx.scene.layout.HBox box = new javafx.scene.layout.HBox(4, zoomIn, zoomOut, reset);
        box.layoutXProperty().bind(canvasPane.widthProperty().subtract(118));
        box.setLayoutY(8);
        box.setMouseTransparent(false);
        return box;
    }

    private void onDictationBehavior() {
        var verdict = DictationJudge.judge(dictationPanel.getReferenceCode(), dictationPanel.getUserCode());
        dictationPanel.setResultText(verdict.text());
        if (currentAlgorithm != null) {
            var entry = NotesStore.get(currentAlgorithm.id());
            entry.lastDictation = dictationPanel.getUserCode();
            entry.behaviorVerdict = verdict.grade();
            NotesStore.save(currentAlgorithm.id(), entry);
        }
    }

    private void onDictationCompare() {
        var lines = DictationDiff.diff(dictationPanel.getUserCode(), dictationPanel.getReferenceCode());
        dictationPanel.setResultText(DictationDiff.report(lines));
        // 持久化
        if (currentAlgorithm != null) {
            var entry = NotesStore.get(currentAlgorithm.id());
            entry.lastDictation = dictationPanel.getUserCode();
            entry.lastDictationDiff = DictationDiff.report(lines);
            NotesStore.save(currentAlgorithm.id(), entry);
        }
    }


    // ---------- 其他 ----------

    private void selectDefault() {
        AlgorithmRegistry.all().stream().findFirst().ifPresent(e -> {
            TreeItem<String> leaf = find(tree.getRoot(), e.algorithm().title());
            if (leaf != null) {
                tree.getSelectionModel().select(leaf);
            } else {
                load(e.algorithm());
            }
        });
    }

    private TreeItem<String> find(TreeItem<String> node, String value) {
        if (value.equals(node.getValue())) {
            return node;
        }
        for (TreeItem<String> c : node.getChildren()) {
            TreeItem<String> r = find(c, value);
            if (r != null) {
                return r;
            }
        }
        return null;
    }

    private void setupKeys(Stage stage) {
        stage.getScene().setOnKeyPressed(e -> {
            switch (e.getCode()) {
                case RIGHT -> playback.next();
                case LEFT -> playback.prev();
                case N -> cycleAlgorithm(1);   // N = 下一个算法
                case R -> {                     // R = 换一批随机数据（同 🎲）
                    if (currentAlgorithm != null) {
                        Algorithm fresh = AlgorithmRegistry.randomVariant(currentAlgorithm);
                        if (fresh != null) load(fresh);
                    }
                }
                case P -> cycleAlgorithm(-1);  // P = 上一个算法
                case SPACE -> {
                    e.consume();
                    togglePlay();
                }
                default -> {
                }
            }
        });
    }

    /** 循环切换算法（Tab 树选择触发 load）。 */
    private void cycleAlgorithm(int delta) {
        var all = AlgorithmRegistry.all();
        if (all.isEmpty()) {
            return;
        }
        int cur = 0;
        for (int i = 0; i < all.size(); i++) {
            if (all.get(i).algorithm().id().equals(currentAlgorithm != null ? currentAlgorithm.id() : "")) {
                cur = i;
                break;
            }
        }
        var next = all.get((cur + delta + all.size()) % all.size());
        TreeItem<String> leaf = find(tree.getRoot(), next.algorithm().title());
        if (leaf != null) {
            tree.getSelectionModel().select(leaf);
        } else {
            load(next.algorithm());
        }
    }
}
