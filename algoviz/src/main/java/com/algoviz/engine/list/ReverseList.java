package com.algoviz.engine.list;

import com.algoviz.engine.Algorithm;
import com.algoviz.engine.Intent;
import com.algoviz.engine.Layer;
import com.algoviz.engine.Loc;
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
 * 链表反转生产者（蓝图 §3.2 的帧序列实物化）。
 * 行号常量必须与 /code/ReverseList.java 对齐（golden 测试会拦不同步）。
 * 画面核心：只看 curr.next 这根箭头——断哪根、接哪根。
 */
public final class ReverseList implements Algorithm {

    private static final String SCHEMA = "1";
    private static final String INVARIANT =
            "prev 开头的链永远是已反转完成的部分，curr 开头的链是待处理部分";

    /** 与 /code/ReverseList.java 的行号对齐 */
    private static final int L_PREV = 3;
    private static final int L_CURR = 4;
    private static final int L_WHILE = 5;
    private static final int L_NEXT = 6;
    private static final int L_CUT = 7;
    private static final int L_PREV2 = 8;
    private static final int L_CURR2 = 9;
    private static final int L_RET = 11;

    private final int[] input;

    public ReverseList(int[] input) {
        this.input = input.clone();
    }

    @Override
    public String id() {
        return "list.reverse";
    }

    @Override
    public String title() {
        return "链表反转";
    }

    @Override
    public String codeResource() {
        return "/code/ReverseList.java";
    }

    @Override
    public String docResource() {
        return "/markdown/reverse-list.md";
    }

    @Override
    public Trace produce() {
        Emitter em = new Emitter(input.clone());

        // L3: Node prev = null
        em.emit(L_PREV, List.of(VizEvent.of(VizEvent.EventType.INIT, "prev ← null", "prev")),
                "prev = null：已反转部分的头，初始为空（空链也是链）");

        // L4: Node curr = head
        em.currDeclared = true;
        em.emit(L_CURR, List.of(VizEvent.of(VizEvent.EventType.INIT,
                        "curr ← head" + (em.currIdx >= 0 ? " = @" + (em.currIdx + 1) : " = null"), "curr")),
                "curr = head：curr 站上待处理部分的头");

        while (em.currIdx != -1) {
            em.nextDeclared = false;
            // L5: while 条件
            em.emit(L_WHILE, List.of(VizEvent.of(VizEvent.EventType.COMPARE,
                            "curr = @" + (em.currIdx + 1) + " ≠ null → 进入循环", "curr")),
                    "curr ≠ null？待处理部分还有节点就继续");
            // L6: Node next = curr.next
            em.nextDeclared = true;
            em.nextVarIdx = em.nextIdx[em.currIdx];
            em.emit(L_NEXT, List.of(VizEvent.of(VizEvent.EventType.SET,
                            "next ← curr.next = " + em.ref(em.nextVarIdx), "next")),
                    "① 先记住下一个：马上要改 curr.next，不记住就断了后路");
            // L7: curr.next = prev —— 反转本体
            int currId = em.currIdx + 1;
            String oldArrow = em.ref(em.nextVarIdx);
            em.nextIdx[em.currIdx] = em.prevIdx;
            String newArrow = em.ref(em.prevIdx);
            em.emit(L_CUT, List.of(VizEvent.of(VizEvent.EventType.SET,
                            "curr.next: " + oldArrow + " → " + newArrow + "（断旧箭、接新箭）",
                            "@" + currId + ".next")),
                    "② 反转本体就这一行：curr 掉头指向 prev");
            // L8: prev = curr
            em.prevIdx = em.currIdx;
            em.emit(L_PREV2, List.of(VizEvent.of(VizEvent.EventType.SET,
                            "prev ← curr = @" + (em.prevIdx + 1), "prev")),
                    "③ 窗口左半右移：prev 前进到已反转部分的新头");
            // L9: curr = next
            em.currIdx = em.nextVarIdx;
            em.emit(L_CURR2, List.of(VizEvent.of(VizEvent.EventType.SET,
                            "curr ← next = " + em.ref(em.currIdx), "curr")),
                    "③ 窗口右半右移：curr 前进到待处理部分");
        }
        em.nextDeclared = false;
        // L5 条件不成立
        em.emit(L_WHILE, List.of(VizEvent.of(VizEvent.EventType.COMPARE,
                        "curr = null → 循环结束", "curr")),
                "curr = null：待处理部分空了，反转完成");
        // L11: return prev
        em.emit(L_RET, List.of(VizEvent.of(VizEvent.EventType.RETURN,
                        "return " + em.ref(em.prevIdx) + "（新头）")),
                "返回 prev：新链表的头（不变量全程成立）");

        return new Trace(SCHEMA, meta(input), em.steps);
    }

