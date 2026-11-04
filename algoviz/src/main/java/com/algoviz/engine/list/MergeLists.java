package com.algoviz.engine.list;

import com.algoviz.engine.OrderedMaps;
import com.algoviz.engine.Algorithm;
import com.algoviz.engine.Complexity;
import com.algoviz.engine.ds.ArrayView;
import com.algoviz.engine.Intent;
import com.algoviz.engine.Layer;
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
 * 合并两个有序链表：双指针比小，谁小谁接——O(n+m) 单趟。
 * 画面：两条链（listA / listB），每帧比较两个头节点，小的接到结果链。
 * 不变量：result 链始终是已合并部分的有序链——从两个源链头节点中「摘」最小的。
 */
public final class MergeLists implements Algorithm {

    private static final String SCHEMA = "1";
    private static final String INVARIANT =
            "result 链始终有序——每步从两个源链头中选较小的接到结果尾部，直到一条源链耗尽";

    private static final int L_CMP = 4;
    private static final int L_END = 8;

    private static final int[] A = {1, 3, 5};
    private static final int[] B = {2, 4, 6};

    private final List<Step> steps = new ArrayList<>();
    private int ia, ib;   // 源链 A/B 的当前头下标

    @Override public String id() { return "list.merge"; }
    @Override public String title() { return "合并有序链表"; }
    @Override public String codeResource() { return "/code/MergeLists.java"; }
    @Override public String docResource() { return "/markdown/merge-lists.md"; }

    @Override
    public Trace produce() {
        ia = 0; ib = 0;
        emit(L_CMP, List.of(VizEvent.of(VizEvent.EventType.INIT,
                "两条有序链就位：A=" + arrStr(A) + "  B=" + arrStr(B))),
                "合并 = 双指针比小：谁的头小谁先接，直到一条耗尽");

        while (ia < A.length && ib < B.length) {
            if (A[ia] <= B[ib]) {
                emit(L_CMP, List.of(VizEvent.of(VizEvent.EventType.COMPARE,
                        "A[" + ia + "]=" + A[ia] + " ≤ B[" + ib + "]=" + B[ib] + " → 接 A 的",
                        "a:" + ia, "b:" + ib)),
                        "A 的头更小：接到 result 尾部，A 头指针后移");
                emit(L_CMP, List.of(VizEvent.of(VizEvent.EventType.SET,
                        "result ← " + A[ia] + "（来自 A），A 头后移", "a:" + ia)),
                        "A 的头已接走——A 指针前进");
                ia++;
            } else {
                emit(L_CMP, List.of(VizEvent.of(VizEvent.EventType.COMPARE,
                        "A[" + ia + "]=" + A[ia] + " > B[" + ib + "]=" + B[ib] + " → 接 B 的",
                        "a:" + ia, "b:" + ib)),
                        "B 的头更小：接到 result 尾部，B 头指针后移");
                emit(L_CMP, List.of(VizEvent.of(VizEvent.EventType.SET,
                        "result ← " + B[ib] + "（来自 B），B 头后移", "b:" + ib)),
                        "B 的头已接走——B 指针前进");
                ib++;
            }
        }
        while (ia < A.length) {
            emit(L_CMP, List.of(VizEvent.of(VizEvent.EventType.SET,
                    "A 剩余 → result ← " + A[ia], "a:" + ia)),
                    "B 耗尽：A 的剩余直接接到尾部");
            ia++;
        }
        while (ib < B.length) {
            emit(L_CMP, List.of(VizEvent.of(VizEvent.EventType.SET,
                    "B 剩余 → result ← " + B[ib], "b:" + ib)),
                    "B 的剩余直接接到尾部");
            ib++;
        }
        emit(L_END, List.of(VizEvent.of(VizEvent.EventType.RETURN,
                "合并完成：两条链合成一条有序链——O(n+m) 单趟")),
                "合并 = 双指针单趟扫，O(n+m)——没有嵌套循环");
        return new Trace(SCHEMA, meta(), steps);
    }

    private String arrStr(int[] a) {
        StringBuilder sb = new StringBuilder("[");
        for (int k = 0; k < a.length; k++) {
            if (k > 0) sb.append(", ");
            sb.append(a[k]);
        }
        return sb.append("]").toString();
    }

    private void emit(int line, List<VizEvent> events, String why) {
        StackFrame frame = new StackFrame("mergeLists",
                OrderedMaps.of("a", Value.prim(arrStr(A)),
                       "b", Value.prim(arrStr(B)),
                       "ia", Value.prim(ia),
                       "ib", Value.prim(ib)), null);
        Map<String, Layer> layers = new LinkedHashMap<>();
        layers.put("mem.stack", new StackLayer(List.of(frame)));
        layers.put("mem.heap", new HeapLayer(snapshotHeap()));
        layers.put("ds.array", snapshotArray());
        steps.add(new Step(steps.size(), Loc.line(line, why),
                Collections.unmodifiableMap(layers), List.copyOf(events), new Intent(why, INVARIANT)));
    }

    private Heap snapshotHeap() {
        Map<String, HeapObject> objs = new LinkedHashMap<>();
        List<Value> aElems = new ArrayList<>();
        for (int v : A) aElems.add(Value.prim(v));
        objs.put("@1", new HeapObject("@1", "A", "ListNode[]",
                new LinkedHashMap<>(), List.copyOf(aElems)));
        List<Value> bElems = new ArrayList<>();
        for (int v : B) bElems.add(Value.prim(v));
        objs.put("@2", new HeapObject("@2", "B", "ListNode[]",
                new LinkedHashMap<>(), List.copyOf(bElems)));
        return new Heap(Collections.unmodifiableMap(objs));
    }

    private ArrayView snapshotArray() {
        List<Integer> vals = new ArrayList<>();
        for (int[] src : new int[][]{A, B}) {
            for (int v : src) vals.add(v);
        }
        return new ArrayView("@1", "A+B", List.copyOf(vals),
                ia < A.length ? ia : null, ib < B.length ? A.length + ib : null,
                null, null, A.length + B.length, -1);
    }

    private Trace.TraceMeta meta() {
        StringBuilder sb = new StringBuilder();
        for (int v : A) sb.append(v).append(',');
        for (int v : B) sb.append(v).append(',');
        return new Trace.TraceMeta(id(), title(), codeResource(), sb.toString(), A.length + B.length);
    }

    @Override
    public java.util.List<String> dataLines() {
        return java.util.List.of(
                "int[] a = " + arrStr(A) + ";",
                "int[] b = " + arrStr(B) + ";");
    }

    @Override
    public Complexity complexity() {
        return new Complexity("O(n+m) 单趟", "O(1) 虚拟头节点",
                "双指针各最多走 n/m 步 → n+m 次比较。没有嵌套循环 → 线性");
    }
}
