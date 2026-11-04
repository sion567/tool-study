package com.algoviz.engine.tree;

import com.algoviz.engine.Algorithm;
import com.algoviz.engine.Complexity;
import com.algoviz.engine.Intent;
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
import com.algoviz.engine.trace.Trace;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** AVL 旋转演示：插入触发失衡 → 旋转恢复平衡。简化版，用 ds.array 显示节点值序列。 */
public final class AvlDemo implements Algorithm {

    private static final String SCHEMA = "1";
    private static final String INVARIANT =
            "AVL 性质：每个节点左右子树高度差 ≤ 1——插入后从底向上检查，旋转恢复平衡";

    private static final int[] INPUT = {10, 20, 30, 40, 50, 25};

    private final List<Step> steps = new ArrayList<>();
    private final List<int[]> nodes = new ArrayList<>();
    private final List<Integer> heights = new ArrayList<>();

    @Override public String id() { return "tree.avl"; }
    @Override public String title() { return "AVL 旋转"; }
    @Override public String codeResource() { return "/code/AvlDemo.java"; }
    @Override public String docResource() { return "/markdown/avl.md"; }

    @Override
    public Trace produce() {
        int root = -1;
        for (int v : INPUT) {
            root = insert(root, v);
            emit(2, List.of(VizEvent.of(VizEvent.EventType.SET,
                    "插入 " + v + " 完成")),
                    "插入 " + v + " 后整棵树重新平衡");
        }
        return new Trace(SCHEMA, meta(), steps);
    }

    private int insert(int idx, int val) {
        if (idx == -1) {
            nodes.add(new int[]{val, -1, -1});
            heights.add(1);
            int ni = nodes.size() - 1;
            emit(3, List.of(VizEvent.of(VizEvent.EventType.SET,
                    "插入 " + val + " = @" + (ni + 1) + "（新叶子）")),
                    "BST 插入：找到空位挂上去");
            return ni;
        }
        if (val < weight(idx)) {
            emit(5, List.of(VizEvent.of(VizEvent.EventType.COMPARE,
                    val + " < " + weight(idx) + " → 往左")),
                    "比当前小 → 去左子树");
            int child = insert(nodes.get(idx)[1], val);
            nodes.get(idx)[1] = child;
        } else if (val > weight(idx)) {
            emit(5, List.of(VizEvent.of(VizEvent.EventType.COMPARE,
                    val + " > " + weight(idx) + " → 往右")),
                    "比当前大 → 去右子树");
            int child = insert(nodes.get(idx)[2], val);
            nodes.get(idx)[2] = child;
        } else {
            return idx;
        }
        updateHeight(idx);
        int bal = balanceOf(idx);
        if (bal > 1) {
            if (val < weight(nodes.get(idx)[1])) {
                emit(12, List.of(VizEvent.of(VizEvent.EventType.SET,
                        "LL 失衡 → 右旋 @" + (idx + 1))),
                        "LL：左子树的左侧过重 → 右旋恢复");
                idx = rotateRight(idx);
                emit(13, List.of(VizEvent.of(VizEvent.EventType.SET,
                        "右旋完成 → 新根 @" + (idx + 1))),
                        "右旋：左孩子上升为根，原根变成右孩子");
            } else {
                emit(12, List.of(VizEvent.of(VizEvent.EventType.SET,
                        "LR 失衡 → 先左旋左子树，再右旋")),
                        "LR：先对左子树左旋转为 LL，再右旋当前节点");
                int newLeft = rotateLeft(nodes.get(idx)[1]);
                nodes.get(idx)[1] = newLeft;
                emit(13, List.of(VizEvent.of(VizEvent.EventType.SET,
                        "左旋左子树完成 → 现在是 LL")),
                        "LR 转为 LL");
                emit(14, List.of(VizEvent.of(VizEvent.EventType.SET,
                        "右旋 @" + (idx + 1))),
                        "再右旋当前节点");
                idx = rotateRight(idx);
            }
        } else if (bal < -1) {
            int rightChild = nodes.get(idx)[2];
            if (val > weight(rightChild)) {
                emit(12, List.of(VizEvent.of(VizEvent.EventType.SET,
                        "RR 失衡 → 左旋 @" + (idx + 1))),
                        "RR：右子树的右侧过重 → 左旋恢复");
                idx = rotateLeft(idx);
                emit(13, List.of(VizEvent.of(VizEvent.EventType.SET,
                        "左旋完成 → 新根 @" + (idx + 1))),
                        "左旋：右孩子上升为根，原根变成左孩子");
            } else {
                emit(12, List.of(VizEvent.of(VizEvent.EventType.SET,
                        "RL 失衡 → 先右旋右子树，再左旋")),
                        "RL：先对右子树右旋转为 RR，再左旋当前节点");
                int newRight = rotateRight(nodes.get(idx)[2]);
                nodes.get(idx)[2] = newRight;
                emit(13, List.of(VizEvent.of(VizEvent.EventType.SET,
                        "右旋右子树完成 → 现在是 RR")),
                        "RL 转为 RR");
                emit(14, List.of(VizEvent.of(VizEvent.EventType.SET,
                        "左旋 @" + (idx + 1))),
                        "最后左旋当前节点");
                idx = rotateLeft(idx);
            }
        }
        return idx;
    }

