package com.algoviz.engine.graph;

import com.algoviz.engine.OrderedMaps;
import com.algoviz.engine.Algorithm;
import com.algoviz.engine.Intent;
import com.algoviz.engine.Complexity;
import com.algoviz.engine.Layer;
import com.algoviz.engine.Loc;
import com.algoviz.engine.Step;
import com.algoviz.engine.Value;
import com.algoviz.engine.VizEvent;
import com.algoviz.engine.ds.ArrayView;
import com.algoviz.engine.mem.Heap;
import com.algoviz.engine.mem.HeapLayer;
import com.algoviz.engine.mem.HeapObject;
import com.algoviz.engine.mem.StackFrame;
import com.algoviz.engine.mem.StackLayer;
import com.algoviz.engine.trace.Trace;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Prim 最小生成树：逐步扩展，每次选最小权边连接新节点（用数组表示边的选择过程）。 */
public final class PrimDemo implements Algorithm {

    private static final String SCHEMA = "1";
    private static final String INVARIANT =
            "每次选最小权边连接已选/未选集合——MST 性质保证全局最优";

    private static final String[] LABELS = {"A", "B", "C", "D", "E", "F"};
    private static final int[][] EDGES = {
            {0, 1, 4}, {0, 2, 2}, {1, 2, 1}, {1, 3, 5}, {2, 3, 8}, {2, 4, 10}, {3, 4, 2}, {3, 5, 6}, {4, 5, 3}
    };
    private static final int E = EDGES.length;

    private final List<Step> steps = new ArrayList<>();
    private final boolean[] inMST = new boolean[6];
    private final List<Integer> mstEdges = new ArrayList<>();   // EDGES 下标
    private String curEdge = "";

    @Override public String id() { return "graph.prim"; }
    @Override public String title() { return "Prim MST"; }
    @Override public String codeResource() { return "/code/PrimDemo.java"; }
    @Override public String docResource() { return "/markdown/prim.md"; }

    @Override
    public Trace produce() {
        inMST[0] = true;
        emit(3, List.of(VizEvent.of(VizEvent.EventType.INIT, "A 入 MST")),
                "从 A 开始逐步扩展——每次选最小权边连接新节点");
        while (mstEdges.size() < 5) {
            int bestW = Integer.MAX_VALUE, bestIdx = -1;
            for (int i = 0; i < E; i++) {
                int u = EDGES[i][0], v = EDGES[i][1];
                if (inMST[u] && !inMST[v] && EDGES[i][2] < bestW) { bestW = EDGES[i][2]; bestIdx = i; }
                if (inMST[v] && !inMST[u] && EDGES[i][2] < bestW) { bestW = EDGES[i][2]; bestIdx = i; }
            }
            if (bestIdx == -1) break;
            int bf = EDGES[bestIdx][0], bt = EDGES[bestIdx][1];
            curEdge = LABELS[bf] + "-" + LABELS[bt] + " w=" + bestW;
            inMST[bf] = true; inMST[bt] = true;
            mstEdges.add(bestIdx);
            emit(5, List.of(VizEvent.of(VizEvent.EventType.MARK_SORTED,
                    curEdge + " → MST 边", "e:" + bestIdx)),
                    "贪心：已选/未选集合间最小权边——MST 性质保证全局最优");
        }
        curEdge = null;
        emit(8, List.of(VizEvent.of(VizEvent.EventType.RETURN,
                "MST 完成：" + mstEdges.size() + " 条边")), "最小生成树 = V-1 条边");
        return new Trace(SCHEMA, meta(), steps);
    }

    private void emit(int line, List<VizEvent> events, String why) {
        StackFrame frame = new StackFrame("prim",
                OrderedMaps.of("mstEdges", Value.prim(String.valueOf(mstEdges.size())),
                       "curEdge", Value.prim(curEdge == null ? "-" : curEdge)), null);
        Map<String, Layer> layers = new LinkedHashMap<>();
        layers.put("mem.stack", new StackLayer(List.of(frame)));
        layers.put("mem.heap", new com.algoviz.engine.mem.HeapLayer(snapshotHeap()));
        layers.put("ds.array", snapshotArray());
        steps.add(new Step(steps.size(), Loc.line(line, why),
                Collections.unmodifiableMap(layers), List.copyOf(events), new Intent(why, INVARIANT)));
    }

    private Heap snapshotHeap() {
        Map<String, HeapObject> objs = new LinkedHashMap<>();
        Map<String, Value> fields = new LinkedHashMap<>();
        fields.put("MST 边数", Value.prim(mstEdges.size()));
        if (curEdge != null) fields.put("curEdge", Value.prim(curEdge));
        objs.put("@1", new HeapObject("@1", "prim", "PrimState",
                Collections.unmodifiableMap(fields), List.of()));
        return new Heap(Collections.unmodifiableMap(objs));
    }

    private ArrayView snapshotArray() {
        List<Integer> vals = new ArrayList<>();
        for (int[] e : EDGES) { vals.add(e[2]); }   // 权重列表
        return new ArrayView("@1", "edges", List.copyOf(vals),
                null, null, null, null, E, -1);
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
        return new Complexity("O(V²) 简单版 / O(E log V) 堆版", "O(V) inMST 数组",
                "每轮扫描全部边找最小 → V²。用优先队列可到 E log V。贪心保证 MST 全局最优");
    }
}
