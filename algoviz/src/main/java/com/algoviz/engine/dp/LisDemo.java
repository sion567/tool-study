package com.algoviz.engine.dp;

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

/** 最长递增子序列 LIS：dp[i] = 以 a[i] 结尾的 LIS 长度。 */
public final class LisDemo implements Algorithm {

    private static final String SCHEMA = "1";
    private static final String INVARIANT =
            "dp[i] = 以 a[i] 结尾的 LIS 长度——对每个 j < i 且 a[j] < a[i]，dp[i] = max(dp[j]) + 1";

    private static final int[] A = {3, 1, 4, 1, 5, 9, 2, 6};

    private final List<Step> steps = new ArrayList<>();
    private final int[] dp = new int[A.length];
    private int curI = -1, curJ = -1;

    @Override public String id() { return "dp.lis"; }
    @Override public String title() { return "最长递增子序列"; }
    @Override public String codeResource() { return "/code/LisDemo.java"; }
    @Override public String docResource() { return "/markdown/lis.md"; }

    @Override
    public Trace produce() {
        java.util.Arrays.fill(dp, 1);
        emit(3, List.of(VizEvent.of(VizEvent.EventType.INIT, "dp 全初始化为 1（每个元素自身是长度 1 的 LIS）")),
                "基线：每个元素自身构成长度 1 的 LIS");
        for (int i = 1; i < A.length; i++) {
            curI = i;
            for (int j = 0; j < i; j++) {
                curJ = j;
                if (A[j] < A[i]) {
                    if (dp[j] + 1 > dp[i]) {
                        dp[i] = dp[j] + 1;
                        emit(6, List.of(VizEvent.of(VizEvent.EventType.SET,
                                "a[" + j + "]=" + A[j] + " < a[" + i + "]=" + A[i]
                                        + " → dp[" + i + "] = dp[" + j + "]+1 = " + dp[i],
                                "a:" + j, "a:" + i)),
                                "a[j] < a[i]：可以接在 j 的 LIS 后面");
                    }
                }
            }
            curJ = -1;
            emit(8, List.of(VizEvent.of(VizEvent.EventType.MARK_SORTED,
                    "dp[" + i + "] = " + dp[i] + "（确定）", "a:" + i)),
                    "a[" + i + "] 的 LIS 长度确定");
        }
        curI = -1; curJ = -1;
        int max = 0;
        for (int v : dp) max = Math.max(max, v);
        emit(9, List.of(VizEvent.of(VizEvent.EventType.RETURN,
                "LIS 长度 = " + max)), "全局最大 dp 值 = LIS 长度");
        return new Trace(SCHEMA, meta(), steps);
    }

    private void emit(int line, List<VizEvent> events, String why) {
        StackFrame frame = new StackFrame("lis",
                OrderedMaps.of("curI", Value.prim(curI), "curJ", Value.prim(curJ)), null);
        Map<String, Layer> layers = new LinkedHashMap<>();
        layers.put("mem.stack", new StackLayer(List.of(frame)));
        layers.put("mem.heap", new HeapLayer(snapshotHeap()));
        layers.put("ds.array", snapshotArray());
        steps.add(new Step(steps.size(), Loc.line(line, why),
                Collections.unmodifiableMap(layers), List.copyOf(events), new Intent(why, INVARIANT)));
    }

    private Heap snapshotHeap() {
        Map<String, HeapObject> objs = new LinkedHashMap<>();
        List<Value> dpElems = new ArrayList<>();
        for (int v : dp) dpElems.add(Value.prim(v));
        List<Value> aElems = new ArrayList<>();
        for (int v : A) aElems.add(Value.prim(v));
        objs.put("@1", new HeapObject("@1", "a", "int[" + A.length + "]",
                new LinkedHashMap<>(), List.copyOf(aElems)));
        objs.put("@2", new HeapObject("@2", "dp", "int[" + A.length + "]",
                new LinkedHashMap<>(), List.copyOf(dpElems)));
        return new Heap(Collections.unmodifiableMap(objs));
    }

    private ArrayView snapshotArray() {
        List<Integer> vals = new ArrayList<>();
        for (int v : A) vals.add(v);
        return new ArrayView("@1", "a", List.copyOf(vals),
                curJ >= 0 ? curJ : null, curI >= 0 ? curI : null, null, null, A.length, -1);
    }

    private Trace.TraceMeta meta() {
        StringBuilder sb = new StringBuilder();
        for (int k = 0; k < A.length; k++) { if (k > 0) sb.append(','); sb.append(A[k]); }
        return new Trace.TraceMeta(id(), title(), codeResource(), sb.toString(), A.length);
    }

    @Override
    public java.util.List<String> dataLines() {
        return java.util.List.of(Algorithm.joinArray(A));
    }

    @Override
    public Complexity complexity() {
        return new Complexity("O(n²) DP 版 / O(n log n) 贪心+二分版", "O(n) dp 数组",
                "双层循环：对每个 i，扫所有 j < i 找最长可接的 LIS → O(n²)。贪心+二分可到 O(n log n)");
    }
}
