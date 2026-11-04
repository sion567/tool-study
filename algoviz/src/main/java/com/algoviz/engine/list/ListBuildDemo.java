package com.algoviz.engine.list;

import com.algoviz.engine.Algorithm;
import com.algoviz.engine.Complexity;
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
 * 建表方式对比：同一批数据 {1,2,3}，头插法得 [3,2,1]（逆序），尾插法得 [1,2,3]（保序）。
 * 画面：头插阶段 head 箭头不断挪到新盒子；尾插阶段 tail 指针沿链尾走。
 * 不变量：头插 = newNode.next = head; head = newNode（两步顺序不能反，否则丢链）；
 * 尾插 = tail.next = newNode; tail = newNode。
 */
public final class ListBuildDemo implements Algorithm {

    private static final String SCHEMA = "1";
    private static final String INVARIANT_HEAD =
            "头插两步序：newNode.next ← head，再 head ← newNode——顺序反了链就断";
    private static final String INVARIANT_TAIL =
            "尾插两步序：tail.next ← newNode，再 tail ← newNode——tail 永远指最后一个";

    private static final int[] DATA = {1, 2, 3};

    private final List<Step> steps = new ArrayList<>();
    /** 展示序（与快照 nodes 一致）：头插阶段新节点插到最前。 */
    private final List<int[]> display = new ArrayList<>();   // {id(1基), val}
    private int headId = -1;      // 1 基 id
    private int tailId = -1;
    private int nextAlloc = 1;
    private String phase = "头插法";
    private String lastNew = "";  // 事件描述用

    @Override public String id() { return "list.build"; }
    @Override public String title() { return "头插法 / 尾插法建表"; }
    @Override public String codeResource() { return "/code/ListBuildDemo.java"; }
    @Override public String docResource() { return "/markdown/list-build.md"; }

    @Override
    public Trace produce() {
        steps.clear();
        display.clear();
        headId = tailId = -1;
        nextAlloc = 1;

        // ===== 阶段一：头插法 → 逆序 [3,2,1] =====
        phase = "头插法";
        emit(4, List.of(VizEvent.of(VizEvent.EventType.INIT, "head = null，准备头插 {1,2,3}")),
                "头插法：新节点永远抢 head 的位置——结果正好是逆序");
        for (int v : DATA) {
            int id = nextAlloc++;
            // 展示序：插最前
            display.add(0, new int[]{id, v});
            lastNew = "newNode(" + v + ") → @" + id;
            emit(6, List.of(VizEvent.of(VizEvent.EventType.CALL,
                    "newNode.next ← head（" + (headId < 0 ? "null" : "@" + headId) + "）")),
                    "第一步：新节点先牵住旧链头——这一步之前旧链完好");
            headId = id;
            if (tailId < 0) tailId = id;
            emit(7, List.of(VizEvent.of(VizEvent.EventType.SET,
                    "head ← @" + id + "（" + v + " 抢占链头）")),
                    "第二步：head 指向新节点——新节点成为链头");
        }
        emit(10, List.of(VizEvent.of(VizEvent.EventType.MARK_SORTED,
                "头插完成：" + chainText() + "——与输入顺序相反")),
                "逆序的来源：越晚插的越靠前——头插法天生会反转");

        // ===== 阶段二：尾插法 → 保序 [1,2,3] =====
        phase = "尾插法";
        display.clear();
        headId = tailId = -1;
        nextAlloc = 1;
        emit(14, List.of(VizEvent.of(VizEvent.EventType.INIT, "推倒重来：head = tail = null，尾插 {1,2,3}")),
                "尾插法：新节点接到 tail 后面——结果保持输入顺序");
        for (int v : DATA) {
            int id = nextAlloc++;
            display.add(new int[]{id, v});
            if (headId < 0) {
                headId = tailId = id;
                emit(17, List.of(VizEvent.of(VizEvent.EventType.SET,
                        "空表：head = tail = @" + id + "（" + v + "）")),
                        "第一个节点：head 和 tail 都指向它");
            } else {
                emit(17, List.of(VizEvent.of(VizEvent.EventType.CALL,
                        "tail.next ← @" + id + "（" + v + " 排队入列）")),
                        "第一步：tail 牵住新节点——接在链尾");
                tailId = id;
                emit(18, List.of(VizEvent.of(VizEvent.EventType.SET,
                        "tail ← @" + id + "（tail 挪到新队尾）")),
                        "第二步：tail 前移到新节点——永远指最后一个");
            }
        }
        emit(21, List.of(VizEvent.of(VizEvent.EventType.RETURN,
                "尾插完成：" + chainText() + "——与输入同序")),
                "保序的来源：排队入列——尾插法 = 建队列，头插法 = 建栈");
        return new Trace(SCHEMA, meta(), steps);
    }

