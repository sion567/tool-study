package com.algoviz.engine.backtracking;

import com.algoviz.engine.Algorithm;
import com.algoviz.engine.Intent;
import com.algoviz.engine.Layer;
import com.algoviz.engine.ds.ArrayView;
import com.algoviz.engine.Complexity;
import com.algoviz.engine.Loc;
import com.algoviz.engine.Step;
import com.algoviz.engine.Value;
import com.algoviz.engine.VizEvent;
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
 * 全排列（回溯）：选择树逐层展开——每层选一个没用过的元素，递归完撤回（回溯）。
 * 画面：递归栈逐层压弹，当前排列实时显示，回溯 = 撤销最后的选择。
 */
public final class PermutationDemo implements Algorithm {

    private static final String SCHEMA = "1";
    private static final String INVARIANT =
            "每一层选一个没用过的元素——递归完撤回（回溯），让同层试下一个";

    private final int[] INPUT;
    private final int N;

    public PermutationDemo() {
        this(new int[]{1, 2, 3});
    }

    public PermutationDemo(int[] input) {
        this.INPUT = input.clone();
        this.N = INPUT.length;
        this.used = new boolean[N];
    }

    private static final int L_CALL = 3;
    private static final int L_DONE = 5;
    private static final int L_RET = 6;
    private static final int L_CHOOSE = 8;
    private static final int L_UNDO = 10;

    private final List<Step> steps = new ArrayList<>();
    private final List<StackFrame> frames = new ArrayList<>();
    private final List<Integer> current = new ArrayList<>();
    private final boolean[] used;
    private final List<List<Integer>> allResults = new ArrayList<>();

    @Override public String id() { return "backtracking.permutation"; }
    @Override public String title() { return "全排列"; }
    @Override public String codeResource() { return "/code/PermutationDemo.java"; }
    @Override public String docResource() { return "/markdown/permutation.md"; }

    @Override
    public Trace produce() {
        backtrack(0);
        return new Trace(SCHEMA, meta(), steps);
    }

    private void backtrack(int depth) {
        push(depth);
        emit(L_CALL, List.of(VizEvent.of(VizEvent.EventType.CALL,
                "进入 backtrack(depth=" + depth + ")，当前排列 " + current)),
                "第 " + depth + " 层：选择排列中的第 " + depth + " 个位置");
        if (depth == N) {
            allResults.add(new ArrayList<>(current));
            emit(L_DONE, List.of(VizEvent.of(VizEvent.EventType.MARK_SORTED,
                    "找到排列: " + current + " ← 第 " + allResults.size() + " 个")),
                    "填满 N 个位置 → 一个完整排列");
            emit(L_RET, List.of(VizEvent.of(VizEvent.EventType.RETURN, "回溯（弹栈）")),
                    "排列已收集，撤销最后选择，回到上一层");
            pop();
            return;
        }
        for (int i = 0; i < N; i++) {
            if (used[i]) continue;
            used[i] = true;
            current.add(INPUT[i]);
            emit(L_CHOOSE, List.of(VizEvent.of(VizEvent.EventType.SET,
                    "选择 " + INPUT[i] + " → 排列变为 " + current)),
                    "选择 INPUT[" + i + "]=" + INPUT[i] + " 放入当前位置");
            backtrack(depth + 1);
            used[i] = false;
            current.remove(current.size() - 1);
            emit(L_UNDO, List.of(VizEvent.of(VizEvent.EventType.SET,
                    "撤销 " + INPUT[i] + " → 排列回到 " + current)),
                    "回溯：撤销本层选择，让同层试下一个候选");
        }
        emit(L_RET, List.of(VizEvent.of(VizEvent.EventType.RETURN, "回溯（弹栈）")),
                "所有候选都试完了，返回上一层");
        pop();
    }

    private void push(int depth) {
        Map<String, Value> locals = new LinkedHashMap<>();
        locals.put("depth", Value.prim(depth));
        List<Value> arr = new ArrayList<>();
        for (int v : current) arr.add(Value.prim(v));
        locals.put("current", Value.prim(current.toString()));
        frames.add(new StackFrame("backtrack(" + depth + ")", Collections.unmodifiableMap(locals), null));
    }

    private void pop() {
        frames.remove(frames.size() - 1);
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
        List<Value> elems = new ArrayList<>();
        for (int v : INPUT) elems.add(Value.prim(v));
        objs.put("@1", new HeapObject("@1", "input", "int[" + N + "]",
                new LinkedHashMap<>(), List.copyOf(elems)));
        return new Heap(Collections.unmodifiableMap(objs));
    }

    private ArrayView snapshotArray() {
        List<Integer> vals = new ArrayList<>();
        for (int v : INPUT) vals.add(v);
        return new ArrayView("@1", "input", List.copyOf(vals),
                null, null, null, null, N, -1);
    }

    private Trace.TraceMeta meta() {
        StringBuilder sb = new StringBuilder();
        for (int k = 0; k < N; k++) {
            if (k > 0) sb.append(',');
            sb.append(INPUT[k]);
        }
        return new Trace.TraceMeta(id(), title(), codeResource(), sb.toString(), N);
    }

    @Override
    public java.util.List<String> dataLines() {
        return java.util.List.of(Algorithm.joinArray(INPUT));
    }

    @Override
    public Complexity complexity() {
        return new Complexity("O(n!)", "O(n) 递归栈 + O(n) 排列",
                "n 个位置每层 n 个候选 → n! 个叶子。回溯 = 试→撤→试下一个，同层候选不重复");
    }
}
