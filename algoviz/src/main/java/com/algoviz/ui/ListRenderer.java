package com.algoviz.ui;

import com.algoviz.engine.Step;
import com.algoviz.engine.Value;
import com.algoviz.engine.ds.ListView;
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
 * 渲染层之一：链表的「盒子 + 箭头」现场（蓝图 §1.2 底板的实物化）。
 * 盒子按原链顺序排一行；前向箭头走直线，掉头/跨接箭头走下方弧线；
 * 变量（head/prev/curr/next）以彩色标签挂在盒子正上方；
 * 黄箭头 = 与上帧相比变化的箭头（视图 C）；紫虚线 = 预测模式选中。
 */
public final class ListRenderer {

    private static final double GAP = 34;
    private static final double BOX_H = 54;

    private static final Color BASE = Color.web("#3b82f6");
    private static final Color ARROW = Color.web("#6b7280");
    private static final Color CHANGED = Color.web("#eab308");
    private static final Color PICKED = Color.web("#8b5cf6");
    private static final Color NULL_C = Color.web("#9ca3af");

    private static final Map<String, Color> VAR_COLORS = Map.of(
            "head", Color.web("#2563eb"),
            "prev", Color.web("#16a34a"),
            "curr", Color.web("#ea580c"),
            "next", Color.web("#9333ea"),
            "slow", Color.web("#0891b2"),
            "fast", Color.web("#e11d48"));

    private ListRenderer() {
    }

    public static void draw(Canvas canvas, Step step, Set<Integer> picked, Set<String> changedArrows) {
        GraphicsContext g = canvas.getGraphicsContext2D();
        g.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
        if (step == null || step.list() == null) {
            return;
        }
        ListView lv = step.list();
        int n = lv.nodes().size();
        if (n == 0) {
            return;
        }
        double w = canvas.getWidth();
        double h = canvas.getHeight();
        double bw = boxWidth(w, n);
        double boxTop = h / 2 - BOX_H - 10;
        double cx0 = (w - (n * bw + (n - 1) * GAP)) / 2;

        // 1. 后方弧线（掉头 / 跨接箭头）
        for (int i = 0; i < n; i++) {
            ListView.NodeBox nb = lv.nodes().get(i);
            if (nb.nextId() == null) {
                continue;
            }
            int t = indexOf(lv, nb.nextId());
            if (t == i + 1) {
                continue;
            }
            double x1 = cx0 + i * (bw + GAP) + bw / 2;
            double x2 = cx0 + t * (bw + GAP) + bw / 2;
            double y1 = boxTop + BOX_H;
            double depth = 38 + Math.abs(t - i) * 10;
            double cx = (x1 + x2) / 2;
            double cy = y1 + depth;
            g.setStroke(isChanged(changedArrows,nb.nodeId()) ? CHANGED : ARROW);
            g.setLineWidth(isChanged(changedArrows,nb.nodeId()) ? 3 : 1.6);
            g.beginPath();
            g.moveTo(x1, y1);
            g.quadraticCurveTo(cx, cy, x2, y1);
            g.stroke();
            headAt(g, x2, y1, Math.atan2(y1 - cy, x2 - cx));
        }

        // 2. 盒子
        g.setTextAlign(TextAlignment.CENTER);
        for (int i = 0; i < n; i++) {
            ListView.NodeBox nb = lv.nodes().get(i);
            double x = cx0 + i * (bw + GAP);
            g.setFill(BASE);
            g.fillRoundRect(x, boxTop, bw, BOX_H, 8, 8);
            g.setFill(Color.web("#dbeafe"));
            g.setFont(Font.font(10));
            g.fillText(nb.nodeId(), x + bw / 2, boxTop + 14);
            g.setFill(Color.WHITE);
            g.setFont(Font.font(16));
            g.fillText(String.valueOf(nb.val()), x + bw / 2, boxTop + 41);
            if (picked != null && picked.contains(i)) {
                g.setStroke(PICKED);
                g.setLineWidth(2.5);
                g.setLineDashes(6, 4);
                g.strokeRoundRect(x - 4, boxTop - 4, bw + 8, BOX_H + 8, 10, 10);
                g.setLineDashes();
            }
        }

        // 3. 直线箭头 + null 端点
        for (int i = 0; i < n; i++) {
            ListView.NodeBox nb = lv.nodes().get(i);
            double xR = cx0 + i * (bw + GAP) + bw;
            double ym = boxTop + BOX_H / 2;
            if (nb.nextId() == null) {
                g.setStroke(isChanged(changedArrows,nb.nodeId()) ? CHANGED : NULL_C);
                g.setLineWidth(isChanged(changedArrows,nb.nodeId()) ? 3 : 1.6);
                g.strokeLine(xR, ym, xR + 18, ym);
                g.setFill(NULL_C);
                g.setFont(Font.font(12));
                g.fillText("null", xR + 36, ym + 4);
            } else if (indexOf(lv, nb.nextId()) == i + 1) {
                g.setStroke(isChanged(changedArrows,nb.nodeId()) ? CHANGED : ARROW);
                g.setLineWidth(isChanged(changedArrows,nb.nodeId()) ? 3 : 1.6);
                arrowHead(g, xR, ym, xR + GAP, ym);
            }
        }

        // 3.5 双链表背向箭（prevId 非空才画）：盒子下方的短反向箭 prev ↔
        boolean anyPrev = lv.nodes().stream().anyMatch(nb -> nb.prevId() != null);
        if (anyPrev) {
            double yb = boxTop + BOX_H + 16;
            g.setFont(Font.font(10));
            for (int i = 0; i < n; i++) {
                ListView.NodeBox nb = lv.nodes().get(i);
                double xL = cx0 + i * (bw + GAP);
                if (nb.prevId() == null) {
                    continue;   // 头节点无 prev
                }
                int p = indexOf(lv, nb.prevId());
                double xFrom = xL;                          // 从本节点左缘出发向左
                double xTo = (p == i - 1)
                        ? cx0 + p * (bw + GAP) + bw         // 相邻：指向前一盒右缘
                        : cx0 + p * (bw + GAP) + bw / 2;    // 跨接：指向前盒中心（配合弧线更清楚）
                boolean changed = isChanged(changedArrows, nb.nodeId());
                g.setStroke(changed ? CHANGED : Color.web("#16a34a"));
                g.setLineWidth(changed ? 3 : 1.6);
                g.strokeLine(xFrom, yb, xTo, yb);
                arrowHead(g, xTo, yb, xTo - (xFrom - xTo) * 0.2, yb);
            }
            g.setFill(Color.web("#16a34a"));
            g.setTextAlign(TextAlignment.LEFT);
            g.fillText("prev", cx0 - 34, yb + 4);
            g.setTextAlign(TextAlignment.CENTER);
        }

        // 4. 变量标签（head/prev/curr/next → 彩色小牌挂在盒子正上方）
        if (step.stack() != null && !step.stack().frames().isEmpty()) {
            var locals = step.stack().frames().get(step.stack().frames().size() - 1).locals();
            Map<Integer, List<String>> tags = new LinkedHashMap<>();
            for (Map.Entry<String, Value> e : locals.entrySet()) {
                Value v = e.getValue();
                if (v.kind() == Value.Kind.REF && v.refId() != null) {
                    int idx = indexOf(lv, v.refId());
                    if (idx >= 0) {
                        tags.computeIfAbsent(idx, k -> new ArrayList<>()).add(e.getKey());
                    }
                }
            }
            for (Map.Entry<Integer, List<String>> entry : tags.entrySet()) {
                int i = entry.getKey();
                List<String> names = entry.getValue();
                double x = cx0 + i * (bw + GAP) + bw / 2;
                for (int k = 0; k < names.size(); k++) {
                    String name = names.get(k);
                    double ty = boxTop - 28 - (names.size() - 1 - k) * 22;
                    Color c = VAR_COLORS.getOrDefault(name, ARROW);
                    g.setFill(c);
                    g.setFont(Font.font(12));
                    g.fillRoundRect(x - 19, ty - 14, 38, 18, 6, 6);
                    g.setFill(Color.WHITE);
                    g.fillText(name, x, ty);
                }
                g.setStroke(VAR_COLORS.getOrDefault(names.get(0), ARROW));
                g.setLineWidth(1.5);
                g.strokeLine(x, boxTop - 28 + 4, x, boxTop - 2);
            }
        }
    }

