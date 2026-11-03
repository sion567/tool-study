package com.algoviz.engine.graph;

import com.algoviz.engine.Complexity;
import com.algoviz.engine.Algorithm;
import com.algoviz.engine.Intent;
import com.algoviz.engine.Layer;
import com.algoviz.engine.Loc;
import com.algoviz.engine.Step;
import com.algoviz.engine.Value;
import com.algoviz.engine.VizEvent;
import com.algoviz.engine.ds.GraphView;
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
 * Dijkstra 生产者：贪心出圈 + 松弛（涟漪扩散）。
 * 行号常量与 /code/Dijkstra.java 对齐；距离表每个节点头顶跳动，出圈即绿。
 * 说明：蓝图原定 WebView+HTML，为接入预测模式与 golden 测试改为同架构 Canvas 渲染（原则 4）。
 */
public final class Dijkstra implements Algorithm {

    private static final String SCHEMA = "1";
    private static final String INVARIANT =
            "已出圈节点的距离永远不再变——出圈时的 dist 就是它真正的最短路";

    private static final int L_INIT = 3;
    private static final int L_WHILE = 4;
    private static final int L_PICK = 5;
    private static final int L_SETTLE = 6;
    private static final int L_FOR = 7;
    private static final int L_IF = 8;
    private static final int L_RELAX = 9;
    private static final int L_RET = 13;

    private static final String[] LABELS = {"A", "B", "C", "D", "E", "F"};
    private static final double[][] POS = {
            {0.10, 0.45}, {0.32, 0.16}, {0.32, 0.76}, {0.58, 0.45}, {0.78, 0.78}, {0.90, 0.20}
    };
    private final int[][] edges;

    public Dijkstra() {
        this(defaultWeights());
    }

    /** 随机权重：结构不变，权重 1~20 随机。 */
    public Dijkstra(int[] weights) {
        edges = new int[SampleGraph.EDGES.length][];
        for (int i = 0; i < edges.length; i++) {
            edges[i] = new int[]{SampleGraph.EDGES[i][0], SampleGraph.EDGES[i][1], weights[i]};
        }
    }

    private static int[] defaultWeights() {
        int[] w = new int[SampleGraph.EDGES.length];
        for (int i = 0; i < w.length; i++) {
            w[i] = SampleGraph.EDGES[i][2];
        }
        return w;
    }

    private final List<Step> steps = new ArrayList<>();
    private final Map<String, Integer> dist = new LinkedHashMap<>();
    private final List<String> settled = new ArrayList<>();
    private String active;
    private String relaxFrom;
    private String relaxTo;

    @Override
    public String id() {
        return "graph.dijkstra";
    }

    @Override
    public String title() {
        return "Dijkstra";
    }

    @Override
    public String codeResource() {
        return "/code/Dijkstra.java";
    }

    @Override
    public String docResource() {
        return "/markdown/dijkstra.md";
    }

    @Override
    public Trace produce() {
        // 重复 produce：清零全部状态（Registry 缓存实例）
        dist.clear();
        settled.clear();
        active = null;
        relaxFrom = null;
        relaxTo = null;
        steps.clear();
        // L3: dist[src] = 0
        dist.put("A", 0);
        emit(L_INIT, List.of(VizEvent.of(VizEvent.EventType.INIT,
                "dist[A] ← 0（起点入圈），其余 ∞", "A")),
                "起点距离 0，其他点还够不着：∞");

        while (settled.size() < LABELS.length) {
            // L5: 挑未定点里 dist 最小的
            String u = pickMin();
            emit(L_PICK, List.of(VizEvent.of(VizEvent.EventType.COMPARE,
                    "未定点里 dist 最小的是 " + u + "(" + dist.get(u) + ") → 涟漪下一个圆心", u)),
                    "贪心：当前够得着的点里最近的那个，它的距离不可能再被绕路改善了");
            // L6: settled[u] = true
            settled.add(u);
            active = u;
            emit(L_SETTLE, List.of(VizEvent.of(VizEvent.EventType.MARK_SORTED,
                    u + " 出圈：dist[" + u + "]=" + dist.get(u) + " 定死（不变量达成）", u)),
                    "出圈即定死——这就是 Dijkstra 只对非负权图成立的全部秘密");
            // L7-L11: 松弛所有邻居
            for (int[] e : edges) {
                for (int dir = 0; dir < 2; dir++) {
                    String from = label(e[dir]);
                    String to = label(e[1 - dir]);
                    if (!from.equals(u)) {
                        continue;
                    }
                    relax(from, to, e[2]);
                }
            }
            relaxFrom = null;
            relaxTo = null;
            active = null;
        }

        emit(L_RET, List.of(VizEvent.of(VizEvent.EventType.RETURN,
                "全部出圈：" + distString())), "所有节点的最短路都定了");
        return new Trace(SCHEMA, meta(new int[0]), steps);
    }

