package com.algoviz.ui;

import com.algoviz.engine.Step;
import com.algoviz.engine.Value;
import com.algoviz.engine.ds.HashView;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 渲染层之一：哈希表的「桶 + 冲突链」现场（ds.hash 图层）。
 * 左列 = 桶槽（0..bucketCount-1），链向右水平生长；
 * 桶槽高亮 = 当前操作的桶；黄箭头 = 与上帧相比变化的链箭；
 * 紫虚线 = 预测模式选中。几何与 hitIndex 一致。
 */
public final class HashRenderer {

    private static final double SLOT_W = 64;
    private static final double SLOT_H = 30;
    private static final double GAP = 30;      // 桶间垂直间距
    private static final double BOX_W = 52;
    private static final double BOX_H = 26;
    private static final double CHAIN_GAP = 14;

    private static final Color SLOT_C = Color.web("#64748b");
    private static final Color BOX_C = Color.web("#3b82f6");
    private static final Color ARROW = Color.web("#6b7280");
    private static final Color CHANGED = Color.web("#eab308");
    private static final Color PICKED = Color.web("#8b5cf6");

    private HashRenderer() {
    }

    public static void draw(Canvas canvas, Step step, Set<Integer> picked, Set<String> changedArrows) {
        GraphicsContext g = canvas.getGraphicsContext2D();
        g.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
        if (step == null || step.hash() == null) {
            return;
        }
        HashView hv = step.hash();
        int buckets = hv.bucketCount();
        double w = canvas.getWidth();
        double h = canvas.getHeight();
        double slotX = Math.max(20, w * 0.14);
        double y0 = Math.max(24, (h - buckets * (SLOT_H + GAP) + GAP) / 2);

        // 桶标签 h(k)=k%N
        g.setTextAlign(TextAlignment.RIGHT);
        g.setFill(Color.web("#9ca3af"));
        g.setFont(Font.font(10));
        g.fillText("h(k) = k % " + buckets, slotX + SLOT_W, y0 - 8);

        // 1. 桶槽列
        g.setTextAlign(TextAlignment.CENTER);
        for (int b = 0; b < buckets; b++) {
            double y = y0 + b * (SLOT_H + GAP);
            boolean active = isActiveBucket(step, b);
            g.setFill(active ? Color.web("#f59e0b") : SLOT_C);
            g.fillRoundRect(slotX, y, SLOT_W, SLOT_H, 6, 6);
            g.setFill(Color.WHITE);
            g.setFont(Font.font(13));
            g.fillText(String.valueOf(b), slotX + SLOT_W / 2, y + 20);
        }

        // 2. 链节点（按桶分组，链头紧贴桶右缘）
        for (int b = 0; b < buckets; b++) {
            List<HashView.HashNode> chain = chainOf(hv, b);
            for (int k = 0; k < chain.size(); k++) {
                HashView.HashNode nd = chain.get(k);
                int idx = hv.nodes().indexOf(nd);
                double x = slotX + SLOT_W + 20 + k * (BOX_W + CHAIN_GAP);
                double y = y0 + b * (SLOT_H + GAP) - (BOX_H - SLOT_H) / 2;
                g.setFill(BOX_C);
                g.fillRoundRect(x, y, BOX_W, BOX_H, 6, 6);
                g.setFill(Color.WHITE);
                g.setFont(Font.font(13));
                g.fillText(String.valueOf(nd.val()), x + BOX_W / 2, y + 18);
                if (picked != null && picked.contains(idx)) {
                    g.setStroke(PICKED);
                    g.setLineWidth(2.5);
                    g.setLineDashes(6, 4);
                    g.strokeRoundRect(x - 4, y - 4, BOX_W + 8, BOX_H + 8, 8, 8);
                    g.setLineDashes();
                }
                // 链箭
                double xR = x + BOX_W;
                double ym = y + BOX_H / 2;
                boolean changed = changedArrows != null && changedArrows.contains(nd.nodeId());
                if (nd.nextId() == null) {
                    g.setStroke(changed ? CHANGED : ARROW);
                    g.setLineWidth(changed ? 3 : 1.5);
                    g.strokeLine(xR, ym, xR + 10, ym);
                    g.setFill(ARROW);
                    g.setFont(Font.font(10));
                    g.fillText("∅", xR + 17, ym + 4);
                } else {
                    g.setStroke(changed ? CHANGED : ARROW);
                    g.setLineWidth(changed ? 3 : 1.5);
                    arrowHead(g, xR, ym, xR + CHAIN_GAP, ym);
                }
            }
        }

        // 3. 变量标签（栈顶 locals 里的 REF → 挂到节点上方）
        if (step.stack() != null && !step.stack().frames().isEmpty()) {
            var locals = step.stack().frames().get(step.stack().frames().size() - 1).locals();
            Map<String, List<String>> tags = new LinkedHashMap<>();
            for (Map.Entry<String, Value> e : locals.entrySet()) {
                Value v = e.getValue();
                if (v.kind() == Value.Kind.REF && v.refId() != null) {
                    int idx = indexOfId(hv, v.refId());
                    if (idx >= 0) {
                        tags.computeIfAbsent(v.refId(), k -> new ArrayList<>()).add(e.getKey());
                    }
                }
            }
            g.setFont(Font.font(11));
            for (Map.Entry<String, List<String>> entry : tags.entrySet()) {
                HashView.HashNode nd = byId(hv, entry.getKey());
                if (nd == null) {
                    continue;
                }
                int k = chainOf(hv, nd.bucket()).indexOf(nd);
                double x = slotX + SLOT_W + 20 + k * (BOX_W + CHAIN_GAP) + BOX_W / 2;
                double ty = y0 + nd.bucket() * (SLOT_H + GAP) - (BOX_H - SLOT_H) / 2 - 8;
                g.setFill(Color.web("#ea580c"));
                g.fillText(String.join("·", entry.getValue()), x, ty);
            }
        }
    }

