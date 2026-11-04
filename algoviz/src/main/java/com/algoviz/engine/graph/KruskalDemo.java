package com.algoviz.engine.graph;

import com.algoviz.engine.OrderedMaps;
import com.algoviz.engine.Algorithm;
import com.algoviz.engine.Layer;
import com.algoviz.engine.Complexity;
import com.algoviz.engine.Intent;
import com.algoviz.engine.Loc;
import com.algoviz.engine.Step;
import com.algoviz.engine.Value;
import com.algoviz.engine.VizEvent;
import com.algoviz.engine.mem.Heap;
import com.algoviz.engine.mem.HeapLayer;
import com.algoviz.engine.mem.HeapObject;
import com.algoviz.engine.mem.StackFrame;
import com.algoviz.engine.mem.StackLayer;
import com.algoviz.engine.ds.ArrayView;
import com.algoviz.engine.trace.Trace;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Kruskal MST：按权重排序 + 并查集判环——不闭环才加边。 */
public final class KruskalDemo implements Algorithm {

    private static final String SCHEMA = "1";
    private static final String INVARIANT =
            "按权重排序 + 并查集判环——不闭环才加边，加完 V-1 条就是 MST";

    private static final String[] LABELS = {"A", "B", "C", "D", "E", "F"};
    private static final int[][] EDGES = {
            {0, 1, 4}, {0, 2, 2}, {1, 2, 1}, {1, 3, 5}, {2, 3, 8}, {2, 4, 10}, {3, 4, 2}, {3, 5, 6}, {4, 5, 3}
    };
    private static final int E = EDGES.length;
    private static final int N = LABELS.length;

    private final List<Step> steps = new ArrayList<>();
    private final int[] parent = new int[N];
    private final List<Integer> mstEdges = new ArrayList<>();
    private int curEdge = -1;

    @Override public String id() { return "graph.kruskal"; }
    @Override public String title() { return "Kruskal MST"; }
    @Override public String codeResource() { return "/code/KruskalDemo.java"; }
    @Override public String docResource() { return "/markdown/kruskal.md"; }

    @Override
    public Trace produce() {
        for (int i = 0; i < N; i++) parent[i] = i;
        // 按权重排序
        Integer[] sorted = new Integer[E];
        for (int i = 0; i < E; i++) sorted[i] = i;
        java.util.Arrays.sort(sorted, (a, b) -> EDGES[a][2] - EDGES[b][2]);
        emit(3, List.of(VizEvent.of(VizEvent.EventType.INIT,
                "按权重排序：" + edgeStr(sorted[0]) + " ≤ " + edgeStr(sorted[1]) + " ≤ …")),
                "Kruskal：先按权重排序所有边——逐个尝试，用并查集判环");
        for (int idx : sorted) {
            curEdge = idx;
            int u = find(EDGES[idx][0]), v = find(EDGES[idx][1]);
            if (u != v) {
                parent[v] = u;
                mstEdges.add(idx);
                emit(6, List.of(VizEvent.of(VizEvent.EventType.MARK_SORTED,
                        "不同集合 → 加入 MST（" + edgeStr(idx) + "）", "e:" + idx)),
                        "两端不同根 → 不闭环 → 安全加入 MST");
                if (mstEdges.size() == N - 1) {
                    emit(8, List.of(VizEvent.of(VizEvent.EventType.RETURN,
                            "MST 完成：" + mstEdges.size() + " 条边")), "V-1 条边 = 生成树");
                    return new Trace(SCHEMA, meta(), steps);
                }
            } else {
                emit(7, List.of(VizEvent.of(VizEvent.EventType.COMPARE,
                        edgeStr(idx) + " → 同一集合，跳过（会闭环！）")),
                        "两端同根 → 加这条边会形成环 → 跳过");
            }
        }
        emit(8, List.of(VizEvent.of(VizEvent.EventType.RETURN,
                "MST 完成：" + mstEdges.size() + " 条边")), "最小生成树构建完成");
        return new Trace(SCHEMA, meta(), steps);
    }

    private int find(int x) {
        while (parent[x] != x) x = parent[x];
        return x;
    }

    private String edgeStr(int idx) {
        return LABELS[EDGES[idx][0]] + "-" + LABELS[EDGES[idx][1]] + " w=" + EDGES[idx][2];
    }

    private void emit(int line, List<VizEvent> events, String why) {
        StackFrame frame = new StackFrame("kruskal",
                OrderedMaps.of("mstEdges", Value.prim(mstEdges.size()),
                       "curEdge", Value.prim(curEdge >= 0 ? edgeStr(curEdge) : "-")), null);
        Map<String, Layer> layers = new LinkedHashMap<>();
        layers.put("mem.stack", new StackLayer(List.of(frame)));
        layers.put("mem.heap", new HeapLayer(snapshotHeap()));
        layers.put("ds.array", snapshotArray());
        steps.add(new Step(steps.size(), Loc.line(line, why),
                Collections.unmodifiableMap(layers), List.copyOf(events), new Intent(why, INVARIANT)));
    }

    private Heap snapshotHeap() {
        Map<String, HeapObject> objs = new LinkedHashMap<>();
        Map<String, Value> fields = new LinkedHashMap<>();
        for (int i = 0; i < N; i++) {
            fields.put(LABELS[i] + ".parent", Value.prim(LABELS[parent[i]]));
        }
        objs.put("@1", new HeapObject("@1", "uf", "UnionFind",
                Collections.unmodifiableMap(fields), List.of()));
        return new Heap(Collections.unmodifiableMap(objs));
    }

    private ArrayView snapshotArray() {
        List<Integer> vals = new ArrayList<>();
        for (int[] e : EDGES) vals.add(e[2]);
        return new ArrayView("@1", "weights", List.copyOf(vals),
                curEdge >= 0 ? curEdge : null, null, null, null, E, -1);
    }

    private Trace.TraceMeta meta() {
        return new Trace.TraceMeta(id(), title(), codeResource(), "6n 9e", E);
    }

    @Override
    public java.util.List<String> dataLines() {
        return java.util.List.of("int[][] edges = " + java.util.Arrays.deepToString(EDGES) + ";");
    }

    @Override
    public Complexity complexity() {
        return new Complexity("O(E log E) 排序主导", "O(V) 并查集数组",
                "排序 O(E log E) + 并查集近乎 O(1) → 总体 O(E log E)。比 Prim 更适合稀疏图");
    }
}
