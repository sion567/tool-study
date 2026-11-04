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
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Bellman-Ford：逐边松弛 V-1 轮——可以处理负权边（Dijkstra 不行）。
 * 不变量：每轮松弛后，dist[u] ≤ 从 src 经过 ≤ k 条边到 u 的最短距离（k = 当前轮数）。
 */
public final class BellmanFordDemo implements Algorithm {

    private static final String SCHEMA = "1";
    private static final String INVARIANT =
            "第 k 轮松弛后，dist[u] ≤ 从 src 走 ≤ k 条边到 u 的最短距离——逐轮扩展可到达范围";

    private static final String[] LABELS = {"A", "B", "C", "D", "E"};
    private static final double[][] POS = {
            {0.12, 0.40}, {0.38, 0.16}, {0.38, 0.72}, {0.66, 0.40}, {0.90, 0.16}
    };
    // 有向带权边（含负权：Dijkstra 不适用，Bellman-Ford 可以）
    private static final int[][] DIR_EDGES = {
            {0, 1, 6}, {0, 2, 7}, {1, 2, 8}, {1, 3, 5}, {1, 3, -4}, {2, 3, -3}, {2, 4, 9}, {3, 4, 7}
    };

    private final List<Step> steps = new ArrayList<>();
    private final int[] dist = new int[LABELS.length];
    private boolean hasNegCycle;

    @Override public String id() { return "graph.bellmanford"; }
    @Override public String title() { return "Bellman-Ford"; }
    @Override public String codeResource() { return "/code/BellmanFordDemo.java"; }
    @Override public String docResource() { return "/markdown/bellman-ford.md"; }

    @Override
    public Trace produce() {
        Arrays.fill(dist, Integer.MAX_VALUE);
        dist[0] = 0;
        emit(3, List.of(VizEvent.of(VizEvent.EventType.INIT,
                "dist[A]=0，其余 ∞（负权边也 OK）")),
                "起点 0，其他 ∞——比 Dijkstra 多了逐边松弛的 V-1 轮");

        int V = LABELS.length;
        for (int round = 1; round < V; round++) {
            boolean changed = false;
            for (int[] e : DIR_EDGES) {
                int u = e[0], v = e[1], w = e[2];
                if (dist[u] == Integer.MAX_VALUE) continue;
                int nd = dist[u] + w;
                if (nd < dist[v]) {
                    dist[v] = nd;
                    changed = true;
                    emit(5, List.of(VizEvent.of(VizEvent.EventType.SET,
                            "第 " + round + " 轮：relax " + LABELS[u] + " → " + LABELS[v]
                                    + "  dist[" + LABELS[v] + "] = " + nd, LABELS[v])),
                            "松弛成功：经过 " + LABELS[u] + " 到 " + LABELS[v] + " 更近了");
                }
            }
            emit(6, List.of(VizEvent.of(VizEvent.EventType.MARK_SORTED,
                    "第 " + round + " 轮完成" + (changed ? "，有更新" : "，无更新（可提前退出）"))),
                    "一轮 = 逐边松弛全部 V-1 条边；无更新时可提前退出");
            if (!changed) break;
        }
        // 负环检测（额外一轮）
        for (int[] e : DIR_EDGES) {
            if (dist[e[0]] != Integer.MAX_VALUE && dist[e[0]] + e[2] < dist[e[1]]) {
                hasNegCycle = true;
                break;
            }
        }
        emit(10, List.of(VizEvent.of(VizEvent.EventType.RETURN,
                hasNegCycle ? "检测到负环！" : "无负环。最终 dist: " + distStr())),
                hasNegCycle ? "负环 = 可以无限绕圈减小距离 → 最短路无意义"
                        : "V-1 轮后全部收敛（无负环）");
        return new Trace(SCHEMA, meta(), steps);
    }

    private String distStr() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < LABELS.length; i++) {
            if (i > 0) sb.append(' ');
            sb.append(LABELS[i]).append('=').append(dist[i] == Integer.MAX_VALUE ? "∞" : dist[i]);
        }
        return sb.toString();
    }

    private void emit(int line, List<VizEvent> events, String why) {
        StackFrame frame = new StackFrame("bellmanFord(src=A)", Map.of(), null);
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
            fields.put(LABELS[i], Value.prim(dist[i] == Integer.MAX_VALUE ? "∞" : String.valueOf(dist[i])));
        }
        HeapObject obj = new HeapObject("@1", "dist", "DistTable",
                Collections.unmodifiableMap(fields), List.of());
        Map<String, HeapObject> objs = new LinkedHashMap<>();
        objs.put("@1", obj);
        return new Heap(Collections.unmodifiableMap(objs));
    }

    private GraphView snapshotGraph() {
        List<GraphVertex> verts = new ArrayList<>();
        for (int i = 0; i < LABELS.length; i++) {
            verts.add(new GraphVertex(nodeId(i), LABELS[i], POS[i][0], POS[i][1]));
        }
        List<GraphEdge> edges = new ArrayList<>();
        for (int[] e : DIR_EDGES) {
            edges.add(new GraphEdge(nodeId(e[0]), nodeId(e[1]), e[2]));
        }
        Map<String, Integer> distMap = new LinkedHashMap<>();
        for (int i = 0; i < LABELS.length; i++) {
            distMap.put(LABELS[i], dist[i] == Integer.MAX_VALUE ? null : dist[i]);
        }
        return new GraphView("Bellman-Ford", Collections.unmodifiableList(verts),
                Collections.unmodifiableList(edges), null, List.of(),
                Collections.unmodifiableMap(distMap), null, null, List.of());
    }

    private String nodeId(int idx) { return "@" + (idx + 1); }

    private Trace.TraceMeta meta() {
        return new Trace.TraceMeta(id(), title(), codeResource(), "5 节点 8 边含负权", LABELS.length);
    }

    @Override
    public com.algoviz.engine.Complexity complexity() {
        return new com.algoviz.engine.Complexity("O(V·E) 逐边松弛 V-1 轮", "O(V) 距离表",
                "V-1 轮 × 每轮 E 条边 = V·E。可以处理负权边（Dijkstra 不行）；负环 → 最短路无意义");
    }
}
