package com.algoviz.ui;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;

import java.util.function.DoubleUnaryOperator;

/**
 * M7 复杂度可视化：增长曲线 + 体感表。
 * 蓝图 §10 的第一版：在画布上叠画 O(1)~O(n²) 的增长曲线，n 可切换。
 */
public final class ComplexityPanel {

    private static final String[] NAMES = {"O(1)", "O(log n)", "O(n)", "O(n log n)", "O(n²)"};
    private static final Color[] COLORS = {
            Color.web("#22c55e"), Color.web("#3b82f6"), Color.web("#8b5cf6"),
            Color.web("#f59e0b"), Color.web("#ef4444")};
    private static final DoubleUnaryOperator[] OPS = {
            n -> 1,
            n -> Math.log(n) / Math.log(2),
            n -> n,
            n -> n * Math.log(n) / Math.log(2),
            n -> n * n};

    private ComplexityPanel() {
    }

    /** 在画布上绘制增长曲线（n 上限 = maxN）。 */
    public static void draw(Canvas canvas, int maxN) {
        GraphicsContext g = canvas.getGraphicsContext2D();
        g.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
        double w = canvas.getWidth(), h = canvas.getHeight();
        double margin = 50;
        double x0 = margin, y0 = h - margin;
        double plotW = w - margin * 2, plotH = h - margin * 2;
        g.setTextAlign(TextAlignment.CENTER);

        // 坐标轴
        g.setStroke(Color.web("#94a3b8"));
        g.setLineWidth(1.5);
        g.strokeLine(x0, y0, x0 + plotW, y0);
        g.strokeLine(x0, y0, x0, y0 - plotH);
        g.setFill(Color.web("#6b7280"));
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
            g.setStroke(Color.web("#d1d5db"));
            g.strokeLine(x, y0, x, y0 - 4);
            g.setFill(Color.web("#9ca3af"));
            g.fillText(String.valueOf(nTick), x, y0 + 14);
        }

        // 归一化：用 O(n²) 在 maxN 时的值作为最高点（其他曲线都被压扁但形态可见）
        double yMax = OPS[4].applyAsDouble(maxN);

        for (int i = 0; i < OPS.length; i++) {
            g.setStroke(COLORS[i]);
            g.setLineWidth(2.2);
            double prevX = 0, prevY = 0;
            for (int px = 0; px <= 200; px++) {
                double n = 1.0 + px / 200.0 * (maxN - 1);
                double steps = OPS[i].applyAsDouble(n);
                double cx = x0 + (n - 1) / (maxN - 1) * plotW;
                double cy = y0 - Math.min(1.0, steps / yMax) * plotH;
                if (px > 0) {
                    g.strokeLine(prevX, prevY, cx, cy);
                }
                prevX = cx;
                prevY = cy;
            }
            // 曲线标签（放在曲线末端附近）
            double endN = maxN;
            double endSteps = OPS[i].applyAsDouble(endN);
            double ex = x0 + plotW;
            double ey = y0 - Math.min(1.0, endSteps / yMax) * plotH;
            g.setFill(COLORS[i]);
            g.setFont(Font.font(12));
            g.fillText(NAMES[i], ex - 28, Math.max(14, ey - 6));
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
            Color.web("#ef4444"), Color.web("#f59e0b"), Color.web("#8b5cf6"),
            Color.web("#22c55e"), Color.web("#3b82f6"), Color.web("#0891b2")};

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
        g.setStroke(Color.web("#94a3b8"));
        g.setLineWidth(1.5);
        g.strokeLine(x0, y0, x0 + plotW, y0);
        g.strokeLine(x0, y0, x0, y0 - plotH);
        g.setFill(Color.web("#6b7280"));
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
            g.setStroke(Color.web("#e5e7eb"));
            g.strokeLine(x0, y, x0 + plotW, y);
            g.setFill(Color.web("#9ca3af"));
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
            g.setFill(Color.web("#374151"));
            g.setTextAlign(TextAlignment.LEFT);
            g.fillText(SORT_NAMES[s], gx + 25, gy + 4);
        }
        g.setTextAlign(TextAlignment.CENTER);
        g.setFill(Color.web("#9ca3af"));
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
