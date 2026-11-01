package com.algoviz.engine.graph;

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
 * 图的 DFS（深度优先）：栈 + 一条路走到黑——弹栈即访问，访问即编号。
 * 不变量：栈顶永远是「下一个要探的路岔」——走到底才回头。
 */
public final class DfsDemo implements Algorithm {

    private static final String SCHEMA = "1";
    private static final String INVARIANT =
            "一条路走到黑，走到底才回头——栈顶永远是下一个要探的路岔（与 BFS 只差一个数据结构）";

    private static final int L_PUSH_SRC = 4;
    private static final int L_POP = 6;
    private static final int L_SKIP = 7;
    private static final int L_MARK = 8;
    private static final int L_VISIT = 9;
    private static final int L_PUSH = 11;
    private static final int L_END = 14;

    private final List<Step> steps = new ArrayList<>();
    private final Deque<String> stack = new ArrayDeque<>();
    private final Set<String> visited = new LinkedHashSet<>();
    private final List<String> order = new ArrayList<>();
    private String active;
    private boolean skipping;

    private final Map<Integer, List<Integer>> adj;

    public DfsDemo() {
        this(0);
    }

    /** 随机邻居顺序：seed 决定洗牌结果，遍历序随之改变。 */
    public DfsDemo(long seed) {
        adj = new LinkedHashMap<>();
        java.util.Random r = new java.util.Random(seed);
        for (int i = 0; i < SampleGraph.LABELS.length; i++) {
            List<Integer> ns = SampleGraph.neighbors(i);
            java.util.Collections.shuffle(ns, r);
            adj.put(i, ns);
        }
    }

    @Override public String id() { return "graph.dfs"; }
    @Override public String title() { return "图的 DFS"; }
    @Override public String codeResource() { return "/code/GraphDFS.java"; }
    @Override public String docResource() { return "/markdown/graph-dfs.md"; }

    @Override
    public Trace produce() {
        stack.clear();
        visited.clear();
        order.clear();
        active = null;
        skipping = false;

        // L4: 起点入栈
        stack.push("A");
        emit(L_PUSH_SRC, List.of(VizEvent.of(VizEvent.EventType.INIT,
                "A 入栈——起点上桌", "A")), "起点入栈：DFS 的所有故事都发生在栈上");
        while (!stack.isEmpty()) {
            String u = stack.pop();
            active = u;
            if (visited.contains(u)) {
                skipping = true;
                // L7: 栈里可能有重复（同一个节点被多个路岔压过），跳过
                emit(L_SKIP, List.of(VizEvent.of(VizEvent.EventType.COMPARE,
                        u + " 已访问 → 跳过", u)),
                        "一个节点可能被压栈多次——弹出来时检查，已访问就扔");
                skipping = false;
                continue;
            }
            // L8: 标记 + L9: 访问
            visited.add(u);
            order.add(u);
            emit(L_MARK, List.of(VizEvent.of(VizEvent.EventType.VISIT,
                    "访问 " + u + "（编号 " + order.size() + "）", u)),
                    "弹栈即访问：一条路走到黑——下一个去栈顶的路岔");
            // 逆序压栈：编号小的先被访问
            List<Integer> neighbors = adj.get(SampleGraph.indexOf(u));
            for (int k = neighbors.size() - 1; k >= 0; k--) {
                String v = SampleGraph.label(neighbors.get(k));
                if (!visited.contains(v)) {
                    stack.push(v);
                    emit(L_PUSH, List.of(VizEvent.of(VizEvent.EventType.SET,
                            v + " 入栈（路岔）", v)),
                            "把 " + u + " 的路岔压进栈——先探编号小的，所以逆序压");
                }
            }
            active = null;
        }
        emit(L_END, List.of(VizEvent.of(VizEvent.EventType.RETURN,
                "栈空：全部 " + visited.size() + " 个节点访问完毕，顺序 " + String.join("→", order))),
                "DFS 完成：一条路走到底再回头——和BFS 只差「队列换栈」");
        return new Trace(SCHEMA, meta(), steps);
    }

    private void emit(int line, List<VizEvent> events, String why) {
        StackFrame frame = new StackFrame("dfs(src=A)", Map.of("stack", Value.prim(stackStr())), null);
        Map<String, Layer> layers = new LinkedHashMap<>();
        layers.put("mem.stack", new StackLayer(List.of(frame)));
        layers.put("mem.heap", new HeapLayer(snapshotHeap()));
        layers.put("ds.graph", snapshotGraph());
        steps.add(new Step(steps.size(), Loc.line(line, why),
                Collections.unmodifiableMap(layers), List.copyOf(events), new Intent(why, INVARIANT)));
    }

    private String stackStr() {
        List<String> copy = new ArrayList<>(stack);
        Collections.reverse(copy);   // 栈顶在左，符合直觉
        return "[" + String.join(", ", copy) + "]";
    }

    private Heap snapshotHeap() {
        Map<String, Value> fields = new LinkedHashMap<>();
        fields.put("stack", Value.prim(stackStr()));
        for (String s : SampleGraph.LABELS) {
            fields.put(s, Value.prim(visited.contains(s) ? "已访问" : "未访问"));
        }
        HeapObject table = new HeapObject("@1", "s", "DfsState",
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
        return new GraphView("DFS", SampleGraph.vertices(), SampleGraph.edges(),
                active == null ? null : SampleGraph.nodeId(SampleGraph.indexOf(active)),
                settled, Map.of(), null, null, orderIds);
    }

    private Trace.TraceMeta meta() {
        return new Trace.TraceMeta(id(), title(), codeResource(), "src=A", 6);
    }
}
