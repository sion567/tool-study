package com.algoviz.engine.list;

import com.algoviz.engine.Algorithm;
import com.algoviz.engine.Complexity;
import com.algoviz.engine.Intent;
import com.algoviz.engine.Layer;
import com.algoviz.engine.Loc;
import com.algoviz.engine.OrderedMaps;
import com.algoviz.engine.Step;
import com.algoviz.engine.Value;
import com.algoviz.engine.VizEvent;
import com.algoviz.engine.ds.ListView;
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
 * 删除节点：prev.next ← curr.next 一句话跳过被删节点。
 * 画面：prev/curr/next 三枚彩色标签沿链走位；命中后 curr 的入链箭改道（黄圈可见），
 * 被删盒子灰显但仍挂在画面上——单链表"删除"只是改指针，节点本身还在堆里等 GC。
 * 不变量：删除 = 让前驱跳过目标——prev.next 改指 curr.next 后链依然完整。
 */
public final class ListDeleteDemo implements Algorithm {

    private static final String SCHEMA = "1";
    private static final String INVARIANT =
            "删除 = 前驱跳过目标：prev.next ← curr.next——单链表回不去，prev 必须提前站好位";

    private static final int[] VALS = {4, 9, 2, 7, 5};
    private static final int TARGET = 7;

    /** 与 /code/ListDeleteDemo.java 的行号对齐 */
    private static final int L_INIT = 4;
    private static final int L_WALK = 6;
    private static final int L_ADV = 7;
    private static final int L_FOUND = 9;
    private static final int L_BYPASS = 10;
    private static final int L_MISS = 12;

    private final List<Step> steps = new ArrayList<>();
    private int prevIdx = -1;
    private int currIdx = 0;
    private boolean deleted = false;

    @Override public String id() { return "list.delete"; }
    @Override public String title() { return "删除节点"; }
    @Override public String codeResource() { return "/code/ListDeleteDemo.java"; }
    @Override public String docResource() { return "/markdown/list-delete.md"; }

    @Override
    public Trace produce() {
        steps.clear();
        prevIdx = -1;
        currIdx = 0;
        deleted = false;
        emit(L_INIT, List.of(VizEvent.of(VizEvent.EventType.INIT,
                "目标值 " + TARGET + "；prev = null，curr = @1")),
                "单链表只能向前看——删除要改的是前驱的 next，所以 prev 必须提前一站");
        while (currIdx >= 0) {
            int v = VALS[currIdx];
            if (v == TARGET) {
                emit(L_FOUND, List.of(VizEvent.of(VizEvent.EventType.MARK_SORTED,
                        "curr=@" + name(currIdx) + " 命中 " + TARGET, "curr")),
                        "找到目标——准备改道");
                deleted = true;
                emit(L_BYPASS, List.of(VizEvent.of(VizEvent.EventType.SWAP,
                        prevIdx < 0 ? "删的是头节点 → head ← curr.next"
                                : "prev.next ← curr.next（@" + name(prevIdx) + " 跳过 @"
                                        + name(currIdx) + " 直指 @"
                                        + name(nextIdx(currIdx)) + "）",
                        prevIdx < 0 ? null : "prev")),
                        prevIdx < 0
                                ? "头节点删除：只需挪 head——没有前驱就不用改 next"
                                : "改道这一下就是删除的全部——被删盒子从此不在链上（等 GC）");
                break;
            }
            emit(L_WALK, List.of(VizEvent.of(VizEvent.EventType.COMPARE,
                    "curr=@" + name(currIdx) + " val=" + v + " ≠ " + TARGET, "curr")),
                    "没命中——prev 和 curr 各前移一站");
            prevIdx = currIdx;
            currIdx = nextIdx(currIdx);
            if (currIdx >= 0) {
                emit(L_ADV, List.of(VizEvent.of(VizEvent.EventType.SET,
                        "prev ← @" + name(prevIdx) + "，curr ← @" + name(currIdx), "prev", "curr")),
                        "双指针同步走——这就是单链表删除比双链表多出来的仪式感");
            } else {
                emit(L_MISS, List.of(VizEvent.of(VizEvent.EventType.RETURN,
                        "curr 走到 null → 没找到 " + TARGET)),
                        "走到头都没命中——链上没有这个值");
            }
        }
        emit(L_MISS, List.of(VizEvent.of(VizEvent.EventType.RETURN,
                deleted ? "删除完成：" + remainText() : "未删除任何节点")),
                "链依然完整：删除只是绕过，其余节点的相对顺序纹丝不动");
        return new Trace(SCHEMA, meta(), steps);
    }

