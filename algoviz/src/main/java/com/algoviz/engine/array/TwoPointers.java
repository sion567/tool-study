package com.algoviz.engine.array;

import com.algoviz.engine.OrderedMaps;
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
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 双指针（左右夹逼）：有序数组中找两数之和等于 target。
 * 不变量：lo 左边的都太小（已排除），hi 右边的都太大（已排除）。
 */
public final class TwoPointers implements Algorithm {

    private static final String SCHEMA = "1";
    private static final String INVARIANT =
            "lo 左边全太小（已排除），hi 右边全太大（已排除）——答案若存在必在 [lo,hi] 内";

    private static final int L_LO = 3;
    private static final int L_HI = 4;
    private static final int L_WHILE = 5;
    private static final int L_SUM = 6;
    private static final int L_FOUND = 7;
    private static final int L_GO_RIGHT = 8;
    private static final int L_GO_LEFT = 9;
    private static final int L_NOT_FOUND = 11;

    private final int[] data;
    private final int target;
    private final List<Step> steps = new ArrayList<>();
    private int lo, hi;

    public TwoPointers(int[] sorted, int target) {
        this.data = sorted.clone();
        this.target = target;
    }

    @Override public String id() { return "array.twopointers"; }
    @Override public String title() { return "双指针夹逼"; }
    @Override public String codeResource() { return "/code/TwoPointers.java"; }
    @Override public String docResource() { return "/markdown/two-pointers.md"; }

    @Override
    public Trace produce() {
        lo = 0;
        hi = data.length - 1;
        emit(L_LO, List.of(VizEvent.of(VizEvent.EventType.INIT,
                "lo = 0, hi = " + hi + "（两端起跑，target = " + target + "）")),
                "左右指针从两端出发——有序数组是前提");

        while (lo < hi) {
            int sum = data[lo] + data[hi];
            emit(L_WHILE, List.of(VizEvent.of(VizEvent.EventType.COMPARE,
                    "a[" + lo + "]+" + "a[" + hi + "] = " + data[lo] + "+" + data[hi] + " = " + sum
                            + (sum == target ? " = target ✓" : sum < target ? " < " + target : " > " + target),
                    "a[" + lo + "]", "a[" + hi + "]")),
                    sum == target ? "命中！" : sum < target
                            ? "太小 → lo 右移（更大的数才可能凑够）"
                            : "太大 → hi 左移（更小的数才可能凑够）");
            if (sum == target) {
                emit(L_FOUND, List.of(VizEvent.of(VizEvent.EventType.MARK_SORTED,
                        "找到！a[" + lo + "] + a[" + hi + "] = " + target, "a[" + lo + "]", "a[" + hi + "]")),
                        "双指针相遇——夹逼完成");
                return new Trace(SCHEMA, meta(), steps);
            }
            if (sum < target) {
                lo++;
                emit(L_GO_RIGHT, List.of(VizEvent.of(VizEvent.EventType.SET,
                        "lo → " + lo + "（太小，右移找更大的）")), "lo 右移");
            } else {
                hi--;
                emit(L_GO_LEFT, List.of(VizEvent.of(VizEvent.EventType.SET,
                        "hi → " + hi + "（太大，左移找更小的）")), "hi 左移");
            }
        }
        emit(L_NOT_FOUND, List.of(VizEvent.of(VizEvent.EventType.RETURN,
                "lo ≥ hi：指针交叉 → 没有满足条件的两个数")), "指针交叉 = 全部排除");
        return new Trace(SCHEMA, meta(), steps);
    }

    private void emit(int line, List<VizEvent> events, String why) {
        StackFrame frame = new StackFrame("twoPointers(target=" + target + ")",
                OrderedMaps.of("a", Value.ref("@1"), "lo", Value.prim(lo), "hi", Value.prim(hi)), null);
        Map<String, Layer> layers = new LinkedHashMap<>();
        layers.put("mem.stack", new StackLayer(List.of(frame)));
        layers.put("mem.heap", new HeapLayer(snapshotHeap()));
        layers.put("ds.array", snapshotArray());
        steps.add(new Step(steps.size(), Loc.line(line, why),
                Collections.unmodifiableMap(layers), List.copyOf(events), new Intent(why, INVARIANT)));
    }

    private Heap snapshotHeap() {
        List<Value> elems = new ArrayList<>();
        for (int v : data) elems.add(Value.prim(v));
        HeapObject arr = new HeapObject("@1", "a", "int[" + data.length + "]",
                new LinkedHashMap<>(), List.copyOf(elems));
        Map<String, HeapObject> objs = new LinkedHashMap<>();
        objs.put("@1", arr);
        return new Heap(Collections.unmodifiableMap(objs));
    }

    private ArrayView snapshotArray() {
        List<Integer> vals = new ArrayList<>();
        for (int v : data) vals.add(v);
        return new ArrayView("@1", "a", List.copyOf(vals),
                lo < data.length ? lo : null, hi >= 0 ? hi : null, null, null, data.length, -1);
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
        return java.util.List.of(Algorithm.joinArray(data), "int target = " + target + ";");
    }

    @Override
    public Complexity complexity() {
        return new Complexity("O(n) 一次遍历", "O(1) 两个指针",
                "lo/hi 各最多走 n 步 → 最多 n 次比较。对比暴力 O(n²)：两两配对要 n²/2 次——双指针是排序后的天然加速器");
    }
}
