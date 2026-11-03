package com.algoviz.ui;

import com.algoviz.engine.Step;
import com.algoviz.engine.math.PlotView;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;

/**
 * 渲染层之一：数学函数图像现场（math.plot 图层）。
 * 左半：单位圆 + 动点（橙）+ 半径线；右半：正在生长的 sin/cos 波。
 * 橙点 = 当前角度在圆上的位置，同步映射到右侧波的末端。
 */
public final class PlotRenderer {

    private PlotRenderer() {
    }

    public static void draw(Canvas canvas, Step step) {
        GraphicsContext g = canvas.getGraphicsContext2D();
        g.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
        if (step == null || step.plot() == null) return;
        PlotView pv = step.plot();
        double w = canvas.getWidth(), h = canvas.getHeight();
        g.setTextAlign(TextAlignment.CENTER);

        // ===== 左半：单位圆 =====
        double circleCx = w * 0.22;
        double circleCy = h * 0.45;
        double circleR = Math.min(w * 0.15, h * 0.30);

        // 圆网格
        g.setStroke(Color.web("#e5e7eb"));
        g.setLineWidth(1);
        g.strokeLine(circleCx - circleR - 20, circleCy, circleCx + circleR + 20, circleCy);
        g.strokeLine(circleCx, circleCy - circleR - 20, circleCx, circleCy + circleR + 20);

        // 单位圆
        g.setStroke(Color.web("#6b7280"));
        g.setLineWidth(1.5);
        g.strokeOval(circleCx - circleR, circleCy - circleR, circleR * 2, circleR * 2);
        g.setFill(Color.web("#6b7280"));
        g.setFont(Font.font(10));
        g.fillText("(1,0)", circleCx + circleR + 6, circleCy + 4);
        g.fillText("(0,1)", circleCx - 4, circleCy - circleR - 8);
        g.fillText("(0,0)", circleCx - 8, circleCy + 14);

        // 动点在圆上
        double px = circleCx + pv.pointX() * circleR;
        double py = circleCy - pv.pointY() * circleR;   // y 翻转（屏幕 y 向下）
        // 半径线（从圆心到动点）
        g.setStroke(Color.web("#f59e0b"));
        g.setLineWidth(2);
        g.strokeLine(circleCx, circleCy, px, py);
        // y 投影线（动点 → x 轴，表示 sin 值）
        g.setStroke(Color.web("#3b82f6"));
        g.setLineWidth(2);
        g.strokeLine(px, py, px, circleCy);
        // x 投影线（动点 → y 轴，表示 cos 值）
        g.setStroke(Color.web("#22c55e"));
        g.setLineWidth(1.5);
        g.strokeLine(circleCx, circleCy, px, circleCy);

        // 动点
        g.setFill(Color.web("#ea580c"));
        g.fillOval(px - 6, py - 6, 12, 12);

        // 标签
        g.setFill(Color.web("#374151"));
        g.setFont(Font.font(11));
        g.fillText(String.format("sin(θ)=%.3f", pv.pointY()), px + 30, py - 4);
        g.setFill(Color.web("#16a34a"));
        g.fillText(String.format("cos(θ)=%.3f", pv.pointX()), circleCx + circleR / 2, circleCy + 18);

        // ===== 右半：正弦波 =====
        double waveX0 = w * 0.45;
        double waveX1 = w * 0.95;
        double waveMidY = h * 0.45;
        double waveAmp = h * 0.25;

        // 网格
        g.setStroke(Color.web("#e5e7eb"));
        g.setLineWidth(1);
        g.strokeLine(waveX0, waveMidY, waveX1, waveMidY);   // x 轴
        g.setStroke(Color.web("#d1d5db"));
        g.strokeLine(waveX0, waveMidY - waveAmp, waveX1, waveMidY - waveAmp);
        g.strokeLine(waveX0, waveMidY + waveAmp, waveX1, waveMidY + waveAmp);
        g.setFill(Color.web("#9ca3af"));
        g.setFont(Font.font(10));
        g.fillText("1", waveX0 - 10, waveMidY - waveAmp + 4);
        g.fillText("-1", waveX0 - 10, waveMidY + waveAmp + 4);
        g.fillText("sin", waveX1 + 6, waveMidY - waveAmp + 4);
        g.fillText("cos", waveX1 + 6, waveMidY + 4);

        // 画 sin 波（蓝色）
        if (!pv.sineTrace().isEmpty()) {
            g.setStroke(Color.web("#3b82f6"));
            g.setLineWidth(2.5);
            drawTrace(g, pv.sineTrace(), waveX0, waveX1, waveMidY, waveAmp, false);
        }

        // 画 cos 波（绿色虚线）
        if (!pv.cosineTrace().isEmpty()) {
            g.setStroke(Color.web("#22c55e"));
            g.setLineWidth(1.5);
            g.setLineDashes(5, 4);
            drawTrace(g, pv.cosineTrace(), waveX0, waveX1, waveMidY, waveAmp, false);
            g.setLineDashes();
        }

        // 波末端动点
        if (!pv.sineTrace().isEmpty()) {
            var lastPt = pv.sineTrace().get(pv.sineTrace().size() - 1);
            double lx = waveX0 + lastPt.get(0) / (2 * Math.PI) * (waveX1 - waveX0);
            double ly = waveMidY - lastPt.get(1) * waveAmp;
            g.setFill(Color.web("#ea580c"));
            g.fillOval(lx - 5, ly - 5, 10, 10);
        }
    }

    private static void drawTrace(GraphicsContext g, List<List<Double>> trace,
                                  double wx0, double wx1, double midY, double amp, boolean dummy) {
        if (trace.size() < 2) return;
        double maxAngle = 2 * Math.PI;
        var it = trace.iterator();
        var prev = it.next();
        while (it.hasNext()) {
            var curr = it.next();
            double px1 = wx0 + prev.get(0) / maxAngle * (wx1 - wx0);
            double py1 = midY - prev.get(1) * amp;
            double px2 = wx0 + curr.get(0) / maxAngle * (wx1 - wx0);
            double py2 = midY - curr.get(1) * amp;
            g.strokeLine(px1, py1, px2, py2);
            prev = curr;
        }
    }
}