    private int newNode(int val) {
        nodes.add(new int[]{val, -1, -1});
        heights.add(1);
        return nodes.size() - 1;
    }

    private int weight(int idx) { return nodes.get(idx)[0]; }

    private int height(int idx) { return idx == -1 ? 0 : heights.get(idx); }

    private void updateHeight(int idx) {
        heights.set(idx, 1 + Math.max(height(nodes.get(idx)[1]), height(nodes.get(idx)[2])));
    }

    private int balanceOf(int idx) {
        return idx == -1 ? 0 : height(nodes.get(idx)[1]) - height(nodes.get(idx)[2]);
    }

    private int rotateRight(int y) {
        int x = nodes.get(y)[1];
        int t2 = nodes.get(x)[2];
        nodes.get(y)[1] = t2;
        nodes.get(x)[2] = y;
        updateHeight(y); updateHeight(x);
        return x;
    }

    private int rotateLeft(int x) {
        int y = nodes.get(x)[2];
        nodes.get(x)[2] = nodes.get(y)[1];
        nodes.get(y)[1] = x;
        updateHeight(x); updateHeight(y);
        return y;
    }

    private void emit(int line, List<VizEvent> events, String why) {
        StackFrame frame = new StackFrame("avl", Map.of(), null);
        Map<String, Layer> layers = new LinkedHashMap<>();
        layers.put("mem.stack", new StackLayer(List.of(frame)));
        layers.put("mem.heap", new HeapLayer(snapshotHeap()));
        layers.put("ds.array", snapshotArray());
        steps.add(new Step(steps.size(), Loc.line(line, why),
                Collections.unmodifiableMap(layers), List.copyOf(events), new Intent(why, INVARIANT)));
    }

    private Heap snapshotHeap() {
        Map<String, HeapObject> objs = new LinkedHashMap<>();
        for (int i = 0; i < nodes.size(); i++) {
            Map<String, Value> fields = new LinkedHashMap<>();
            fields.put("val", Value.prim(weight(i)));
            fields.put("height", Value.prim(height(i)));
            fields.put("balance", Value.prim(balanceOf(i)));
            objs.put("@" + (i + 1), new HeapObject("@" + (i + 1), "n" + (i + 1), "AVLNode",
                    Collections.unmodifiableMap(fields), List.of()));
        }
        return new Heap(Collections.unmodifiableMap(objs));
    }

    private ArrayView snapshotArray() {
        List<Integer> vals = new ArrayList<>();
        for (int[] nd : nodes) vals.add(nd[0]);
        return new ArrayView("@1", "nodes", List.copyOf(vals),
                null, null, null, null, vals.size(), -1);
    }

    private Trace.TraceMeta meta() {
        StringBuilder sb = new StringBuilder();
        for (int k = 0; k < INPUT.length; k++) {
            if (k > 0) sb.append(',');
            sb.append(INPUT[k]);
        }
        return new Trace.TraceMeta(id(), title(), codeResource(), sb.toString(), INPUT.length);
    }

    @Override
    public java.util.List<String> dataLines() {
        return java.util.List.of(Algorithm.joinArray(INPUT));
    }

    @Override
    public Complexity complexity() {
        return new Complexity("O(log n) 插入", "O(n) 存储",
                "AVL 树高 ≤ 1.44 log₂(n+2)——旋转 O(1) 恢复平衡，保证最坏也是 O(log n)");
    }
}
