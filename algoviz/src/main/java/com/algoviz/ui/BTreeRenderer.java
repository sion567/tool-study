package com.algoviz.ui;

import com.algoviz.engine.Step;
import com.algoviz.engine.ds.BTreeView;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 渲染层之一：B+ 树的多路节点现场（ds.btree 图层）。
 * 节点 = 多槽盒子（3 槽，空槽灰显）；父子箭从盒子底部出发；
 * 叶子节点绿色 + 底部横向叶链箭（范围查询的骨架）；
 * onstack = 橙描边（下探路径），fresh = 黄底（刚分裂），黄字 = 本步写入的 key。
 */
public final class BTreeRenderer {

    private static final double SLOT_W = 34;
    private static final double SLOT_H = 26;
    private static final int MAX_KEYS = 3;

    private static final Color LEAF_C = Color.web("#059669");
    private static final Color INNER_C = Color.web("#3b82f6");
    private static final Color FRESH_C = Color.web("#f59e0b");
    private static final Color ARROW = Color.web("#6b7280");
    private static final Color ONSTACK = Color.web("#ea580c");
    private static final Color PICKED = Color.web("#8b5cf6");
    private static final Color CHAIN_C = Color.web("#16a34a");

    private BTreeRenderer() {
    }

    public static void draw(Canvas canvas, Step step, Set<Integer> picked, Set<String> changedNodes) {
        GraphicsContext g = canvas.getGraphicsContext2D();
        g.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
        if (step == null || step.btree() == null) {
            return;
        }
        BTreeView tv = step.btree();
        if (tv.nodes().isEmpty()) {
            return;
        }
        double w = canvas.getWidth(), h = canvas.getHeight();
        Map<String, double[]> pos = layout(tv, w, h);
        double nodeW = MAX_KEYS * SLOT_W + 12;
        double nodeH = SLOT_H + 10;

        // 1. 父子箭（从父盒底部中点到子盒顶部中点）
        for (BTreeView.BNode nd : tv.nodes()) {
            if (nd.childIds() == null) continue;
            double[] from = pos.get(nd.nodeId());
            for (String cid : nd.childIds()) {
                double[] to = pos.get(cid);
                if (to == null) continue;
                boolean changed = changedNodes != null && (changedNodes.contains(nd.nodeId()) || changedNodes.contains(cid));
                g.setStroke(changed ? Color.web("#eab308") : ARROW);
                g.setLineWidth(changed ? 3 : 1.5);
                g.strokeLine(from[0] + nodeW / 2, from[1] + nodeH, to[0] + nodeW / 2, to[1]);
            }
        }

        // 2. 叶链箭（叶子右缘 → 下一个叶子左缘，画在叶层下方）
        for (int i = 0; i < tv.leafChainIds().size() - 1; i++) {
            double[] a = pos.get(tv.leafChainIds().get(i));
            double[] b = pos.get(tv.leafChainIds().get(i + 1));
            if (a == null || b == null) continue;
            double y = a[1] + nodeH + 12;
            g.setStroke(CHAIN_C);
            g.setLineWidth(1.6);
            g.strokeLine(a[0] + nodeW, y, b[0], y);
            arrowHead(g, b[0], y, Math.PI);
            g.setFill(CHAIN_C);
            g.setFont(Font.font(9));
            g.setTextAlign(TextAlignment.CENTER);
            g.fillText("叶链", (a[0] + nodeW + b[0]) / 2, y + 12);
        }

        // 3. 节点盒子（多槽）
        g.setTextAlign(TextAlignment.CENTER);
        for (int i = 0; i < tv.nodes().size(); i++) {
            BTreeView.BNode nd = tv.nodes().get(i);
            double[] p = pos.get(nd.nodeId());
            boolean onstack = "onstack".equals(nd.state());
            boolean fresh = "fresh".equals(nd.state());
            Color base = nd.leaf() ? LEAF_C : INNER_C;
            g.setFill(fresh ? FRESH_C : base);
            g.fillRoundRect(p[0], p[1], nodeW, nodeH, 6, 6);
            if (onstack) {
                g.setStroke(ONSTACK);
                g.setLineWidth(2.5);
                g.strokeRoundRect(p[0] - 2, p[1] - 2, nodeW + 4, nodeH + 4, 8, 8);
            }
            if (picked != null && picked.contains(i)) {
                g.setStroke(PICKED);
                g.setLineWidth(2.5);
                g.setLineDashes(6, 4);
                g.strokeRoundRect(p[0] - 4, p[1] - 4, nodeW + 8, nodeH + 8, 10, 10);
                g.setLineDashes();
            }
            for (int k = 0; k < MAX_KEYS; k++) {
                double sx = p[0] + 4 + k * SLOT_W;
                boolean hasKey = k < nd.keys().size();
                // 槽底
                g.setStroke(hasKey ? Color.web("#ffffff55") : Color.web("#ffffff33"));
                g.setLineWidth(1);
                g.strokeRect(sx, p[1] + 5, SLOT_W - 4, SLOT_H - 10);
                if (hasKey) {
                    boolean active = nd.activeKey() != null && nd.activeKey() == nd.keys().get(k);
                    g.setFill(active ? Color.web("#fde047") : Color.WHITE);
                    g.setFont(Font.font(active ? 14 : 13));
                    g.fillText(String.valueOf(nd.keys().get(k)), sx + (SLOT_W - 4) / 2, p[1] + SLOT_H - 4);
                }
            }
        }

        // 图例
        g.setTextAlign(TextAlignment.LEFT);
        g.setFont(Font.font(10));
        g.setFill(Color.web("#9ca3af"));
        g.fillText("蓝=内部  绿=叶子  黄=刚分裂  橙描边=下探路径  亮字=本步写入", 12, h - 8);
    }

