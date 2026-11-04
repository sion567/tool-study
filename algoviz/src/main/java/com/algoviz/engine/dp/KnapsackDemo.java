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
import com.algoviz.engine.mem.Heap;
import com.algoviz.engine.mem.HeapLayer;
import com.algoviz.engine.mem.HeapObject;
import com.algoviz.engine.math.MathView;
import com.algoviz.engine.mem.StackFrame;
import com.algoviz.engine.mem.StackLayer;
import com.algoviz.engine.trace.Trace;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import java.util.Map;

/**
 * 0/1 背包：表格逐行填值——每行考虑一件物品放或不放，取最大值。
 * 不变量：dp[i][w] = 前 i 件物品在容量 w 下的最大价值。
 */
public final class KnapsackDemo implements Algorithm {

    private static final String SCHEMA = "1";
    private static final String INVARIANT =
            "dp[i][w] = 前 i 件在容量 w 下的最大价值——每行多考虑一件，价值只增不减";

    private static final int[] WEIGHTS = {1, 3, 4};
    private static final int[] VALUES = {15, 20, 30};
    private static final int CAPACITY = 4;
    private static final int N = WEIGHTS.length;

    private final List<Step> steps = new ArrayList<>();
    private final int[][] dp = new int[N + 1][CAPACITY + 1];
    private int curI = -1, curW = -1;

    @Override public String id() { return "dp.knapsack"; }
    @Override public String title() { return "0/1 背包"; }
    @Override public String codeResource() { return "/code/KnapsackDemo.java"; }
    @Override public String docResource() { return "/markdown/knapsack.md"; }

    @Override
    public Trace produce() {
        emit(3, List.of(VizEvent.of(VizEvent.EventType.INIT,
                "dp[0][*] = 0（没有物品可放）")), "第 0 行：没有物品，价值全 0");

        for (int i = 1; i <= N; i++) {
            curI = i;
            int w = WEIGHTS[i - 1], v = VALUES[i - 1];
            emit(5, List.of(VizEvent.of(VizEvent.EventType.INIT,
                    "考虑物品 " + i + "（重量=" + w + ", 价值=" + v + "）")),
                    "第 " + i + " 件物品：放或不放，取最大值");
            for (int cap = 0; cap <= CAPACITY; cap++) {
                curW = cap;
                if (w > cap) {
                    dp[i][cap] = dp[i - 1][cap];
                    emit(7, List.of(VizEvent.of(VizEvent.EventType.COMPARE,
                            "重 " + w + " > 容量 " + cap + " → 放不下，继承上一行",
                            "dp:" + (i * (CAPACITY + 1) + cap))),
                            "装不下：dp[i][cap] = dp[i-1][cap]");
                } else {
                    int skip = dp[i - 1][cap];
                    int take = dp[i - 1][cap - w] + v;
                    dp[i][cap] = Math.max(skip, take);
                    emit(8, List.of(VizEvent.of(VizEvent.EventType.COMPARE,
                            "不放=" + skip + " vs 放=" + take + " → dp[" + i + "][" + cap + "]=" + dp[i][cap],
                            "dp:" + (i * (CAPACITY + 1) + cap))),
                            "max(不放=" + skip + ", 放=" + take + ") → 取较大者");
                }
            }
        }
        curI = -1; curW = -1;
        emit(10, List.of(VizEvent.of(VizEvent.EventType.RETURN,
                "最优解 = dp[" + N + "][" + CAPACITY + "] = " + dp[N][CAPACITY])),
                "右下角就是答案：前 N 件在最大容量下的最大价值");
        return new Trace(SCHEMA, meta(), steps);
    }

    private void emit(int line, List<VizEvent> events, String why) {
        StackFrame frame = new StackFrame("knapsack",
                OrderedMaps.of("物品", Value.prim(curI >= 0 ? "第" + curI + "件" : "初始化"),
                       "容量", Value.prim(curW >= 0 ? String.valueOf(curW) : "-")), null);
        Map<String, Layer> layers = new LinkedHashMap<>();
        layers.put("mem.stack", new StackLayer(List.of(frame)));
        layers.put("mem.heap", new HeapLayer(snapshotHeap()));
        layers.put("math.matrix", snapshotMatrix());
        steps.add(new Step(steps.size(), Loc.line(line, why),
                Collections.unmodifiableMap(layers), List.copyOf(events), new Intent(why, INVARIANT)));
    }

    private Heap snapshotHeap() {
        Map<String, HeapObject> objs = new LinkedHashMap<>();
        Map<String, Value> fields = new LinkedHashMap<>();
        for (int i = 0; i <= N; i++) {
            for (int w = 0; w <= CAPACITY; w++) {
                fields.put("dp" + i + "_" + w, Value.prim(dp[i][w]));
            }
        }
        objs.put("@1", new HeapObject("@1", "dp", "DP_TABLE",
                Collections.unmodifiableMap(fields), List.of()));
        return new Heap(Collections.unmodifiableMap(objs));
    }

    private MathView snapshotMatrix() {
        List<MathView.Grid> grids = new ArrayList<>();
        for (int i = 0; i <= N; i++) {
            List<Integer> row = new ArrayList<>();
            for (int w = 0; w <= CAPACITY; w++) {
                row.add(dp[i][w]);
            }
            boolean isActive = i == curI;
            grids.add(new MathView.Grid("dp" + i, "物品" + i, 1, CAPACITY + 1,
                    row.stream().map(String::valueOf).toList(),
                    null, null,
                    isActive && curW >= 0 ? Set.of(curW) : Set.of(), Set.of()));
        }
        return new MathView("0/1 背包 DP 表", Collections.unmodifiableList(grids));
    }

    private Trace.TraceMeta meta() {
        return new Trace.TraceMeta(id(), title(), codeResource(), "3 物品 容量4", N + 1);
    }

    @Override
    public java.util.List<String> dataLines() {
        return java.util.List.of(
                "int[] weights = " + java.util.Arrays.toString(WEIGHTS) + ";",
                "int[] values = " + java.util.Arrays.toString(VALUES) + ";",
                "int capacity = " + CAPACITY + ";");
    }

    @Override
    public Complexity complexity() {
        return new Complexity("O(nW) 两层循环", "O(nW) 二维表",
                "n 件物品 × W 个容量 → nW 个子问题，每个 O(1)。可优化到 O(W)（一维滚动数组）");
    }
}