    /** 预测模式点击命中：返回节点下标（hv.nodes() 里的位置），未命中 -1。 */
    public static int hitIndex(Canvas canvas, Step step, double x, double y) {
        if (step == null || step.hash() == null) {
            return -1;
        }
        HashView hv = step.hash();
        double w = canvas.getWidth();
        double h = canvas.getHeight();
        int buckets = hv.bucketCount();
        double slotX = Math.max(20, w * 0.14);
        double y0 = Math.max(24, (h - buckets * (SLOT_H + GAP) + GAP) / 2);
        for (int b = 0; b < buckets; b++) {
            List<HashView.HashNode> chain = chainOf(hv, b);
            for (int k = 0; k < chain.size(); k++) {
                double bx = slotX + SLOT_W + 20 + k * (BOX_W + CHAIN_GAP);
                double by = y0 + b * (SLOT_H + GAP) - (BOX_H - SLOT_H) / 2;
                if (x >= bx - 4 && x <= bx + BOX_W + 4 && y >= by - 4 && y <= by + BOX_H + 4) {
                    return hv.nodes().indexOf(chain.get(k));
                }
            }
        }
        return -1;
    }

    /** hash diff：哪些节点的链箭变了。 */
    public static Set<String> changedArrows(HashView prev, HashView cur) {
        if (prev == null || cur == null) {
            return Set.of();
        }
        java.util.Set<String> out = new java.util.LinkedHashSet<>();
        Map<String, String> oldNext = new LinkedHashMap<>();
        for (HashView.HashNode nd : prev.nodes()) {
            oldNext.put(nd.nodeId(), nd.nextId());
        }
        for (HashView.HashNode nd : cur.nodes()) {
            if (!java.util.Objects.equals(oldNext.get(nd.nodeId()), nd.nextId())) {
                out.add(nd.nodeId());
            }
        }
        return out;
    }

    // ---------- 几何/工具 ----------

    private static List<HashView.HashNode> chainOf(HashView hv, int bucket) {
        List<HashView.HashNode> out = new ArrayList<>();
        Map<String, HashView.HashNode> byId = new LinkedHashMap<>();
        for (HashView.HashNode nd : hv.nodes()) {
            byId.put(nd.nodeId(), nd);
        }
        // 链头 = 桶内没有被任何 next 指到的节点
        java.util.Set<String> referenced = new java.util.HashSet<>();
        for (HashView.HashNode nd : hv.nodes()) {
            if (nd.bucket() == bucket && nd.nextId() != null) {
                referenced.add(nd.nextId());
            }
        }
        HashView.HashNode head = hv.nodes().stream()
                .filter(nd -> nd.bucket() == bucket && !referenced.contains(nd.nodeId()))
                .findFirst().orElse(null);
        while (head != null) {
            out.add(head);
            head = head.nextId() == null ? null : byId.get(head.nextId());
        }
        return out;
    }

    private static boolean isActiveBucket(Step step, int b) {
        if (step.stack() == null || step.stack().frames().isEmpty()) {
            return false;
        }
        var locals = step.stack().frames().get(step.stack().frames().size() - 1).locals();
        Value bucket = locals.get("bucket");
        return bucket != null && bucket.text() != null && bucket.text().equals(String.valueOf(b));
    }

    private static int indexOfId(HashView hv, String id) {
        for (int i = 0; i < hv.nodes().size(); i++) {
            if (hv.nodes().get(i).nodeId().equals(id)) {
                return i;
            }
        }
        return -1;
    }

    private static HashView.HashNode byId(HashView hv, String id) {
        for (HashView.HashNode nd : hv.nodes()) {
            if (nd.nodeId().equals(id)) {
                return nd;
            }
        }
        return null;
    }

    private static void arrowHead(GraphicsContext g, double x1, double y1, double x2, double y2) {
        g.strokeLine(x1, y1, x2, y2);
        double angle = Math.atan2(y2 - y1, x2 - x1);
        int len = 7;
        g.strokeLine(x2, y2,
                x2 - len * Math.cos(angle - Math.PI / 7),
                y2 - len * Math.sin(angle - Math.PI / 7));
        g.strokeLine(x2, y2,
                x2 - len * Math.cos(angle + Math.PI / 7),
                y2 - len * Math.sin(angle + Math.PI / 7));
    }
}