    private int nextIdx(int i) {
        return i + 1 < VALS.length ? i + 1 : -1;
    }

    private String name(int i) {
        return i < 0 ? "null" : String.valueOf(i + 1);
    }

    private String remainText() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < VALS.length; i++) {
            if (VALS[i] == TARGET) continue;
            if (sb.length() > 0) sb.append(" → ");
            sb.append(VALS[i]);
        }
        return sb.append(" → null").toString();
    }

    /** 被删节点在展示序里跳过（删除后画面上灰显放在原位还是移除？——移除，靠事件说明）。 */
    private boolean removed(int i) {
        return deleted && VALS[i] == TARGET;
    }

    private void emit(int line, List<VizEvent> events, String why) {
        Map<String, Value> locals = new LinkedHashMap<>();
        locals.put("head", Value.ref("@1"));
        locals.put("prev", prevIdx < 0 ? Value.NULL : Value.ref("@" + (prevIdx + 1)));
        locals.put("curr", currIdx < 0 ? Value.NULL : Value.ref("@" + (currIdx + 1)));
        StackFrame frame = new StackFrame("delete",
                Collections.unmodifiableMap(new LinkedHashMap<>(locals)), null);
        Map<String, Layer> layers = new LinkedHashMap<>();
        layers.put("mem.stack", new StackLayer(List.of(frame)));
        layers.put("mem.heap", new HeapLayer(snapshotHeap()));
        layers.put("ds.list", snapshotList());
        steps.add(new Step(steps.size(), Loc.line(line, why),
                Collections.unmodifiableMap(layers), List.copyOf(events), new Intent(why, INVARIANT)));
    }

    private Heap snapshotHeap() {
        Map<String, HeapObject> objs = new LinkedHashMap<>();
        for (int i = 0; i < VALS.length; i++) {
            Map<String, Value> fields = new LinkedHashMap<>();
            fields.put("val", Value.prim(VALS[i]));
            if (removed(i)) {
                fields.put("next", Value.NULL);   // 被删：出链
            } else {
                int nx = nextIdx(i);
                // 越过被删节点（改道）：删除发生后，next 直指被删节点的 next
                while (nx >= 0 && removed(nx)) {
                    nx = nextIdx(nx);
                }
                fields.put("next", nx >= 0 ? Value.ref("@" + (nx + 1)) : Value.NULL);
            }
            objs.put("@" + (i + 1), new HeapObject("@" + (i + 1), "node", "Node",
                    Collections.unmodifiableMap(fields), List.of()));
        }
        return new Heap(Collections.unmodifiableMap(objs));
    }

    private ListView snapshotList() {
        List<ListView.NodeBox> nodes = new ArrayList<>();
        for (int i = 0; i < VALS.length; i++) {
            String nextId;
            if (removed(i)) {
                nextId = null;
            } else {
                int nx = nextIdx(i);
                while (nx >= 0 && removed(nx)) {
                    nx = nextIdx(nx);
                }
                nextId = nx >= 0 ? "@" + (nx + 1) : null;
            }
            nodes.add(ListView.NodeBox.singly("@" + (i + 1), VALS[i], nextId));
        }
        return new ListView("删除 " + TARGET, "@1", Collections.unmodifiableList(nodes));
    }

    private Trace.TraceMeta meta() {
        StringBuilder sb = new StringBuilder();
        for (int k = 0; k < VALS.length; k++) {
            if (k > 0) sb.append(',');
            sb.append(VALS[k]);
        }
        return new Trace.TraceMeta(id(), title(), codeResource(), sb.toString(), VALS.length);
    }

    @Override
    public java.util.List<String> dataLines() {
        return java.util.List.of(
                "int[] vals = " + java.util.Arrays.toString(VALS) + ";",
                "int target = " + TARGET + ";");
    }

    @Override
    public Complexity complexity() {
        return new Complexity("O(n) 查找主导，改指针 O(1)", "O(1) 双指针",
                "找到目标和改道各是一遍——合起来 O(n)。"
                        + "双链表删已知节点只要 O(1)（prev 直接可得），单链表必须先走一遍找 prev");
    }
}
