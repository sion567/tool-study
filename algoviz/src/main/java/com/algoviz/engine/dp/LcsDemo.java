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
import java.util.Map;
import java.util.Set;

/**
 * 最长公共子序列（LCS）：二维 DP 表逐格填值——匹配↖、跳左←、跳上↑。
 * 不变量：dp[i][j] = s1[0..i) 与 s2[0..j) 的 LCS 长度——每格只依赖左、上、左上三格。
 */
public final class LcsDemo implements Algorithm {

    private static final String SCHEMA = "1";
    private static final String INVARIANT =
            "dp[i][j] = LCS(s1[0..i), s2[0..j))——字符相等取↖+1，不等取 max(←,↑)";

    private static final String S1 = "ABCBDAB";
    private static final String S2 = "BDCABA";

    private final List<Step> steps = new ArrayList<>();
    private final int[][] dp;
    private int curI = -1, curJ = -1;

    public LcsDemo() {
        dp = new int[S1.length() + 1][S2.length() + 1];
    }

    @Override public String id() { return "dp.lcs"; }
    @Override public String title() { return "LCS"; }
    @Override public String codeResource() { return "/code/LcsDemo.java"; }
    @Override public String docResource() { return "/markdown/lcs.md"; }

    @Override
    public Trace produce() {
        int m = S1.length(), n = S2.length();
        emit(3, List.of(VizEvent.of(VizEvent.EventType.INIT,
                "DP 表初始化：dp[0][*] = dp[*][0] = 0")),
                "边界：空串与任何串的 LCS = 0");
        for (int i = 1; i <= m; i++) {
            for (int j = 1; j <= n; j++) {
                curI = i; curJ = j;
                if (S1.charAt(i - 1) == S2.charAt(j - 1)) {
                    dp[i][j] = dp[i - 1][j - 1] + 1;
                    emit(5, List.of(VizEvent.of(VizEvent.EventType.SET,
                            S1.charAt(i - 1) + " == " + S2.charAt(j - 1)
                                    + " → dp[" + i + "][" + j + "] = dp[" + (i - 1) + "][" + (j - 1) + "] + 1 = "
                                    + dp[i][j],
                            "dp:" + (i * (n + 1) + j))),
                            "字符相等 → 取↖+1（两个指针同时后退）");
                } else {
                    int fromLeft = dp[i][j - 1], fromUp = dp[i - 1][j];
                    dp[i][j] = Math.max(fromLeft, fromUp);
                    emit(7, List.of(VizEvent.of(VizEvent.EventType.COMPARE,
                            S1.charAt(i - 1) + " ≠ " + S2.charAt(j - 1)
                                    + " → max(←" + fromLeft + ", ↑" + fromUp + ") = " + dp[i][j],
                            "dp:" + (i * (n + 1) + j))),
                            "不等 → 跳过一个（← 或 ↑），取较大者");
                }
            }
        }
        curI = -1; curJ = -1;
        emit(9, List.of(VizEvent.of(VizEvent.EventType.RETURN,
                "LCS 长度 = " + dp[m][n] + "（右下角）——回溯箭头可还原具体序列")),
                "右下角 = LCS 长度，沿箭头回溯可还原子序列本身");
        return new Trace(SCHEMA, meta(), steps);
    }

    private void emit(int line, List<VizEvent> events, String why) {
        StackFrame frame = new StackFrame("lcs",
                OrderedMaps.of("s1", Value.prim(S1), "s2", Value.prim(S2),
                       "dp[" + curI + "][" + curJ + "]",
                       Value.prim(curI >= 0 && curJ >= 0 ? String.valueOf(dp[curI][curJ]) : "-")), null);
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
        for (int i = 0; i <= S1.length(); i++) {
            for (int j = 0; j <= S2.length(); j++) {
                fields.put("dp" + i + "_" + j, Value.prim(dp[i][j]));
            }
        }
        objs.put("@1", new HeapObject("@1", "dp", "DP_TABLE",
                Collections.unmodifiableMap(fields), List.of()));
        return new Heap(Collections.unmodifiableMap(objs));
    }

    private MathView snapshotMatrix() {
        List<MathView.Grid> grids = new ArrayList<>();
        for (int i = 0; i <= S1.length(); i++) {
            List<String> row = new ArrayList<>();
            for (int j = 0; j <= S2.length(); j++) {
                row.add(String.valueOf(dp[i][j]));
            }
            grids.add(new MathView.Grid("row" + i, "dp[" + i + "]", 1, S2.length() + 1,
                    row, null, null, Set.of(), Set.of()));
        }
        return new MathView("LCS DP 表", Collections.unmodifiableList(grids));
    }

    private Trace.TraceMeta meta() {
        return new Trace.TraceMeta(id(), title(), codeResource(),
                S1 + "/" + S2, S1.length() + S2.length());
    }

    @Override
    public java.util.List<String> dataLines() {
        return java.util.List.of(
                "String s1 = \"" + S1 + "\";",
                "String s2 = \"" + S2 + "\";");
    }

    @Override
    public Complexity complexity() {
        return new Complexity("O(mn) 两层循环", "O(mn) 二维表",
                "m 行 n 列逐格填值，每格 O(1)。可优化到 O(min(m,n)) 空间（滚动数组）");
    }
}
