package com.algoviz.ui;

import com.algoviz.engine.Step;
import com.algoviz.engine.ds.SeqView;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;

import java.util.Set;

/**
 * 渲染层之一：设计模式的「泳道 + 消息时间轴」现场（蓝图 §5.1）。
 * 泳道 = 参与者（顶部标签 + 竖虚线），消息自上而下排列，实线 = 调用、虚线 = 返回；
 * 橙色粗箭头 = 本步激活的消息；黄框 = 本步消息经过的泳道；紫虚线 = 预测模式选中。
 * 静态 UML 类图（Mermaid）在课件页，交互时序在这里——各司其职。
 */
public final class SequenceRenderer {

    private static final Color LIFELINE = Color.web("#cbd5e1");
    private static final Color HEADER = Color.web("#334155");
    private static final Color PAST = Color.web("#94a3b8");
    private static final Color ACTIVE = Color.web("#ea580c");
    private static final Color CHANGED = Color.web("#eab308");
    private static final Color PICKED = Color.web("#8b5cf6");

    private SequenceRenderer() {
    }

    public static void draw(Canvas canvas, Step step, Set<Integer> picked, Set<String> changedIds) {
        GraphicsContext g = canvas.getGraphicsContext2D();
        g.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
        if (step == null || step.seq() == null) {
            return;
        }
        SeqView sv = step.seq();
        int n = sv.participants().size();
        if (n == 0) {
            return;
        }
        double w = canvas.getWidth();
        double h = canvas.getHeight();
        double margin = 24;
        double colWidth = (w - margin * 2) / n;
        double headerY = 14;
        double headerH = 30;
        double lifelineTop = headerY + headerH + 6;
        double rowH = 34;
        double firstMsgY = lifelineTop + 28;

        double[] xs = new double[n];
        for (int i = 0; i < n; i++) {
            xs[i] = margin + colWidth * (i + 0.5);
        }

        g.setTextAlign(TextAlignment.CENTER);

        // 1. 生命线
        g.setStroke(LIFELINE);
        g.setLineWidth(1.4);
        g.setLineDashes(4, 5);
        for (int i = 0; i < n; i++) {
            g.strokeLine(xs[i], lifelineTop, xs[i], h - 14);
        }
        g.setLineDashes();

        // 2. 消息（时间轴）
        for (int i = 0; i <= sv.activeIndex() && i < sv.messages().size(); i++) {
            SeqView.Message m = sv.messages().get(i);
            double y = firstMsgY + i * rowH;
            boolean active = i == sv.activeIndex();
            Color c = active ? ACTIVE : PAST;
            g.setStroke(c);
            g.setLineWidth(active ? 2.6 : 1.4);
            if ("RETURN".equals(m.kind())) {
                g.setLineDashes(5, 4);
            }
            if (m.from() == m.to()) {
                // 自消息：右侧小回环
                double x = xs[m.from()];
                g.strokeLine(x, y, x + 20, y);
                g.strokeLine(x + 20, y, x + 20, y + 12);
                g.strokeLine(x + 20, y + 12, x + 5, y + 12);
                headAt(g, x + 5, y + 12, Math.PI);
                label(g, m.label(), x + 26, y + 9, active, TextAlignment.LEFT);
            } else {
                double x1 = xs[m.from()];
                double x2 = xs[m.to()];
                double dir = x2 > x1 ? 1 : -1;
                double shrink = 4 * dir;
                g.strokeLine(x1 + shrink, y, x2 - shrink, y);
                headAt(g, x2 - shrink, y, Math.atan2(0, dir));
                label(g, m.label(), (x1 + x2) / 2, y - 6, active, TextAlignment.CENTER);
            }
            if ("RETURN".equals(m.kind())) {
                g.setLineDashes();
            }
        }

        // 3. 泳道头（画在最上层）
        for (int i = 0; i < n; i++) {
            SeqView.Participant p = sv.participants().get(i);
            double hx = xs[i] - (colWidth - 12) / 2;
            g.setFill(HEADER);
            g.fillRoundRect(hx, headerY, colWidth - 12, headerH, 8, 8);
            if (changedIds != null && changedIds.contains(p.id())) {
                g.setStroke(CHANGED);
                g.setLineWidth(2.5);
                g.strokeRoundRect(hx - 2, headerY - 2, colWidth - 8, headerH + 4, 10, 10);
            }
            if (picked != null && picked.contains(i)) {
                g.setStroke(PICKED);
                g.setLineWidth(2.5);
                g.setLineDashes(6, 4);
                g.strokeRoundRect(hx - 4, headerY - 4, colWidth - 4, headerH + 8, 12, 12);
                g.setLineDashes();
            }
            g.setFill(Color.WHITE);
            g.setFont(Font.font(12));
            g.fillText(p.label(), xs[i], headerY + 19);
        }
    }

    private static void label(GraphicsContext g, String text, double x, double y,
                              boolean active, TextAlignment align) {
        g.setTextAlign(align);
        g.setFill(active ? Color.web("#111827") : Color.web("#6b7280"));
        g.setFont(Font.font(active ? 12 : 11));
        g.fillText(text, x, y);
        g.setTextAlign(TextAlignment.CENTER);
    }

    private static void headAt(GraphicsContext g, double x, double y, double ang) {
        double ah = 8;
        double x1 = x - ah * Math.cos(ang - Math.PI / 7);
        double y1 = y - ah * Math.sin(ang - Math.PI / 7);
        double x2 = x - ah * Math.cos(ang + Math.PI / 7);
        double y2 = y - ah * Math.sin(ang + Math.PI / 7);
        g.setFill(g.getStroke());
        g.fillPolygon(new double[]{x, x1, x2}, new double[]{y, y1, y2}, 3);
    }

    /** 供预测模式点击命中：泳道下标（只认头部区域），未命中 -1。 */
    public static int hitIndex(Canvas canvas, Step step, double x, double y) {
        if (step == null || step.seq() == null) {
            return -1;
        }
        SeqView sv = step.seq();
        int n = sv.participants().size();
        if (n == 0 || y > 56) {
            return -1;
        }
        double margin = 24;
        double colWidth = (canvas.getWidth() - margin * 2) / n;
        for (int i = 0; i < n; i++) {
            double cx = margin + colWidth * (i + 0.5);
            if (Math.abs(x - cx) <= colWidth / 2) {
                return i;
            }
        }
        return -1;
    }
}
