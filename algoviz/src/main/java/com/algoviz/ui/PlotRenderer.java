package com.algoviz.ui;

import com.algoviz.engine.Step;
import com.algoviz.engine.math.PlotView;
import javafx.scene.paint.Color;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
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
            case PlotView.RIEMANN -> drawRiemann(g, w, h, pv);
            case PlotView.CARTESIAN -> drawCartesian(g, w, h, pv);
            case PlotView.SCATTER -> drawScatter(g, w, h, pv);
            case PlotView.DOUBLE_PENDULUM -> drawDoublePendulum(g, w, h, pv);
            case PlotView.TAYLOR -> drawTaylor(g, w, h, pv);
            case PlotView.WIRE3D -> drawWireframe3d(g, w, h, pv);
            default -> drawUnitCircle(g, w, h, pv);
        }
    }

    // ==================== kind = wire3d（3D 线框：立方体绕轴旋转）====================

    /** 弱透视投影：scale = fov/(fov−z)，近大远小；深度感 = 近粗远细 + alpha。 */
    private static void drawWireframe3d(GraphicsContext g, double w, double h, PlotView pv) {
        double fov = pv.pointX() > 0 ? pv.pointX() : 4;       // 视距（世界单位）
        double s = pv.pointY() > 0 ? pv.pointY() : 90;        // 世界缩放（像素/世界单位）
        double cx = w / 2, cy = h / 2 + 8;
        // 投影：世界 (x,y,z) → 屏幕 = center + (x,y)·s·scale(z)；z 朝观察者为正，scale=fov/(fov−z) 近大远小。
        java.util.function.DoubleBinaryOperator pxF =
                (x, z) -> cx + x * s * (fov / (fov - z));
        java.util.function.DoubleBinaryOperator pyF =
                (y, z) -> cy - y * s * (fov / (fov - z));

        // 坐标轴：X 红 / Y 绿 / Z 蓝
        double[][] axes = {
                {1, 0, 0, -1, 0, 0}, {0, 1, 0, 0, -1, 0}, {0, 0, 1, 0, 0, -1}};
        javafx.scene.paint.Color[] axisCols = {
                Palette.RED, Palette.GREEN_DEEP, Palette.BLUE};
        String[] axisNames = {"X", "Y", "Z"};
        for (int i = 0; i < 3; i++) {
            double[] a = axes[i];
            g.setStroke(axisCols[i]);
            g.setLineWidth(2);
            g.strokeLine(pxF.applyAsDouble(a[0], a[2]), pyF.applyAsDouble(a[1], a[2]), pxF.applyAsDouble(a[3], a[5]), pyF.applyAsDouble(a[4], a[5]));
            g.setFill(axisCols[i]);
            g.setFont(Font.font(13));
            g.fillText(axisNames[i],
                    pxF.applyAsDouble(a[0], a[2]) + (i == 0 ? 14 : 0),
                    pyF.applyAsDouble(a[1], a[2]) - (i == 1 ? 8 : (i == 2 ? -6 : 0)));
        }

        // 边：近粗远细 + alpha 渐变（深度感）
        for (List<Double> e : pv.trace3()) {
            double zAvg = (e.get(2) + e.get(5)) / 2;
            double depth = Math.max(0, Math.min(1, (zAvg + 1.8) / 3.6));   // 0(远)..1(近)
            g.setStroke(Palette.AXIS);
            g.setGlobalAlpha(0.35 + 0.6 * depth);
            g.setLineWidth(1.2 + 1.8 * depth);
            g.strokeLine(pxF.applyAsDouble(e.get(0), e.get(2)), pyF.applyAsDouble(e.get(1), e.get(2)),
                    pxF.applyAsDouble(e.get(3), e.get(5)), pyF.applyAsDouble(e.get(4), e.get(5)));
        }
        g.setGlobalAlpha(1.0);

        // 顶点：空心圆点（深度调半径），高亮顶点琥珀实心
        for (List<Double> v : pv.rects()) {
            double pxx = pxF.applyAsDouble(v.get(0), v.get(2)), pyy = pyF.applyAsDouble(v.get(1), v.get(2));
            double r = 3 + 2 * Math.max(0, Math.min(1, (v.get(2) + 1.8) / 3.6));
            boolean hl = v.size() > 3 && v.get(3) > 0.5;
            if (hl) {
                g.setFill(Palette.AMBER);
                g.fillOval(pxx - r, pyy - r, 2 * r, 2 * r);
            } else {
                g.setStroke(Palette.INK);
                g.setLineWidth(1.4);
                g.strokeOval(pxx - r, pyy - r, 2 * r, 2 * r);
            }
        }

        // 左上角：旋转矩阵三行 + 状态行（⚠ = 万向锁警示 → 玫红强调）
        g.setTextAlign(TextAlignment.LEFT);
        g.setFont(Font.font(12));
        double ly = 26;
        for (int i = 0; i < pv.labels().size(); i++) {
            String text = pv.labels().get(i);
            boolean warn = text.startsWith("⚠");
            g.setFill(warn ? Palette.ROSE : (i == 0 ? Palette.INK : Palette.MUTED));
            g.setFont(Font.font(warn ? 13 : 12));
            g.fillText(text, 14, ly + i * 18);
        }
        g.setTextAlign(TextAlignment.CENTER);
    }

    // ==================== kind = unitCircle（原单位圆→正弦波）====================

    private static void drawUnitCircle(GraphicsContext g, double w, double h, PlotView pv) {
        // ===== 左半：单位圆 =====
        double circleCx = w * 0.22;
        double circleCy = h * 0.45;
        double circleR = Math.min(w * 0.15, h * 0.30);

        // 圆网格（背景网格开关 ▦）
        if (Theme.showGrid()) {
            g.setStroke(Palette.GRID_SOFT);
            g.setLineWidth(1);
            g.strokeLine(circleCx - circleR - 20, circleCy, circleCx + circleR + 20, circleCy);
            g.strokeLine(circleCx, circleCy - circleR - 20, circleCx, circleCy + circleR + 20);
        }

        // 单位圆
        g.setStroke(Palette.MUTED);
        g.setLineWidth(1.5);
        g.strokeOval(circleCx - circleR, circleCy - circleR, circleR * 2, circleR * 2);
        g.setFill(Palette.MUTED);
        g.setFont(Font.font(10));
        g.fillText("(1,0)", circleCx + circleR + 6, circleCy + 4);
        g.fillText("(0,1)", circleCx - 4, circleCy - circleR - 8);
        g.fillText("(0,0)", circleCx - 8, circleCy + 14);

        // 动点在圆上
        double px = circleCx + pv.pointX() * circleR;
        double py = circleCy - pv.pointY() * circleR;   // y 翻转（屏幕 y 向下）
        // 半径线（从圆心到动点）
        g.setStroke(Palette.AMBER);
        g.setLineWidth(2);
        g.strokeLine(circleCx, circleCy, px, py);
        // y 投影线（动点 → x 轴，表示 sin 值）
        g.setStroke(Palette.BLUE);
        g.setLineWidth(2);
        g.strokeLine(px, py, px, circleCy);
        // x 投影线（动点 → y 轴，表示 cos 值）
        g.setStroke(Palette.GREEN);
        g.setLineWidth(1.5);
        g.strokeLine(circleCx, circleCy, px, circleCy);

        // 动点
        g.setFill(Palette.ORANGE);
        g.fillOval(px - 6, py - 6, 12, 12);

        // 标签
        g.setFill(Palette.INK);
        g.setFont(Font.font(11));
        g.fillText(String.format("sin(θ)=%.3f", pv.pointY()), px + 30, py - 4);
        g.setFill(Palette.GREEN_DEEP);
        g.fillText(String.format("cos(θ)=%.3f", pv.pointX()), circleCx + circleR / 2, circleCy + 18);

        // ===== 右半：正弦波 =====
        double waveX0 = w * 0.45;
        double waveX1 = w * 0.95;
        double waveMidY = h * 0.45;
        double waveAmp = h * 0.25;

        // 网格 + 方格背景（背景网格开关 ▦）
        if (Theme.showGrid()) {
            g.setStroke(Palette.GRID_SOFT);
            g.setLineWidth(1);
            g.strokeLine(waveX0, waveMidY, waveX1, waveMidY);   // x 轴
            g.setStroke(Palette.GRID);
            g.strokeLine(waveX0, waveMidY - waveAmp, waveX1, waveMidY - waveAmp);
            g.strokeLine(waveX0, waveMidY + waveAmp, waveX1, waveMidY + waveAmp);
            // 方格背景（竖刻度）——与黄金螺旋同款读感，缩放时有参照
            g.setStroke(Palette.CARD);
            g.setLineWidth(1);
            int vLines = 8;
            for (int i = 1; i < vLines; i++) {
                double vx = waveX0 + (waveX1 - waveX0) * i / vLines;
                g.strokeLine(vx, waveMidY - waveAmp, vx, waveMidY + waveAmp);
            }
        }
        g.setFill(Palette.FAINT);
        g.setFont(Font.font(10));
        g.fillText("1", waveX0 - 10, waveMidY - waveAmp + 4);
        g.fillText("-1", waveX0 - 10, waveMidY + waveAmp + 4);
        g.fillText("sin", waveX1 + 6, waveMidY - waveAmp + 4);
        g.fillText("cos", waveX1 + 6, waveMidY + 4);

        // y 域：取图层声明的 yMin/yMax（泰勒 ±1.4；单位圆 ±1）——超出部分钳在边缘，不让线飞出画面
        double visible = Math.max(Math.abs(pv.yMin()), Math.abs(pv.yMax()));
        if (visible <= 0) {
            visible = 1;
        }
        double xSpan = pv.xMax() - pv.xMin();
        if (xSpan <= 0) {
            xSpan = 2 * Math.PI;
        }

        // 画 sin 波（蓝色）
        if (!pv.sineTrace().isEmpty()) {
            g.setStroke(Palette.BLUE);
            g.setLineWidth(2.5);
            drawTrace(g, pv.sineTrace(), waveX0, waveX1, waveMidY, waveAmp,
                    pv.xMin(), xSpan, pv.yMin(), pv.yMax(), visible);
        }

        // 画 cos 波（绿色虚线）
        if (!pv.cosineTrace().isEmpty()) {
            g.setStroke(Palette.GREEN);
            g.setLineWidth(1.5);
            g.setLineDashes(5, 4);
            drawTrace(g, pv.cosineTrace(), waveX0, waveX1, waveMidY, waveAmp,
                    pv.xMin(), xSpan, pv.yMin(), pv.yMax(), visible);
            g.setLineDashes();
        }

        // 波末端动点
        if (!pv.sineTrace().isEmpty()) {
            var lastPt = pv.sineTrace().get(pv.sineTrace().size() - 1);
            double lx = waveX0 + (lastPt.get(0) - pv.xMin()) / xSpan * (waveX1 - waveX0);
            double cy = Math.max(pv.yMin(), Math.min(pv.yMax(), lastPt.get(1)));
            double ly = waveMidY - (cy / visible) * waveAmp;
            g.setFill(Palette.ORANGE);
            g.fillOval(lx - 5, ly - 5, 10, 10);
        }
    }

    // ==================== kind = taylor（泰勒展开逼近 sin/cos）====================

    private static void drawTaylor(GraphicsContext g, double w, double h, PlotView pv) {
        // 绘图区（留边给刻度与图例）
        double x0 = w * 0.07, x1 = w * 0.96;
        double y0 = h * 0.10, y1 = h * 0.90;
        double dx = pv.xMax() - pv.xMin();
        double dy = pv.yMax() - pv.yMin();

        java.util.function.DoubleUnaryOperator sx =
                x -> x0 + (x - pv.xMin()) / dx * (x1 - x0);
        java.util.function.DoubleUnaryOperator sy =
                y -> y1 - (y - pv.yMin()) / dy * (y1 - y0);

        // π 刻度竖网格（浅）——背景网格开关 ▦
        if (Theme.showGrid()) {
            g.setStroke(Palette.CARD);
            g.setLineWidth(1);
            double[] ticks = {Math.PI / 2, Math.PI, 3 * Math.PI / 2, 2 * Math.PI};
            String[] tickNames = {"π/2", "π", "3π/2", "2π"};
            g.setFill(Palette.FAINT);
            g.setFont(Font.font(10));
            for (int i = 0; i < ticks.length; i++) {
                if (ticks[i] > pv.xMin() && ticks[i] < pv.xMax()) {
                    double tx = sx.applyAsDouble(ticks[i]);
                    g.strokeLine(tx, y0, tx, y1);
                    g.fillText(tickNames[i], tx, y1 + 14);
                }
            }
        }
        // 水平参考线 y=±1（浅）+ 坐标轴
        g.setStroke(Palette.CARD);
        for (double yv : new double[]{-1, 1}) {
            if (yv > pv.yMin() && yv < pv.yMax()) {
                g.strokeLine(x0, sy.applyAsDouble(yv), x1, sy.applyAsDouble(yv));
            }
        }
        g.setStroke(Palette.FAINT);
        if (pv.yMin() <= 0 && pv.yMax() >= 0) {
            g.strokeLine(x0, sy.applyAsDouble(0), x1, sy.applyAsDouble(0));
        }
        g.strokeLine(x0, y0, x0, y1);
        g.setTextAlign(TextAlignment.RIGHT);
        for (double yv : new double[]{-1, 0, 1}) {
            if (yv >= pv.yMin() && yv <= pv.yMax()) {
                g.fillText(fmt(yv), x0 - 6, sy.applyAsDouble(yv) + 4);
            }
        }
        g.setTextAlign(TextAlignment.CENTER);

        // 真实曲线（蓝实线）
        g.setStroke(Palette.BLUE);
        g.setLineWidth(2.5);
        strokeInBand(g, pv.sineTrace(), sx, sy, pv.yMin(), pv.yMax());

        // 近似曲线（橙实线）——低次多项式在远端爆冲：超域段裁剪在边框（经典泰勒图读感）
        g.setStroke(Palette.AMBER);
        g.setLineWidth(2);
        strokeInBand(g, pv.cosineTrace(), sx, sy, pv.yMin(), pv.yMax());

        // 新项单独曲线（绿虚线，甲帧才有——看这一项自己把曲线往哪拉）
        if (pv.trace3().size() >= 2) {
            g.setStroke(Palette.GREEN);
            g.setLineWidth(1.5);
            g.setLineDashes(5, 4);
            strokeInBand(g, pv.trace3(), sx, sy, pv.yMin(), pv.yMax());
            g.setLineDashes();
        }

        // 误差历史迷你图（rects = [项数, 全段最大误差] 逐乙帧追加）。
        // 位置：左下角 y<-1 的空白带——真实 sin 与近似曲线都不经过的区域，不遮挡主图。
        if (!pv.rects().isEmpty()) {
            double bw = 180, bh = 46;
            double bx = x0 + 10, by = y1 - bh - 6;
            g.setStroke(Palette.GRID);
            g.setLineWidth(1);
            g.strokeRect(bx, by, bw, bh);
            g.setFill(Palette.MUTED);
            g.setFont(Font.font(10));
            g.setTextAlign(TextAlignment.LEFT);
            g.fillText("全段最大误差", bx + 6, by + 12);
            double maxErr = 0;
            for (List<Double> pt : pv.rects()) {
                maxErr = Math.max(maxErr, pt.get(1));
            }
            if (maxErr <= 0) {
                maxErr = 1;
            }
            double padL = 10, padT = 20, padR = 10, padB = 8;
            double iw = bw - padL - padR, ih = bh - padT - padB;
            int n = pv.rects().size();
            double pxPrev = 0, pyPrev = 0;
            for (int i = 0; i < n; i++) {
                List<Double> pt = pv.rects().get(i);
                double px = bx + padL + (n == 1 ? iw / 2 : iw * i / (n - 1));
                double py = by + padT + ih * (1 - pt.get(1) / (maxErr * 1.15));
                if (i > 0) {
                    g.setStroke(Palette.AMBER);
                    g.setLineWidth(1.5);
                    g.strokeLine(pxPrev, pyPrev, px, py);
                }
                g.setFill(Palette.AMBER);
                g.fillOval(px - 2.5, py - 2.5, 5, 5);
                pxPrev = px;
                pyPrev = py;
            }
            g.setTextAlign(TextAlignment.CENTER);
        }

        // 图例（最多三行：真实/累加/新项）+ 读数（最高次幂 / 全段最大误差）
        javafx.scene.paint.Color[] legendColors = {Palette.BLUE, Palette.AMBER, Palette.GREEN};
        double lx = x1 - 210, ly = y0 + 6;
        g.setTextAlign(TextAlignment.LEFT);
        g.setFont(Font.font(11));
        for (int i = 0; i < Math.min(legendColors.length, pv.labels().size()); i++) {
            g.setFill(legendColors[i]);
            g.fillRect(lx, ly + i * 18, 14, 3);
            g.setFill(Palette.INK);
            g.fillText(pv.labels().get(i), lx + 20, ly + i * 18 + 5);
        }
        g.setFill(Palette.MUTED);
        g.fillText(String.format("最高次幂 %d · 全段最大误差 %.4f", (int) pv.angle(), pv.pointY()),
                x0 + 8, y0 + 10);
        g.setTextAlign(TextAlignment.CENTER);
    }

    /** 折线绘制 + 水平带裁剪：整段在带内照画；跨界段线性插值截断在边界；带外整段跳过。 */
    private static void strokeInBand(GraphicsContext g, List<List<Double>> trace,
                                     java.util.function.DoubleUnaryOperator sx,
                                     java.util.function.DoubleUnaryOperator sy,
                                     double yLo, double yHi) {
        if (trace == null || trace.size() < 2) return;
        for (int i = 1; i < trace.size(); i++) {
            double ax = trace.get(i - 1).get(0), ay = trace.get(i - 1).get(1);
            double bx = trace.get(i).get(0), by = trace.get(i).get(1);
            double lo = Math.min(ay, by), hi = Math.max(ay, by);
            if (hi < yLo || lo > yHi) continue;
            double[] seg = clipSegmentY(ax, ay, bx, by, yLo, yHi);
            if (seg == null) continue;
            g.strokeLine(sx.applyAsDouble(seg[0]), sy.applyAsDouble(seg[1]),
                    sx.applyAsDouble(seg[2]), sy.applyAsDouble(seg[3]));
        }
    }

    /** 线段与水平带 [yLo,yHi] 求交：返回带内子段 (x1,y1,x2,y2)，无交返回 null。 */
    private static double[] clipSegmentY(double ax, double ay, double bx, double by,
                                         double yLo, double yHi) {
        double t0 = 0, t1 = 1;
        double dy = by - ay;
        double[] r = clipAxis(ay, dy, yLo, false);          // 约束 y >= yLo
        if (r == null) return null;
        t0 = Math.max(t0, r[0]);
        t1 = Math.min(t1, r[1]);
        r = clipAxis(ay, dy, yHi, true);                    // 约束 y <= yHi
        if (r == null) return null;
        t0 = Math.max(t0, r[0]);
        t1 = Math.min(t1, r[1]);
        if (t0 > t1) return null;
        return new double[]{ax + (bx - ax) * t0, ay + dy * t0, ax + (bx - ax) * t1, ay + dy * t1};
    }

    /** 单边界约束：upper=true 要求 y(t)<=bound，否则 y(t)>=bound；返回满足约束的参数区间，恒不满足返回 null。 */
    private static double[] clipAxis(double y0, double dy, double bound, boolean upper) {
        if (dy == 0) {
            boolean ok = upper ? y0 <= bound : y0 >= bound;
            return ok ? new double[]{0, 1} : null;
        }
        double t = (bound - y0) / dy;
        boolean keepAbove = upper ? dy < 0 : dy > 0;   // y 朝远离边界方向走 → t 在交点之后
        return keepAbove ? new double[]{t, 1} : new double[]{0, t};
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
        g.setStroke(Palette.FAINT);
        g.setLineWidth(1);
        if (pv.yMin() <= 0 && pv.yMax() >= 0) {
            g.strokeLine(x0, sy.applyAsDouble(0), x1, sy.applyAsDouble(0));
        }
        if (pv.xMin() <= 0 && pv.xMax() >= 0) {
            g.strokeLine(sx.applyAsDouble(0), y0, sx.applyAsDouble(0), y1);
        }
        // y 轴刻度
        g.setFill(Palette.FAINT);
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
            g.setStroke(Palette.BLUE);
            g.setLineWidth(2.5);
            var pts = pv.sineTrace();
            for (int i = 1; i < pts.size(); i++) {
                g.strokeLine(sx.applyAsDouble(pts.get(i - 1).get(0)), sy.applyAsDouble(pts.get(i - 1).get(1)),
                        sx.applyAsDouble(pts.get(i).get(0)), sy.applyAsDouble(pts.get(i).get(1)));
            }
        }

        // 零点参考线（目标根 √2 附近竖虚线）
        g.setStroke(Palette.GRID);
        g.setLineDashes(4, 4);
        g.setLineWidth(1);
        double root = pv.trace3().isEmpty() ? Double.NaN : pv.trace3().get(pv.trace3().size() - 1).get(0);
        if (!Double.isNaN(root)) {
            g.strokeLine(sx.applyAsDouble(root), y0, sx.applyAsDouble(root), y1);
        }
        g.setLineDashes();

        // 历代近似根（灰点，画在 x 轴上）
        g.setFill(Palette.FAINT);
        for (List<Double> p : pv.trace3()) {
            double mx = sx.applyAsDouble(p.get(0));
            double my = sy.applyAsDouble(0);
            g.fillOval(mx - 4, my - 4, 8, 8);
        }

        // 当前切线段（橙）
        if (pv.cosineTrace().size() >= 2) {
            g.setStroke(Palette.AMBER);
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
        g.setFill(Palette.ORANGE);
        g.fillOval(cx - 6, cy - 6, 12, 12);
        // 切点 → x 轴投影线
        g.setStroke(Palette.RED);
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
        g.setStroke(Palette.MUTED);
        g.setLineWidth(1.5);
        g.strokeOval(cx - r, cy - r, r * 2, r * 2);
        // 十字轴
        g.setStroke(Palette.GRID_SOFT);
        g.setLineWidth(1);
        g.strokeLine(cx - r - 25, cy, cx + r + 25, cy);
        g.strokeLine(cx, cy - r - 25, cx, cy + r + 25);

        // 目标角射线（绿虚线）
        double tx = cx + Math.cos(pv.angle()) * r * 1.35;
        double ty = cy - Math.sin(pv.angle()) * r * 1.35;
        g.setStroke(Palette.GREEN);
        g.setLineWidth(2);
        g.setLineDashes(6, 4);
        g.strokeLine(cx, cy, tx, ty);
        g.setLineDashes();
        g.setFill(Palette.GREEN_DEEP);
        g.setFont(Font.font(11));
        g.fillText(String.format("目标 %.2f°", Math.toDegrees(pv.angle())), tx + 8, ty - 6);

        // 历代向量（灰，渐弱）
        for (int i = 0; i < pv.trace3().size(); i++) {
            List<Double> p = pv.trace3().get(i);
            double alpha = 0.15 + 0.5 * (i + 1.0) / pv.trace3().size();
            javafx.scene.paint.Color c = Palette.FAINT;
            g.setStroke(new javafx.scene.paint.Color(c.getRed(), c.getGreen(), c.getBlue(), alpha));
            g.setLineWidth(1.2);
            g.strokeLine(cx, cy, cx + p.get(0) * r, cy - p.get(1) * r);
        }

        // 当前向量（橙加粗）
        double px = cx + pv.pointX() * r;
        double py = cy - pv.pointY() * r;
        g.setStroke(Palette.AMBER);
        g.setLineWidth(3);
        g.strokeLine(cx, cy, px, py);

        // 当前端点（红点）
        g.setFill(Palette.ORANGE);
        g.fillOval(px - 6, py - 6, 12, 12);

        drawLabels(g, pv, w * 0.80, h * 0.16);
    }


    // ==================== kind = riemann（定积分·黎曼和）====================

    private static void drawRiemann(GraphicsContext g, double w, double h, PlotView pv) {
        double x0 = w * 0.10, x1 = w * 0.92;
        double yTop = h * 0.10, yBase = h * 0.88;
        double dx = pv.xMax() - pv.xMin();
        double dy = pv.yMax() - pv.yMin();
        java.util.function.DoubleUnaryOperator sx = x -> x0 + (x - pv.xMin()) / dx * (x1 - x0);
        java.util.function.DoubleUnaryOperator sy = y -> yBase - (y - pv.yMin()) / dy * (yBase - yTop);

        // 坐标轴 + y=1 参考线
        g.setStroke(Palette.FAINT);
        g.setLineWidth(1.2);
        g.strokeLine(x0, sy.applyAsDouble(0), x1, sy.applyAsDouble(0));
        g.strokeLine(x0, yTop, x0, yBase);
        g.setStroke(Palette.GRID);
        g.setLineDashes(4, 4);
        g.strokeLine(x0, sy.applyAsDouble(1), x1, sy.applyAsDouble(1));
        g.setLineDashes();
        g.setFill(Palette.FAINT);
        g.setFont(Font.font(10));
        g.setTextAlign(TextAlignment.LEFT);
        g.fillText("y=1", x1 - 28, sy.applyAsDouble(1) - 4);

        // 矩形（黎曼和的"条"）
        for (List<Double> r : pv.rects()) {
            double rx0 = sx.applyAsDouble(r.get(0)), rx1 = sx.applyAsDouble(r.get(1));
            double ry = sy.applyAsDouble(r.get(2));
            g.setFill(new Color(Palette.BLUE.getRed(), Palette.BLUE.getGreen(), Palette.BLUE.getBlue(), 0.35));
            g.fillRect(rx0, ry, rx1 - rx0, sy.applyAsDouble(0) - ry);
            g.setStroke(Palette.BLUE_MID);
            g.setLineWidth(1);
            g.strokeRect(rx0, ry, rx1 - rx0, sy.applyAsDouble(0) - ry);
        }

        // 曲线（绿）
        if (pv.sineTrace().size() >= 2) {
            g.setStroke(Palette.GREEN_DEEP);
            g.setLineWidth(2.5);
            var pts = pv.sineTrace();
            for (int i = 1; i < pts.size(); i++) {
                g.strokeLine(sx.applyAsDouble(pts.get(i - 1).get(0)), sy.applyAsDouble(pts.get(i - 1).get(1)),
                        sx.applyAsDouble(pts.get(i).get(0)), sy.applyAsDouble(pts.get(i).get(1)));
            }
        }

        // 注解
        g.setTextAlign(TextAlignment.LEFT);
        g.setFont(Font.font(12));
        drawLabelsWithBackdrop(g, pv, x0 + 8, yTop + 16);
    }

    // ==================== kind = cartesian（笛卡尔直角坐标平面）====================

    private static void drawCartesian(GraphicsContext g, double w, double h, PlotView pv) {
        double x0 = w * 0.08, x1 = w * 0.94;
        double yTop = h * 0.08, yBase = h * 0.90;
        double dx = pv.xMax() - pv.xMin();
        double dy = pv.yMax() - pv.yMin();
        java.util.function.DoubleUnaryOperator sx = x -> x0 + (x - pv.xMin()) / dx * (x1 - x0);
        java.util.function.DoubleUnaryOperator sy = y -> yBase - (y - pv.yMin()) / dy * (yBase - yTop);

        // 网格（背景网格开关 ▦）+ 坐标轴（0 线永远保留）
        if (Theme.showGrid()) {
            g.setStroke(Palette.CARD);
            g.setLineWidth(1);
            for (int gx = (int) Math.ceil(pv.xMin()); gx <= pv.xMax(); gx++) {
                g.strokeLine(sx.applyAsDouble(gx), yTop, sx.applyAsDouble(gx), yBase);
            }
            for (int gy = (int) Math.ceil(pv.yMin()); gy <= pv.yMax(); gy++) {
                g.strokeLine(x0, sy.applyAsDouble(gy), x1, sy.applyAsDouble(gy));
            }
        }
        if (pv.yMin() <= 0 && pv.yMax() >= 0) {
            g.setStroke(Palette.MUTED);
            g.setLineWidth(1.5);
            g.strokeLine(x0, sy.applyAsDouble(0), x1, sy.applyAsDouble(0));
        }
        if (pv.xMin() <= 0 && pv.xMax() >= 0) {
            g.setStroke(Palette.MUTED);
            g.setLineWidth(1.5);
            g.strokeLine(sx.applyAsDouble(0), yTop, sx.applyAsDouble(0), yBase);
        }

        // 累积曲线（蓝）——极坐标点逐个落到直角坐标系里
        if (pv.sineTrace().size() >= 2) {
            g.setStroke(Palette.BLUE);
            g.setLineWidth(2.5);
            var pts = pv.sineTrace();
            for (int i = 1; i < pts.size(); i++) {
                g.strokeLine(sx.applyAsDouble(pts.get(i - 1).get(0)), sy.applyAsDouble(pts.get(i - 1).get(1)),
                        sx.applyAsDouble(pts.get(i).get(0)), sy.applyAsDouble(pts.get(i).get(1)));
            }
        }

        // 参考线（绿虚线，如复利 demo 的 y=e 极限线）
        if (pv.cosineTrace().size() >= 2) {
            g.setStroke(Palette.GREEN_DEEP);
            g.setLineWidth(1.5);
            g.setLineDashes(5, 4);
            var ln = pv.cosineTrace();
            for (int i = 1; i < ln.size(); i++) {
                g.strokeLine(sx.applyAsDouble(ln.get(i - 1).get(0)), sy.applyAsDouble(ln.get(i - 1).get(1)),
                        sx.applyAsDouble(ln.get(i).get(0)), sy.applyAsDouble(ln.get(i).get(1)));
            }
            g.setLineDashes();
        }

        // 当前动点（橙）
        double cx = sx.applyAsDouble(pv.pointX()), cy = sy.applyAsDouble(pv.pointY());
        g.setFill(Palette.ORANGE);
        g.fillOval(cx - 6, cy - 6, 12, 12);
        // 参考线二选一，按声明域语义分流：
        //   原点在域内 → 极坐标语义 → 淡极径线（心形线/螺线/旋转/抛体的发射原点）
        //   原点不在域内 → 纯函数图像 → 动点向两轴的投影虚线 + 轴脚读数（如复利的 n 与终值）
        boolean originInDomain = pv.xMin() <= 0 && pv.xMax() >= 0 && pv.yMin() <= 0 && pv.yMax() >= 0;
        if (originInDomain) {
            g.setStroke(new Color(Palette.AMBER.getRed(), Palette.AMBER.getGreen(), Palette.AMBER.getBlue(), 0.35));
            g.setLineWidth(1);
            g.strokeLine(sx.applyAsDouble(0), sy.applyAsDouble(0), cx, cy);
        } else {
            g.setStroke(new Color(Palette.AMBER.getRed(), Palette.AMBER.getGreen(), Palette.AMBER.getBlue(), 0.5));
            g.setLineWidth(1);
            g.setLineDashes(4, 4);
            g.strokeLine(cx, cy, cx, yBase);   // 竖虚线落到 x 轴：读 n
            g.strokeLine(cx, cy, x0, cy);      // 横虚线落到 y 轴：读终值
            g.setLineDashes();
            // 轴脚读数小字
            g.setFill(Palette.RUST);
            g.setFont(Font.font(10));
            g.setTextAlign(TextAlignment.CENTER);
            g.fillText(fmtTick(pv.pointX()), cx, yBase + 14);
            g.fillText(fmtTick(pv.pointY()), x0 - 6, cy + 3);
        }

        // 注解
        g.setTextAlign(TextAlignment.LEFT);
        g.setFont(Font.font(12));
        drawLabelsWithBackdrop(g, pv, x0 + 8, yTop + 16);
    }


    // ==================== kind = scatter（随机撒点）====================

    private static void drawScatter(GraphicsContext g, double w, double h, PlotView pv) {
        double x0 = w * 0.10, x1 = w * 0.92;
        double yTop = h * 0.10, yBase = h * 0.90;
        double dx = pv.xMax() - pv.xMin();
        double dy = pv.yMax() - pv.yMin();
        java.util.function.DoubleUnaryOperator sx = x -> x0 + (x - pv.xMin()) / dx * (x1 - x0);
        java.util.function.DoubleUnaryOperator sy = y -> yBase - (y - pv.yMin()) / dy * (yBase - yTop);

        // 正方形边界
        g.setStroke(Palette.GRID);
        g.setLineWidth(1.2);
        g.strokeRect(sx.applyAsDouble(pv.xMin()), yTop, sx.applyAsDouble(pv.xMax()) - x0, yBase - yTop);

        // 圆外点（橙）
        for (List<Double> d : pv.trace3()) {
            g.setFill(new Color(Palette.ORANGE_BRIGHT.getRed(), Palette.ORANGE_BRIGHT.getGreen(), Palette.ORANGE_BRIGHT.getBlue(), 0.75));
            double r = Math.max(2, d.get(2) * (x1 - x0) * 3);
            g.fillOval(sx.applyAsDouble(d.get(0)) - r, sy.applyAsDouble(d.get(1)) - r, r * 2, r * 2);
        }
        // 圆内点（蓝）
        for (List<Double> d : pv.rects()) {
            g.setFill(new Color(Palette.BLUE.getRed(), Palette.BLUE.getGreen(), Palette.BLUE.getBlue(), 0.8));
            double r = Math.max(2, d.get(2) * (x1 - x0) * 3);
            g.fillOval(sx.applyAsDouble(d.get(0)) - r, sy.applyAsDouble(d.get(1)) - r, r * 2, r * 2);
        }
        // 四分之一圆弧（绿粗线）
        if (pv.sineTrace().size() >= 2) {
            g.setStroke(Palette.GREEN_DEEP);
            g.setLineWidth(2.5);
            var pts = pv.sineTrace();
            for (int i = 1; i < pts.size(); i++) {
                g.strokeLine(sx.applyAsDouble(pts.get(i - 1).get(0)), sy.applyAsDouble(pts.get(i - 1).get(1)),
                        sx.applyAsDouble(pts.get(i).get(0)), sy.applyAsDouble(pts.get(i).get(1)));
            }
        }
        // 注解
        g.setTextAlign(TextAlignment.LEFT);
        g.setFont(Font.font(12));
        drawLabelsWithBackdrop(g, pv, x0 + 8, yTop + 16);
    }

    // ==================== kind = doublePendulum（双摆混沌）====================

    private static void drawDoublePendulum(GraphicsContext g, double w, double h, PlotView pv) {
        double cx = w * 0.5, cy = h * 0.50;
        double scale = Math.min(w * 0.36, h * 0.40) / 2.2;   // 世界半径 ±2.2（L1+L2=2 留余量）
        java.util.function.DoubleUnaryOperator sx = x -> cx + x * scale;
        java.util.function.DoubleUnaryOperator sy = y -> cy - y * scale;

        // 十字参考线 + 挂点
        g.setStroke(Palette.GRID_SOFT);
        g.setLineWidth(1);
        g.strokeLine(cx - scale * 2.3, cy, cx + scale * 2.3, cy);
        g.strokeLine(cx, cy - scale * 2.3, cx, cy + scale * 2.3);
        g.setFill(Palette.INK);
        g.fillOval(cx - 3, cy - 3, 6, 6);

        // 主摆锤 2 轨迹（蓝细线）
        if (pv.sineTrace().size() >= 2) {
            g.setStroke(new Color(Palette.BLUE.getRed(), Palette.BLUE.getGreen(), Palette.BLUE.getBlue(), 0.55));
            g.setLineWidth(1.4);
            var pts = pv.sineTrace();
            for (int i = 1; i < pts.size(); i++) {
                g.strokeLine(sx.applyAsDouble(pts.get(i - 1).get(0)), sy.applyAsDouble(pts.get(i - 1).get(1)),
                        sx.applyAsDouble(pts.get(i).get(0)), sy.applyAsDouble(pts.get(i).get(1)));
            }
        }

        // 幽灵摆锤 2 轨迹（灰虚点——初值差 0.001 的另一条命运）
        if (pv.cosineTrace().size() >= 2) {
            g.setStroke(new Color(Palette.FAINT.getRed(), Palette.FAINT.getGreen(), Palette.FAINT.getBlue(), 0.5));
            g.setLineWidth(1.2);
            g.setLineDashes(2, 5);
            var ghosts = pv.cosineTrace();
            for (int i = 1; i < ghosts.size(); i++) {
                g.strokeLine(sx.applyAsDouble(ghosts.get(i - 1).get(0)), sy.applyAsDouble(ghosts.get(i - 1).get(1)),
                        sx.applyAsDouble(ghosts.get(i).get(0)), sy.applyAsDouble(ghosts.get(i).get(1)));
            }
            g.setLineDashes();
        }

        // 主双摆：θ₁ 定摆臂 1，摆锤 1 处接摆臂 2（点X/Y = 主摆锤 2）
        double x1 = Math.sin(pv.angle()), y1 = -Math.cos(pv.angle());
        double x2 = pv.pointX(), y2 = pv.pointY();
        g.setStroke(Palette.AMBER);
        g.setLineWidth(2.5);
        g.strokeLine(cx, cy, sx.applyAsDouble(x1), sy.applyAsDouble(y1));
        g.strokeLine(sx.applyAsDouble(x1), sy.applyAsDouble(y1), sx.applyAsDouble(x2), sy.applyAsDouble(y2));
        g.setFill(Palette.AMBER);
        g.fillOval(sx.applyAsDouble(x1) - 6, sy.applyAsDouble(y1) - 6, 12, 12);
        g.setFill(Palette.ORANGE);
        g.fillOval(sx.applyAsDouble(x2) - 7, sy.applyAsDouble(y2) - 7, 14, 14);

        // 幽灵摆锤 2 当前位置（灰空心圆，取幽灵轨迹末点）
        if (!pv.cosineTrace().isEmpty()) {
            var gp = pv.cosineTrace().get(pv.cosineTrace().size() - 1);
            double gx = sx.applyAsDouble(gp.get(0)), gy = sy.applyAsDouble(gp.get(1));
            g.setStroke(Palette.FAINT);
            g.setLineWidth(1.6);
            g.strokeOval(gx - 6, gy - 6, 12, 12);
        }

        // 注解
        drawLabels(g, pv, w * 0.04, h * 0.12);
    }

    // ==================== 公共 ====================

    private static void drawLabels(GraphicsContext g, PlotView pv, double lx, double ly) {
        g.setTextAlign(TextAlignment.LEFT);
        g.setFont(Font.font(12));
        drawLabelsWithBackdrop(g, pv, lx, ly);
    }

    /** 注解统一加白色半透明底条——密集点云/曲线上文字不再被挡（蒙特卡洛等）。 */
    private static void drawLabelsWithBackdrop(GraphicsContext g, PlotView pv, double lx, double ly) {
        g.setTextAlign(TextAlignment.LEFT);
        g.setFont(Font.font(12));
        double lineH = 18;
        double pad = 4;
        for (int i = 0; i < pv.labels().size(); i++) {
            String text = pv.labels().get(i);
            double tw = text.length() * 12.0 * 0.62 + 8;   // 估宽（中英混排近似）
            g.setFill(new Color(Palette.WHITE.getRed(), Palette.WHITE.getGreen(), Palette.WHITE.getBlue(), 0.78));
            g.fillRoundRect(lx - pad, ly + i * lineH - 12, tw, lineH - 2, 4, 4);
            g.setFill(i == 0 ? Palette.INK : Palette.MUTED);
            g.fillText(text, lx, ly + i * lineH);
        }
    }

    private static String fmt(double v) {
        return Math.abs(v) >= 10 ? String.valueOf((int) v) : String.format("%.1f", v);
    }

    /** 轴脚刻度小字：整数不带小数点（8 → "8"），小数保留两位（2.5356 → "2.54"）。 */
    private static String fmtTick(double v) {
        if (Math.abs(v - Math.rint(v)) < 1e-9) {
            return String.valueOf((long) Math.rint(v));
        }
        return String.format("%.2f", v);
    }

    /** 波形折线：x 按图层 x 域映射，y 钳制在 [yMin,yMax]——低阶泰勒近似在远端会爆到 ±6，不钳就飞出画面。 */
    private static void drawTrace(GraphicsContext g, List<List<Double>> trace,
                                  double wx0, double wx1, double midY, double amp,
                                  double xMin, double xSpan, double yMin, double yMax, double visible) {
        if (trace.size() < 2) return;
        var it = trace.iterator();
        var prev = it.next();
        while (it.hasNext()) {
            var curr = it.next();
            double px1 = wx0 + (prev.get(0) - xMin) / xSpan * (wx1 - wx0);
            double py1 = midY - (clamp(prev.get(1), yMin, yMax) / visible) * amp;
            double px2 = wx0 + (curr.get(0) - xMin) / xSpan * (wx1 - wx0);
            double py2 = midY - (clamp(curr.get(1), yMin, yMax) / visible) * amp;
            g.strokeLine(px1, py1, px2, py2);
            prev = curr;
        }
    }

    private static double clamp(double v, double lo, double hi) {
        return Math.max(lo, Math.min(hi, v));
    }
}
