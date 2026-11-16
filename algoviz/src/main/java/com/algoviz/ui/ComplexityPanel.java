package com.algoviz.ui;

import javafx.scene.paint.Color;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;

import java.util.function.DoubleUnaryOperator;

/**
 * M7 复杂度可视化：增长曲线 + 体感表。
 * 蓝图 §10 的第一版：在画布上叠画 O(1)~O(n²) 的增长曲线，n 可切换。
 */
public final class ComplexityPanel {

    /** 曲线族 id 与 ComplexityCurve.classify 的返回值一一对应。 */
    private static final String[] IDS = {"constant", "log", "linear", "nlogn", "nsq", "exp", "fact"};
    private static final String[] NAMES = {"O(1)", "O(log n)", "O(n)", "O(n log n)", "O(n²)", "O(2ⁿ)", "O(n!)"};
    private static final Color[] COLORS = {
            Palette.GREEN, Palette.BLUE, Palette.VIOLET,
            Palette.AMBER, Palette.RED, Palette.PINK, Palette.RED_DEEP};
    private static final DoubleUnaryOperator[] OPS = {
            n -> 1,
            n -> Math.log(n) / Math.log(2),
            n -> n,
            n -> n * Math.log(n) / Math.log(2),
            n -> n * n,
            n -> Math.pow(2, Math.min(n, 60)),       // 封顶防溢出：n=50 已出画布
            n -> fact((int) Math.min(n, 25))};             // 同上，n! 增长更凶

    private static double fact(int n) {
        double f = 1;
        for (int i = 2; i <= n; i++) {
            f *= i;
        }
        return f;
    }

    private ComplexityPanel() {
    }

    /** 在画布上绘制增长曲线（n 上限 = maxN）——无高亮的旧签名。 */
    public static void draw(Canvas canvas, int maxN) {
        draw(canvas, maxN, java.util.List.of(), -1, null);
    }

