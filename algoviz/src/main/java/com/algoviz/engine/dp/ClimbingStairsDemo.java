package com.algoviz.engine.dp;

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
 * 爬楼梯（DP）：每次爬 1 或 2 阶，到第 n 阶有多少种走法。
 * 不变量：ways[i] = ways[i-1] + ways[i-2]——到第 i 阶的最后一步只能从 i-1 或 i-2 迈过来。
 */
public final class ClimbingStairsDemo implements Algorithm {

    private static final String SCHEMA = "1";
    private static final String INVARIANT =
            "ways[i] = ways[i-1] + ways[i-2]——最后一步只能跨 1 或 2 阶，所以走到 i 的方法数 = 走到 i-1 的 + 走到 i-2 的";

    private final int N;

    public ClimbingStairsDemo() {
        this(6);
    }

    public ClimbingStairsDemo(int n) {
        this.N = n;
        this.ways = new int[N + 1];
    }

    private final List<Step> steps = new ArrayList<>();
    private final int[] ways;
    private int curIdx = -1;

    @Override public String id() { return "dp.climbingstairs"; }
    @Override public String title() { return "爬楼梯"; }
    @Override public String codeResource() { return "/code/ClimbingStairsDemo.java"; }
    @Override public String docResource() { return "/markdown/climbing-stairs.md"; }

    @Override
    public Trace produce() {
        ways[0] = 1;
        curIdx = 0;
        emit(3, List.of(VizEvent.of(VizEvent.EventType.INIT,
                "ways[0] = 1（站在地面，不动也算一种方式）")),
                "基线：站在第 0 阶，什么都不做");
        if (N >= 1) {
            ways[1] = 1;
            curIdx = 1;
            emit(4, List.of(VizEvent.of(VizEvent.EventType.SET,
                    "ways[1] = 1（只能跨 1 阶到这）", "dp:1")),
                    "从 0 到 1 只能走一步：1 种方式");
        }
        for (int i = 2; i <= N; i++) {
            curIdx = i;
            ways[i] = ways[i - 1] + ways[i - 2];
            emit(6, List.of(VizEvent.of(VizEvent.EventType.SET,
                    "ways[" + i + "] = ways[" + (i - 1) + "] + ways[" + (i - 2) + "] = "
                            + ways[i - 1] + " + " + ways[i - 2] + " = " + ways[i], "dp:" + i)),
                    "最后一步跨 1 阶（从 " + (i - 1) + "）或跨 2 阶（从 " + (i - 2) + "）→ 两种走法之和");
        }
        curIdx = -1;
        emit(8, List.of(VizEvent.of(VizEvent.EventType.RETURN,
                "走到第 " + N + " 阶共有 " + ways[N] + " 种方式")),
                "O(n) 时间 O(1) 空间（只保留前两个值）——就是斐波那契换了个故事");
        return new Trace(SCHEMA, meta(), steps);
    }

    private void emit(int line, List<VizEvent> events, String why) {
        Map<String, Value> lm = new LinkedHashMap<>();
        lm.put("n", Value.prim(N));
        lm.put("cur", Value.prim(curIdx >= 0 ? "ways[" + curIdx + "]" : "-"));
        StackFrame frame = new StackFrame("climbStairs", Collections.unmodifiableMap(lm), null);
        Map<String, Layer> layers = new LinkedHashMap<>();
        layers.put("mem.stack", new StackLayer(List.of(frame)));
        layers.put("mem.heap", new HeapLayer(snapshotHeap()));
        layers.put("ds.array", snapshotArray());
        steps.add(new Step(steps.size(), Loc.line(line, why),
                Collections.unmodifiableMap(layers), List.copyOf(events), new Intent(why, INVARIANT)));
    }

    private Heap snapshotHeap() {
        Map<String, HeapObject> objs = new LinkedHashMap<>();
        List<Value> elems = new ArrayList<>();
        for (int v : ways) elems.add(Value.prim(v));
        objs.put("@1", new HeapObject("@1", "ways", "int[" + ways.length + "]",
                new LinkedHashMap<>(), List.copyOf(elems)));
        return new Heap(Collections.unmodifiableMap(objs));
    }

    private ArrayView snapshotArray() {
        List<Integer> vals = new ArrayList<>();
        for (int v : ways) vals.add(v);
        return new ArrayView("@1", "ways", List.copyOf(vals),
                curIdx >= 0 ? curIdx : null, null, null, null, ways.length, -1);
    }

    private Trace.TraceMeta meta() {
        return new Trace.TraceMeta(id(), title(), codeResource(), "n=" + N, N + 1);
    }

    @Override
    public java.util.List<String> dataLines() {
        return java.util.List.of("int n = " + N + ";");
    }

    @Override
    public Complexity complexity() {
        return new Complexity("O(n) 一趟填表", "O(1) 只保留前两个值",
                "递推式和斐波那契完全一样——爬楼梯就是「穿了马甲的斐波那契」");
    }
}
