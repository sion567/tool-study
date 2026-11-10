package com.algoviz.engine.math;

import com.algoviz.engine.Algorithm;
import com.algoviz.engine.Complexity;
import com.algoviz.engine.Intent;
import com.algoviz.engine.Layer;
import com.algoviz.engine.Loc;
import com.algoviz.engine.OrderedMaps;
import com.algoviz.engine.Step;
import com.algoviz.engine.Value;
import com.algoviz.engine.VizEvent;
import com.algoviz.engine.math.MathView;
import com.algoviz.engine.mem.Heap;
import com.algoviz.engine.mem.HeapLayer;
import com.algoviz.engine.mem.HeapObject;
import com.algoviz.engine.mem.StackFrame;
import com.algoviz.engine.mem.StackLayer;
import com.algoviz.engine.trace.Trace;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 杨辉三角（帕斯卡三角）：C(n,k) = C(n−1,k−1) + C(n,k−1)——每行由上一行相邻两数相加生成。
 * 一行一步，逐行点亮；它就是 DP 的祖先：每个数 = 两个已知子问题的和。
 */
public final class YangHuiDemo implements Algorithm {

    private static final String SCHEMA = "1";
    private static final String INVARIANT =
            "C(n,k) = C(n−1,k−1) + C(n,k−1)（边界 C(n,0)=1）——每个数都是上面两个数的和，且行内对称";

    private static final int ROWS = 9;   // 0..8 行

    private final List<Step> steps = new ArrayList<>();
    private final List<List<Integer>> triangle = new ArrayList<>();
    private int curRow = -1;

    @Override public String id() { return "math.yanghui"; }
    @Override public String title() { return "杨辉三角"; }
    @Override public String codeResource() { return "/code/YangHuiDemo.java"; }
    @Override public String docResource() { return "/markdown/yanghui.md"; }

    @Override
    public Trace produce() {
        steps.clear();
        triangle.clear();
        curRow = -1;
        emit(3, List.of(VizEvent.of(VizEvent.EventType.INIT,
                "两腰全是 1；中间每个数 = 肩上两数之和")),
                "贾宪（北宋）比帕斯卡早 600 年提出——名称之争本身就是数学史");
        for (int n = 0; n < ROWS; n++) {
            curRow = n;
            List<Integer> row = new ArrayList<>();
            List<Integer> prev = n > 0 ? triangle.get(n - 1) : null;
            for (int k = 0; k <= n; k++) {
                if (k == 0 || k == n) {
                    row.add(1);
                } else {
                    row.add(prev.get(k - 1) + prev.get(k));
                }
            }
            triangle.add(row);
            emit(6, List.of(VizEvent.of(VizEvent.EventType.SET,
                    "第 " + n + " 行：" + row +
                            (n >= 2 ? "   （如 " + (n / 2) + " 列 = 肩上 " + prev.get(Math.max(0, n / 2 - 1)) + "+" + prev.get(n / 2) + "）" : ""),
                    "第 " + n + " 行")),
                    "肩上两数相加——与 DP 状态转移方程同构：dp[n][k] = dp[n-1][k-1] + dp[n-1][k]");        }
        curRow = -1;
        emit(9, List.of(VizEvent.of(VizEvent.EventType.MARK_SORTED,
                "对角线读法：第 3 斜线 = 1,3,6,10…（三角形数）、第 2 斜线 = 自然数——藏在一张图里的数列全家福")),
                "对称性 C(n,k)=C(n,n−k)、行和 = 2ⁿ（第 8 行 1+8+28+…+1 = 256）——都在这张表里");
        emit(11, List.of(VizEvent.of(VizEvent.EventType.RETURN,
                "应用：C(n,k) 直接查表 O(1)——比递归组合计算快一万倍")),
                "空间换时间的鼻祖：一次加法生成，终身免算");
        return new Trace(SCHEMA, meta(), steps);
    }

    private void emit(int line, List<VizEvent> events, String why) {
        StackFrame frame = new StackFrame("yanghui",
                OrderedMaps.of("行", Value.prim(curRow >= 0 ? String.valueOf(curRow) : "-"),
                        "C(n,k)含义", Value.prim("从 n 个里选 k 个")), null);
        Map<String, Layer> layers = new LinkedHashMap<>();
        layers.put("mem.stack", new StackLayer(List.of(frame)));
        layers.put("mem.heap", new HeapLayer(snapshotHeap()));
        layers.put("math.matrix", snapshotMatrix());
        steps.add(new Step(steps.size(), Loc.line(line, why),
                Collections.unmodifiableMap(layers), List.copyOf(events), new Intent(why, INVARIANT)));
    }

    private Heap snapshotHeap() {
        Map<String, Value> fields = new LinkedHashMap<>();
        List<Integer> last = triangle.isEmpty() ? List.of() : triangle.get(triangle.size() - 1);
        fields.put("已生成行", Value.prim(triangle.size() + " / " + ROWS));
        fields.put("末行", Value.prim(last.toString()));
        int sum = last.stream().mapToInt(Integer::intValue).sum();
        fields.put("末行和 = 2ⁿ", Value.prim(String.valueOf(sum)));
        Map<String, HeapObject> objs = new LinkedHashMap<>();
        objs.put("@1", new HeapObject("@1", "triangle", "PascalTriangle",
                Collections.unmodifiableMap(fields), List.of()));
        return new Heap(Collections.unmodifiableMap(objs));
    }

    private MathView snapshotMatrix() {
        List<String> vals = new ArrayList<>();
        Set<Integer> done = new LinkedHashSet<>();
        Set<Integer> active = new LinkedHashSet<>();
        for (int r = 0; r < ROWS; r++) {
            List<Integer> row = r < triangle.size() ? triangle.get(r) : null;
            for (int c = 0; c < ROWS; c++) {
                int idx = r * ROWS + c;
                if (row != null && c < row.size()) {
                    vals.add(String.valueOf(row.get(c)));
                    done.add(idx);
                    if (r == curRow) active.add(idx);
                } else {
                    vals.add("");
                }
            }
        }
        Integer hlRow = curRow >= 0 ? curRow : null;
        MathView.Grid grid = new MathView.Grid("pascal", "杨辉三角 C(n,k)", ROWS, ROWS,
                Collections.unmodifiableList(vals), hlRow, null,
                Collections.unmodifiableSet(active), Collections.unmodifiableSet(done));
        return new MathView("杨辉三角", List.of(grid));
    }

    private Trace.TraceMeta meta() {
        return new Trace.TraceMeta(id(), title(), codeResource(), "9 行", ROWS);
    }

    @Override
    public java.util.List<String> dataLines() {
        return java.util.List.of("int rows = " + ROWS + ";");
    }

    @Override
    public Complexity complexity() {
        return new Complexity("O(n²) 全表生成", "O(n²) 表",
                "递归算 C(n,k) 是指数爆炸；打表 O(n²) 一次到位——和 DP 的关系：这就是最古老的二维表");
    }
}
