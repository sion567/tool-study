package com.algoviz.engine.graph;

import com.algoviz.engine.OrderedMaps;
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
 * 拓扑排序（Kahn 算法/BFS 版）：入度表驱动，入度 0 的节点逐个脱落。
 * 不变量：入度减到 0 的节点 → 出圈，它指向的邻居入度各减 1。
 */
public final class TopoSort implements Algorithm {

    private static final String SCHEMA = "1";
    private static final String INVARIANT =
            "入度 0 → 没有任何前置依赖 → 可以安全出圈（结果序列就是合法的执行顺序）";

    private static final String[] LABELS = {"A", "B", "C", "D", "E", "F"};
    private static final double[][] POS = {
            {0.10, 0.45}, {0.32, 0.16}, {0.32, 0.76}, {0.58, 0.45}, {0.78, 0.78}, {0.90, 0.20}
    };
    // 有向无权边（from → to），DAG 保证无环
    private static final int[][] DIR_EDGES = {{0, 1}, {0, 2}, {1, 3}, {2, 3}, {1, 4}, {3, 4}, {3, 5}, {2, 5}};

    private final List<Step> steps = new ArrayList<>();
    private final int[] inDeg = new int[LABELS.length];
    private final List<String> queue = new ArrayList<>();
    private final List<String> result = new ArrayList<>();
    private String active;
    private String relaxFrom, relaxTo;

    @Override public String id() { return "graph.toposort"; }
    @Override public String title() { return "拓扑排序"; }
    @Override public String codeResource() { return "/code/TopoSort.java"; }
    @Override public String docResource() { return "/markdown/topo-sort.md"; }

    @Override
    public Trace produce() {
        // 初始化入度
        for (int[] e : DIR_EDGES) inDeg[e[1]]++;
        emit(3, List.of(VizEvent.of(VizEvent.EventType.INIT,
                "入度表：" + inDegStr())), "入度 = 指向我的边数，入度 0 = 无前置依赖");
        // 入度 0 的先入队
        for (int i = 0; i < LABELS.length; i++) {
            if (inDeg[i] == 0) { queue.add(LABELS[i]); }
        }
        emit(4, List.of(VizEvent.of(VizEvent.EventType.INIT,
                "队列初始化：" + queueStr())), "入度 0 的节点先入队——它们没有任何前置");
        while (!queue.isEmpty()) {
            String u = queue.remove(0);
            active = u;
            result.add(u);
            emit(6, List.of(VizEvent.of(VizEvent.EventType.MARK_SORTED,
                    u + " 出队 → 加入结果（第 " + result.size() + " 个）", u)),
                    "出圈：入度 0 = 没有任何前置依赖 → 可以安全执行");
            // 邻居入度减 1
            for (int[] e : DIR_EDGES) {
                if (LABELS[e[0]].equals(u)) {
                    String v = LABELS[e[1]];
                    inDeg[e[1]]--;
                    relaxFrom = u; relaxTo = v;
                    if (inDeg[e[1]] == 0) {
                        queue.add(v);
                        emit(9, List.of(VizEvent.of(VizEvent.EventType.SET,
                                v + " 入度 → 0，入队", v)),
                                v + " 的所有前置都出圈了 → 它也可以出圈了");
                    } else {
                        emit(9, List.of(VizEvent.of(VizEvent.EventType.COMPARE,
                                v + " 入度 → " + inDeg[e[1]] + "（还有前置）", u, v)),
                                "入度还没归零——等前面的先出圈");
                    }
                    relaxFrom = null; relaxTo = null;
                }
            }
            active = null;
        }
        if (result.size() < LABELS.length) {
            emit(12, List.of(VizEvent.of(VizEvent.EventType.RETURN,
                    "结果不完整 → 图有环！")), "有环 → 有些节点的入度永远 > 0 → 无法拓扑排序");
        } else {
            emit(12, List.of(VizEvent.of(VizEvent.EventType.RETURN,
                    "拓扑序：" + String.join(" → ", result))), "DAG 的合法拓扑序");
        }
        return new Trace(SCHEMA, meta(), steps);
    }

    private String inDegStr() {
        StringBuilder sb = new StringBuilder("{");
        for (int i = 0; i < LABELS.length; i++) {
            if (i > 0) sb.append(", ");
            sb.append(LABELS[i]).append(":").append(inDeg[i]);
        }
        return sb.append("}").toString();
    }

    private String queueStr() {
        return "[" + String.join(", ", queue) + "]";
    }

    private void emit(int line, List<VizEvent> events, String why) {
        StackFrame frame = new StackFrame("topoSort",
                OrderedMaps.of("queue", Value.prim(queueStr()),
                       "result", Value.prim(String.join("→", result)),
                       "inDeg", Value.prim(inDegStr())), null);
        Map<String, Layer> layers = new LinkedHashMap<>();
        layers.put("mem.stack", new StackLayer(List.of(frame)));
        layers.put("mem.heap", new HeapLayer(snapshotHeap()));
        layers.put("ds.graph", snapshotGraph());
        steps.add(new Step(steps.size(), Loc.line(line, why),
                Collections.unmodifiableMap(layers), List.copyOf(events), new Intent(why, INVARIANT)));
    }

    private Heap snapshotHeap() {
        Map<String, Value> fields = new LinkedHashMap<>();
        fields.put("queue", Value.prim(queueStr()));
        fields.put("inDeg", Value.prim(inDegStr()));
        HeapObject obj = new HeapObject("@1", "state", "TopoState",
                Collections.unmodifiableMap(fields), List.of());
        Map<String, HeapObject> objs = new LinkedHashMap<>();
        objs.put("@1", obj);
        return new Heap(Collections.unmodifiableMap(objs));
    }

    private String nodeId(int idx) { return "@" + (idx + 1); }

    private String nodeId(String label) {
        for (int i = 0; i < LABELS.length; i++) {
            if (LABELS[i].equals(label)) return "@" + (i + 1);
        }
        return null;
    }

    private GraphView snapshotGraph() {
        List<GraphVertex> verts = new ArrayList<>();
        for (int i = 0; i < LABELS.length; i++) {
            verts.add(new GraphVertex(nodeId(i), LABELS[i], POS[i][0], POS[i][1]));
        }
        List<GraphEdge> edges = new ArrayList<>();
        for (int[] e : DIR_EDGES) {
            edges.add(new GraphEdge(nodeId(e[0]), nodeId(e[1]), 0));
        }
        List<String> settled = new ArrayList<>();
        for (String label : result) {
            settled.add(nodeId(label));
        }
        return new GraphView("拓扑排序", Collections.unmodifiableList(verts),
                Collections.unmodifiableList(edges),
                active == null ? null : nodeId(LABELS[active.charAt(0) - 'A']),
                Collections.unmodifiableList(settled), Map.of(),
                relaxFrom == null ? null : nodeId(relaxFrom),
                relaxTo == null ? null : nodeId(relaxTo),
                List.copyOf(settled));
    }

    private Trace.TraceMeta meta() {
        return new Trace.TraceMeta(id(), title(), codeResource(), "DAG 6 节点", LABELS.length);
    }
}
