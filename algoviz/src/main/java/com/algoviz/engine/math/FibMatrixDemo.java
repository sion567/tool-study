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
 * 斐波那契 × 矩阵幂：M = [[1,1],[1,0]]，Mⁿ 的右上角就是 F(n)。
 * 平方链 M→M²→M⁴→M⁸→M¹⁶→M³²——n=32 只用 5 次矩阵乘法（对比递推 31 次加法、朴素递归 250 万次调用）。
 * 快速幂的矩阵版：指数减半的套路原封不动，只是"乘"换成了"2×2 矩阵乘法"。
 */
public final class FibMatrixDemo implements Algorithm {

    private static final String SCHEMA = "1";
    private static final String INVARIANT =
            "Mⁿ 的右上角 = F(n)（M=[[1,1],[1,0]]）——平方一次指数翻倍，F(2¹⁰⁰) 也只需 100 次矩阵乘";

    private static final int TARGET = 32;   // 2⁵：平方链 5 步到位

    private final List<Step> steps = new ArrayList<>();
    private final long[][] m = new long[2][2];
    private int exponent;

    @Override public String id() { return "math.fibmatrix"; }
    @Override public String title() { return "斐波那契×矩阵幂"; }
    @Override public String codeResource() { return "/code/FibMatrixDemo.java"; }
    @Override public String docResource() { return "/markdown/fib-matrix.md"; }

    @Override
    public Trace produce() {
        steps.clear();
        m[0][0] = 1;
        m[0][1] = 1;
        m[1][0] = 1;
        m[1][1] = 0;
        exponent = 1;
        emit(5, List.of(VizEvent.of(VizEvent.EventType.INIT,
                "M = [[1,1],[1,0]]，M¹ 右上角 = 1 = F(1)")),
                "神奇的对应：Mⁿ = [[F(n+1),F(n)],[F(n),F(n−1)]]——矩阵乘法天然生成斐波那契");
        for (int e = 2; e <= TARGET; e *= 2) {
            square();
            exponent = e;
            emit(8, List.of(VizEvent.of(VizEvent.EventType.SET,
                    String.format("M^%d = M^%d · M^%d → F(%d) = %d",
                            e, e / 2, e / 2, e, m[0][1]))),
                    "平方一次 = 指数翻倍——这就是快速幂 O(log n) 的全部秘密");
        }
        emit(11, List.of(VizEvent.of(VizEvent.EventType.MARK_SORTED,
                String.format("M^%d 右上角 = F(%d) = %d——5 次矩阵乘法直达", TARGET, TARGET, m[0][1]))),
                "递推 O(n)：n=32 要 31 步；矩阵幂 O(log n)：n=2¹⁰⁰ 只要 100 步");
        emit(13, List.of(VizEvent.of(VizEvent.EventType.RETURN,
                "延伸：任何线性递推（Lucas、 Tribonacci）都能写成矩阵幂——通解是特征根（黄金螺旋那一课）")),
                "快速幂的三次出场：整数幂（math.fastpower）→ RSA 模幂（math.rsa）→ 矩阵幂（本节）");
        return new Trace(SCHEMA, meta(), steps);
    }

    /** 2×2 矩阵自乘。 */
    private void square() {
        long a = m[0][0], b = m[0][1], c = m[1][0], d = m[1][1];
        m[0][0] = a * a + b * c;
        m[0][1] = a * b + b * d;
        m[1][0] = c * a + d * c;
        m[1][1] = c * b + d * d;
    }

    private void emit(int line, List<VizEvent> events, String why) {
        StackFrame frame = new StackFrame("fibMatrix",
                OrderedMaps.of("指数", Value.prim(exponent),
                        "F(n) = Mⁿ 右上角", Value.prim(String.valueOf(m[0][1]))), null);
        Map<String, Layer> layers = new LinkedHashMap<>();
        layers.put("mem.stack", new StackLayer(List.of(frame)));
        layers.put("mem.heap", new HeapLayer(snapshotHeap()));
        layers.put("math.matrix", snapshotMatrix());
        steps.add(new Step(steps.size(), Loc.line(line, why),
                Collections.unmodifiableMap(layers), List.copyOf(events), new Intent(why, invariant())));
    }

    private String invariant() {
        return INVARIANT;
    }

    private Heap snapshotHeap() {
        Map<String, Value> fields = new LinkedHashMap<>();
        fields.put("指数 n", Value.prim(exponent));
        fields.put("F(n)", Value.prim(String.valueOf(m[0][1])));
        fields.put("乘法次数", Value.prim(String.valueOf(Integer.bitCount(TARGET) + Integer.toBinaryString(TARGET).length() - 1)));
        Map<String, HeapObject> objs = new LinkedHashMap<>();
        objs.put("@1", new HeapObject("@1", "matrix", "FibMatrix",
                Collections.unmodifiableMap(fields), List.of()));
        return new Heap(Collections.unmodifiableMap(objs));
    }

    private MathView snapshotMatrix() {
        List<String> vals = List.of(
                String.valueOf(m[0][0]), String.valueOf(m[0][1]),
                String.valueOf(m[1][0]), String.valueOf(m[1][1]));
        Set<Integer> done = new LinkedHashSet<>();
        done.add(1);   // 右上角 = F(n)，主角位
        Set<Integer> active = new LinkedHashSet<>();
        MathView.Grid grid = new MathView.Grid("m", "M^" + exponent + "（右上角 = F(" + exponent + ")）", 2, 2,
                vals, null, null, active, done);
        return new MathView("斐波那契矩阵", List.of(grid));
    }

    private Trace.TraceMeta meta() {
        return new Trace.TraceMeta(id(), title(), codeResource(), "M^32", 5);
    }

    @Override
    public java.util.List<String> dataLines() {
        return java.util.List.of("int target = " + TARGET + ";   // M=[[1,1],[1,0]]");
    }

    @Override
    public Complexity complexity() {
        return new Complexity("O(log n) 次矩阵乘法", "O(1) 2×2 固定",
                "线性递推的通解引擎：k 阶递推 → k×k 矩阵 → O(k³ log n)。竞赛与工程通吃（菲波那契取模、马尔可夫链）");
    }
}
