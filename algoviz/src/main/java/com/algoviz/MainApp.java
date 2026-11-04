package com.algoviz;

import com.algoviz.engine.Algorithm;
import com.algoviz.engine.AlgorithmRegistry;
import com.algoviz.engine.Step;
import com.algoviz.engine.ds.ListView;
import com.algoviz.engine.math.MathView;
import com.algoviz.engine.trace.Trace;
import com.algoviz.ui.BarRenderer;
import com.algoviz.ui.BitmapRenderer;
import com.algoviz.ui.CodePanel;
import com.algoviz.ui.ComplexityPanel;
import com.algoviz.ui.DictationDiff;
import com.algoviz.ui.DictationJudge;
import com.algoviz.ui.DiffHighlighter;
import com.algoviz.ui.NotesStore;
import com.algoviz.ui.DictationPanel;


import com.algoviz.ui.GraphRenderer;
import com.algoviz.ui.IntentBar;
import com.algoviz.ui.BTreeRenderer;
import com.algoviz.ui.HashRenderer;
import com.algoviz.ui.ListRenderer;
import com.algoviz.ui.MarkdownView;
import com.algoviz.ui.MatrixRenderer;
import com.algoviz.ui.PlaybackController;
import com.algoviz.ui.PlotRenderer;
import com.algoviz.ui.PredictionPanel;
import com.algoviz.ui.SequenceRenderer;
import com.algoviz.ui.Theme;
import com.algoviz.ui.TreeRenderer;
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

    private enum PredictPhase { ARMED, REVEALED }

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
    private Label stepLabel;
    private Button playBtn;

    // 预测模式状态机
    private PredictPhase phase;
    private int armedIndex = -1;
    private final Set<Integer> picked = new LinkedHashSet<>();
    private int asked;
    private int correct;
    private int streak;

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
        canvas.setOnMouseClicked(e -> onCanvasClick(e.getX(), e.getY()));
        canvasPane = new Pane(canvas);
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
        predictPanel.onSubmit = this::onPredictSubmit;
        predictPanel.onReveal = this::onPredictReveal;
        Tab predictTab = new Tab("🎯 预测", predictPanel);
        predictTab.setClosable(false);
        dictationPanel = new DictationPanel();
        dictationPanel.onCompare = this::onDictationCompare;
        dictationPanel.onBehaviorJudge = this::onDictationBehavior;
        Tab dictationTab = new Tab("📝 默写", dictationPanel);
        dictationTab.setClosable(false);

        // M7 复杂度 Tab：增长曲线 + 体感表（画布复用中间区域）
        complexityTab = new Tab("📈 复杂度", buildComplexityContent());
        complexityTab.setClosable(false);
        complexityTab.setOnSelectionChanged(e -> {
            if (complexityTab.isSelected()) {
                stopPlayback();
                ComplexityPanel.draw(canvas, 50);
            }
        });
        compareTab = new Tab("⚖ 对比", buildCompareContent());
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
        Button randBtn = new Button("🎲");
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
            Set<Integer> changed = DiffHighlighter.changedIndices(
                    prev == null ? null : prev.array(), cur == null ? null : cur.array());
            Set<String> changedArrows = DiffHighlighter.changedArrows(
                    prev == null ? null : prev.list(), cur == null ? null : cur.list());
            Set<String> changedTreeNodes = DiffHighlighter.changedTreeNodes(
                    prev == null ? null : prev.tree(), cur == null ? null : cur.tree());
            Set<String> changedTreeEdges = DiffHighlighter.changedTreeEdges(
                    prev == null ? null : prev.tree(), cur == null ? null : cur.tree());
            Set<String> changedGraphNodes = DiffHighlighter.changedGraphNodes(
                    prev == null ? null : prev.graph(), cur == null ? null : cur.graph());
            Set<String> changedHashArrows = HashRenderer.changedArrows(
                    prev == null ? null : prev.hash(), cur == null ? null : cur.hash());
            Set<String> changedBTree = BTreeRenderer.changedNodes(
                    prev == null ? null : prev.btree(), cur == null ? null : cur.btree());
            Set<String> changedSeq = DiffHighlighter.changedSeqParticipants(
                    prev == null ? null : prev.seq(), cur == null ? null : cur.seq());
            Set<String> changedMatrix = DiffHighlighter.changedMatrixCells(
                    prev == null ? null : prev.math(), cur == null ? null : cur.math());
            Set<Integer> changedBits = new java.util.LinkedHashSet<>();
            for (String k : changedMatrix) {
                if (k.startsWith("bits:")) {
                    changedBits.add(Integer.parseInt(k.substring(5)));
                }
            }
            Set<Integer> pickedNow = phase == PredictPhase.ARMED ? picked : null;
            if (cur != null && cur.list() != null) {
                ListRenderer.draw(canvas, cur, pickedNow, changedArrows);
            } else if (cur != null && cur.tree() != null) {
                TreeRenderer.draw(canvas, cur, pickedNow, changedTreeNodes, changedTreeEdges);
            } else if (cur != null && cur.btree() != null) {
                BTreeRenderer.draw(canvas, cur, pickedNow, changedBTree);
            } else if (cur != null && cur.graph() != null) {
                GraphRenderer.draw(canvas, cur, pickedNow, changedGraphNodes);
            } else if (cur != null && cur.hash() != null) {
                HashRenderer.draw(canvas, cur, pickedNow, changedHashArrows);
            } else if (cur != null && cur.seq() != null) {
                SequenceRenderer.draw(canvas, cur, pickedNow, changedSeq);
            } else if (cur != null && cur.math() != null && cur.bitmap() == null) {
                MatrixRenderer.draw(canvas, cur, pickedNow, changedMatrix);
            } else if (cur != null && cur.plot() != null) {
                PlotRenderer.draw(canvas, cur);
            } else if (cur != null && cur.bitmap() != null) {
                BitmapRenderer.draw(canvas, cur, pickedNow, changedBits);
            } else {
                BarRenderer.draw(canvas, cur, pickedNow, changed);
            }
            if (cur != null) {
                codePanel.highlight(cur.loc().startLine());
            }
            varsPanel.render(cur, prev);
            intentBar.render(cur);
            slider.setMax(Math.max(0, playback.size() - 1));
            slider.setValue(playback.index());
            stepLabel.setText((playback.index() + 1) + " / " + playback.size());
            updatePrediction();
        } catch (Exception e) {
            System.err.println(">>> render 失败 @step " + playback.index() + "：");
            e.printStackTrace();
        }
    }

    /** ⚖ 对比 Tab：六种排序在同一批数据上的实测步数曲线。 */
    private javafx.scene.layout.VBox buildCompareContent() {
        Label title = new Label("实测对比：同一批数据，六种排序各跑一遍");
        title.setStyle("-fx-font-size:15px; -fx-font-weight:bold; -fx-text-fill:#111;");
        Label desc = new Label("纵轴 = 步数（trace 长度），横轴 = n。固定种子随机数据，六个算法吃完全相同的输入。"
                + "看冒泡（红）与快排（绿）的裂口随 n 拉大——公式里的 O(n²) vs O(n log n) 落到实数上就是这样。");
        desc.setWrapText(true);
        desc.setStyle("-fx-text-fill:#374151; -fx-font-size:12px;");
        var box = new javafx.scene.layout.VBox(8, title, desc);
        box.setPadding(new Insets(10));
        return box;
    }

    /** 复杂度 Tab 内容：说明文本 + 体感表。 */
    private javafx.scene.layout.VBox buildComplexityContent() {
        Label title = new Label("复杂度可视化");
        title.setStyle("-fx-font-size:15px; -fx-font-weight:bold; -fx-text-fill:#111;");
        Label desc = new Label("中间画布显示 O(1)~O(n²) 增长曲线（切到此 Tab 即绘制）。"
                + "曲线在 n=50 内对比；不同算法可切过去按 🎲 换 n 看步数变化。");
        desc.setWrapText(true);
        desc.setStyle("-fx-text-fill:#374151; -fx-font-size:12px;");
        Label table = new Label(ComplexityPanel.feelTable());
        table.setStyle("-fx-font-family:monospace; -fx-font-size:12px; -fx-text-fill:#111827;"
                + " -fx-background-color:#f6f8fa; -fx-padding:10; -fx-background-radius:4;");
        var box = new javafx.scene.layout.VBox(8, title, desc, table);
        box.setPadding(new Insets(10));
        return box;
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
        phase = null;
        picked.clear();
        playback.load(trace);
    }

    // ---------- 预测模式（蓝图 §2.5-A） ----------

    private void onCanvasClick(double x, double y) {
        if (phase != PredictPhase.ARMED) {
            return;
        }
        Step cur = playback.current();
        if (cur == null) {
            return;
        }
        int idx;
        if (cur.list() != null) {
            idx = ListRenderer.hitIndex(canvas.getWidth(), canvas.getHeight(),
                    cur.list().nodes().size(), x, y);
        } else if (cur.tree() != null) {
            idx = TreeRenderer.hitIndex(canvas, cur, x, y);
        } else if (cur.graph() != null) {
            idx = GraphRenderer.hitIndex(canvas, cur, x, y);
        } else if (cur.hash() != null) {
            idx = HashRenderer.hitIndex(canvas, cur, x, y);
        } else if (cur.btree() != null) {
            idx = BTreeRenderer.hitIndex(canvas, cur, x, y);
        } else if (cur.seq() != null) {
            idx = SequenceRenderer.hitIndex(canvas, cur, x, y);
        } else if (cur.math() != null) {
            idx = MatrixRenderer.hitIndex(canvas, cur, x, y);
        } else if (cur.bitmap() != null) {
            idx = BitmapRenderer.hitIndex(canvas, cur, x, y);
        } else if (cur.array() != null) {
            idx = BarRenderer.hitIndex(canvas.getWidth(), canvas.getHeight(),
                    cur.array().values().size(), x, y);
        } else {
            return;
        }
        if (idx >= 0) {
            if (!picked.add(idx)) {
                picked.remove(idx);
            }
            render();
        }
    }

    private void updatePrediction() {
        int idx = playback.index();
        boolean keepArmed = phase == PredictPhase.ARMED && idx == armedIndex;
        boolean keepRevealed = phase == PredictPhase.REVEALED && idx == armedIndex + 1;
        if (!keepArmed && !keepRevealed) {
            // 导航漂移 → 重新出题（清掉旧结果）；提交揭晓后的帧保留结果
            arm(phase != PredictPhase.REVEALED);
            return;
        }
        if (phase == PredictPhase.ARMED) {
            int size = playback.size();
            if (idx >= size - 1) {
                predictPanel.setQuestion("已到最后一步，没有下一帧了。按 ⏮ 回到开头再练一轮。");
                predictPanel.setPicked("");
                predictPanel.setButtons(false, false, "提交答案");
            } else {
                Step cur = playback.current();
                boolean listMode = cur != null && cur.list() != null;
                boolean treeMode = cur != null && cur.tree() != null;
                boolean graphMode = cur != null && cur.graph() != null;
                boolean hashMode = cur != null && cur.hash() != null;
                boolean btreeMode = cur != null && cur.btree() != null;
                boolean seqMode = cur != null && cur.seq() != null;
                boolean mathMode = cur != null && cur.math() != null && cur.bitmap() == null;
                boolean bitmapMode = cur != null && cur.bitmap() != null;
                String target = bitmapMode ? "BitSet 上哪些格子会翻转？（点格子）"
                        : mathMode ? "C 的哪个格子会写数？（点 C 矩阵的格子）"
                        : seqMode ? "下一条消息会经过哪些泳道？（点泳道头选择）"
                        : graphMode || treeMode || hashMode || btreeMode ? "哪些节点会变？（点节点选择）"
                        : listMode ? "哪些节点的 next 箭头会变？（点盒子选择）"
                        : "哪些数组格子会变？（点柱子选择）";
                String noneCase = bitmapMode ? "没有格子翻转"
                        : mathMode ? "本步没有格子写数"
                        : seqMode ? "没有消息要发"
                        : graphMode ? "没有节点变化" : treeMode ? "没有任何节点变化"
                        : hashMode ? "链没动" : btreeMode ? "树纹丝不动" 
                        : listMode ? "一根箭头都不断" : "数组不变";
                predictPanel.setQuestion("第 " + (idx + 1) + " 步 → 第 " + (idx + 2) + " 步：" + target
                        + "\n全不选 = 预测「" + noneCase + "」");
                predictPanel.setPicked(picked.isEmpty() ? "已选：（无）" : "已选：" + fmtIdx(picked, cur));
                predictPanel.setButtons(true, true, "提交答案");
            }
        }
    }

    private void arm(boolean clearResult) {
        phase = PredictPhase.ARMED;
        armedIndex = playback.index();
        picked.clear();
        if (clearResult) {
            predictPanel.setResult("");
        }
    }

    /** 默写对比：用户代码 vs 参考答案，逐行 LCS diff。 */
    /** 行为级判卷：用户代码真的编译跑一遍，与参考代码的输出比对。 */
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

    private void onPredictSubmit() {
        if (phase == PredictPhase.REVEALED) {
            arm(false);
            render();
            return;
        }
        int idx = playback.index();
        if (idx >= playback.size() - 1) {
            return;
        }
        Step cur = playback.current();
        Step next = playback.trace().steps().get(idx + 1);
        Set<Integer> truth = truthIndices(cur, next);
        boolean listMode = cur.list() != null;
        boolean treeMode = cur.tree() != null;
        boolean graphMode = cur.graph() != null;
        boolean mathMode = cur.math() != null && cur.bitmap() == null;
        boolean bitmapMode = cur.bitmap() != null;

        asked++;
        List<Integer> missed = truth.stream().filter(t -> !picked.contains(t)).toList();
        List<Integer> extra = picked.stream().filter(p -> !truth.contains(p)).toList();
        boolean full = missed.isEmpty() && extra.isEmpty();
        if (full) {
            correct++;
            streak++;
        } else {
            streak = 0;
        }

        String truthStr = truth.isEmpty()
                ? (bitmapMode ? "没有格子翻转（只是读了一下）"
                : mathMode ? "本步没有格子写数（只是移动了读取的行列）"
                        : treeMode || graphMode ? "没有节点变化（只动了栈/表）"
                        : cur.hash() != null ? "链一条都没变（变的是桶高亮/变量）"
                        : cur.btree() != null ? "树没变（只动了下探路径）"
                        : listMode ? "一根箭头都没断（变的是栈里的指针变量！）"
                        : "数组一个格子都没变（变的是栈里的变量！）")
                : fmtIdx(truth, cur);
        StringBuilder sb = new StringBuilder(full ? "✅ 全对！实际变化：" : "❌ 实际变化：");
        sb.append(truthStr);
        if (!missed.isEmpty()) {
            sb.append("；漏了 ").append(fmtIdx(missed, cur));
        }
        if (!extra.isEmpty()) {
            sb.append("；多选了 ").append(fmtIdx(extra, cur));
        }
        predictPanel.setResult(sb.toString());
        predictPanel.setScore(asked + " 题 · " + correct + " 全对 · 连对 " + streak);
        // 持久化预测得分
        if (currentAlgorithm != null) {
            var entry = NotesStore.get(currentAlgorithm.id());
            entry.predictionAsked = asked;
            entry.predictionCorrect = correct;
            entry.predictionStreak = streak;
            NotesStore.save(currentAlgorithm.id(), entry);
        }

        phase = PredictPhase.REVEALED;
        playback.go(idx + 1);   // 揭晓：跳到下一帧，diff 黄圈可见
        predictPanel.setButtons(true, true, "下一题 ▶");
    }

    private void onPredictReveal() {
        if (phase == PredictPhase.REVEALED) {
            return;
        }
        int idx = playback.index();
        if (idx >= playback.size() - 1) {
            return;
        }
        Step cur = playback.current();
        Step next = playback.trace().steps().get(idx + 1);
        Set<Integer> truth = truthIndices(cur, next);
        predictPanel.setResult("（跳过，未计分）实际变化："
                + (truth.isEmpty() ? (cur.list() != null ? "箭头没变" : "数组没变") : fmtIdx(truth, cur)));
        phase = PredictPhase.REVEALED;
        playback.go(idx + 1);
        predictPanel.setButtons(true, true, "下一题 ▶");
    }

    /** 预测的真值：与下一帧相比，变的是哪些下标（数组格子 / 链表箭头 / 树节点）。 */
    private static Set<Integer> truthIndices(Step cur, Step next) {
        if (cur == null || next == null) {
            return Set.of();
        }
        if (cur.array() != null) {
            return DiffHighlighter.changedIndices(cur.array(), next.array());
        }
        if (cur.list() != null) {
            Set<Integer> out = new LinkedHashSet<>();
            List<ListView.NodeBox> nodes = cur.list().nodes();
            for (String nodeId : DiffHighlighter.changedArrows(cur.list(), next.list())) {
                for (int i = 0; i < nodes.size(); i++) {
                    if (nodes.get(i).nodeId().equals(nodeId)) {
                        out.add(i);
                        break;
                    }
                }
            }
            return out;
        }
        if (cur.tree() != null) {
            Set<Integer> out = new LinkedHashSet<>();
            var nodes = cur.tree().nodes();
            for (String nodeId : DiffHighlighter.changedTreeNodes(cur.tree(), next.tree())) {
                for (int i = 0; i < nodes.size(); i++) {
                    if (nodes.get(i).nodeId().equals(nodeId)) {
                        out.add(i);
                        break;
                    }
                }
            }
            return out;
        }
        if (cur.btree() != null) {
            Set<Integer> out = new LinkedHashSet<>();
            var nodes = cur.btree().nodes();
            for (String nodeId : BTreeRenderer.changedNodes(cur.btree(), next.btree())) {
                for (int i = 0; i < nodes.size(); i++) {
                    if (nodes.get(i).nodeId().equals(nodeId)) {
                        out.add(i);
                        break;
                    }
                }
            }
            return out;
        }
        if (cur.hash() != null) {
            Set<Integer> out = new LinkedHashSet<>();
            var nodes = cur.hash().nodes();
            for (String nodeId : HashRenderer.changedArrows(cur.hash(), next.hash())) {
                for (int i = 0; i < nodes.size(); i++) {
                    if (nodes.get(i).nodeId().equals(nodeId)) {
                        out.add(i);
                        break;
                    }
                }
            }
            return out;
        }
        if (cur.graph() != null) {
            Set<Integer> out = new LinkedHashSet<>();
            var vertices = cur.graph().vertices();
            for (String nodeId : DiffHighlighter.changedGraphNodes(cur.graph(), next.graph())) {
                for (int i = 0; i < vertices.size(); i++) {
                    if (vertices.get(i).nodeId().equals(nodeId)) {
                        out.add(i);
                        break;
                    }
                }
            }
            return out;
        }
        if (cur.seq() != null) {
            Set<Integer> out = new LinkedHashSet<>();
            var participants = cur.seq().participants();
            for (String pid : DiffHighlighter.changedSeqParticipants(cur.seq(), next.seq())) {
                for (int i = 0; i < participants.size(); i++) {
                    if (participants.get(i).id().equals(pid)) {
                        out.add(i);
                        break;
                    }
                }
            }
            return out;
        }
        if (cur.bitmap() != null) {
            Set<Integer> out = new LinkedHashSet<>();
            var nb = next.bitmap();
            for (int b : nb.ones()) {
                if (!cur.bitmap().ones().contains(b)) {
                    out.add(b);
                }
            }
            if (nb.dupBit() != null) {
                out.add(nb.dupBit());
            }
            return out;
        }
        if (cur.math() != null) {
            Set<Integer> out = new LinkedHashSet<>();
            for (String key : DiffHighlighter.changedMatrixCells(cur.math(), next.math())) {
                if (key.startsWith("C:")) {
                    out.add(Integer.parseInt(key.substring(2)));
                }
            }
            return out;
        }
        return Set.of();
    }

    private static String fmtIdx(Collection<Integer> idxs, Step refStep) {
        boolean listMode = refStep != null && refStep.list() != null;
        boolean treeMode = refStep != null && refStep.tree() != null;
        boolean graphMode = refStep != null && refStep.graph() != null;
        boolean hashMode = refStep != null && refStep.hash() != null;
        boolean btreeMode = refStep != null && refStep.btree() != null;
        boolean seqMode = refStep != null && refStep.seq() != null;
        boolean mathMode = refStep != null && refStep.math() != null && refStep.bitmap() == null;
        boolean bitmapMode = refStep != null && refStep.bitmap() != null;
        return idxs.stream().sorted()
                .map(i -> {
                    if (bitmapMode) {
                        return "bit[" + i + "]";
                    }
                    if (mathMode) {
                        var grids = refStep.math().grids();
                        MathView.Grid c = grids.stream().filter(g -> g.id().equals("C")).findFirst().orElse(null);
                        if (c != null && i >= 0 && i < c.values().size()) {
                            return "C[" + (i / c.cols()) + "][" + (i % c.cols()) + "]";
                        }
                        return "?" + i;
                    }
                    if (seqMode) {
                        var participants = refStep.seq().participants();
                        return i >= 0 && i < participants.size() ? participants.get(i).label() : "?" + i;
                    }
                    if (graphMode) {
                        var vertices = refStep.graph().vertices();
                        return i >= 0 && i < vertices.size() ? vertices.get(i).label() : "?" + i;
                    }
                    if (hashMode) {
                        var nodes = refStep.hash().nodes();
                        return i >= 0 && i < nodes.size() ? "node[" + nodes.get(i).val() + "]" : "?" + i;
                    }
                    if (btreeMode) {
                        var nodes = refStep.btree().nodes();
                        return i >= 0 && i < nodes.size() ? "节点" + nodes.get(i).keys() : "?" + i;
                    }
                    if (treeMode) {
                        var nodes = refStep.tree().nodes();
                        return i >= 0 && i < nodes.size() ? nodes.get(i).nodeId() : "?" + i;
                    }
                    return listMode ? "@" + (i + 1) + ".next" : "a[" + i + "]";
                })
                .collect(Collectors.joining(", "));
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
