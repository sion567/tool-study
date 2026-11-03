package com.algoviz.engine.graph;

import com.algoviz.engine.Algorithm;
import com.algoviz.engine.Complexity;
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

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 图的 BFS（广度优先）：队列 + 逐层扩散——入队即编号。
 * 不变量：队列里的节点按「离起点的跳数」分层排列——先入队的先扩散（这就是「广度」）。
 */
public final class BfsDemo implements Algorithm {

    private static final String SCHEMA = "1";
    private static final String INVARIANT =
            "队列按层扩散：先入队的先出队——编号顺序就是离起点的跳数顺序（最短边数路径）";

    private static final int L_OFFER_SRC = 4;
    private static final int L_POLL = 6;
    private static final int L_VISIT = 7;
    private static final int L_CHECK = 9;
    private static final int L_OFFER = 11;
    private static final int L_END = 14;

    private final List<Step> steps = new ArrayList<>();
    private final Deque<String> queue = new ArrayDeque<>();
    private final Set<String> visited = new LinkedHashSet<>();
    private final List<String> order = new ArrayList<>();
    private String active;
    private String checkFrom;
    private String checkTo;

    private final Map<Integer, List<Integer>> adj;

    public BfsDemo() {
        this(0);
    }

    /** 随机邻居顺序：seed 决定洗牌结果，遍历序随之改变。 */
    public BfsDemo(long seed) {
        adj = new LinkedHashMap<>();
        java.util.Random r = new java.util.Random(seed);
        for (int i = 0; i < SampleGraph.LABELS.length; i++) {
            List<Integer> ns = SampleGraph.neighbors(i);
            java.util.Collections.shuffle(ns, r);
            adj.put(i, ns);
        }
    }

    @Override public String id() { return "graph.bfs"; }
    @Override public String title() { return "图的 BFS"; }
    @Override public String codeResource() { return "/code/GraphBFS.java"; }
    @Override public String docResource() { return "/markdown/graph-bfs.md"; }

    @Override
    public Trace produce() {
        queue.clear();
        visited.clear();
        order.clear();
        active = null;
        checkFrom = null;
        checkTo = null;

        // L4: 起点入队
        enqueue("A");
        emit(L_OFFER_SRC, List.of(VizEvent.of(VizEvent.EventType.INIT,
                "A 入队（编号 1）——起点上桌", "A")), "起点入队，同时标记已访问：入队即编号");
        while (!queue.isEmpty()) {
            String u = queue.poll();
            active = u;
            // L6: 出队
            emit(L_POLL, List.of(VizEvent.of(VizEvent.EventType.SET,
                    u + " 出队（队列剩 " + queue.size() + "）", u)),
                    "队首出队——这一层最先进来的先扩散");
            // L7: 访问
            emit(L_VISIT, List.of(VizEvent.of(VizEvent.EventType.VISIT,
                    "访问 " + u + "（编号 " + (order.indexOf(u) + 1) + "）", u)),
                    "出队即访问：绿 = 已访问，数字 = 编号");
            // 邻居检查
            for (int n : adj.get(SampleGraph.indexOf(u))) {
                String v = SampleGraph.label(n);
                checkFrom = u;
                checkTo = v;
                if (visited.contains(v)) {
                    emit(L_CHECK, List.of(VizEvent.of(VizEvent.EventType.COMPARE,
                            v + " 已访问 → 跳过", u, v)),
                            "标记在入队时就打了：队列里不会有重复节点");
                } else {
                    emit(L_CHECK, List.of(VizEvent.of(VizEvent.EventType.COMPARE,
                            v + " 未访问 → 入队", u, v)),
                            "第一次见到 " + v + "：标记 + 入队一步完成");
                    enqueue(v);
                    emit(L_OFFER, List.of(VizEvent.of(VizEvent.EventType.SET,
                            v + " 入队（编号 " + (order.indexOf(v) + 1) + "）", v)),
                            "下一层进队列——等这一层全部扩散完才轮到它们");
                }
                checkFrom = null;
                checkTo = null;
            }
            active = null;
        }
        emit(L_END, List.of(VizEvent.of(VizEvent.EventType.RETURN,
                "队列空：全部 " + visited.size() + " 个节点访问完毕，顺序 " + String.join("→", order))),
                "BFS 完成：编号顺序 = 按层扩散顺序");
        return new Trace(SCHEMA, meta(), steps);
    }

    private void enqueue(String label) {
        queue.offer(label);
        visited.add(label);
        order.add(label);
    }

    private void emit(int line, List<VizEvent> events, String why) {
        StackFrame frame = new StackFrame("bfs(src=A)", Map.of("q", Value.prim(queueStr())), null);
        Map<String, Layer> layers = new LinkedHashMap<>();
        layers.put("mem.stack", new StackLayer(List.of(frame)));
        layers.put("mem.heap", new HeapLayer(snapshotHeap()));
        layers.put("ds.graph", snapshotGraph());
        steps.add(new Step(steps.size(), Loc.line(line, why),
                Collections.unmodifiableMap(layers), List.copyOf(events), new Intent(why, INVARIANT)));
    }

    private String queueStr() {
        return "[" + String.join(", ", queue) + "]";
    }

    private Heap snapshotHeap() {
        Map<String, Value> fields = new LinkedHashMap<>();
        fields.put("queue", Value.prim(queueStr()));
        for (String s : SampleGraph.LABELS) {
            fields.put(s, Value.prim(visited.contains(s) ? "已访问" : "未访问"));
        }
        HeapObject table = new HeapObject("@1", "q", "BfsState",
                Collections.unmodifiableMap(fields), List.of());
        Map<String, HeapObject> objs = new LinkedHashMap<>();
        objs.put("@1", table);
        return new Heap(Collections.unmodifiableMap(objs));
    }

    private GraphView snapshotGraph() {
        List<String> settled = visited.stream()
                .map(s -> SampleGraph.nodeId(SampleGraph.indexOf(s)))
                .collect(java.util.stream.Collectors.toList());
        List<String> orderIds = new ArrayList<>();
        for (String s : order) {
            orderIds.add(SampleGraph.nodeId(SampleGraph.indexOf(s)));
        }
        return new GraphView("BFS", SampleGraph.vertices(), SampleGraph.edges(),
                active == null ? null : SampleGraph.nodeId(SampleGraph.indexOf(active)),
                settled, Map.of(), null, null, orderIds);
    }

    private Trace.TraceMeta meta() {
        return new Trace.TraceMeta(id(), title(), codeResource(), "src=A", 6);
    }

    @Override
    public Complexity complexity() {
        return new Complexity("O(V + E)", "O(V) 队列", "每个节点入队/出队一次 + 每条边检查一次 → V + E。边权全为 1 时 = 最短路（分层扩散）");
    }
}
