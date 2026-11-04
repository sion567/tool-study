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
 * 双链表插入 / 删除：每节点多一根 prev 箭——改一处要同步改两根。
 * 画面：盒子下方的绿色背向箭 = prev；黄箭头 = 本步变化的箭。
 * 演示：建表 [4,9,2]，中插 7（四步：新.prev/新.next/左.next/右.prev），再删 9（两根箭同时改道）。
 * 不变量：任意时刻 prev 与 next 互为镜像——A.next=B ⟺ B.prev=A，破坏即链断。
 */
public final class DoublyListDemo implements Algorithm {

    private static final String SCHEMA = "1";
    private static final String INVARIANT =
            "prev 与 next 互为镜像：A.next=B ⟺ B.prev=A——插入改 4 根箭，删除改 2 根，漏一根链就断";

    private static final int[] INIT = {4, 9, 2};
    private static final int INSERT_VAL = 7;
    private static final int DELETE_VAL = 9;

    /** 与 /code/DoublyListDemo.java 的行号对齐 */
    private static final int L_BUILD = 4;
    private static final int L_INSERT1 = 9;
    private static final int L_INSERT2 = 10;
    private static final int L_DELETE = 15;

    private final List<Step> steps = new ArrayList<>();
    /** 展示序节点：{id(1基), val}。 */
    private final List<int[]> display = new ArrayList<>();
    private final Map<Integer, Integer> prevOf = new LinkedHashMap<>();   // id → prev id
    private final Map<Integer, Integer> nextOf = new LinkedHashMap<>();   // id → next id
    private int nextAlloc = 1;
    private String phase = "建表";
    private int currId = -1;

    @Override public String id() { return "list.doubly"; }
    @Override public String title() { return "双链表插入 / 删除"; }
    @Override public String codeResource() { return "/code/DoublyListDemo.java"; }
    @Override public String docResource() { return "/markdown/doubly-list.md"; }

    @Override
    public Trace produce() {
        steps.clear();
        display.clear();
        prevOf.clear();
        nextOf.clear();
        nextAlloc = 1;
        currId = -1;

        // ===== 建表：尾插 [4,9,2]，prev/next 成对建立 =====
        phase = "建表（尾插）";
        int head = -1, tail = -1;
        emit(L_BUILD, List.of(VizEvent.of(VizEvent.EventType.INIT, "head = tail = null")),
                "双链表建表 = 单链尾插 + 每步多写一根 prev");
        for (int v : INIT) {
            int id = nextAlloc++;
            display.add(new int[]{id, v});
            prevOf.put(id, tail);
            nextOf.put(id, -1);
            if (tail > 0) {
                nextOf.put(tail, id);
            } else {
                head = id;
            }
            tail = id;
            emit(L_BUILD, List.of(VizEvent.of(VizEvent.EventType.SET,
                    "@" + id + "(" + v + ")：prev ← " + (prevOf.get(id) > 0 ? "@" + prevOf.get(id) : "null")
                            + "，右邻.next ← @" + id,
                    "@" + id)),
                    "新节点入列：prev 指旧尾，旧尾的 next 指回来——镜像当场成立");
        }
        currId = head;

        // ===== 中间插入 7：定位到 9（中点），四根箭 =====
        phase = "插入 7";
        int afterId = findIdByVal(INIT[1]);   // 插在 9 之后
        currId = afterId;
        int newNode = nextAlloc++;
        int pos = 0;
        for (int i = 0; i < display.size(); i++) {
            if (display.get(i)[0] == afterId) {
                pos = i + 1;   // int[] 是身份比较，indexOf 不可用——按下标扫描
                break;
            }
        }
        display.add(pos, new int[]{newNode, INSERT_VAL});
        int beforeId = nextOf.get(afterId);
        prevOf.put(newNode, afterId);
        nextOf.put(newNode, beforeId);
        emit(L_INSERT1, List.of(VizEvent.of(VizEvent.EventType.CALL,
                "newNode=7：new.prev ← @" + afterId + "，new.next ← "
                        + (beforeId > 0 ? "@" + beforeId : "null"),
                "@" + afterId)),
                "插入前两步只动新节点自己的两根箭——此刻链还是原样");
        if (beforeId > 0) {
            prevOf.put(beforeId, newNode);
            emit(L_INSERT2, List.of(VizEvent.of(VizEvent.EventType.SET,
                    "右邻.prev ← @" + newNode + "（镜像补齐 ①）", "@" + beforeId)),
                    "右邻居回头指新节点——漏了这根，反向遍历就会跳过 7");
        }
        nextOf.put(afterId, newNode);
        emit(L_INSERT2, List.of(VizEvent.of(VizEvent.EventType.SET,
                "左邻.next ← @" + newNode + "（镜像补齐 ②）——插入完成", "@" + afterId)),
                "四根箭齐：prev/next 镜像恢复——双链表插入的固定成本");
        currId = newNode;

        // ===== 删除 9：prev 与 next 两根箭同时改道 =====
        phase = "删除 9";
        int delId = findIdByVal(DELETE_VAL);
        currId = delId;
        int p = prevOf.get(delId), n = nextOf.get(delId);
        emit(L_DELETE, List.of(VizEvent.of(VizEvent.EventType.MARK_SORTED,
                "命中 @" + delId + "(9)：prev=@" + nm(p) + " next=" + nm(n))),
                "双链表删已知节点不用找前驱——del.prev 现成");
        if (p > 0) {
            nextOf.put(p, n);
        }
        if (n > 0) {
            prevOf.put(n, p);
        }
        prevOf.put(delId, -1);
        nextOf.put(delId, -1);
        display.removeIf(arr -> arr[0] == delId);
        currId = p > 0 ? p : head;
        emit(L_DELETE, List.of(VizEvent.of(VizEvent.EventType.SWAP,
                "p.next ← n，n.prev ← p——两根箭同时改道，9 出链")),
                "对比单链表：不用从头找 prev——这正是双向链表存在的意义");

        phase = "完成";
        currId = findFirst();
        emit(L_DELETE, List.of(VizEvent.of(VizEvent.EventType.RETURN,
                "终态：" + chainText() + "（双向可遍历）")),
                "检验镜像：从 head 正着走、从尾倒着走，序列必须一致");
        return new Trace(SCHEMA, meta(), steps);
    }

