package com.algoviz.ui;

import com.algoviz.engine.Step;
import com.algoviz.engine.ds.TreeView;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * 渲染层之一：树的「圆点 + 边」现场。
 * 颜色：浅灰 = 未访问，蓝 = 在调用栈上（橙圈 = 栈顶，即当前节点），绿 = 已访问（右上角访问序号）；
 * 黄圈 = 与上帧相比变化的节点，黄边 = 本步改接的孩子指针；紫虚线 = 预测模式选中。
 * 调用栈视图 = 变量面板的多层栈帧；调用树视图 = 本渲染器的访问序号——两者同开（蓝图 M4）。
 */
public final class TreeRenderer {

    private static final Color UNVISITED = Color.web("#e2e8f0");
    private static final Color ONSTACK = Color.web("#3b82f6");
    private static final Color VISITED = Color.web("#22c55e");
    private static final Color EDGE = Color.web("#94a3b8");
    private static final Color CHANGED = Color.web("#eab308");
    private static final Color TOP = Color.web("#ea580c");
    private static final Color PICKED = Color.web("#8b5cf6");

    private TreeRenderer() {
    }

    public static void draw(Canvas canvas, Step step, Set<Integer> picked,
                            Set<String> changedNodes, Set<String> changedEdges) {
        GraphicsContext g = canvas.getGraphicsContext2D();
        g.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
        if (step == null || step.tree() == null) {
            return;
        }
        TreeView tv = step.tree();
        int n = tv.nodes().size();
        if (n == 0) {
            return;
        }
        double w = canvas.getWidth();
        double h = canvas.getHeight();
        Map<String, double[]> pos = layout(tv, w, h);
        double r = radius(w, n);

        // 1. 边（先画，压在节点下面）
        g.setTextAlign(TextAlignment.CENTER);
        for (TreeView.TreeNodeBox nb : tv.nodes()) {
            double[] p = pos.get(nb.nodeId());
            for (String childId : new String[]{nb.leftId(), nb.rightId()}) {
                if (childId == null) {
                    continue;
                }
                double[] c = pos.get(childId);
                if (c == null) {
                    continue;
                }
                boolean edgeChanged = changedEdges != null && changedEdges.contains(nb.nodeId());
                g.setStroke(edgeChanged ? CHANGED : EDGE);
                g.setLineWidth(edgeChanged ? 3 : 1.8);
                g.strokeLine(p[0], p[1], c[0], c[1]);
            }
        }

        // 2. 节点
        String topId = tv.callStackIds().isEmpty() ? null : tv.callStackIds().get(tv.callStackIds().size() - 1);
        for (int i = 0; i < n; i++) {
            TreeView.TreeNodeBox nb = tv.nodes().get(i);
            double[] p = pos.get(nb.nodeId());
            g.setFill(switch (nb.state()) {
                case "onstack" -> ONSTACK;
                case "visited" -> VISITED;
                default -> "RED".equals(nb.rbColor()) ? Color.web("#ef4444")
                        : "BLACK".equals(nb.rbColor()) ? Color.web("#334155") : UNVISITED;
            });
            g.fillOval(p[0] - r, p[1] - r, r * 2, r * 2);
            boolean isChanged = changedNodes != null && changedNodes.contains(nb.nodeId());
            if (nb.nodeId().equals(topId)) {
                g.setStroke(TOP);
                g.setLineWidth(3);
                g.strokeOval(p[0] - r - 3, p[1] - r - 3, r * 2 + 6, r * 2 + 6);
            }
            if (isChanged) {
                g.setStroke(CHANGED);
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
            boolean dark = "unvisited".equals(nb.state()) && nb.rbColor() == null;
            g.setFill(dark ? Color.web("#334155") : Color.WHITE);
            g.setFont(Font.font(14));
            g.fillText(String.valueOf(nb.val()), p[0], p[1] + 5);
            g.setFill(Color.web("#64748b"));
            g.setFont(Font.font(9));
            g.fillText(nb.nodeId(), p[0], p[1] - r - 4);
            if (nb.visitOrder() != null) {
                g.setFill(TOP);
                g.fillOval(p[0] + r - 6, p[1] - r - 6, 16, 16);
                g.setFill(Color.WHITE);
                g.setFont(Font.font(10));
                g.fillText(String.valueOf(nb.visitOrder()), p[0] + r + 2, p[1] - r + 6);
            }
        }

        // 3. 指针标签（如 BST 删除的 min，紫色小牌）
        if (tv.varTags() != null) {
            for (Map.Entry<String, String> e : tv.varTags().entrySet()) {
                double[] p = pos.get(e.getValue());
                if (p == null) {
                    continue;
                }
                double ty = p[1] - r - 24;
                g.setFill(Color.web("#9333ea"));
                g.setFont(Font.font(12));
                g.fillRoundRect(p[0] - 19, ty - 13, 38, 18, 6, 6);
                g.setFill(Color.WHITE);
                g.fillText(e.getKey(), p[0], ty);
            }
        }
    }

    /** 中序位置 → x，深度 → y。返回 nodeId → {cx, cy}。 */
    private static Map<String, double[]> layout(TreeView tv, double w, double h) {
        Map<String, double[]> pos = new HashMap<>();
        String root = tv.rootId();
        if (root == null) {
            return pos;
        }
        Map<String, TreeView.TreeNodeBox> byId = new HashMap<>();
        for (TreeView.TreeNodeBox nb : tv.nodes()) {
            byId.put(nb.nodeId(), nb);
        }
        int[] counter = {0};
        int maxDepth = assign(root, byId, pos, counter, 0, w, h);
        return pos;
    }

    private static int assign(String id, Map<String, TreeView.TreeNodeBox> byId,
                              Map<String, double[]> pos, int[] counter, int depth,
                              double w, double h) {
        TreeView.TreeNodeBox nb = byId.get(id);
        if (nb == null) {
            return depth;
        }
        int maxDepth = depth;
        if (nb.leftId() != null) {
            maxDepth = Math.max(maxDepth, assign(nb.leftId(), byId, pos, counter, depth + 1, w, h));
        }
        double x = 50 + (counter[0] + 0.5) * (w - 100) / Math.max(1, byId.size());
        double y = 60 + depth * Math.min(110, (h - 140) / Math.max(1, maxDepthOf(byId)));
        pos.put(id, new double[]{x, y});
        counter[0]++;
        if (nb.rightId() != null) {
            maxDepth = Math.max(maxDepth, assign(nb.rightId(), byId, pos, counter, depth + 1, w, h));
        }
        return maxDepth;
    }

    private static int maxDepthOf(Map<String, TreeView.TreeNodeBox> byId) {
        int[] max = {0};
        for (TreeView.TreeNodeBox nb : byId.values()) {
            depthOf(nb, byId, 0, max);
        }
        return Math.max(1, max[0]);
    }

    private static void depthOf(TreeView.TreeNodeBox nb, Map<String, TreeView.TreeNodeBox> byId,
                                int d, int[] max) {
        max[0] = Math.max(max[0], d);
        if (nb.leftId() != null && byId.containsKey(nb.leftId())) {
            depthOf(byId.get(nb.leftId()), byId, d + 1, max);
        }
        if (nb.rightId() != null && byId.containsKey(nb.rightId())) {
            depthOf(byId.get(nb.rightId()), byId, d + 1, max);
        }
    }

    /** 供预测模式点击命中：返回最近的节点下标（nodes 列表位置），未命中 -1。 */
    public static int hitIndex(Canvas canvas, Step step, double x, double y) {
        if (step == null || step.tree() == null) {
            return -1;
        }
        TreeView tv = step.tree();
        if (tv.nodes().isEmpty()) {
            return -1;
        }
        Map<String, double[]> pos = layout(tv, canvas.getWidth(), canvas.getHeight());
        double r = radius(canvas.getWidth(), tv.nodes().size());
        int best = -1;
        double bestDist = r + 8;
        for (int i = 0; i < tv.nodes().size(); i++) {
            double[] p = pos.get(tv.nodes().get(i).nodeId());
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

    private static double radius(double w, int n) {
        return Math.max(14, Math.min(24, (w - 100) / Math.max(1, n) / 2 - 4));
    }
}
