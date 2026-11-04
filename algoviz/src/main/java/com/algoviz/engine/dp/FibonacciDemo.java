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
 * 斐波那契三版对比：递归（O(2ⁿ)）→ 备忘录（O(n) 但有栈）→ DP（O(n) O(1) 空间）。
 * 画面：三个数组并排，递归调用标红重复，备忘录/DP 逐格填值。
 */
public final class FibonacciDemo implements Algorithm {

    private static final String SCHEMA = "1";
    private static final String INVARIANT =
            "fib(n) = fib(n-1) + fib(n-2)——备忘录消除重复子问题；DP 从底向上填表；空间从 O(n) 降到 O(1)";

    private static final int N = 7;   // fib(7) = 13
    private static final int[] EXPECTED = {0, 1, 1, 2, 3, 5, 8, 13};

    private final List<Step> steps = new ArrayList<>();
    private final int[] dp = new int[N + 1];
    private final boolean[] memo = new boolean[N + 1];
    private int[] callCounts;   // 递归版每个 fib(k) 被调用几次
    private int curIdx = -1;
    private String versionTag = "";

    @Override public String id() { return "dp.fibonacci"; }
    @Override public String title() { return "斐波那契三版对比"; }
    @Override public String codeResource() { return "/code/FibonacciDemo.java"; }
    @Override public String docResource() { return "/markdown/fibonacci.md"; }

    @Override
    public Trace produce() {
        java.util.Arrays.fill(dp, 0);
        java.util.Arrays.fill(memo, false);
        callCounts = new int[N + 1];

        // ===== 版本一：纯递归（展示重复调用） =====
        versionTag = "递归 O(2ⁿ)";
        curIdx = N;
        emit(4, List.of(VizEvent.of(VizEvent.EventType.INIT,
                "递归版：fib(" + N + ")——注意重复子问题（红圈标出）")),
                "递归版：fib(5) 算了 2 次、fib(4) 算了 3 次、fib(3) 算了 5 次——大量浪费");
        int calls = countCalls(N);
        emit(6, List.of(VizEvent.of(VizEvent.EventType.MARK_SORTED,
                "递归完成：总共 " + calls + " 次函数调用（O(2ⁿ)）")),
                "指数爆炸：fib(n) 的调用次数 ≈ 2 × fib(n-1) 的调用次数");
        showCallCounts();

        // ===== 版本二：备忘录（缓存消除重复） =====
        versionTag = "备忘录 O(n)";
        java.util.Arrays.fill(callCounts, 0);
        java.util.Arrays.fill(dp, 0);
        java.util.Arrays.fill(memo, false);
        curIdx = -1;
        emit(9, List.of(VizEvent.of(VizEvent.EventType.INIT,
                "备忘录版：memo 数组缓存已算过的结果")),
                "备忘录 = 缓存已算过的子问题——重复调用直接查表");
        for (int i = 0; i <= N; i++) {
            curIdx = i;
            if (!memo[i]) {
                dp[i] = fibMemo(i);
                memo[i] = true;
            }
            emit(10, List.of(VizEvent.of(VizEvent.EventType.SET,
                    "fib(" + i + ") = " + dp[i] + "（缓存命中或首次计算）", "dp:" + i)),
                    "备忘录版：每个子问题只算一次 → O(n)");
        }

        // ===== 版本三：DP（从底向上，O(1) 空间） =====
        versionTag = "DP O(1) 空间";
        java.util.Arrays.fill(dp, 0);
        curIdx = -1;
        emit(13, List.of(VizEvent.of(VizEvent.EventType.INIT,
                "DP 版：从底向上填表，只保留前两个值 → O(1) 空间")),
                "DP 版：不递归、不缓存全部——只保留前两个值");
        int prev2 = 0, prev1 = 1;
        dp[0] = 0; dp[1] = 1;
        for (int i = 2; i <= N; i++) {
            curIdx = i;
            dp[i] = dp[i - 1] + dp[i - 2];
            prev2 = dp[i - 2]; prev1 = dp[i - 1];
            dp[i] = prev2 + prev1;
            emit(15, List.of(VizEvent.of(VizEvent.EventType.SET,
                    "dp[" + i + "] = dp[" + (i - 1) + "] + dp[" + (i - 2) + "] = " + prev1 + " + " + prev2
                            + " = " + dp[i], "dp:" + i)),
                    "DP 填表：从底向上，每个格子只依赖前两个");
        }
        emit(17, List.of(VizEvent.of(VizEvent.EventType.RETURN,
                "三版对比：递归 O(2ⁿ) → 备忘录 O(n)/O(n) → DP O(n)/O(1)")),
                "DP = 递归 + 备忘录 + 空间优化——同一问题的三种「记忆策略」");
        return new Trace(SCHEMA, meta(), steps);
    }

    private int fibMemo(int i) {
        callCounts[i]++;
        if (i <= 1) return i;
        if (memo[i]) return dp[i];
        dp[i] = fibMemo(i - 1) + fibMemo(i - 2);
        memo[i] = true;
        return dp[i];
    }

    private int countCalls(int n) {
        // fib(n) 的调用次数 = 2 * fib(n+1) - 1（标准公式）
        // 简化：用递推计数
        int[] calls = new int[N + 2];
        calls[0] = 1; calls[1] = 1;
        for (int i = 2; i <= N; i++) calls[i] = calls[i - 1] + calls[i - 2] + 1;
        int total = 0;
        for (int i = 0; i <= N; i++) total += calls[i];
        return total;
    }

    private void showCallCounts() {
        versionTag = "递归调用统计";
        for (int i = 0; i <= N; i++) {
            curIdx = i;
            emit(6, List.of(VizEvent.of(VizEvent.EventType.COMPARE,
                    "fib(" + i + ") 被调用 " + callCounts[i] + " 次", "dp:" + i)),
                    "递归版调用统计：小下标被重复调用多次——这就是 O(2ⁿ) 的来源");
        }
    }

    private void emit(int line, List<VizEvent> events, String why) {
        Map<String, Value> lm = new LinkedHashMap<>();
        lm.put("version", Value.prim(versionTag));
        lm.put("cur", Value.prim(curIdx >= 0 ? "fib(" + curIdx + ")" : "-"));
        StackFrame frame = new StackFrame(versionTag, Collections.unmodifiableMap(lm), null);
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
        objs.put("@1", new HeapObject("@1", "dp/memo", "int[" + dp.length + "]",
                new LinkedHashMap<>(), List.copyOf(dpElems)));
        return new Heap(Collections.unmodifiableMap(objs));
    }

    private ArrayView snapshotArray() {
        List<Integer> vals = new ArrayList<>();
        for (int v : dp) vals.add(v);
        return new ArrayView("@1", "dp", List.copyOf(vals),
                curIdx >= 0 ? curIdx : null, null, null, null, dp.length, -1);
    }

    private Trace.TraceMeta meta() {
        return new Trace.TraceMeta(id(), title(), codeResource(), "fib(0..7)", N + 1);
    }

    @Override
    public java.util.List<String> dataLines() {
        return java.util.List.of("int n = " + N + ";");
    }

    @Override
    public Complexity complexity() {
        return new Complexity("递归 O(2ⁿ) → 备忘录/DP O(n)", "递归 O(h) 栈 → DP O(1) 空间",
                "三版对比：递归有大量重复子问题；备忘录缓存消除重复；DP 从底向上 + 空间优化到 O(1)");
    }
}
