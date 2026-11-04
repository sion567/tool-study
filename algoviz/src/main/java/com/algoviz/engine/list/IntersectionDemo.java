package com.algoviz.engine.list;

import com.algoviz.engine.OrderedMaps;
import com.algoviz.engine.Algorithm;
import com.algoviz.engine.Complexity;
import com.algoviz.engine.ds.ArrayView;
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 相交链表：两条链在某个节点汇合——双指针走完自己的走对方的，对齐长度后同步走。
 * 画面：两条链的元素在上方数组合并显示，橙色/青色分别标注两条链的当前指针。
 * 不变量：双指针走过的总步数相同时必然同步——若相交则同时到达交点。
 */
public final class IntersectionDemo implements Algorithm {

    private static final String SCHEMA = "1";
    private static final String INVARIANT =
            "双指针各走 a+c 和 b+c 步（c = 公共段），总步数相等 → 同步到达交点";

    private static final int[] A = {1, 2, 3, 7, 8, 9};   // A 独有 3 个 + 公共 3 个
    private static final int[] B = {4, 5, 7, 8, 9};       // B 独有 2 个 + 公共 3 个
    private static final int INTERSECT_START_A = 3;       // A[3] 开始公共段

    private final List<Step> steps = new ArrayList<>();
    private int pa, pb;
    private String phaseStr = "";

    @Override public String id() { return "list.intersection"; }
    @Override public String title() { return "相交链表"; }
    @Override public String codeResource() { return "/code/IntersectionDemo.java"; }
    @Override public String docResource() { return "/markdown/intersection.md"; }

    @Override
    public Trace produce() {
        pa = 0; pb = 0;
        phaseStr = "各自走";
        emit(3, List.of(VizEvent.of(VizEvent.EventType.INIT,
                "双指针起跑：pA → A 头，pB → B 头")),
                "A 有 " + A.length + " 个，B 有 " + B.length + " 个——长度不同，但总步数会相同");

        // 阶段 1：pA 走 A（含公共段），pB 走 B（含公共段）
        while (pa < A.length || pb < B.length) {
            if (pa >= A.length && pb < B.length) {
                phaseStr = "pA 切到 B";
                emit(6, List.of(VizEvent.of(VizEvent.EventType.SET,
                        "pA 走完 A → 切到 B 头（pB 的起跑线）")),
                        "关键技巧：走完自己的链就切到对方的链——总步数拉平");
                pa = A.length; // pA 走完 A
                // 切换：pA 从 B 的头开始
                // 简化：pA 直接从 B 的下标 0 开始
                break;
            }
            if (pb >= B.length && pa < A.length) {
                phaseStr = "pB 切到 A";
                emit(6, List.of(VizEvent.of(VizEvent.EventType.SET,
                        "pB 走完 B → 切到 A 头")), "同理：pB 切到 A 的头");
                pb = B.length;
                break;
            }
            // 正常前进
            if (pa < A.length) pa++;
            if (pb < B.length) pb++;
        }
        // 重新模拟简化版（对齐后同步走）
        int aOffset = A.length - B.length;   // A 比 B 长 1
        if (aOffset > 0) {
            pa = aOffset; pb = 0;
        } else {
            pa = 0; pb = -aOffset;
        }
        emit(7, List.of(VizEvent.of(VizEvent.EventType.SET,
                "对齐后同步走：pA=A[" + pa + "], pB=B[" + pb + "]")),
                "长链先走完差值，然后双指针同一起跑线同步前进");
        while (pa < A.length && pb < B.length) {
            if (A[pa] == B[pb]) {
                emit(8, List.of(VizEvent.of(VizEvent.EventType.MARK_SORTED,
                        "相交！a[" + pa + "] = b[" + pb + "] = " + A[pa], "a:" + pa, "b:" + pb)),
                        "两个指针同时到达同一节点 → 这就是交点");
                return new Trace(SCHEMA, meta(), steps);
            }
            emit(8, List.of(VizEvent.of(VizEvent.EventType.COMPARE,
                    "A[" + pa + "]=" + A[pa] + " vs B[" + pb + "]=" + B[pb] + " → 未相交，继续",
                    "a:" + pa, "b:" + pb)),
                    "不相等 → 双指针同步前进");
            pa++; pb++;
        }
        emit(10, List.of(VizEvent.of(VizEvent.EventType.RETURN, "走完没相交 → null")),
                "两条链不相交");
        return new Trace(SCHEMA, meta(), steps);
    }

    private void emit(int line, List<VizEvent> events, String why) {
        StackFrame frame = new StackFrame("intersection",
                OrderedMaps.of("pA", Value.prim(pa < A.length ? A[pa] : -1),
                       "pB", Value.prim(pb < B.length ? B[pb] : -1),
                       "phase", Value.prim(phaseStr)), null);
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
        for (int v : A) vals.add(v);
        for (int v : B) vals.add(v);
        return new ArrayView("@1", "A|B", List.copyOf(vals),
                pa < A.length ? pa : null, pb < B.length ? A.length + pb : null,
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
        StringBuilder s1 = new StringBuilder("int[] a = {");
        StringBuilder s2 = new StringBuilder("int[] b = {");
        for (int k = 0; k < A.length; k++) { if (k > 0) s1.append(", "); s1.append(A[k]); }
        for (int k = 0; k < B.length; k++) { if (k > 0) s2.append(", "); s2.append(B[k]); }
        return java.util.List.of(s1.append("};").toString(), s2.append("};").toString());
    }

    @Override
    public Complexity complexity() {
        return new Complexity("O(n+m) 双指针总步数恒等", "O(1) 两个指针",
                "pA 走 a+c 步后切到 B 走 b 步 = a+b+c；pB 同理 = b+a+c。总步数相等 → 同步到达交点或同时到尾");
    }
}