    /** 深拷贝快照发射器：栈帧局部变量 + Node 堆盒子 + ds.list 视图。 */
    private static final class Emitter {
        final int[] vals;
        final int[] nextIdx;
        final int n;
        final List<Step> steps = new ArrayList<>();

        int headIdx;        // head 变量指向的节点下标，-1 = null
        int prevIdx = -1;   // prev 变量
        int currIdx = -1;   // curr 变量
        int nextVarIdx = -1;// next 变量（仅循环体内声明）
        boolean currDeclared;
        boolean nextDeclared;

        Emitter(int[] input) {
            this.vals = input;
            this.n = input.length;
            this.nextIdx = new int[n];
            for (int i = 0; i < n; i++) {
                nextIdx[i] = i + 1 < n ? i + 1 : -1;
            }
            this.headIdx = n > 0 ? 0 : -1;
            this.currIdx = headIdx;
        }

        String ref(int idx) {
            return idx >= 0 ? "@" + (idx + 1) : "null";
        }

        void emit(int line, List<VizEvent> events, String why) {
            // mem.stack：局部变量表（null = 未声明）
            Map<String, Value> locals = new LinkedHashMap<>();
            locals.put("head", headIdx >= 0 ? Value.ref("@" + (headIdx + 1)) : Value.NULL);
            locals.put("prev", prevIdx >= 0 ? Value.ref("@" + (prevIdx + 1)) : Value.NULL);
            if (currDeclared) {
                locals.put("curr", currIdx >= 0 ? Value.ref("@" + (currIdx + 1)) : Value.NULL);
            }
            if (nextDeclared) {
                locals.put("next", nextVarIdx >= 0 ? Value.ref("@" + (nextVarIdx + 1)) : Value.NULL);
            }
            StackFrame frame = new StackFrame("reverse",
                    Collections.unmodifiableMap(locals), null);
            Map<String, Layer> layers = new LinkedHashMap<>();
            layers.put("mem.stack", new StackLayer(List.of(frame)));
            layers.put("mem.heap", new HeapLayer(snapshotHeap()));
            layers.put("ds.list", snapshotList());
            steps.add(new Step(steps.size(), Loc.line(line, why),
                    Collections.unmodifiableMap(layers),
                    List.copyOf(events), new Intent(why, INVARIANT)));
        }

        private Heap snapshotHeap() {
            Map<String, HeapObject> objs = new LinkedHashMap<>();
            for (int i = 0; i < n; i++) {
                Map<String, Value> fields = new LinkedHashMap<>();
                fields.put("val", Value.prim(vals[i]));
                fields.put("next", nextIdx[i] >= 0 ? Value.ref("@" + (nextIdx[i] + 1)) : Value.NULL);
                objs.put("@" + (i + 1), new HeapObject("@" + (i + 1), "node", "Node",
                        Collections.unmodifiableMap(fields), List.of()));
            }
            return new Heap(Collections.unmodifiableMap(objs));
        }

        private ListView snapshotList() {
            List<ListView.NodeBox> nodes = new ArrayList<>(n);
            for (int i = 0; i < n; i++) {
                nodes.add(new ListView.NodeBox("@" + (i + 1), vals[i],
                        nextIdx[i] >= 0 ? "@" + (nextIdx[i] + 1) : null));
            }
            return new ListView("反转链表", headIdx >= 0 ? "@" + (headIdx + 1) : null,
                    Collections.unmodifiableList(nodes));
        }
    }
}