    /**
     * 按演示高亮：曲线家族灰线打底（40% 透明），highlightIds 中的曲线加粗 + 图例挂 ⭐；
     * markerN（演示实际 inputSize，>0 且 ≤maxN）画竖虚线 + 各高亮曲线交点圆点 + 顶部 n 标注；
     * tag（Complexity.time() 原串）画在图例下。exp/fact 冲出画布顶部即截断——"指数爆炸出画"本身是教学点。
     */
    public static void draw(Canvas canvas, int maxN, java.util.List<String> highlightIds,
                            int markerN, String tag) {
        GraphicsContext g = canvas.getGraphicsContext2D();
        g.setGlobalAlpha(1.0);
        g.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
        double w = canvas.getWidth(), h = canvas.getHeight();
        double margin = 50;
        double x0 = margin, y0 = h - margin;
        double plotW = w - margin * 2, plotH = h - margin * 2;
        g.setTextAlign(TextAlignment.CENTER);

        // 坐标轴
        g.setStroke(Palette.AXIS);
        g.setLineWidth(1.5);
        g.strokeLine(x0, y0, x0 + plotW, y0);
        g.strokeLine(x0, y0, x0, y0 - plotH);
        g.setFill(Palette.MUTED);
        g.setFont(Font.font(11));
        g.fillText("n（数据量）", x0 + plotW / 2, y0 + 24);
        g.save();
        g.translate(x0 - 26, y0 - plotH / 2);
        g.rotate(-90);
        g.fillText("步数", 0, 0);
        g.restore();

        // x 轴刻度
        for (int nTick : new int[]{10, 20, 30, 40, 50}) {
            double x = x0 + (double) nTick / maxN * plotW;
            g.setStroke(Palette.GRID);
            g.strokeLine(x, y0, x, y0 - 4);
            g.setFill(Palette.FAINT);
            g.fillText(String.valueOf(nTick), x, y0 + 14);
        }

        // 归一化锚定 O(n²) 在 maxN 时的值；exp/fact 超过即冲出顶部（截断 = 指数爆炸的教学点）
        double yMax = OPS[4].applyAsDouble(maxN);

        for (int i = 0; i < OPS.length; i++) {
            boolean hl = highlightIds.contains(IDS[i]);
            g.setGlobalAlpha(hl ? 1.0 : 0.35);
            g.setStroke(COLORS[i]);
            g.setLineWidth(hl ? 3.2 : 1.6);
            double prevX = 0, prevY = 0;
            double exitX = -1;
            for (int px = 0; px <= 200; px++) {
                double n = 1.0 + px / 200.0 * (maxN - 1);
                double steps = OPS[i].applyAsDouble(n);
                double cx = x0 + (n - 1) / (maxN - 1) * plotW;
                if (steps > yMax) {                      // 冲出顶部：画到出画处即停
                    exitX = cx;
                    break;
                }
                double cy = y0 - steps / yMax * plotH;
                if (px > 0) {
                    g.strokeLine(prevX, prevY, cx, cy);
                }
                prevX = cx;
                prevY = cy;
            }
            // 曲线标签：普通曲线在末端，出画曲线在出画点上方
            double lx, ly;
            if (exitX > 0) {
                lx = Math.min(exitX, x0 + plotW - 20);
                ly = 20;
            } else {
                double endSteps = OPS[i].applyAsDouble(maxN);
                lx = x0 + plotW;
                ly = Math.max(14, y0 - Math.min(1.0, endSteps / yMax) * plotH - 6);
            }
            g.setFill(COLORS[i]);
            g.setFont(Font.font(hl ? 13 : 12));
            g.fillText((hl ? "⭐ " : "") + NAMES[i], Math.max(lx - 30, 24), ly - (exitX > 0 ? 4 : 0));
        }
        g.setGlobalAlpha(1.0);

        // 实际 n 标记：竖虚线 + 高亮曲线交点圆点 + 顶部标注
        if (markerN > 0 && markerN <= maxN) {
            double mx = x0 + (double) (markerN - 1) / (maxN - 1) * plotW;
            g.setStroke(Palette.SKY);
            g.setLineWidth(1.2);
            g.setLineDashes(4, 4);
            g.strokeLine(mx, y0, mx, y0 - plotH);
            g.setLineDashes();
            for (int i = 0; i < OPS.length; i++) {
                if (!highlightIds.contains(IDS[i])) {
                    continue;
                }
                double steps = OPS[i].applyAsDouble(markerN);
                if (steps > yMax) {
                    continue;
                }
                double cy = y0 - steps / yMax * plotH;
                g.setFill(COLORS[i]);
                g.fillOval(mx - 4, cy - 4, 8, 8);
            }
            g.setFill(Palette.SKY);
            g.setFont(Font.font(12));
            g.fillText("本演示 n = " + markerN, mx, y0 + 38);
        }

        // 图例下的原串（复杂度文本）
        if (tag != null && !tag.isBlank()) {
            g.setFill(Palette.FAINT);
            g.setFont(Font.font(11));
            g.fillText("⭐ 本演示 = " + tag, x0 + plotW / 2, 36);
        }
    }

    /** 体感表文本（n = 100 万时）。 */
    public static String feelTable() {
        return """
                数据量 n = 1,000,000（100 万）时的步数与体感：

                O(1)        直接拿，不找        1 步          秒出
                O(log n)    每次砍一半          ~20 步        极快
                O(n)        一个个看            100 万步      能感觉到
                O(n log n)  每个元素砍半排序    ~2000 万步    排序够用
                O(n²)       两两比              1 万亿步      慢，数据大了崩
                """;
    }

    // ==================== ⚖ 对比模式：六种排序的实测步数曲线 ====================

    /** 参赛选手：六个真实 trace 的步数（不是公式，是跑出来的）。 */
    private static final String[] SORT_NAMES = {"冒泡", "选择", "插入", "快排", "归并", "堆排"};
    private static final Color[] SORT_COLORS = {
            Palette.RED, Palette.AMBER, Palette.VIOLET,
            Palette.GREEN, Palette.BLUE, Palette.TEAL};