    private void relax(String from, String to, int weight) {
        Integer du = dist.get(from);
        Integer dv = dist.get(to);
        int candidate = du + weight;
        relaxFrom = from;
        relaxTo = to;
        if (dv == null || candidate < dv) {
            emit(L_IF, List.of(VizEvent.of(VizEvent.EventType.COMPARE,
                    "dist[" + from + "]+" + weight + " = " + candidate
                            + (dv == null ? " < ∞" : " < dist[" + to + "]=" + dv) + " → 松弛",
                    from, to)),
                    "经过 " + from + " 绕过去更近？涟漪推到 " + to);
            dist.put(to, candidate);
            emit(L_RELAX, List.of(VizEvent.of(VizEvent.EventType.SET,
                    "dist[" + to + "] ← " + candidate, to)),
                    "松弛成功：到 " + to + " 的已知最短路被更新");
        } else {
            emit(L_IF, List.of(VizEvent.of(VizEvent.EventType.COMPARE,
                    "dist[" + from + "]+" + weight + " = " + candidate
                            + " ≥ dist[" + to + "]=" + dv + " → 不动", from, to)),
                    "绕路不划算：已知最短路保住");
        }
        relaxFrom = null;
        relaxTo = null;
    }

    private String pickMin() {
        String best = null;
        int bestDist = Integer.MAX_VALUE;
        for (String s : LABELS) {
            Integer d = dist.get(s);
            if (!settled.contains(s) && d != null && d < bestDist) {
                best = s;
                bestDist = d;
            }
        }
        return best;
    }

    private String distString() {
        StringBuilder sb = new StringBuilder();
        for (String s : LABELS) {
            if (sb.length() > 0) {
                sb.append(' ');
            }
            sb.append(s).append(dist.get(s));
        }
        return sb.toString();
    }

    private String label(int idx) {
        return LABELS[idx];
    }

    private void emit(int line, List<VizEvent> events, String why) {
        StackFrame frame = new StackFrame("dijkstra(src=A)",
                Map.of("u", Value.prim(active == null ? "-" : active)), null);
        Map<String, Layer> layers = new LinkedHashMap<>();
        layers.put("mem.stack", new StackLayer(List.of(frame)));
        layers.put("mem.heap", new HeapLayer(snapshotHeap()));
        layers.put("ds.graph", snapshotGraph());
        steps.add(new Step(steps.size(), Loc.line(line, why),
                Collections.unmodifiableMap(layers),
                List.copyOf(events), new Intent(why, INVARIANT)));
    }

    /** dist 表实物化为堆里的 DistanceTable 对象——它就是这道题的"堆"。 */
    private Heap snapshotHeap() {
        Map<String, Value> fields = new LinkedHashMap<>();
        for (String s : LABELS) {
            Integer d = dist.get(s);
            fields.put(s, d == null ? Value.NULL : Value.prim(d));
        }
        HeapObject table = new HeapObject("@1", "dist", "DistanceTable",
                Collections.unmodifiableMap(fields), List.of());
        Map<String, HeapObject> objs = new LinkedHashMap<>();
        objs.put("@1", table);
        return new Heap(Collections.unmodifiableMap(objs));
    }

    private GraphView snapshotGraph() {
        List<GraphView.GraphVertex> vertices = new ArrayList<>();
        for (int i = 0; i < LABELS.length; i++) {
            vertices.add(new GraphView.GraphVertex("@" + (i + 1), LABELS[i], POS[i][0], POS[i][1]));
        }
        List<GraphView.GraphEdge> edgeList = new ArrayList<>();
        for (int[] e : edges) {
            edgeList.add(new GraphView.GraphEdge("@" + (e[0] + 1), "@" + (e[1] + 1), e[2]));
        }
        Map<String, Integer> distCopy = new LinkedHashMap<>();
        for (String s : LABELS) {
            distCopy.put(s, dist.get(s)); // null = ∞
        }
        return new GraphView("Dijkstra", Collections.unmodifiableList(vertices),
                Collections.unmodifiableList(edgeList),
                active == null ? null : nodeId(active),
                settled.stream().map(this::nodeId).collect(java.util.stream.Collectors.toList()),
                Collections.unmodifiableMap(distCopy),
                relaxFrom == null ? null : nodeId(relaxFrom),
                relaxTo == null ? null : nodeId(relaxTo),
                List.of());
    }

    private String nodeId(String label) {
        for (int i = 0; i < LABELS.length; i++) {
            if (LABELS[i].equals(label)) {
                return "@" + (i + 1);
            }
        }
        return null;
    }

    @Override
    public Complexity complexity() {
        return new Complexity("O(V²) 简单版 / O((V+E) log V) 堆版", "O(V) 距离表", "V 轮出圈 × 每轮扫邻居 → V²。优先队列把「挑最小」从 O(V) 降到 O(log V)。负权边会破坏出圈定死的不变量 → 不适用");
    }
}
