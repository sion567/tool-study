package com.algoviz.engine.tree;

import com.algoviz.engine.Intent;
import com.algoviz.engine.Layer;
import com.algoviz.engine.Loc;
import com.algoviz.engine.Step;
import com.algoviz.engine.Value;
import com.algoviz.engine.VizEvent;
import com.algoviz.engine.ds.TreeView;
import com.algoviz.engine.mem.Heap;
import com.algoviz.engine.mem.HeapLayer;
import com.algoviz.engine.mem.HeapObject;
import com.algoviz.engine.mem.StackFrame;
import com.algoviz.engine.mem.StackLayer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 树生产者共用的快照发射器（蓝图 §2.1 铁律 1：每步深拷贝）。
 * 维护：节点数组（val/left/right）、每节点状态与访问序号、调用栈（含 null 调用）。
 */
final class TreeEmitter {

    final List<int[]> nodes = new ArrayList<>();          // 每项 {val, left, right}，-1 = 无
    int[] state;                                          // 0 unvisited, 1 onstack, 2 visited/new
    int[] visitOrder;                                     // 0 = 未访问
    final List<Integer> callStack = new ArrayList<>();    // bottom→top，-1 = null 调用
    final Map<String, Integer> pointerTags = new LinkedHashMap<>(); // 画布指针标签：min/root…（-1 = 不画）
    final Map<Integer, String> perCallVals = new LinkedHashMap<>(); // 调用深度 → 该层的 val（如 delete 的目标值）
    final List<Step> steps = new ArrayList<>();

    private final String methodBase;
    private final String invariant;
    private final Map<String, String> extraLocals;        // 如 insert 的 val=4

    TreeEmitter(int[] values, String methodBase, String invariant, Map<String, String> extraLocals) {
        this.methodBase = methodBase;
        this.invariant = invariant;
        this.extraLocals = extraLocals;
        for (int v : values) {
            nodes.add(new int[]{v, -1, -1});
        }
        this.state = new int[nodes.size()];
        this.visitOrder = new int[nodes.size()];
    }

    int newNode(int val) {
        nodes.add(new int[]{val, -1, -1});
        state = java.util.Arrays.copyOf(state, nodes.size());
        visitOrder = java.util.Arrays.copyOf(visitOrder, nodes.size());
        return nodes.size() - 1;
    }

    void push(int idx) {
        callStack.add(idx);
        if (idx >= 0) {
            state[idx] = 1;
        }
    }

    void pop() {
        int idx = callStack.remove(callStack.size() - 1);
        if (idx >= 0 && state[idx] == 1) {
            state[idx] = 0;
        }
    }

    private int visitCounter;

    /** 标记访问：写序号 + 置为已访问态，返回访问序号。 */
    int visit(int idx) {
        visitCounter++;
        visitOrder[idx] = visitCounter;
        state[idx] = 2;
        return visitCounter;
    }

    /** 固定为已访问态（弹栈不回落）——BST 新节点挂上后保持高亮。 */
    void keepVisited(int idx) {
        state[idx] = 2;
    }

    private final Map<Integer, String> rbMap = new LinkedHashMap<>();

    /** 红黑树颜色（RED/BLACK），null = 不适用。 */
    void setRb(int idx, String color) {
        if (color == null) {
            rbMap.remove(idx);
        } else {
            rbMap.put(idx, color);
        }
    }

    String rbOf(int idx) {
        return rbMap.get(idx);
    }

    /** 设置画布指针标签（-1 = 移除）。 */
    void tag(String name, int idx) {
        pointerTags.put(name, idx);
    }

    void untag(String name) {
        pointerTags.remove(name);
    }

    void emit(int line, List<VizEvent> events, String why) {
        List<StackFrame> frames = new ArrayList<>();
        for (int d = 0; d < callStack.size(); d++) {
            int idx = callStack.get(d);
            Map<String, Value> locals = new LinkedHashMap<>();
            locals.put("node", ref(idx));
            String perCallVal = perCallVals.get(d);
            if (perCallVal != null) {
                locals.put("val", Value.prim(perCallVal));
            } else {
                for (Map.Entry<String, String> e : extraLocals.entrySet()) {
                    locals.put(e.getKey(), Value.prim(e.getValue()));
                }
            }
            frames.add(new StackFrame(methodBase + "(" + refText(idx) + ")",
                    Collections.unmodifiableMap(locals), null));
        }
        Map<String, Layer> layers = new LinkedHashMap<>();
        layers.put("mem.stack", new StackLayer(Collections.unmodifiableList(frames)));
        layers.put("mem.heap", new HeapLayer(snapshotHeap()));
        layers.put("ds.tree", snapshotTree());
        steps.add(new Step(steps.size(), Loc.line(line, why),
                Collections.unmodifiableMap(layers),
                List.copyOf(events), new Intent(why, invariant)));
    }

