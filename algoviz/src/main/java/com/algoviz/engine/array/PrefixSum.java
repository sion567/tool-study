package com.algoviz.engine.array;

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
 * 前缀和：prefix[i] = prefix[i-1] + a[i]——一次预处理，区间和 O(1) 查询。
 * 画面：原始数组在上方，前缀和在下方逐格生长。
 */
public final class PrefixSum implements Algorithm {

    private static final String SCHEMA = "1";
    private static final String INVARIANT =
            "prefix[i] = a[0]+…+a[i]——每一个格子都是从头到当前的累计和";

    private static final int L_DATA = 3;
    private static final int L_LOOP = 5;
    private static final int L_BODY = 6;
    private static final int L_END = 8;

    private final int[] input;
    private final int[] prefix;
    private final List<Step> steps = new ArrayList<>();
    private int curIdx = -1;

    public PrefixSum(int[] input) {
        this.input = input.clone();
        this.prefix = new int[input.length];
    }

    @Override public String id() { return "array.prefixsum"; }
    @Override public String title() { return "前缀和"; }
    @Override public String codeResource() { return "/code/PrefixSum.java"; }
    @Override public String docResource() { return "/markdown/prefix-sum.md"; }

    @Override
    public Trace produce() {
        emit(L_DATA, List.of(VizEvent.of(VizEvent.EventType.INIT,
                "原始数组就位，prefix 全 0")), "原始数据在上方，前缀和在下方逐格生长");

        prefix[0] = input[0];
        curIdx = 0;
        emit(L_LOOP, List.of(VizEvent.of(VizEvent.EventType.SET,
                "prefix[0] ← a[0] = " + input[0], "prefix:0")),
                "第一个元素的前缀和就是它自己");

        for (int i = 1; i < input.length; i++) {
            curIdx = i;
            prefix[i] = prefix[i - 1] + input[i];
            emit(L_BODY, List.of(VizEvent.of(VizEvent.EventType.SET,
                    "prefix[" + i + "] ← prefix[" + (i - 1) + "] + a[" + i + "] = "
                            + prefix[i - 1] + " + " + input[i] + " = " + prefix[i],
                    "prefix:" + i, "a:" + i)),
                    "每个前缀和 = 前一个前缀和 + 当前元素——只看一眼邻居，不回头重算");
        }
        curIdx = -1;
        emit(L_END, List.of(VizEvent.of(VizEvent.EventType.RETURN,
                "前缀和完成：区间和 [l,r] = prefix[r] - prefix[l-1]，查询 O(1)")),
                "预处理 O(n) 一次，之后任意区间和都是 O(1)——用空间换时间");
        return new Trace(SCHEMA, meta(), steps);
    }

    private void emit(int line, List<VizEvent> events, String why) {
        StackFrame frame = new StackFrame("prefixSum",
                Map.of("a", Value.ref("@1"), "prefix", Value.ref("@2")), null);
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
        for (int v : input) aElems.add(Value.prim(v));
        objs.put("@1", new HeapObject("@1", "a", "int[" + input.length + "]",
                new LinkedHashMap<>(), List.copyOf(aElems)));
        List<Value> pElems = new ArrayList<>();
        for (int v : prefix) pElems.add(Value.prim(v));
        objs.put("@2", new HeapObject("@2", "prefix", "int[" + prefix.length + "]",
                new LinkedHashMap<>(), List.copyOf(pElems)));
        return new Heap(Collections.unmodifiableMap(objs));
    }

    private ArrayView snapshotArray() {
        List<Integer> aVals = new ArrayList<>();
        for (int v : input) aVals.add(v);
        List<Integer> pVals = new ArrayList<>();
        for (int v : prefix) pVals.add(v);
        return new ArrayView("@1", "a", List.copyOf(aVals),
                curIdx >= 0 ? curIdx : null, null, null, null, input.length, -1);
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
        return new Complexity("O(n) 预处理 + O(1) 查询", "O(n) 前缀数组",
                "遍历一次建表 O(n)；之后任意区间和 [l,r] = prefix[r] - prefix[l-1]，O(1)——用空间换时间");
    }
}
