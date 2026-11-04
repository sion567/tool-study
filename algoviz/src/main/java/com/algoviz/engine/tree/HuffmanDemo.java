package com.algoviz.engine.tree;

import com.algoviz.engine.Algorithm;
import com.algoviz.engine.Intent;
import com.algoviz.engine.Layer;
import com.algoviz.engine.Loc;
import com.algoviz.engine.mem.Heap;
import com.algoviz.engine.mem.HeapLayer;
import com.algoviz.engine.mem.HeapObject;
import com.algoviz.engine.mem.StackFrame;
import com.algoviz.engine.mem.StackLayer;
import com.algoviz.engine.math.MathView;
import com.algoviz.engine.Step;
import com.algoviz.engine.Value;
import com.algoviz.engine.VizEvent;
import com.algoviz.engine.trace.Trace;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 霍夫曼编码：贪心合并最小两棵 → 编码树。
 * 自含发射器（不依赖 TreeEmitter），内部维护节点列表和优先队列。
 */
public final class HuffmanDemo implements Algorithm {

    private static final String SCHEMA = "1";
    private static final String INVARIANT =
            "每次合并最小权重的两棵子树——频率高的字符自然离根近（编码短）";

    private static final char[] CHARS = {'A', 'B', 'C', 'D'};
    private static final int[] FREQS = {45, 13, 12, 9};

    private final List<Step> steps = new ArrayList<>();
    private final List<int[]> nodes = new ArrayList<>();     // {weight, left, right}
    private final List<String> labels = new ArrayList<>();
    private final List<Integer> queue = new ArrayList<>();   // 当前队列中的节点下标

    @Override public String id() { return "greedy.huffman"; }
    @Override public String title() { return "霍夫曼编码"; }
    @Override public String codeResource() { return "/code/HuffmanDemo.java"; }
    @Override public String docResource() { return "/markdown/huffman.md"; }

    @Override
    public Trace produce() {
        // 初始化叶子
        for (int i = 0; i < CHARS.length; i++) {
            nodes.add(new int[]{FREQS[i], -1, -1});
            labels.add(String.valueOf(CHARS[i]));
            queue.add(i);
        }
        emit(3, List.of(VizEvent.of(VizEvent.EventType.INIT,
                "初始化：" + queueStr() + "（每个字符一棵单节点树）")),
                "每个字符一棵权重为频率的单节点树——全部放进优先队列");

        while (queue.size() > 1) {
            Collections.sort(queue, (a, b) -> nodes.get(a)[0] - nodes.get(b)[0]);
            int a = queue.remove(0);
            int b = queue.remove(0);
            int parent = nodes.size();
            nodes.add(new int[]{nodes.get(a)[0] + nodes.get(b)[0], a, b});
            labels.add("N" + parent);
            queue.add(parent);
            emit(5, List.of(VizEvent.of(VizEvent.EventType.SWAP,
                    "合并 " + wText(a) + "+" + wText(b) + " → " + wText(parent),
                    "@" + (parent + 1))),
                    "贪心：每次合并最小两棵——频率低的离根远（编码长），频率高的离根近（编码短）");
        }
        int root = queue.get(0);
        emit(9, List.of(VizEvent.of(VizEvent.EventType.RETURN,
                "编码树完成：根 = " + wText(root) + "（总频率 " + nodes.get(root)[0] + "）")),
                "编码树建好：从根到每个叶子的路径就是该字符的霍夫曼编码");
        return new Trace(SCHEMA, meta(), steps);
    }

    private int weight(int idx) { return nodes.get(idx)[0]; }

    private String wText(int idx) {
        return idx < labels.size() && !labels.get(idx).startsWith("N")
                ? labels.get(idx) + "(" + weight(idx) + ")"
                : "(" + weight(idx) + ")";
    }

    private String queueStr() {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < queue.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append(wText(queue.get(i)));
        }
        return sb.append("]").toString();
    }

    private void emit(int line, List<VizEvent> events, String why) {
        StackFrame frame = new StackFrame("huffman",
                Map.of("queue", Value.prim(queueStr())), null);
        Map<String, Layer> layers = new LinkedHashMap<>();
        layers.put("mem.stack", new StackLayer(List.of(frame)));
        layers.put("mem.heap", new HeapLayer(snapshotHeap()));
        layers.put("math.matrix", snapshotMatrix());
        steps.add(new Step(steps.size(), Loc.line(line, why),
                Collections.unmodifiableMap(layers), List.copyOf(events), new Intent(why, INVARIANT)));
    }

    private Heap snapshotHeap() {
        Map<String, HeapObject> objs = new LinkedHashMap<>();
        Map<String, Value> fields = new LinkedHashMap<>();
        for (int i = 0; i < nodes.size(); i++) {
            fields.put("n" + i, Value.prim(weight(i)));
        }
        objs.put("@1", new HeapObject("@1", "tree", "HuffmanTree",
                Collections.unmodifiableMap(fields), List.of()));
        return new Heap(Collections.unmodifiableMap(objs));
    }

    private MathView snapshotMatrix() {
        List<MathView.Grid> grids = new ArrayList<>();
        List<String> vals = new ArrayList<>();
        for (int q : queue) vals.add(wText(q));
        grids.add(new MathView.Grid("queue", "优先队列", 1, queue.size(),
                vals, null, null, java.util.Set.of(), java.util.Set.of()));
        return new MathView("霍夫曼优先队列", Collections.unmodifiableList(grids));
    }

    private Trace.TraceMeta meta() {
        return new Trace.TraceMeta(id(), title(), codeResource(),
                "chars=" + CHARS.length, CHARS.length);
    }
}