    private int findIdByVal(int v) {
        for (int[] arr : display) {
            if (arr[1] == v) return arr[0];   // display 只含在链节点
        }
        return display.get(0)[0];
    }

    private int findFirst() {
        // 链头 = 没被任何 next 指到的在链节点
        for (int[] arr : display) {
            int id = arr[0];
            boolean referenced = false;
            for (int[] other : display) {
                if (other[0] != id && nextOf.getOrDefault(other[0], -1) == id) {
                    referenced = true;
                    break;
                }
            }
            if (!referenced) return id;
        }
        return display.get(0)[0];
    }

    private String nm(int id) {
        return id > 0 ? "@" + id : "null";
    }

    private String chainText() {
        int head = findFirst();
        StringBuilder sb = new StringBuilder();
        int cur = head;
        while (cur > 0) {
            if (sb.length() > 0) sb.append(" ↔ ");
            sb.append(valOf(cur));
            cur = nextOf.getOrDefault(cur, -1);
        }
        return sb.toString();
    }

    private int valOf(int id) {
        for (int[] arr : display) {
            if (arr[0] == id) return arr[1];
        }
        return -1;
    }

    private void emit(int line, List<VizEvent> events, String why) {
        Map<String, Value> locals = new LinkedHashMap<>();
        if (currId > 0) {
            locals.put("curr", Value.ref("@" + currId));
        }
        if (phase.contains("删除")) {
            locals.put("del", Value.prim(String.valueOf(DELETE_VAL)));
        }
        StackFrame frame = new StackFrame(phase, Collections.unmodifiableMap(locals), null);
        Map<String, Layer> layers = new LinkedHashMap<>();
        layers.put("mem.stack", new StackLayer(List.of(frame)));
        layers.put("mem.heap", new HeapLayer(snapshotHeap()));
        layers.put("ds.list", snapshotList());
        steps.add(new Step(steps.size(), Loc.line(line, why),
                Collections.unmodifiableMap(layers), List.copyOf(events), new Intent(why, INVARIANT)));
    }

    private Heap snapshotHeap() {
        Map<String, HeapObject> objs = new LinkedHashMap<>();
        for (int[] arr : display) {
            int id = arr[0];
            Map<String, Value> fields = new LinkedHashMap<>();
            fields.put("val", Value.prim(arr[1]));
            int pv = prevOf.getOrDefault(id, -1);
            int nx = nextOf.getOrDefault(id, -1);
            // 删除阶段的 delId 已从 display 移除——只输出在链节点
            fields.put("prev", pv > 0 && onDisplay(pv) ? Value.ref("@" + pv) : Value.NULL);
            fields.put("next", nx > 0 && onDisplay(nx) ? Value.ref("@" + nx) : Value.NULL);
            objs.put("@" + id, new HeapObject("@" + id, "dnode", "DNode",
                    Collections.unmodifiableMap(fields), List.of()));
        }
        return new Heap(Collections.unmodifiableMap(objs));
    }

    private boolean onDisplay(int id) {
        for (int[] arr : display) {
            if (arr[0] == id) return true;
        }
        return false;
    }

    private ListView snapshotList() {
        List<ListView.NodeBox> nodes = new ArrayList<>();
        for (int[] arr : display) {
            int id = arr[0];
            int pv = prevOf.getOrDefault(id, -1);
            int nx = nextOf.getOrDefault(id, -1);
            nodes.add(new ListView.NodeBox("@" + id, arr[1],
                    nx > 0 && onDisplay(nx) ? "@" + nx : null,
                    pv > 0 && onDisplay(pv) ? "@" + pv : null));
        }
        return new ListView(phase, display.isEmpty() ? null : "@" + findFirst(),
                Collections.unmodifiableList(nodes));
    }

    private Trace.TraceMeta meta() {
        StringBuilder sb = new StringBuilder();
        for (int k = 0; k < INIT.length; k++) {
            if (k > 0) sb.append(',');
            sb.append(INIT[k]);
        }
        return new Trace.TraceMeta(id(), title(), codeResource(), sb.toString(), INIT.length);
    }

    @Override
    public java.util.List<String> dataLines() {
        return java.util.List.of(
                "int[] init = " + java.util.Arrays.toString(INIT) + ";",
                "int insertVal = " + INSERT_VAL + ", deleteVal = " + DELETE_VAL + ";");
    }

    @Override
    public Complexity complexity() {
        return new Complexity("插入 O(1) / 删除已知节点 O(1)", "O(n) 多一根 prev 指针",
                "每节点多 8 字节（64 位 prev），换来：删已知节点不用找前驱、"
                        + "可以倒着遍历、LRU 的 O(1) 淘汰全靠它");
    }
}
