package com.algoviz;

import com.algoviz.engine.Algorithm;
import com.algoviz.engine.AlgorithmRegistry;
import com.algoviz.engine.Step;
import com.algoviz.engine.ds.ListView;
import com.algoviz.engine.math.MathView;
import com.algoviz.engine.trace.Trace;
import com.algoviz.ui.CodePanel;
import com.algoviz.ui.ComplexityCurve;
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
    private TabPane tabPane;
    /** 固定 Tab（代码/说明/预测/默写）——复杂度/对比按演示能力动态增删，顺序保持。 */
    private final java.util.List<Tab> fixedTabs = new java.util.ArrayList<>();
    private VarsPanel varsPanel;
    private IntentBar intentBar;
    private Canvas canvas;
    private Slider slider;
    private Slider dataNSlider;
    private Button randBtn;
    private Label stepLabel;
    private Button playBtn;
    private final java.util.List<javafx.scene.control.ToggleButton> levelBtns = new java.util.ArrayList<>();

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
            if (Math.abs(dx) + Math.abs(dy) > 8) {
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
            double newZoom = Math.max(0.4, Math.min(6, viewZoom * f));
            // 以鼠标位置为锚缩放：光标下的内容保持不动（白板体感）
            double mx = e.getX() - canvas.getWidth() / 2.0;
            double my = e.getY() - canvas.getHeight() / 2.0;
            double ratio = newZoom / viewZoom;
            viewPanX = mx * (1 - ratio) + viewPanX * ratio;
            viewPanY = my * (1 - ratio) + viewPanY * ratio;
            viewZoom = newZoom;
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
            }
            render();   // 选中→画曲线；切走→恢复算法现场（render 按 Tab 分流）
        });
        compareTab = new Tab("⚖ 对比", TabContents.compare());
        compareTab.setClosable(false);
        compareTab.setOnSelectionChanged(e -> {
            if (compareTab.isSelected()) {
                stopPlayback();
            }
            render();
        });
        fixedTabs.clear();
        fixedTabs.add(codeTab);
        fixedTabs.add(docTab);
        fixedTabs.add(predictTab);
        fixedTabs.add(dictationTab);
        tabPane = new TabPane(codeTab, docTab, predictTab, dictationTab, complexityTab, compareTab);
        tabPane.setPrefWidth(500);

        // 底：意图栏 + 控制栏 + 现场面板
        intentBar = new IntentBar();
        HBox controls = buildControls();
        varsPanel = new VarsPanel();
        VBox bottom = new VBox(6, intentBar, controls, varsPanel);
        bottom.setPadding(new Insets(8));
        bottom.setStyle(Theme.rootStyle());

        // 三段可拖拽：左目录 / 中动画 / 右面板（代码/说明/预测）——分隔条随意拉
        SplitPane split = new SplitPane(tree, canvasPane, tabPane);
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

    private HBox buildControls() {
        // 走带按钮语义（对称）：|< 到第一步 · < 上一步 · ▶ 播放(播放中 ||) · > 下一步 · >| 到最后一步
        // 跳转仍用 ASCII |< >|（用户约定）；▶ 是唯一 Unicode——主题按钮 ☀🌙 早已用 Unicode 且
        // Win11 字体回退链正常（Segoe UI Symbol 兜底）；仍避开 ⏮◀⏭ 这类缺字形风险的生僻符号
        Button first = new Button("|<");
        Button prev = new Button("<");
        playBtn = new Button("▶");
        Button next = new Button(">");
        Button last = new Button(">|");
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

        // 数据量滑杆：n = 5..15（与难度三档同域），松手才触发（valueChanging 防止连发）
        Label nLabel = new Label("n=5");
        nLabel.setStyle("-fx-font-size:11px; -fx-text-fill:#6b7280;");
        Slider nSlider = new Slider(5, 15, 5);
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

        // 难度三档（需求 3）：简单/中等/复杂 = n 5/10/15 的预设——与滑杆同域、双向回写
        // （成功重生成后 syncDataControls 对齐三控件；固定剧本类置灰）
        javafx.scene.control.ToggleGroup levelGroup = new javafx.scene.control.ToggleGroup();
        for (int[] lv : new int[][]{{0, 5}, {1, 10}, {2, 15}}) {
            javafx.scene.control.ToggleButton tb = new javafx.scene.control.ToggleButton(
                    new String[]{"简单", "中等", "复杂"}[lv[0]]);
            tb.setToggleGroup(levelGroup);
            tb.setUserData(lv[1]);
            tb.setStyle("-fx-font-size: 11px; -fx-padding: 2 7 2 7;");
            Tooltip.install(tb, new Tooltip("预设 n=" + lv[1]
                    + "（各演示按步数预算自行钳制实际规模，如 n≤10/12/16）"));
            tb.setOnAction(e -> regenerateWithN((int) tb.getUserData()));
            levelBtns.add(tb);
        }
        levelGroup.getToggles().get(0).setSelected(true);
        javafx.scene.layout.HBox levelBox = new javafx.scene.layout.HBox(2,
                levelBtns.get(0), levelBtns.get(1), levelBtns.get(2));

        first.setOnAction(e -> navigate(0));
        prev.setOnAction(e -> navigate(playback.index() - 1));
        next.setOnAction(e -> navigate(playback.index() + 1));
        last.setOnAction(e -> navigate(playback.size() - 1));
        playBtn.setOnAction(e -> togglePlay());

        slider = new Slider(0, 1, 0);
        HBox.setHgrow(slider, Priority.ALWAYS);
        slider.valueProperty().addListener((o, ov, nv) -> navigate((int) Math.round(nv.doubleValue())));

        stepLabel = new Label("0 / 0");
        HBox bar = new HBox(8, first, prev, playBtn, next, last, randBtn, levelBox, nLabel, themeBtn, slider, stepLabel);
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
        playBtn.setText("||");
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
     *  try/catch 保证单个算法渲染异常不会把整个播放器打死成白板。
     *  画布内容跟随 Tab：复杂度/对比 Tab 选中时画曲线，其余 Tab 画算法现场——
     *  切回/resize/load 全部经这里收敛，图表画上去之后必有恢复路径。 */
    private void render() {
        try {
            if (canvas.getWidth() < 10 || canvas.getHeight() < 10) {
                return; // 画布还没就绪，画了也白画
            }
            Step cur = playback.current();
            Step prev = playback.index() > 0 ? playback.trace().steps().get(playback.index() - 1) : null;
            if (compareTab.isSelected()) {
                ComplexityPanel.drawEmpirical(canvas, 50);
            } else if (complexityTab.isSelected()) {
                var cx = currentAlgorithm == null ? null : currentAlgorithm.complexity();
                if (cx != null) {
                    var curve = ComplexityCurve.parse(cx.time());
                    int n = playback.trace().meta().inputSize();
                    ComplexityPanel.draw(canvas, 50, curve.ids(), n, cx.time());
                } else {
                    ComplexityPanel.draw(canvas, 50);
                }
            } else {
                Set<Integer> pickedNow = prediction.armed() ? prediction.picked() : null;
                CanvasDispatch.paint(canvas, prev, cur, pickedNow, viewZoom, viewPanX, viewPanY);
            }
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

    /** 数据量滑杆/难度三档共用：按 n 重生成；成功后回写滑杆值与难度选中态——三控件同源不说谎。 */
    private void regenerateWithN(int n) {
        if (currentAlgorithm == null) {
            return;
        }
        Algorithm fresh = AlgorithmRegistry.randomVariant(currentAlgorithm, n);
        if (fresh != null) {
            load(fresh);
            syncDataControls(n);
        }
    }

    /** n 的唯一显示源：滑杆值 + 难度三档选中态（n≠5/10/15 时三档全部浮起——诚实反映"自定义 n"）。 */
    private void syncDataControls(int n) {
        if ((int) dataNSlider.getValue() != n) {
            dataNSlider.setValue(n);   // 程序化赋值不触发 valueChanging，不会递归重生成
        }
        int[] presets = {5, 10, 15};
        for (int i = 0; i < levelBtns.size() && i < presets.length; i++) {
            levelBtns.get(i).setSelected(n == presets[i]);
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
        for (javafx.scene.control.ToggleButton tb : levelBtns) {   // 难度三档同步置灰
            tb.setDisable(!canRoll);
        }
        // 复杂度/对比 Tab 按当前演示能力显隐：无复杂度标注的（模式/物理/…）不显示 📈；
        // 对比图是六种排序同数据实测，写死排序专用——非排序族不显示 ⚖
        syncTabs(alg.complexity() != null, alg.id().startsWith("sort."));
    }

    /** 复杂度/对比 Tab 显隐同步：被隐藏的是选中 Tab 时先落回「代码」页再移除（触发其取消选中 → render 恢复现场）。 */
    private void syncTabs(boolean showComplexity, boolean showCompare) {
        java.util.List<Tab> wanted = new java.util.ArrayList<>(fixedTabs);
        if (showComplexity) {
            wanted.add(complexityTab);
        }
        if (showCompare) {
            wanted.add(compareTab);
        }
        if (tabPane.getTabs().equals(wanted)) {
            return;   // 常见路径：能力未变，不动 Tab 栏（选中态原样保留）
        }
        Tab selected = tabPane.getSelectionModel().getSelectedItem();
        Tab toRestore = selected;
        if ((selected == complexityTab && !showComplexity) || (selected == compareTab && !showCompare)) {
            toRestore = codeTab;   // 当前页要被隐藏 → 落回「代码」
        }
        tabPane.getTabs().setAll(wanted);
        if (toRestore != null && wanted.contains(toRestore)) {
            tabPane.getSelectionModel().select(toRestore);   // setAll 会清选中态，恢复到原 Tab
        }
    }

    // ---------- 预测模式（蓝图 §2.5-A） ----------

    /** 显式导航（按钮/滑杆/键盘）：平移自动归零——防止拖动残留导致步进"看起来没反应"（缩放保留）。 */
    private void navigate(int i) {
        viewPanX = 0;
        viewPanY = 0;
        playback.go(i);
    }

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
        // 用 addEventFilter（tunnel 阶段）而非 setOnKeyPressed：
        // 焦点在滑杆/树/面板时，方向键会被组件先行消费——filter 保证左右键永远步进
        stage.getScene().addEventFilter(javafx.scene.input.KeyEvent.KEY_PRESSED, e -> {
            switch (e.getCode()) {
                case RIGHT -> {
                    navigate(playback.index() + 1);
                    e.consume();
                }
                case LEFT -> {
                    navigate(playback.index() - 1);
                    e.consume();
                }
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
