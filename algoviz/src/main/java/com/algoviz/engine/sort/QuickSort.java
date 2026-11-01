package com.algoviz.engine.sort;

import com.algoviz.engine.Algorithm;
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
 * 快速排序生产者：分区（Lomuto）+ 递归——数组域第一个「调用栈多层」的演示。
 * 不变量：分区完成后，pivot 左边全部 ≤ 它、右边全部 ≥ 它——pivot 一步到位，再也不动。
 */
public final class QuickSort implements Algorithm {

    private static final String SCHEMA = "1";
    private static final String INVARIANT =
            "分区后：pivot 左边全部 ≤ pivot、右边全部 ≥ pivot——pivot 一步到位，再也不动";

    /** 与 /code/QuickSort.java 的行号对齐 */
    private static final int L_QS = 2;
    private static final int L_BASE = 3;
    private static final int L_PART = 4;
    private static final int L_LEFT = 5;
    private static final int L_RIGHT = 6;
    private static final int L_QS_END = 7;
    private static final int L_PIVOT = 9;
    private static final int L_CMP = 12;
    private static final int L_SWAP = 13;
    private static final int L_FIX = 17;
    private static final int L_RET_P = 18;

    private final int[] input;
    private int[] a;                                   // 工作副本（快照的数据源）
    private final List<Step> steps = new ArrayList<>();
    private final List<StackFrame> frames = new ArrayList<>();   // 调用栈，bottom→top

    public QuickSort(int[] input) {
        this.input = input.clone();
    }

    /** 无参构造：默认示例输入（注册表/测试用）。 */
    public QuickSort() {
        this(new int[]{5, 2, 9, 1, 5});
    }

    @Override public String id() { return "sort.quick"; }
    @Override public String title() { return "快速排序"; }
    @Override public String codeResource() { return "/code/QuickSort.java"; }
    @Override public String docResource() { return "/markdown/quick-sort.md"; }

    @Override
    public Trace produce() {
        a = input.clone();                             // 重复 produce：重建工作副本
        steps.clear();
        frames.clear();
        qs(0, a.length - 1);
        return new Trace(SCHEMA, meta(), steps);
    }

    private void qs(int lo, int hi) {
        push("qs(" + lo + "," + hi + ")", locals(lo, hi, null, null));
        emit(L_QS, List.of(VizEvent.of(VizEvent.EventType.CALL,
                "调用 qs(lo=" + lo + ", hi=" + hi + ") —— 压栈")),
                "进入 qs(" + lo + "," + hi + ")：负责让这段区间有序");
        if (lo >= hi) {
            emit(L_BASE, List.of(VizEvent.of(VizEvent.EventType.COMPARE,
                    "lo ≥ hi → return（基线：0 或 1 个元素天然有序）")),
                    "区间长度 ≤ 1：递归出口");
            emit(L_QS_END, List.of(VizEvent.of(VizEvent.EventType.RETURN, "return —— 弹栈")), "弹栈回到父调用");
            pop();
            return;
        }
        int p = partition(lo, hi);
        emit(L_PART, List.of(VizEvent.of(VizEvent.EventType.MARK_SORTED,
                "分区完成：pivot 归位到 a[" + p + "]=" + a[p] + "（左边全 ≤，右边全 ≥）", "a[" + p + "]")),
                "一次分区定死一个元素——这就是快排的「分」");
        emit(L_LEFT, List.of(VizEvent.of(VizEvent.EventType.CALL,
                "qs(a, " + lo + ", " + (p - 1) + ") —— 递归左半")),
                "左半区间 [lo," + (p - 1) + "] 交给递归");
        qs(lo, p - 1);
        emit(L_RIGHT, List.of(VizEvent.of(VizEvent.EventType.CALL,
                "qs(a, " + (p + 1) + ", " + hi + ") —— 递归右半")),
                "右半区间 [" + (p + 1) + "," + hi + "] 交给递归");
        qs(p + 1, hi);
        emit(L_QS_END, List.of(VizEvent.of(VizEvent.EventType.RETURN,
                "qs(" + lo + "," + hi + ") 完成 —— 弹栈")), "本段有序，弹栈");
        pop();
    }