    /**
     * 实测：每个 n 用同一批（固定种子）随机数据跑六种排序，纵轴 = 步数。
     * 冒泡 vs 快排的差距在 n=50 时就已经一目了然——这就是"同一问题、不同代价"的现场证据。
     */
    public static void drawEmpirical(Canvas canvas, int maxN) {
        GraphicsContext g = canvas.getGraphicsContext2D();
        g.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
        double w = canvas.getWidth(), h = canvas.getHeight();
        double margin = 60;
        double x0 = margin, y0 = h - margin;
        double plotW = w - margin * 2, plotH = h - margin * 2;
        g.setTextAlign(TextAlignment.CENTER);

        // 实测：n = 5..maxN 步长 5
        java.util.List<Integer> ns = new java.util.ArrayList<>();
        for (int n = 5; n <= maxN; n += 5) ns.add(n);
        java.util.List<long[]> series = new java.util.ArrayList<>();   // 6 组 × ns.size() 步数
        for (int s = 0; s < SORT_NAMES.length; s++) series.add(new long[ns.size()]);
        java.util.Random r = new java.util.Random(42);   // 固定种子：同一批数据六个算法共用
        for (int i = 0; i < ns.size(); i++) {
            int n = ns.get(i);
            int[] base = new int[n];
            for (int k = 0; k < n; k++) base[k] = 1 + r.nextInt(99);
            for (int s = 0; s < SORT_NAMES.length; s++) {
                series.get(s)[i] = runSort(s, base);
            }
        }
        long yMax = 1;
        for (long[] s1 : series) {
            for (long v : s1) yMax = Math.max(yMax, v);
        }

        // 坐标轴
        g.setStroke(Palette.AXIS);
        g.setLineWidth(1.5);
        g.strokeLine(x0, y0, x0 + plotW, y0);
        g.strokeLine(x0, y0, x0, y0 - plotH);
        g.setFill(Palette.MUTED);
        g.setFont(Font.font(11));
        g.fillText("n（数据量）", x0 + plotW / 2, y0 + 26);
        g.save();
        g.translate(x0 - 30, y0 - plotH / 2);
        g.rotate(-90);
        g.fillText("实测步数", 0, 0);
        g.restore();
        // y 轴刻度
        for (int t = 0; t <= 4; t++) {
            double v = yMax * t / 4.0;
            double y = y0 - (double) t / 4 * plotH;
            g.setStroke(Palette.GRID_SOFT);
            g.strokeLine(x0, y, x0 + plotW, y);
            g.setFill(Palette.FAINT);
            g.fillText(String.valueOf(Math.round(v)), x0 - 8, y + 4);
        }

        // 六条实测曲线
        for (int s = 0; s < SORT_NAMES.length; s++) {
            g.setStroke(SORT_COLORS[s]);
            g.setLineWidth(2.4);
            double prevX = 0, prevY = 0;
            for (int i = 0; i < ns.size(); i++) {
                double cx = x0 + (double) (ns.get(i) - 5) / (maxN - 5) * plotW;
                double cy = y0 - Math.min(1.0, (double) series.get(s)[i] / yMax) * plotH;
                if (i > 0) g.strokeLine(prevX, prevY, cx, cy);
                g.setFill(SORT_COLORS[s]);
                g.fillOval(cx - 2.5, cy - 2.5, 5, 5);
                prevX = cx;
                prevY = cy;
            }
        }

        // 图例（右上角）
        double lx = x0 + plotW - 130, ly = y0 - plotH + 14;
        g.setFont(Font.font(12));
        for (int s = 0; s < SORT_NAMES.length; s++) {
            g.setStroke(SORT_COLORS[s]);
            g.setLineWidth(3);
            int row = s / 3, col = s % 3;
            double gx = lx + col * 92, gy = ly + row * 18;
            g.strokeLine(gx, gy, gx + 20, gy);
            g.setFill(Palette.INK);
            g.setTextAlign(TextAlignment.LEFT);
            g.fillText(SORT_NAMES[s], gx + 25, gy + 4);
        }
        g.setTextAlign(TextAlignment.CENTER);
        g.setFill(Palette.FAINT);
        g.setFont(Font.font(10));
        g.fillText("同一批随机数据（种子 42）· 步数 = trace 长度 · 冒泡在 n=50 时步数 ≈ 快排 10 倍",
                x0 + plotW / 2, y0 + 44);
    }

    private static long runSort(int which, int[] base) {
        int[] a = base.clone();
        return switch (which) {
            case 0 -> new com.algoviz.engine.sort.BubbleSort(a).produce().steps().size();
            case 1 -> new com.algoviz.engine.sort.SelectionSort(a).produce().steps().size();
            case 2 -> new com.algoviz.engine.sort.InsertionSort(a).produce().steps().size();
            case 3 -> new com.algoviz.engine.sort.QuickSort(a).produce().steps().size();
            case 4 -> new com.algoviz.engine.sort.MergeSort(a).produce().steps().size();
            default -> new com.algoviz.engine.sort.HeapSort(a).produce().steps().size();
        };
    }
}