    Value ref(int idx) {
        return idx >= 0 ? Value.ref("@" + (idx + 1)) : Value.NULL;
    }

    String refText(int idx) {
        return idx >= 0 ? "@" + (idx + 1) : "null";
    }

    int weightOf(int idx) {
        return nodes.get(idx)[0];
    }

    String weightText(int idx) {
        return LABELS_PREFIX + (idx + 1) + "(" + weightOf(idx) + ")";
    }

    void setLeafWeight(int idx, int w) {
        nodes.get(idx)[0] = w;
    }

    private static final String LABELS_PREFIX = "n";

    private Heap snapshotHeap() {
        Map<String, HeapObject> objs = new LinkedHashMap<>();
        for (int i = 0; i < nodes.size(); i++) {
            int[] nd = nodes.get(i);
            Map<String, Value> fields = new LinkedHashMap<>();
            fields.put("val", Value.prim(nd[0]));
            fields.put("left", ref(nd[1]));
            fields.put("right", ref(nd[2]));
            objs.put("@" + (i + 1), new HeapObject("@" + (i + 1), "node", "Node",
                    Collections.unmodifiableMap(fields), List.of()));
        }
        return new Heap(Collections.unmodifiableMap(objs));
    }

    private TreeView snapshotTree() {
        List<TreeView.TreeNodeBox> boxes = new ArrayList<>(nodes.size());
        List<String> stackIds = new ArrayList<>();
        for (int i = 0; i < nodes.size(); i++) {
            int[] nd = nodes.get(i);
            String st = state[i] == 2 ? "visited" : state[i] == 1 ? "onstack" : "unvisited";
            boxes.add(new TreeView.TreeNodeBox("@" + (i + 1), nd[0],
                    nd[1] >= 0 ? "@" + (nd[1] + 1) : null,
                    nd[2] >= 0 ? "@" + (nd[2] + 1) : null,
                    st, visitOrder[i] > 0 ? visitOrder[i] : null,
                    rbMap.get(i)));
        }
        for (int idx : callStack) {
            if (idx >= 0) {
                stackIds.add("@" + (idx + 1));
            }
        }
        // 真根 = 没有被任何 left/right 指向的节点——旋转/删根后真根会变，写死 @1 是 bug
        Set<String> referenced = new java.util.HashSet<>();
        for (int i = 0; i < nodes.size(); i++) {
            int[] nd = nodes.get(i);
            if (nd[1] >= 0) {
                referenced.add("@" + (nd[1] + 1));
            }
            if (nd[2] >= 0) {
                referenced.add("@" + (nd[2] + 1));
            }
        }
        String rootId = null;
        for (int i = 0; i < nodes.size(); i++) {
            if (!referenced.contains("@" + (i + 1))) {
                rootId = "@" + (i + 1);
                break;
            }
        }
        return new TreeView(methodBase, rootId,
                Collections.unmodifiableList(boxes), Collections.unmodifiableList(stackIds),
                snapshotVarTags());
    }

    private Map<String, String> snapshotVarTags() {
        Map<String, String> tags = new LinkedHashMap<>();
        for (Map.Entry<String, Integer> e : pointerTags.entrySet()) {
            if (e.getValue() >= 0) {
                tags.put(e.getKey(), "@" + (e.getValue() + 1));
            }
        }
        return Collections.unmodifiableMap(tags);
    }

    /** 按 values 顺序插入构建 BST 的静态工具（-1 = 空孩子）。 */
    static TreeEmitter bst(int[] values, String methodBase, String invariant, Map<String, String> extraLocals) {
        TreeEmitter em = new TreeEmitter(values, methodBase, invariant, extraLocals);
        for (int k = 1; k < values.length; k++) {
            int v = values[k];
            int cur = 0;
            while (true) {
                if (v < em.nodes.get(cur)[0]) {
                    if (em.nodes.get(cur)[1] == -1) {
                        em.nodes.get(cur)[1] = k;
                        break;
                    }
                    cur = em.nodes.get(cur)[1];
                } else if (v > em.nodes.get(cur)[0]) {
                    if (em.nodes.get(cur)[2] == -1) {
                        em.nodes.get(cur)[2] = k;
                        break;
                    }
                    cur = em.nodes.get(cur)[2];
                } else {
                    break; // 相等忽略
                }
            }
        }
        return em;
    }
}
