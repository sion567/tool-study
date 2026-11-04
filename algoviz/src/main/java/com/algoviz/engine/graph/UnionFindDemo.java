package com.algoviz.engine.graph;

import com.algoviz.engine.Algorithm;
import com.algoviz.engine.Intent;
import com.algoviz.engine.Layer;
import com.algoviz.engine.Loc;
import com.algoviz.engine.Step;
import com.algoviz.engine.Value;
import com.algoviz.engine.VizEvent;
import com.algoviz.engine.ds.GraphView;
import com.algoviz.engine.ds.GraphView.GraphEdge;
import com.algoviz.engine.ds.GraphView.GraphVertex;
import com.algoviz.engine.mem.Heap;
import com.algoviz.engine.mem.HeapLayer;
import com.algoviz.engine.mem.HeapObject;
import com.algoviz.engine.mem.StackFrame;
import com.algoviz.engine.mem.StackLayer;
import com.algoviz.engine.trace.Trace;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 并查集演示：Union（合并集合）+ Find（查找根，含路径压缩）。
 * 画面：森林合并，路径压缩后所有节点直接指向根。
 */
public final class UnionFindDemo implements Algorithm {

    private static final String SCHEMA = "1";
    private static final String INVARIANT =
            "Find 返回同一个根 → 同一集合；路径压缩让树越走越扁平 → 查找接近 O(1)";

    private static final String[] LABELS = {"A", "B", "C", "D", "E", "F"};

    private final List<Step> steps = new ArrayList<>();
    private final int[] parent = new int[LABELS.length];
    private int findCur = -1;

    @Override public String id() { return "graph.unionfind"; }
    @Override public String title() { return "并查集"; }
    @Override public String codeResource() { return "/code/UnionFindDemo.java"; }
    @Override public String docResource() { return "/markdown/union-find.md"; }

    @Override
    public Trace produce() {
        for (int i = 0; i < LABELS.length; i++) parent[i] = i;  // 初始：各自为根
        emit(2, List.of(VizEvent.of(VizEvent.EventType.INIT,
                "初始化：6 个节点各自为根（6 个独立集合）")), "初始：每个元素自己是一个集合");

        union(0, 1);   // A-B
        union(2, 3);   // C-D
        union(1, 3);   // 合并 {A,B} 和 {C,D} → {A,B,C,D}
        union(4, 5);   // E-F

        find(1);        // 路径压缩演示
        union(0, 4);    // 合并最后两个集合

        emit(13, List.of(VizEvent.of(VizEvent.EventType.RETURN,
                "最终：所有节点指向同一个根 → 只剩 1 个集合")),
                "并查集 = 用一棵树表示一个集合；森林的形状决定查找效率");
        return new Trace(SCHEMA, meta(), steps);
    }

    private int find(int x) {
        if (parent[x] == x) return x;
        // 路径压缩：递归找到根后，把沿途节点直接指向根
        int root = find(parent[x]);
        if (parent[x] != root) {
            findCur = x;
            emit(7, List.of(VizEvent.of(VizEvent.EventType.SET,
                    "路径压缩：" + LABELS[x] + " 直接指向根 " + LABELS[root], "@" + (x + 1))),
                    "路径压缩：下次查找 O(1)——树越走越扁");
            parent[x] = root;
        }
        return root;
    }

    private void union(int a, int b) {
        int ra = find(a), rb = find(b);
        String la = LABELS[a], lb = LABELS[b];
        emit(4, List.of(VizEvent.of(VizEvent.EventType.COMPARE,
                "find(" + la + ") = " + LABELS[ra] + ", find(" + lb + ") = " + LABELS[rb])),
                "Find 两根——相同 = 已在同一集合，不同 = 合并");
        if (ra == rb) {
            emit(5, List.of(VizEvent.of(VizEvent.EventType.COMPARE,
                    la + " 和 " + lb + " 已在同一集合")), "已在同一集合，跳过");
            return;
        }
        parent[rb] = ra;
        emit(6, List.of(VizEvent.of(VizEvent.EventType.SWAP,
                "union(" + la + "," + lb + ")：" + LABELS[rb] + " 的根指向 " + LABELS[ra],
                "@" + (ra + 1), "@" + (rb + 1))),
                "合并集合：一棵树的根挂到另一棵树的根上——森林少一棵树");
        findCur = -1;
    }

    private void emit(int line, List<VizEvent> events, String why) {
        StackFrame frame = new StackFrame("unionFind", Map.of(), null);
        Map<String, Layer> layers = new LinkedHashMap<>();
        layers.put("mem.stack", new StackLayer(List.of(frame)));
        layers.put("mem.heap", new HeapLayer(snapshotHeap()));
        layers.put("ds.graph", snapshotGraph());
        steps.add(new Step(steps.size(), Loc.line(line, why),
                Collections.unmodifiableMap(layers), List.copyOf(events), new Intent(why, INVARIANT)));
    }

    private Heap snapshotHeap() {
        Map<String, Value> fields = new LinkedHashMap<>();
        for (int i = 0; i < LABELS.length; i++) {
            fields.put(LABELS[i] + ".parent", Value.prim(LABELS[parent[i]]));
        }
        HeapObject obj = new HeapObject("@1", "parent", "UnionFind",
                Collections.unmodifiableMap(fields), List.of());
        Map<String, HeapObject> objs = new LinkedHashMap<>();
        objs.put("@1", obj);
        return new Heap(Collections.unmodifiableMap(objs));
    }

    private GraphView snapshotGraph() {
        List<GraphVertex> verts = new ArrayList<>();
        for (int i = 0; i < LABELS.length; i++) {
            verts.add(new GraphVertex(nodeId(i), LABELS[i], 0.5, 0.5));
        }
        List<GraphEdge> edges = new ArrayList<>();
        for (int i = 0; i < LABELS.length; i++) {
            if (parent[i] != i) {
                edges.add(new GraphEdge(nodeId(i), nodeId(parent[i]), 0));
            }
        }
        return new GraphView("并查集", Collections.unmodifiableList(verts),
                Collections.unmodifiableList(edges),
                findCur >= 0 ? nodeId(findCur) : null,
                List.of(), Map.of(), null, null, List.of());
    }

    private String nodeId(int idx) { return "@" + (idx + 1); }

    private Trace.TraceMeta meta() {
        return new Trace.TraceMeta(id(), title(), codeResource(), "6 节点 4 次 union", 6);
    }

    @Override
    public com.algoviz.engine.Complexity complexity() {
        return new com.algoviz.engine.Complexity("O(α(n)) ≈ O(1) 含路径压缩", "O(n) parent 数组",
                "路径压缩 + 按秩合并 → 反阿克曼函数 α(n) < 5（对任何实际 n 都是常数）");
    }
}
