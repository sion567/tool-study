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
}
