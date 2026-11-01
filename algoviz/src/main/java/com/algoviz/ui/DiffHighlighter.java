package com.algoviz.ui;

import com.algoviz.engine.Value;
import com.algoviz.engine.ds.ArrayView;
import com.algoviz.engine.ds.GraphView;
import com.algoviz.engine.ds.ListView;
import com.algoviz.engine.ds.SeqView;
import com.algoviz.engine.ds.TreeView;
import com.algoviz.engine.math.MathView;
import com.algoviz.engine.mem.HeapLayer;
import com.algoviz.engine.mem.StackLayer;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 视图 C：状态 diff（蓝图 §2.2）——两帧之间谁变了。
 * 数组格子变化 → 黄圈；局部变量变化 → 黄字加粗；堆变化 → 黄字。
 */
public final class DiffHighlighter {

    private DiffHighlighter() {
    }

    public static Set<Integer> changedIndices(ArrayView prev, ArrayView cur) {
        if (prev == null || cur == null || prev.values().size() != cur.values().size()) {
            return Set.of();
        }
        Set<Integer> out = new LinkedHashSet<>();
        for (int i = 0; i < cur.values().size(); i++) {
            if (!prev.values().get(i).equals(cur.values().get(i))) {
                out.add(i);
            }
        }
        return out;
    }

    /** 链表 diff：哪些节点的 next 箭头变了（返回节点 id，如 "@2"）。 */
    public static Set<String> changedArrows(ListView prev, ListView cur) {
        if (prev == null || cur == null || prev.nodes().size() != cur.nodes().size()) {
            return Set.of();
        }
        Set<String> out = new LinkedHashSet<>();
        for (int i = 0; i < cur.nodes().size(); i++) {
            String oldNext = prev.nodes().get(i).nextId();
            String newNext = cur.nodes().get(i).nextId();
            if (!Objects.equals(oldNext, newNext)) {
                out.add(cur.nodes().get(i).nodeId());
            }
        }
        return out;
    }

    /** 树 diff：状态 / 访问序号 / 孩子指针 / 值 有任一变化的节点 id。 */
    public static Set<String> changedTreeNodes(TreeView prev, TreeView cur) {
        if (prev == null || cur == null) {
            return Set.of();
        }
        Map<String, TreeView.TreeNodeBox> old = prev.nodes().stream()
                .collect(Collectors.toMap(TreeView.TreeNodeBox::nodeId, Function.identity()));
        Set<String> out = new LinkedHashSet<>();
        for (TreeView.TreeNodeBox nb : cur.nodes()) {
            TreeView.TreeNodeBox o = old.get(nb.nodeId());
            boolean changed = o == null
                    || o.val() != nb.val()
                    || !Objects.equals(o.state(), nb.state())
                    || !Objects.equals(o.visitOrder(), nb.visitOrder())
                    || !Objects.equals(o.leftId(), nb.leftId())
                    || !Objects.equals(o.rightId(), nb.rightId());
            if (changed) {
                out.add(nb.nodeId());
            }
        }
        return out;
    }

    /** 树 diff：孩子指针（left/right）发生变化的父节点 id——渲染层用它把边标黄。 */
    public static Set<String> changedTreeEdges(TreeView prev, TreeView cur) {
        if (prev == null || cur == null) {
            return Set.of();
        }
        Map<String, TreeView.TreeNodeBox> old = prev.nodes().stream()
                .collect(Collectors.toMap(TreeView.TreeNodeBox::nodeId, Function.identity()));
        Set<String> out = new LinkedHashSet<>();
        for (TreeView.TreeNodeBox nb : cur.nodes()) {
            TreeView.TreeNodeBox o = old.get(nb.nodeId());
            if (o == null
                    || !Objects.equals(o.leftId(), nb.leftId())
                    || !Objects.equals(o.rightId(), nb.rightId())) {
                out.add(nb.nodeId());
            }
        }
        return out;
    }

    /** 图 diff：距离变化 / 出圈状态变化 / 成为当前圆心 的节点 id。 */
    public static Set<String> changedGraphNodes(GraphView prev, GraphView cur) {
        if (prev == null || cur == null) {
            return Set.of();
        }
        Map<String, Integer> oldDist = prev.dist();
        Set<String> oldSettled = new java.util.HashSet<>(prev.settledIds());
        Set<String> out = new LinkedHashSet<>();
        for (GraphView.GraphVertex v : cur.vertices()) {
            boolean changed = !Objects.equals(oldDist.get(v.label()), cur.dist().get(v.label()))
                    || oldSettled.contains(v.nodeId()) != cur.settledIds().contains(v.nodeId())
                    || !Objects.equals(prev.activeId(), cur.activeId()) && v.nodeId().equals(cur.activeId());
            if (changed) {
                out.add(v.nodeId());
            }
        }
        return out;
    }

    /** 时序 diff：本步激活的消息经过的泳道 id。 */
    public static Set<String> changedSeqParticipants(SeqView prev, SeqView cur) {
        if (cur == null || cur.activeIndex() < 0 || cur.activeIndex() >= cur.messages().size()) {
            return Set.of();
        }
        SeqView.Message m = cur.messages().get(cur.activeIndex());
        Set<String> out = new LinkedHashSet<>();
        out.add(cur.participants().get(m.from()).id());
        out.add(cur.participants().get(m.to()).id());
        return out;
    }

    /** 矩阵 diff：值发生变化的格子，key = "gridId:cellIdx"（行主序）。 */
    public static Set<String> changedMatrixCells(MathView prev, MathView cur) {
        if (prev == null || cur == null) {
            return Set.of();
        }
        Map<String, MathView.Grid> old = new java.util.HashMap<>();
        for (MathView.Grid g : prev.grids()) {
            old.put(g.id(), g);
        }
        Set<String> out = new LinkedHashSet<>();
        for (MathView.Grid g : cur.grids()) {
            MathView.Grid o = old.get(g.id());
            for (int i = 0; i < g.values().size(); i++) {
                String oldV = o == null || i >= o.values().size() ? null : o.values().get(i);
                if (!java.util.Objects.equals(oldV, g.values().get(i))) {
                    out.add(g.id() + ":" + i);
                }
            }
        }
        return out;
    }

    public static Set<String> changedLocalKeys(StackLayer prev, StackLayer cur) {
        if (prev == null || cur == null || prev.frames().isEmpty() || cur.frames().isEmpty()) {
            return Set.of();
        }
        Map<String, Value> oldLocals = prev.frames().get(prev.frames().size() - 1).locals();
        Map<String, Value> newLocals = cur.frames().get(cur.frames().size() - 1).locals();
        Set<String> out = new LinkedHashSet<>();
        for (Map.Entry<String, Value> e : newLocals.entrySet()) {
            Value oldV = oldLocals.get(e.getKey());
            if (oldV == null || !oldV.equals(e.getValue())) {
                out.add(e.getKey());
            }
        }
        return out;
    }

    public static boolean heapChanged(HeapLayer prev, HeapLayer cur) {
        if (prev == null || cur == null) {
            return false;
        }
        return !prev.heap().equals(cur.heap());
    }
}
