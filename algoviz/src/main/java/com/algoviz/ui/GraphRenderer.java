package com.algoviz.ui;

import com.algoviz.engine.Step;
import com.algoviz.engine.ds.GraphView;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * 渲染层之一：图的「圆点 + 带权边 + 距离表」现场（Dijkstra）。
 * 颜色：绿 = 已出圈（距离定死），蓝 = 未出圈，橙圈 = 当前圆心，
 * 黄圈 = 与上帧相比变化的节点，黄边 = 本步正在松弛的边；紫虚线 = 预测模式选中。
 * 距离表同时实物化在变量面板下方的堆盒子 DistanceTable 里。
 */
public final class GraphRenderer {

    private static final Color BASE = Color.web("#3b82f6");
    private static final Color SETTLED = Color.web("#22c55e");
    private static final Color EDGE = Color.web("#94a3b8");
    private static final Color RELAX = Color.web("#eab308");
    private static final Color TOP = Color.web("#ea580c");
    private static final Color PICKED = Color.web("#8b5cf6");

    private GraphRenderer() {
    }

    public static void draw(Canvas canvas, Step step, Set<Integer> picked, Set<String> changedNodes) {
        GraphicsContext g = canvas.getGraphicsContext2D();
        g.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
        if (step == null || step.graph() == null) {
            return;
        }
        GraphView gv = step.graph();
        if (gv.vertices().isEmpty()) {
            return;
        }
        double w = canvas.getWidth();
        double h = canvas.getHeight();
        double r = 20;
        Map<String, double[]> pos = positions(gv, w, h);

        // 1. 边 + 权重
        g.setTextAlign(TextAlignment.CENTER);
        for (GraphView.GraphEdge e : gv.edges()) {
            double[] a = pos.get(e.fromId());
            double[] b = pos.get(e.toId());
            if (a == null || b == null) {
                continue;
            }
            boolean relaxing = isRelaxing(gv, e);
            g.setStroke(relaxing ? RELAX : EDGE);
            g.setLineWidth(relaxing ? 3.5 : 1.8);
            g.strokeLine(a[0], a[1], b[0], b[1]);
            double mx = (a[0] + b[0]) / 2;
            double my = (a[1] + b[1]) / 2;
            g.setFill(Color.web("#6b7280"));
            g.setFont(Font.font(11));
            g.fillText(String.valueOf(e.weight()), mx, my - 4);
        }

        // 2. 顶点 + 距离
        for (int i = 0; i < gv.vertices().size(); i++) {
            GraphView.GraphVertex v = gv.vertices().get(i);
            double[] p = pos.get(v.nodeId());
            boolean settledNow = gv.settledIds().contains(v.nodeId());
            g.setFill(settledNow ? SETTLED : BASE);
            g.fillOval(p[0] - r, p[1] - r, r * 2, r * 2);
            if (v.nodeId().equals(gv.activeId())) {
                g.setStroke(TOP);
                g.setLineWidth(3);
                g.strokeOval(p[0] - r - 3, p[1] - r - 3, r * 2 + 6, r * 2 + 6);
            }
            if (changedNodes != null && changedNodes.contains(v.nodeId())) {
                g.setStroke(RELAX);
                g.setLineWidth(2.5);
                g.strokeOval(p[0] - r - 3, p[1] - r - 3, r * 2 + 6, r * 2 + 6);
            }
            if (picked != null && picked.contains(i)) {
                g.setStroke(PICKED);
                g.setLineWidth(2.5);
                g.setLineDashes(6, 4);
                g.strokeOval(p[0] - r - 5, p[1] - r - 5, r * 2 + 10, r * 2 + 10);
                g.setLineDashes();
            }
            g.setFill(Color.WHITE);
            g.setFont(Font.font(14));
            g.fillText(v.label(), p[0], p[1] + 5);
            Integer d = gv.dist().get(v.label());
            if (d != null) {
                g.setFill(settledNow ? Color.web("#15803d") : Color.web("#1d4ed8"));
                g.setFont(Font.font(12));
                g.fillText("d=" + d, p[0], p[1] + r + 16);
            }
            // 访问顺序角标（BFS/DFS）
            if (gv.visitOrderIds() != null && gv.visitOrderIds().contains(v.nodeId())) {
                int seq = gv.visitOrderIds().indexOf(v.nodeId()) + 1;
                g.setFill(TOP);
                g.fillOval(p[0] + r - 7, p[1] - r - 7, 16, 16);
                g.setFill(Color.WHITE);
                g.setFont(Font.font(10));
                g.fillText(String.valueOf(seq), p[0] + r + 1, p[1] - r + 5);
            }
        }
    }

    private static boolean isRelaxing(GraphView gv, GraphView.GraphEdge e) {
        if (gv.relaxFrom() == null || gv.relaxTo() == null) {
            return false;
        }
        return (e.fromId().equals(gv.relaxFrom()) && e.toId().equals(gv.relaxTo()))
                || (e.fromId().equals(gv.relaxTo()) && e.toId().equals(gv.relaxFrom()));
    }

    private static Map<String, double[]> positions(GraphView gv, double w, double h) {
        Map<String, double[]> pos = new HashMap<>();
        for (GraphView.GraphVertex v : gv.vertices()) {
            pos.put(v.nodeId(), new double[]{60 + v.x() * (w - 120), 50 + v.y() * (h - 130)});
        }
        return pos;
    }

    /** 供预测模式点击命中：最近的顶点下标（vertices 列表位置），未命中 -1。 */
    public static int hitIndex(Canvas canvas, Step step, double x, double y) {
        if (step == null || step.graph() == null) {
            return -1;
        }
        GraphView gv = step.graph();
        Map<String, double[]> pos = positions(gv, canvas.getWidth(), canvas.getHeight());
        int best = -1;
        double bestDist = 30;
        for (int i = 0; i < gv.vertices().size(); i++) {
            double[] p = pos.get(gv.vertices().get(i).nodeId());
            if (p == null) {
                continue;
            }
            double d = Math.hypot(x - p[0], y - p[1]);
            if (d <= bestDist) {
                bestDist = d;
                best = i;
            }
        }
        return best;
    }
}
