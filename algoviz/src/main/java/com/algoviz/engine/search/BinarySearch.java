package com.algoviz.engine.search;

import com.algoviz.engine.Complexity;
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
 * 二分查找生产者：折半排除——low/high/mid 三根指针在有序数组上跳舞（数组不动，指针动）。
 * 不变量：target 若存在，必在 [lo, hi] 之内——每轮比较排除一半，区间收缩不回头。
 */
public final class BinarySearch implements Algorithm {

    private static final String SCHEMA = "1";
    private static final String INVARIANT =
            "target 若存在，必在 [lo, hi] 之内——每轮比较排除一半，区间只缩不涨";

    /** 与 /code/BinarySearch.java 的行号对齐 */
    private static final int L_INIT = 3;
    private static final int L_WHILE = 4;
    private static final int L_MID = 5;
    private static final int L_HIT = 6;
    private static final int L_GO_RIGHT = 7;
    private static final int L_GO_LEFT = 8;
    private static final int L_NOT_FOUND = 10;

    private static final int[] A = {1, 2, 5, 5, 9, 12, 15};
    private static final int TARGET = 9;

    private final int[] data;
    private final int target;

    private final List<Step> steps = new ArrayList<>();
    private int lo;
    private int hi;
    private int mid = -1;

    public BinarySearch() {
        this(A, TARGET);
    }

    /** 随机数据：有序数组 + 必定存在的目标。 */
    public BinarySearch(int[] sorted, int target) {
        this.data = sorted.clone();
        this.target = target;
    }

    @Override public String id() { return "search.binary"; }
    @Override public String title() { return "二分查找"; }
    @Override public String codeResource() { return "/code/BinarySearch.java"; }
    @Override public String docResource() { return "/markdown/binary-search.md"; }

    @Override
    public Trace produce() {
        lo = 0;
        hi = data.length - 1;
        mid = -1;
        emit(L_INIT, List.of(VizEvent.of(VizEvent.EventType.INIT,
                "lo=0, hi=" + hi + "（目标 " + target + "）")),
                "前提：数组必须有序——乱序二分等于抽签");
        while (lo <= hi) {
            emit(L_WHILE, List.of(VizEvent.of(VizEvent.EventType.COMPARE,
                    "lo=" + lo + " ≤ hi=" + hi + " → 继续折半")),
                    "lo ≤ hi：搜索区间还活着");
            mid = (lo + hi) / 2;
            emit(L_MID, List.of(VizEvent.of(VizEvent.EventType.SET,
                    "mid = (" + lo + "+" + hi + ")/2 = " + mid + "，a[mid]=" + data[mid], "a[" + mid + "]")),
                    "折半点：一次比较排除一半——这就是 O(log n) 的来源");
            emit(L_HIT, List.of(VizEvent.of(VizEvent.EventType.COMPARE,
                    "a[" + mid + "]=" + data[mid] + (data[mid] == target ? " == " : (data[mid] < target ? " < " : " > "))
                            + "target=" + target,
                    "a[" + mid + "]")),
                    "三种走向：等于→命中；小于→去右半；大于→去左半");
            if (data[mid] == target) {
                emit(L_HIT, List.of(VizEvent.of(VizEvent.EventType.RETURN,
                        "命中！return " + mid)), "找到目标，返回下标");
                return new Trace(SCHEMA, meta(), steps);
            }
            if (data[mid] < target) {
                lo = mid + 1;
                emit(L_GO_RIGHT, List.of(VizEvent.of(VizEvent.EventType.SET,
                        "a[mid] < target → lo = mid+1 = " + lo + "（左半被整段排除）")),
                        "比目标小：mid 和它左边全部出局——区间砍半");
            } else {
                hi = mid - 1;
                emit(L_GO_LEFT, List.of(VizEvent.of(VizEvent.EventType.SET,
                        "a[mid] > target → hi = mid-1 = " + hi + "（右半被整段排除）")),
                        "比目标大：mid 和它右边全部出局——区间砍半");
            }
        }
        emit(L_NOT_FOUND, List.of(VizEvent.of(VizEvent.EventType.RETURN,
                "lo > hi：区间空了 → return -1（没找到）")), "区间缩没了都没有 → 目标不存在");
        return new Trace(SCHEMA, meta(), steps);
    }

    private void emit(int line, List<VizEvent> events, String why) {
        StackFrame frame = new StackFrame("search(target=" + target + ")",
                localsMap(), null);
        Map<String, Layer> layers = new LinkedHashMap<>();
        layers.put("mem.stack", new StackLayer(List.of(frame)));
        layers.put("mem.heap", new HeapLayer(snapshotHeap()));
        layers.put("ds.array", snapshotArray());
        steps.add(new Step(steps.size(), Loc.line(line, why),
                Collections.unmodifiableMap(layers), List.copyOf(events), new Intent(why, INVARIANT)));
    }

    private Map<String, Value> localsMap() {
        Map<String, Value> m = new LinkedHashMap<>();
        m.put("a", Value.ref("@1"));
        m.put("target", Value.prim(target));
        m.put("lo", Value.prim(lo));
        m.put("hi", Value.prim(hi));
        if (mid >= 0) {
            m.put("mid", Value.prim(mid));
        }
        return m;
    }

    private Heap snapshotHeap() {
        List<Value> elems = new ArrayList<>();
        for (int v : data) {
            elems.add(Value.prim(v));
        }
        HeapObject arr = new HeapObject("@1", "a", "int[" + data.length + "]",
                new LinkedHashMap<>(), List.copyOf(elems));
        Map<String, HeapObject> objs = new LinkedHashMap<>();
        objs.put("@1", arr);
        return new Heap(Collections.unmodifiableMap(objs));
    }

    private ArrayView snapshotArray() {
        List<Integer> vals = new ArrayList<>();
        for (int v : data) {
            vals.add(v);
        }
        // mid 橙色高亮；lo..hi 之外（已排除区）用 sortedFrom/sortedTo 反着标灰不了——
        // v0 只高亮 mid 与当前区间端点：compareB 放 hi，lo 由事件文本说明
        return new ArrayView("@1", "a", List.copyOf(vals),
                mid >= 0 ? mid : null, hi, null, null, data.length, -1);
    }

    private Trace.TraceMeta meta() {
        StringBuilder sb = new StringBuilder();
        for (int k = 0; k < data.length; k++) {
            if (k > 0) sb.append(',');
            sb.append(data[k]);
        }
        return new Trace.TraceMeta(id(), title(), codeResource(), sb.toString(), data.length);
    }

    @Override
    public java.util.List<String> dataLines() {
        StringBuilder sb = new StringBuilder("int[] a = {");
        for (int k = 0; k < data.length; k++) {
            if (k > 0) sb.append(", ");
            sb.append(data[k]);
        }
        return java.util.List.of(sb.append("};").toString(), "int target = " + target + ";");
    }

    @Override
    public Complexity complexity() {
        return new Complexity("O(log n)", "O(1)", "每轮排除一半：n → n/2 → n/4 → … → 1。比较次数 = log₂n。100 万数据只要 ~20 次——这就是「有序」的威力");
    }
}
