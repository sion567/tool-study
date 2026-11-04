package com.algoviz.engine.sort;

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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 归并排序生产者：分治递归 + 合并（辅助数组）。
 * 画面：递归栈逐层压弹（split 阶段），合并阶段辅助数组 tmp 逐格生长、
 * 写回主数组时黄圈可见。
 * 不变量：merge 完成后 [lo,hi] 区间有序——左右两半各自有序，合并出来的整段也有序。
 */
public final class MergeSort implements Algorithm {

    private static final String SCHEMA = "1";
    private static final String INVARIANT =
            "合并完成后 [lo,hi] 有序——左右两半各自有序，双指针比小逐个放进 tmp";

    /** 与 /code/MergeSort.java 的行号对齐 */
    private static final int L_MS = 2;
    private static final int L_BASE = 3;
    private static final int L_MID = 5;
    private static final int L_LEFT = 6;
    private static final int L_RIGHT = 7;
    private static final int L_MERGE = 8;
    private static final int L_MERGE_END = 20;

    private final int[] input;
    private int[] a;          // 工作副本
    private int[] tmp;        // 辅助数组（画面第二排）
    private final List<Step> steps = new ArrayList<>();
    private final List<StackFrame> frames = new ArrayList<>();

    public MergeSort(int[] input) {
        this.input = input.clone();
    }

    @Override public String id() { return "sort.merge"; }
    @Override public String title() { return "归并排序"; }
    @Override public String codeResource() { return "/code/MergeSort.java"; }
    @Override public String docResource() { return "/markdown/merge-sort.md"; }

    @Override
    public Trace produce() {
        a = input.clone();
        tmp = new int[a.length];
        steps.clear();
        frames.clear();
        ms(0, a.length - 1);
        return new Trace(SCHEMA, meta(), steps);
    }

    private void ms(int lo, int hi) {
        push("ms(" + lo + "," + hi + ")", locals(lo, hi));
        emit(L_MS, List.of(VizEvent.of(VizEvent.EventType.CALL,
                "调用 ms(lo=" + lo + ", hi=" + hi + ") —— 压栈")),
                "进入 ms(" + lo + "," + hi + ")：负责让这段区间有序");
        if (lo >= hi) {
            emit(L_BASE, List.of(VizEvent.of(VizEvent.EventType.COMPARE,
                    "lo ≥ hi → return（基线：单元素天然有序）")),
                    "区间长度 ≤ 1：递归出口");
            emit(L_MERGE, List.of(VizEvent.of(VizEvent.EventType.RETURN, "return —— 弹栈")),
                    "弹栈回到父调用");
            pop();
            return;
        }
        int mid = (lo + hi) / 2;
        emit(L_MID, List.of(VizEvent.of(VizEvent.EventType.COMPARE,
                "mid = " + mid + "（从中间劈开）")),
                "分：一分为二，各管各的");
        ms(lo, mid);
        emit(L_LEFT, List.of(VizEvent.of(VizEvent.EventType.CALL,
                "ms(" + lo + "," + mid + ") 完成 —— 左半有序")),
                "左半 [lo," + mid + "] 已有序");
        ms(mid + 1, hi);
        emit(L_RIGHT, List.of(VizEvent.of(VizEvent.EventType.CALL,
                "ms(" + (mid + 1) + "," + hi + ") 完成 —— 右半有序")),
                "右半 [" + (mid + 1) + "," + hi + "] 已有序");
        merge(lo, mid, hi);
        emit(L_MERGE_END, List.of(VizEvent.of(VizEvent.EventType.MARK_SORTED,
                "合并完成 [" + lo + "," + hi + "] —— 本段有序", "a:" + lo)),
                "治：两半合并成一整段有序（不变量达成）");
        emit(L_MERGE, List.of(VizEvent.of(VizEvent.EventType.RETURN, "ms(" + lo + "," + hi + ") 弹栈")),
                "本段处理完，弹栈");
        pop();
    }