    /** 供预测模式点击命中：返回盒子下标，未命中 -1。几何与 draw() 一致。 */
    public static int hitIndex(double w, double h, int n, double x, double y) {
        if (n <= 0) {
            return -1;
        }
        double bw = boxWidth(w, n);
        double boxTop = h / 2 - BOX_H - 10;
        double cx0 = (w - (n * bw + (n - 1) * GAP)) / 2;
        if (y < boxTop - 16 || y > boxTop + BOX_H + 16) {
            return -1;
        }
        for (int i = 0; i < n; i++) {
            double x0 = cx0 + i * (bw + GAP);
            if (x >= x0 - 4 && x <= x0 + bw + 4) {
                return i;
            }
        }
        return -1;
    }

    private static double boxWidth(double w, int n) {
        return Math.min(86, Math.max(40, (w - 80 - GAP * (n - 1)) / n));
    }

    private static boolean isChanged(Set<String> set, String id) {
        return set != null && set.contains(id);
    }

    private static int indexOf(ListView lv, String nodeId) {
        for (int i = 0; i < lv.nodes().size(); i++) {
            if (lv.nodes().get(i).nodeId().equals(nodeId)) {
                return i;
            }
        }
        return -1;
    }

    private static void arrowHead(GraphicsContext g, double x1, double y1, double x2, double y2) {
        g.strokeLine(x1, y1, x2, y2);
        headAt(g, x2, y2, Math.atan2(y2 - y1, x2 - x1));
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
}
