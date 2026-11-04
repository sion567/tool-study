package com.algoviz.ui;

import com.algoviz.engine.Step;
import com.algoviz.engine.math.PlotView;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import java.util.List;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;

/**
 * 渲染层之一：数学函数图像现场（math.plot 图层，按 kind 分形态）。
 * unitCircle：左半单位圆 + 动点，右半正在生长的 sin/cos 波。
 * newton：全幅坐标图——曲线 + 当前切线 + 历代近似根收敛到零点。
 * cordic：全幅——单位圆上的向量逐次旋转逼近目标角射线。
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
        switch (pv.kind() == null ? PlotView.UNIT_CIRCLE : pv.kind()) {
            case PlotView.NEWTON -> drawNewton(g, w, h, pv);
            case PlotView.CORDIC -> drawCordic(g, w, h, pv);
            default -> drawUnitCircle(g, w, h, pv);
        }
    }

    // ==================== kind = unitCircle（原单位圆→正弦波）====================

    private static void drawUnitCircle(GraphicsContext g, double w, double h, PlotView pv) {
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

    // ==================== kind = newton（牛顿迭代）====================

    private static void drawNewton(GraphicsContext g, double w, double h, PlotView pv) {
        // 绘图区（留边给轴标签）
        double x0 = w * 0.08, x1 = w * 0.95;
        double y0 = h * 0.12, y1 = h * 0.90;
        double dx = pv.xMax() - pv.xMin();
        double dy = pv.yMax() - pv.yMin();

        java.util.function.DoubleUnaryOperator sx =
                x -> x0 + (x - pv.xMin()) / dx * (x1 - x0);
        java.util.function.DoubleUnaryOperator sy =
                y -> y1 - (y - pv.yMin()) / dy * (y1 - y0);

        // 坐标轴：y=0 与 x=0（若在域内）
        g.setStroke(Color.web("#9ca3af"));
        g.setLineWidth(1);
        if (pv.yMin() <= 0 && pv.yMax() >= 0) {
            g.strokeLine(x0, sy.applyAsDouble(0), x1, sy.applyAsDouble(0));
        }
        if (pv.xMin() <= 0 && pv.xMax() >= 0) {
            g.strokeLine(sx.applyAsDouble(0), y0, sx.applyAsDouble(0), y1);
        }
        // y 轴刻度
        g.setFill(Color.web("#9ca3af"));
        g.setFont(Font.font(10));
        g.setTextAlign(TextAlignment.RIGHT);
        g.fillText(fmt(pv.yMin()), x0 - 6, y1 + 4);
        g.fillText(fmt(pv.yMax()), x0 - 6, y0 + 4);
        if (pv.yMin() <= 0 && pv.yMax() >= 0) {
            g.fillText("0", x0 - 6, sy.applyAsDouble(0) + 4);
        }
        g.setTextAlign(TextAlignment.CENTER);
        g.fillText(fmt(pv.xMin()), x0, y1 + 16);
        g.fillText(fmt(pv.xMax()), x1, y1 + 16);

        // 曲线 y=f(x)（蓝）
        if (pv.sineTrace().size() >= 2) {
            g.setStroke(Color.web("#3b82f6"));
            g.setLineWidth(2.5);
            var pts = pv.sineTrace();
            for (int i = 1; i < pts.size(); i++) {
                g.strokeLine(sx.applyAsDouble(pts.get(i - 1).get(0)), sy.applyAsDouble(pts.get(i - 1).get(1)),
                        sx.applyAsDouble(pts.get(i).get(0)), sy.applyAsDouble(pts.get(i).get(1)));
            }
        }

        // 零点参考线（目标根 √2 附近竖虚线）
        g.setStroke(Color.web("#d1d5db"));
        g.setLineDashes(4, 4);
        g.setLineWidth(1);
        double root = pv.trace3().isEmpty() ? Double.NaN : pv.trace3().get(pv.trace3().size() - 1).get(0);
        if (!Double.isNaN(root)) {
            g.strokeLine(sx.applyAsDouble(root), y0, sx.applyAsDouble(root), y1);
        }
        g.setLineDashes();

        // 历代近似根（灰点，画在 x 轴上）
        g.setFill(Color.web("#9ca3af"));
        for (List<Double> p : pv.trace3()) {
            double mx = sx.applyAsDouble(p.get(0));
            double my = sy.applyAsDouble(0);
            g.fillOval(mx - 4, my - 4, 8, 8);
        }

        // 当前切线段（橙）
        if (pv.cosineTrace().size() >= 2) {
            g.setStroke(Color.web("#f59e0b"));
            g.setLineWidth(2);
            var t = pv.cosineTrace();
            for (int i = 1; i < t.size(); i++) {
                g.strokeLine(sx.applyAsDouble(t.get(i - 1).get(0)), sy.applyAsDouble(t.get(i - 1).get(1)),
                        sx.applyAsDouble(t.get(i).get(0)), sy.applyAsDouble(t.get(i).get(1)));
            }
        }

        // 当前切点（红）
        double cx = sx.applyAsDouble(pv.pointX());
        double cy = sy.applyAsDouble(pv.pointY());
        g.setFill(Color.web("#ea580c"));
        g.fillOval(cx - 6, cy - 6, 12, 12);
        // 切点 → x 轴投影线
        g.setStroke(Color.web("#ef4444"));
        g.setLineWidth(1.2);
        g.strokeLine(cx, cy, cx, sy.applyAsDouble(0));

        // 注解（左上角逐行）
        drawLabels(g, pv, x0 + 4, y0 - 4);
    }

    // ==================== kind = cordic（向量旋转逼近）====================

    private static void drawCordic(GraphicsContext g, double w, double h, PlotView pv) {
        double cx = w * 0.42, cy = h * 0.52;
        // 向量长度会随伪旋转增长（最长 ~1.65），半径按 1.7 缩放
        double r = Math.min(w * 0.36, h * 0.42) / 1.7;

        // 单位圆（参考）
        g.setStroke(Color.web("#6b7280"));
        g.setLineWidth(1.5);
        g.strokeOval(cx - r, cy - r, r * 2, r * 2);
        // 十字轴
        g.setStroke(Color.web("#e5e7eb"));
        g.setLineWidth(1);
        g.strokeLine(cx - r - 25, cy, cx + r + 25, cy);
        g.strokeLine(cx, cy - r - 25, cx, cy + r + 25);

        // 目标角射线（绿虚线）
        double tx = cx + Math.cos(pv.angle()) * r * 1.35;
        double ty = cy - Math.sin(pv.angle()) * r * 1.35;
        g.setStroke(Color.web("#22c55e"));
        g.setLineWidth(2);
        g.setLineDashes(6, 4);
        g.strokeLine(cx, cy, tx, ty);
        g.setLineDashes();
        g.setFill(Color.web("#16a34a"));
        g.setFont(Font.font(11));
        g.fillText(String.format("目标 %.2f°", Math.toDegrees(pv.angle())), tx + 8, ty - 6);

        // 历代向量（灰，渐弱）
        for (int i = 0; i < pv.trace3().size(); i++) {
            List<Double> p = pv.trace3().get(i);
            double alpha = 0.15 + 0.5 * (i + 1.0) / pv.trace3().size();
            g.setStroke(Color.web("#9ca3af", alpha));
            g.setLineWidth(1.2);
            g.strokeLine(cx, cy, cx + p.get(0) * r, cy - p.get(1) * r);
        }

        // 当前向量（橙加粗）
        double px = cx + pv.pointX() * r;
        double py = cy - pv.pointY() * r;
        g.setStroke(Color.web("#f59e0b"));
        g.setLineWidth(3);
        g.strokeLine(cx, cy, px, py);

        // 当前端点（红点）
        g.setFill(Color.web("#ea580c"));
        g.fillOval(px - 6, py - 6, 12, 12);

        drawLabels(g, pv, w * 0.80, h * 0.16);
    }

    // ==================== 公共 ====================

    private static void drawLabels(GraphicsContext g, PlotView pv, double lx, double ly) {
        g.setTextAlign(TextAlignment.LEFT);
        g.setFont(Font.font(12));
        for (int i = 0; i < pv.labels().size(); i++) {
            g.setFill(i == 0 ? Color.web("#374151") : Color.web("#6b7280"));
            g.fillText(pv.labels().get(i), lx, ly + i * 18);
        }
    }

    private static String fmt(double v) {
        return Math.abs(v) >= 10 ? String.valueOf((int) v) : String.format("%.1f", v);
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