    private int partition(int lo, int hi) {
        push("partition(" + lo + "," + hi + ")", locals(lo, hi, a[hi], null));
        emit(L_PIVOT, List.of(VizEvent.of(VizEvent.EventType.INIT,
                "pivot ← a[" + hi + "] = " + a[hi] + "（以最后一个为基准）", "a[" + hi + "]")),
                "选基准：最后一个元素（Lomuto 方案）");
        int i = lo;
        for (int j = lo; j < hi; j++) {
            emit(L_CMP, List.of(VizEvent.of(VizEvent.EventType.COMPARE,
                    "a[" + j + "]=" + a[j] + (a[j] < a[hi] ? " < " : " ≥ ") + "pivot=" + a[hi],
                    "a[" + j + "]", "a[" + hi + "]")),
                    "a[j] 和 pivot 比大小——决定去左区还是留在右区");
            if (a[j] < a[hi]) {
                int t = a[i];
                a[i] = a[j];
                a[j] = t;
                emit(L_SWAP, List.of(VizEvent.of(VizEvent.EventType.SWAP,
                        "swap a[" + i + "] ↔ a[" + j + "]（小于区扩张一格）",
                        "a[" + i + "]", "a[" + j + "]")),
                        "换到 i 的位置：小于区向右长一格");
                i++;
            }
        }
        int t = a[i];
        a[i] = a[hi];
        a[hi] = t;
        emit(L_FIX, List.of(VizEvent.of(VizEvent.EventType.SWAP,
                "swap a[" + i + "] ↔ a[" + hi + "]：pivot 归位", "a[" + i + "]", "a[" + hi + "]")),
                "把 pivot 从队尾换到它的最终位置");
        emit(L_RET_P, List.of(VizEvent.of(VizEvent.EventType.RETURN, "return " + i + " —— 弹栈")),
                "返回 pivot 的最终下标");
        pop();
        return i;
    }

    private void push(String method, Map<String, Value> locals) {
        frames.add(new StackFrame(method, Collections.unmodifiableMap(new LinkedHashMap<>(locals)), null));
    }

    private void pop() {
        frames.remove(frames.size() - 1);
    }

    private Map<String, Value> locals(Integer lo, Integer hi, Integer pivot, Integer i) {
        Map<String, Value> m = new LinkedHashMap<>();
        m.put("a", Value.ref("@1"));
        if (lo != null) m.put("lo", Value.prim(lo));
        if (hi != null) m.put("hi", Value.prim(hi));
        if (pivot != null) m.put("pivot", Value.prim(pivot));
        if (i != null) m.put("i", Value.prim(i));
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
        List<Value> elems = new ArrayList<>();
        for (int v : a) {
            elems.add(Value.prim(v));
        }
        HeapObject arr = new HeapObject("@1", "a", "int[" + a.length + "]",
                new LinkedHashMap<>(), List.copyOf(elems));
        Map<String, HeapObject> objs = new LinkedHashMap<>();
        objs.put("@1", arr);
        return new Heap(Collections.unmodifiableMap(objs));
    }

    private ArrayView snapshotArray() {
        List<Integer> vals = new ArrayList<>();
        for (int v : a) {
            vals.add(v);
        }
        return new ArrayView("@1", "a", List.copyOf(vals), null, null, null, null, a.length, -1);
    }

    private Trace.TraceMeta meta() {
        StringBuilder sb = new StringBuilder();
        for (int k = 0; k < input.length; k++) {
            if (k > 0) sb.append(',');
            sb.append(input[k]);
        }
        return new Trace.TraceMeta(id(), title(), codeResource(), sb.toString(), input.length);
    }
}