    private void merge(int lo, int mid, int hi) {
        int i = lo, j = mid + 1, k = lo;
        emit(L_MERGE, List.of(VizEvent.of(VizEvent.EventType.INIT,
                "合并 [" + lo + "," + mid + "] + [" + (mid + 1) + "," + hi + "] → tmp")),
                "双指针比小：i 指左半，j 指右半，谁小谁先进 tmp");
        while (i <= mid && j <= hi) {
            if (a[i] <= a[j]) {
                tmp[k++] = a[i++];
                emit(L_MERGE, List.of(VizEvent.of(VizEvent.EventType.COMPARE,
                        "a[" + (i - 1) + "]=" + a[i - 1] + " ≤ a[" + j + "]=" + a[j]
                                + " → tmp[" + (k - 1) + "] ← " + a[i - 1],
                        "a:" + (i - 1), "a:" + j)),
                        "左边的先进（≤ 保证稳定性）");
            } else {
                tmp[k++] = a[j++];
                emit(L_MERGE, List.of(VizEvent.of(VizEvent.EventType.COMPARE,
                        "a[" + i + "]=" + a[i] + " > a[" + (j - 1) + "]=" + a[j - 1]
                                + " → tmp[" + (k - 1) + "] ← " + a[j - 1],
                        "a:" + i, "a:" + (j - 1))),
                        "右边的先进");
            }
        }
        while (i <= mid) {
            tmp[k++] = a[i++];
            emit(L_MERGE, List.of(VizEvent.of(VizEvent.EventType.SET,
                    "左半剩余 → tmp[" + (k - 1) + "] ← " + a[i - 1], "a:" + (i - 1))),
                    "一边扫完后另一边整体搬入");
        }
        while (j <= hi) {
            tmp[k++] = a[j++];
            emit(L_MERGE, List.of(VizEvent.of(VizEvent.EventType.SET,
                    "右半剩余 → tmp[" + (k - 1) + "] ← " + a[j - 1], "a:" + (j - 1))),
                    "一边扫完后另一边整体搬入");
        }
        for (int t = lo; t <= hi; t++) {
            a[t] = tmp[t];
        }
        emit(L_MERGE, List.of(VizEvent.of(VizEvent.EventType.SWAP,
                "tmp 写回 a[" + lo + "," + hi + "] —— 合并完成")),
                "tmp 只是草稿，结果写回主数组");
    }

    private void push(String method, Map<String, Value> locals) {
        frames.add(new StackFrame(method, Collections.unmodifiableMap(new LinkedHashMap<>(locals)), null));
    }

    private void pop() {
        frames.remove(frames.size() - 1);
    }

    private Map<String, Value> locals(Integer lo, Integer hi) {
        Map<String, Value> m = new LinkedHashMap<>();
        m.put("a", Value.ref("@1"));
        m.put("tmp", Value.ref("@2"));
        if (lo != null) m.put("lo", Value.prim(lo));
        if (hi != null) m.put("hi", Value.prim(hi));
        return m;
    }

    private void emit(int line, List<VizEvent> events, String why) {
        Map<String, Layer> layers = new LinkedHashMap<>();
        layers.put("mem.stack", new StackLayer(List.copyOf(frames)));
        layers.put("mem.heap", new HeapLayer(snapshotHeap()));
        layers.put("ds.array", snapshotArray());
        steps.add(new Step(steps.size(), Loc.line(line, why),
                Collections.unmodifiableMap(layers), List.copyOf(events), new Intent(why, INVARIANT)));
    }

    private Heap snapshotHeap() {
        Map<String, HeapObject> objs = new LinkedHashMap<>();
        List<Value> aElems = new ArrayList<>();
        for (int v : a) aElems.add(Value.prim(v));
        objs.put("@1", new HeapObject("@1", "a", "int[" + a.length + "]",
                new LinkedHashMap<>(), List.copyOf(aElems)));
        List<Value> tElems = new ArrayList<>();
        for (int v : tmp) tElems.add(Value.prim(v));
        objs.put("@2", new HeapObject("@2", "tmp", "int[" + tmp.length + "]（辅助）",
                new LinkedHashMap<>(), List.copyOf(tElems)));
        return new Heap(Collections.unmodifiableMap(objs));
    }

    private ArrayView snapshotArray() {
        List<Integer> vals = new ArrayList<>();
        for (int v : a) vals.add(v);
        return new ArrayView("@1", "a", List.copyOf(vals),
                null, null, null, null, a.length, -1);
    }

    private Trace.TraceMeta meta() {
        StringBuilder sb = new StringBuilder();
        for (int k = 0; k < input.length; k++) {
            if (k > 0) sb.append(',');
            sb.append(input[k]);
        }
        return new Trace.TraceMeta(id(), title(), codeResource(), sb.toString(), input.length);
    }

    @Override
    public java.util.List<String> dataLines() {
        return java.util.List.of(Algorithm.joinArray(input));
    }

    @Override
    public Complexity complexity() {
        return new Complexity("O(n log n) 稳定", "O(n) 辅助数组 tmp",
                "递归深度 log n × 每层合并 O(n) → n log n。最好最坏都一样（没有退化情况）；代价是 O(n) 额外空间");
    }
}
