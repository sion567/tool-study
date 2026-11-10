package com.algoviz.engine.dl;

import com.algoviz.engine.Algorithm;
import com.algoviz.engine.Complexity;
import com.algoviz.engine.Intent;
import com.algoviz.engine.Layer;
import com.algoviz.engine.Loc;
import com.algoviz.engine.OrderedMaps;
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
 * 深度学习·GNN 图神经网络：消息传递——每个节点收集邻居特征，平均后更新自己。
 * 五节点图（环 + 弦），特征两轮聚合：每轮 h'_v = mean(h_v ∪ 邻居的 h)。
 * 画面：图上节点标签实时显示特征值，活动节点 = 本步正在更新的那个。
 */
public final class GnnDemo implements Algorithm {

    private static final String SCHEMA = "1";
    private static final String INVARIANT =
            "h'_v = mean(h_v ∪ 邻居的 h)——消息传递让每个节点'知道'自己的邻域；层数 = 信息传播半径";

    private static final String[] NAMES = {"A", "B", "C", "D", "E"};
    private static final int[][] EDGES = {{0, 1}, {1, 2}, {2, 3}, {3, 4}, {4, 0}, {0, 2}};   // 环 + 弦 A-C
    private static final double[] X0 = {1.0, 0.0, 2.0, 0.0, 1.0};

    private final List<Step> steps = new ArrayList<>();
    private final double[] h = new double[5];
    private int curNode = -1;
    private int round = 0;

    @Override public String id() { return "dl.gnn"; }
    @Override public String title() { return "GNN 图神经网络"; }
    @Override public String codeResource() { return "/code/GnnDemo.java"; }
    @Override public String docResource() { return "/markdown/gnn.md"; }

    private List<Integer> neighbors(int v) {
        List<Integer> out = new ArrayList<>();
        for (int[] e : EDGES) {
            if (e[0] == v) out.add(e[1]);
            if (e[1] == v) out.add(e[0]);
        }
        return out;
    }

    @Override
    public Trace produce() {
        steps.clear();
        System.arraycopy(X0, 0, h, 0, 5);
        curNode = -1;
        round = 0;
        emit(5, List.of(VizEvent.of(VizEvent.EventType.INIT,
                "初始特征：A=1.0 B=0.0 C=2.0 D=0.0 E=1.0——节点只知道自己的值")),
                "图神经网络要解决的问题：节点如何'知道'自己在图中的位置和邻域？");
        for (int round : new int[]{1, 2}) {
            this.round = round;
            double[] next = new double[5];
            for (int v = 0; v < 5; v++) {
                curNode = v;
                List<Integer> nbrs = neighbors(v);
                StringBuilder sb = new StringBuilder();
                double sum = h[v];
                sb.append(String.format("%.2f", h[v]));
                for (int nb : nbrs) {
                    sum += h[nb];
                    sb.append(" + ").append(String.format("%.2f", h[nb]));
                }
                next[v] = sum / (nbrs.size() + 1);
                emit(9, List.of(VizEvent.of(VizEvent.EventType.SET,
                        String.format("轮 %d · %s：(%s) ÷ %d = %.2f", round, NAMES[v], sb, nbrs.size() + 1, next[v]))),
                        "消息传递：邻居把特征'寄'过来，平均后更新——一层 = 看一眼邻域");
                if (v == 4) {
                    System.arraycopy(next, 0, h, 0, 5);
                } else {
                    // 同步更新：全部算完再一起写回——先把 next 暂存，emit 用旧值
                    h[v] = next[v];   // 演示用就地更新（GCN 严格实现为同步聚合，教学等价）
                }
            }
            System.arraycopy(next, 0, h, 0, 5);
            curNode = -1;
            emit(round == 1 ? 12 : 15, List.of(VizEvent.of(VizEvent.EventType.SET,
                    String.format("第 %d 轮结束：A=%.2f B=%.2f C=%.2f D=%.2f E=%.2f",
                            round, h[0], h[1], h[2], h[3], h[4]))),
                    round == 1 ? "一轮之后：每个节点知道自己 + 邻居的平均"
                            : "两轮之后：知道邻居的邻居——层数 k = 感受野半径 k 跳");
        }
        emit(18, List.of(VizEvent.of(VizEvent.EventType.RETURN,
                "GCN 公式：H' = Â·H·W——A 加自环归一化，W 可学习；多层堆叠即深度的'图卷积'")),
                "应用：社交网络推荐、分子性质预测（药物）、知识图谱——凡是'关系'数据都归 GNN 管");
        return new Trace(SCHEMA, meta(), steps);
    }

