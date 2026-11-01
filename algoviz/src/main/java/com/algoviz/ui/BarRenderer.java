package com.algoviz.ui;

import com.algoviz.engine.Step;
import com.algoviz.engine.ds.ArrayView;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;

import java.util.Set;

/**
 * 渲染层之一（蓝图 §6.2）：订阅 ds.array 图层画柱状图。
 * 颜色语义：橙 = 比较，红 = 交换/移动，绿 = 已就位（头区或尾区），
 * 黄圈 = 与上帧相比改值（视图 C diff），紫虚线 = 预测模式选中。
 */
public final class BarRenderer {

    private static final double PAD = 40;
    private static final double GAP = 8;

    private static final Color BASE = Color.web("#3b82f6");
    private static final Color COMPARE = Color.web("#f59e0b");
    private static final Color SWAP = Color.web("#ef4444");
    private static final Color SORTED = Color.web("#22c55e");
    private static final Color CHANGED = Color.web("#eab308");
    private static final Color PICKED = Color.web("#8b5cf6");

    private BarRenderer() {
    }

    public static void draw(Canvas canvas, Step step, Set<Integer> picked, Set<Integer> changed) {
        GraphicsContext g = canvas.getGraphicsContext2D();
        g.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
        if (step == null || step.array() == null) {
            return;
        }
        ArrayView av = step.array();
        int n = av.values().size();
        if (n == 0) {
            return;
        }
        double w = canvas.getWidth();
        double h = canvas.getHeight();
        double bw = barWidth(w, n);
        int max = av.values().stream().mapToInt(Integer::intValue).max().orElse(1);

        g.setTextAlign(TextAlignment.CENTER);
        for (int i = 0; i < n; i++) {
            int v = av.values().get(i);
            double bh = (h - 90) * v / (double) max;
            double x = PAD + i * (bw + GAP);
            double y = h - 50 - bh;
            g.setFill(colorOf(av, i));
            g.fillRoundRect(x, y, bw, bh, 4, 4);
            if (changed != null && changed.contains(i)) {
                g.setStroke(CHANGED);
                g.setLineWidth(2.5);
                g.strokeRoundRect(x - 2, y - 2, bw + 4, bh + 4, 6, 6);
            }
            if (picked != null && picked.contains(i)) {
                g.setStroke(PICKED);
                g.setLineWidth(2.5);
                g.setLineDashes(6, 4);
                g.strokeRoundRect(x - 4, y - 4, bw + 8, bh + 8, 8, 8);
                g.setLineDashes();
            }
            g.setFill(Color.GRAY);
            g.setFont(Font.font(11));
            g.fillText(String.valueOf(v), x + bw / 2, y - 6);
            g.fillText("[" + i + "]", x + bw / 2, h - 30);
        }
    }

    /** 供预测模式点击命中：返回柱子下标，未命中 -1。几何与 draw() 完全一致。 */
    public static int hitIndex(double w, double h, int n, double x, double y) {
        if (n <= 0) {
            return -1;
        }
        double bw = barWidth(w, n);
        if (y < 5 || y > h - 40) {
            return -1;
        }
        for (int i = 0; i < n; i++) {
            double x0 = PAD + i * (bw + GAP);
            if (x >= x0 - 4 && x <= x0 + bw + 4) {
                return i;
            }
        }
        return -1;
    }

    private static double barWidth(double w, int n) {
        return Math.max(6, (w - PAD * 2 - GAP * (n - 1)) / n);
    }

    private static Color colorOf(ArrayView av, int i) {
        if (av.swapA() != null && (i == av.swapA() || i == av.swapB())) {
            return SWAP;
        }
        if (av.compareA() != null && (i == av.compareA() || i == av.compareB())) {
            return COMPARE;
        }
        if (i >= av.sortedFrom() || (av.sortedTo() >= 0 && i <= av.sortedTo())) {
            return SORTED;
        }
        return BASE;
    }
}