    private String chainText() {
        StringBuilder sb = new StringBuilder();
        for (int[] nd : display) {
            if (sb.length() > 0) sb.append(" → ");
            sb.append(nd[1]);
        }
        return sb.append(" → null").toString();
    }

    private void emit(int line, List<VizEvent> events, String why) {
        String invariant = phase.equals("头插法") ? INVARIANT_HEAD : INVARIANT_TAIL;
        Map<String, Value> locals = new LinkedHashMap<>();
        locals.put("head", headId > 0 ? Value.ref("@" + headId) : Value.NULL);
        if (phase.equals("尾插法")) {
            locals.put("tail", tailId > 0 ? Value.ref("@" + tailId) : Value.NULL);
        } else {
            locals.put("newNode", lastNew.isEmpty() ? Value.prim("-") : Value.prim(lastNew));
        }
        StackFrame frame = new StackFrame(phase, Collections.unmodifiableMap(locals), null);
        Map<String, Layer> layers = new LinkedHashMap<>();
        layers.put("mem.stack", new StackLayer(List.of(frame)));
        layers.put("mem.heap", new HeapLayer(snapshotHeap()));
        layers.put("ds.list", snapshotList());
        steps.add(new Step(steps.size(), Loc.line(line, why),
                Collections.unmodifiableMap(layers), List.copyOf(events), new Intent(why, invariant)));
    }

    private Heap snapshotHeap() {
        Map<String, HeapObject> objs = new LinkedHashMap<>();
        for (int i = 0; i < display.size(); i++) {
            int id = display.get(i)[0];
            int v = display.get(i)[1];
            Map<String, Value> fields = new LinkedHashMap<>();
            fields.put("val", Value.prim(v));
            int nextInDisplay = (i + 1 < display.size()) ? display.get(i + 1)[0] : -1;
            // 头插阶段 display 即当前逻辑链（新在前）；next 按展示序连接
            fields.put("next", nextInDisplay > 0 ? Value.ref("@" + nextInDisplay) : Value.NULL);
            objs.put("@" + id, new HeapObject("@" + id, "node", "Node",
                    Collections.unmodifiableMap(fields), List.of()));
        }
        return new Heap(Collections.unmodifiableMap(objs));
    }

    private ListView snapshotList() {
        List<ListView.NodeBox> nodes = new ArrayList<>();
        for (int i = 0; i < display.size(); i++) {
            int id = display.get(i)[0];
            int v = display.get(i)[1];
            int nextInDisplay = (i + 1 < display.size()) ? display.get(i + 1)[0] : -1;
            nodes.add(ListView.NodeBox.singly("@" + id, v,
                    nextInDisplay > 0 ? "@" + nextInDisplay : null));
        }
        return new ListView(phase, headId > 0 ? "@" + headId : null,
                Collections.unmodifiableList(nodes));
    }

    private Trace.TraceMeta meta() {
        StringBuilder sb = new StringBuilder();
        for (int k = 0; k < DATA.length; k++) {
            if (k > 0) sb.append(',');
            sb.append(DATA[k]);
        }
        return new Trace.TraceMeta(id(), title(), codeResource(), sb.toString(), DATA.length);
    }

    @Override
    public java.util.List<String> dataLines() {
        return java.util.List.of("int[] data = " + java.util.Arrays.toString(DATA) + ";");
    }

    @Override
    public Complexity complexity() {
        return new Complexity("O(n) 每个节点 O(1) 接入", "O(1) 只多 head/tail 两个指针",
                "头插 O(1) 但逆序；尾插 O(1) 保序但要多养一个 tail 指针——"
                        + "只给 head 的 singly 链表想保序？只能头插后整体反转，或每次 O(n) 走到尾");
    }
}