    /** 预测模式点击命中：返回节点在 nodes() 里的下标，未命中 -1。 */
    public static int hitIndex(Canvas canvas, Step step, double x, double y) {
        if (step == null || step.btree() == null) {
            return -1;
        }
        BTreeView tv = step.btree();
        if (tv.nodes().isEmpty()) {
            return -1;
        }
        double w = canvas.getWidth(), h = canvas.getHeight();
        Map<String, double[]> pos = layout(tv, w, h);
        double nodeW = MAX_KEYS * SLOT_W + 12;
        double nodeH = SLOT_H + 10;
        for (int i = 0; i < tv.nodes().size(); i++) {
            double[] p = pos.get(tv.nodes().get(i).nodeId());
            if (x >= p[0] - 4 && x <= p[0] + nodeW + 4 && y >= p[1] - 4 && y <= p[1] + nodeH + 4) {
                return i;
            }
        }
        return -1;
    }

    /** B+ 树 diff：keys/children/next/新节点 任一变化。 */
    public static Set<String> changedNodes(BTreeView prev, BTreeView cur) {
        if (prev == null || cur == null) {
            return Set.of();
        }
        Map<String, BTreeView.BNode> old = new LinkedHashMap<>();
        for (BTreeView.BNode nd : prev.nodes()) old.put(nd.nodeId(), nd);
        Set<String> out = new java.util.LinkedHashSet<>();
        for (BTreeView.BNode nd : cur.nodes()) {
            BTreeView.BNode o = old.get(nd.nodeId());
            if (o == null || !o.keys().equals(nd.keys())
                    || !java.util.Objects.equals(o.nextId(), nd.nextId())
                    || !java.util.Objects.equals(o.childIds(), nd.childIds())) {
                out.add(nd.nodeId());
            }
        }
        return out;
    }

    // ---------- 布局：递归子树宽度，自底向上定 x ----------

    private static Map<String, double[]> layout(BTreeView tv, double w, double h) {
        Map<String, BTreeView.BNode> byId = new LinkedHashMap<>();
        for (BTreeView.BNode nd : tv.nodes()) byId.put(nd.nodeId(), nd);
        Map<String, List<String>> childrenOf = new LinkedHashMap<>();
        for (BTreeView.BNode nd : tv.nodes()) {
            childrenOf.put(nd.nodeId(), nd.childIds() == null ? List.of() : nd.childIds());
        }
        // 层序：根 → 叶
        Map<String, Integer> depth = new LinkedHashMap<>();
        String root = tv.rootId();
        java.util.ArrayDeque<String> q = new java.util.ArrayDeque<>();
        q.add(root);
        depth.put(root, 0);
        while (!q.isEmpty()) {
            String cur = q.poll();
            for (String c : childrenOf.get(cur)) {
                if (!depth.containsKey(c)) {
                    depth.put(c, depth.get(cur) + 1);
                    q.add(c);
                }
            }
        }
        // 叶子层宽 = 叶数 × (nodeW + 24)；每个叶子占一列，内部节点居中于孩子
        double nodeW = MAX_KEYS * SLOT_W + 12;
        List<String> leaves = tv.leafChainIds();
        double slotW = nodeW + 24;
        double totalW = Math.max(leaves.size() * slotW, 220);
        double scale = Math.min(1.0, (w - 60) / totalW);
        double x0 = (w - totalW * scale) / 2;
        Map<String, double[]> pos = new LinkedHashMap<>();
        // 叶子按链顺序铺
        for (int i = 0; i < leaves.size(); i++) {
            pos.put(leaves.get(i), new double[]{x0 + (i + 0.5) * slotW * scale - nodeW / 2, 0});
        }
        // 自底向上：内部节点 x = 孩子 x 的平均
        int maxDepth = 0;
        for (int d : depth.values()) maxDepth = Math.max(maxDepth, d);
        for (int d = maxDepth - 1; d >= 0; d--) {
            for (BTreeView.BNode nd : tv.nodes()) {
                if (depth.get(nd.nodeId()) != d) continue;
                double sum = 0;
                List<String> ch = childrenOf.get(nd.nodeId());
                for (String c : ch) sum += pos.get(c)[0];
                pos.put(nd.nodeId(), new double[]{sum / ch.size(), 0});
            }
        }
        // y：按深度铺
        double top = 40;
        double rowH = Math.min(110, (h - top - 60) / Math.max(1, maxDepth));
        for (BTreeView.BNode nd : tv.nodes()) {
            pos.get(nd.nodeId())[1] = top + depth.get(nd.nodeId()) * rowH;
        }
        return pos;
    }

    private static void arrowHead(GraphicsContext g, double x, double y, double angle) {
        int len = 7;
        g.strokeLine(x, y,
                x - len * Math.cos(angle - Math.PI / 7),
                y - len * Math.sin(angle - Math.PI / 7));
        g.strokeLine(x, y,
                x - len * Math.cos(angle + Math.PI / 7),
                y - len * Math.sin(angle + Math.PI / 7));
    }
}