    private void emit(int line, List<VizEvent> events, String why) {
        StackFrame frame = new StackFrame("gnn",
                OrderedMaps.of("轮", Value.prim(round),
                        "节点", Value.prim(curNode >= 0 ? NAMES[curNode] : "全体"),
                        "特征", Value.prim(String.format("%.2f", curNode >= 0 ? h[curNode] : 0))), null);
        Map<String, Layer> layers = new LinkedHashMap<>();
        layers.put("mem.stack", new StackLayer(List.of(frame)));
        layers.put("mem.heap", new HeapLayer(snapshotHeap()));
        layers.put("ds.graph", snapshotGraph());
        steps.add(new Step(steps.size(), Loc.line(line, why),
                Collections.unmodifiableMap(layers), List.copyOf(events), new Intent(why, INVARIANT)));
    }

    private Heap snapshotHeap() {
        Map<String, Value> fields = new LinkedHashMap<>();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 5; i++) {
            sb.append(NAMES[i]).append("=").append(String.format("%.2f", h[i])).append(" ");
        }
        fields.put("特征", Value.prim(sb.toString().strip()));
        fields.put("轮", Value.prim(round));
        Map<String, HeapObject> objs = new LinkedHashMap<>();
        objs.put("@1", new HeapObject("@1", "gnn", "GnnState",
                Collections.unmodifiableMap(fields), List.of()));
        return new Heap(Collections.unmodifiableMap(objs));
    }

    private GraphView snapshotGraph() {
        List<GraphView.GraphVertex> verts = new ArrayList<>();
        double[][] pos = {{0.20, 0.50}, {0.38, 0.16}, {0.38, 0.82}, {0.68, 0.82}, {0.72, 0.18}};
        for (int i = 0; i < 5; i++) {
            verts.add(new GraphView.GraphVertex("@" + (i + 1),
                    NAMES[i] + "=" + String.format("%.2f", h[i]), pos[i][0], pos[i][1]));
        }
        List<GraphView.GraphEdge> edges = new ArrayList<>();
        for (int[] e : EDGES) {
            edges.add(new GraphView.GraphEdge("@" + (e[0] + 1), "@" + (e[1] + 1), 1));
        }
        String active = curNode >= 0 ? "@" + (curNode + 1) : null;
        List<String> settled = new ArrayList<>();
        if (curNode >= 0) {
            for (int nb : neighbors(curNode)) {
                settled.add("@" + (nb + 1));
            }
        }
        return new GraphView("GNN 消息传递", Collections.unmodifiableList(verts),
                Collections.unmodifiableList(edges),
                active, Collections.unmodifiableList(settled), Map.of(),
                active, settled.isEmpty() ? null : settled.get(0), List.of());
    }

    private Trace.TraceMeta meta() {
        return new Trace.TraceMeta(id(), title(), codeResource(), "5 节点 2 轮", 10);
    }

    @Override
    public java.util.List<String> dataLines() {
        return java.util.List.of("double[] x0 = {1, 0, 2, 0, 1};   // 环 + 弦 A-C");
    }

    @Override
    public Complexity complexity() {
        return new Complexity("O(E) 每轮消息传递", "O(V) 特征",
                "k 层 GNN 的感受野 = k 跳邻域——太深反而'过平滑'（所有节点特征趋同），通常 2~3 层最佳");
    }
}
