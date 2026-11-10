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
 * 牛顿迭代法求根：解 f(x) = x² − 2 = 0（即求 √2）。
 * 画面：曲线 y = x²−2，每步在 (x, f(x)) 画切线，切线与 x 轴的交点就是下一轮的 x。
 * 不变量：x_{n+1} = x_n − f(x_n)/f'(x_n) —— 切线与 x 轴交点，每步有效数字翻倍（平方收敛）。
 */
public final class NewtonDemo implements Algorithm {

    private static final String SCHEMA = "1";
    private static final String INVARIANT =
            "x_{n+1} = x_n − f(x_n)/f'(x_n)：切线零点做下一步——平方收敛，有效数字每步翻倍";

    private static final double X0 = 2.0;
    private static final double ROOT = Math.sqrt(2);
    private static final int MAX_ITER = 6;

    private final List<Step> steps = new ArrayList<>();
    private final List<List<Double>> roots = new ArrayList<>();   // 历代近似根 (x, 0)
    private double x = X0;

    @Override public String id() { return "math.newton"; }
    @Override public String title() { return "牛顿迭代法"; }
    @Override public String codeResource() { return "/code/NewtonDemo.java"; }
    @Override public String docResource() { return "/markdown/newton.md"; }

    @Override
    public Trace produce() {
        steps.clear();
        roots.clear();
        x = X0;
        emit(3, List.of(VizEvent.of(VizEvent.EventType.INIT,
                "x₀ = " + fmt(x) + "，求 x²−2 = 0 的正根（√2）")),
                "从 x₀=2 出发——迭代公式 x ← x − f(x)/f'(x)");
        for (int i = 0; i < MAX_ITER && Math.abs(x - ROOT) > 1e-12; i++) {
            double fx = f(x), dfx = df(x);
            double next = x - fx / dfx;
            emit(6, List.of(VizEvent.of(VizEvent.EventType.COMPARE,
                            "f(" + fmt(x) + ") = " + fmt(fx) + "，切线斜率 f' = " + fmt(dfx))),
                    "在 (" + fmt(x) + ", " + fmt(fx) + ") 处作切线——斜率 " + fmt(dfx));
            emit(7, List.of(VizEvent.of(VizEvent.EventType.SET,
                            "切线交 x 轴于 " + fmt(next) + " → x" + (i + 1) + " = " + fmt(next))),
                    "切线零点 = 下一步的位置（误差 " + fmt(Math.abs(next - ROOT)) + "）");
            roots.add(List.of(x, 0.0));   // 本轮的 x 成为历史
            x = next;
        }
        emit(9, List.of(VizEvent.of(VizEvent.EventType.RETURN,
                "x = " + String.format("%.12f", x) + " ≈ √2（" + steps.size() + " 步内 12 位精度）")),
                "平方收敛：第 1 步对 1 位，第 3 步就对 12 位——这就是 CPU/库算 sqrt 的思路");
        return new Trace(SCHEMA, meta(), steps);
    }

    private double f(double v) { return v * v - 2; }
    private double df(double v) { return 2 * v; }

    private static String fmt(double v) {
        return v == (long) v ? String.valueOf((long) v) : String.format("%.6f", v).replaceAll("0+$", "").replaceAll("\\.$", "");
    }

    private void emit(int line, List<VizEvent> events, String why) {
        StackFrame frame = new StackFrame("newton",
                OrderedMaps.of("x", Value.prim(fmt(x)),
                        "f(x)", Value.prim(fmt(f(x))),
                        "f'(x)", Value.prim(fmt(df(x)))), null);
        Map<String, Layer> layers = new LinkedHashMap<>();
        layers.put("mem.stack", new StackLayer(List.of(frame)));
        layers.put("mem.heap", new HeapLayer(snapshotHeap()));
        layers.put("math.plot", snapshotPlot());
        steps.add(new Step(steps.size(), Loc.line(line, why),
                Collections.unmodifiableMap(layers), List.copyOf(events), new Intent(why, INVARIANT)));
    }

    private Heap snapshotHeap() {
        Map<String, Value> fields = new LinkedHashMap<>();
        fields.put("迭代次数", Value.prim(steps.size()));
        fields.put("当前 x", Value.prim(fmt(x)));
        fields.put("|x−√2|", Value.prim(String.format("%.2e", Math.abs(x - ROOT))));
        Map<String, HeapObject> objs = new LinkedHashMap<>();
        objs.put("@1", new HeapObject("@1", "state", "NewtonState",
                Collections.unmodifiableMap(fields), List.of()));
        return new Heap(Collections.unmodifiableMap(objs));
    }

    private PlotView snapshotPlot() {
        // 曲线采样：x ∈ [0, 2.3]，y ∈ [−2.5, 3.3]
        List<List<Double>> curve = new ArrayList<>();
        for (int i = 0; i <= 60; i++) {
            double xv = 2.3 * i / 60.0;
            curve.add(List.of(xv, f(xv)));
        }
        // 当前切线段：从 (x, f(x)) 到与 x 轴交点 (x_next, 0)，再往两端各延一点
        List<List<Double>> tangent = new ArrayList<>();
        double fx = f(x), dfx = df(x);
        double xNext = Math.abs(dfx) < 1e-12 ? x : x - fx / dfx;
        tangent.add(List.of(Math.max(0, x - 0.6), fx + dfx * (Math.max(0, x - 0.6) - x)));
        tangent.add(List.of(x, fx));
        tangent.add(List.of(xNext, 0.0));
        double xEnd = Math.min(2.3, xNext + 0.35);
        tangent.add(List.of(xEnd, dfx * (xEnd - xNext)));
        // 历代根：本步之后才把当前 x 计入（画面上灰点 = 已确定的近似根）
        List<List<Double>> rootPts = new ArrayList<>();
        for (List<Double> p : roots) rootPts.add(List.copyOf(p));
        rootPts.add(List.of(x, 0.0));

        List<String> labels = new ArrayList<>();
        labels.add("f(x) = x² − 2");
        labels.add("切线: y = f'·(x−x₀) + f(x₀)");
        labels.add(String.format("x = %.6f   误差 %.2e", x, Math.abs(x - ROOT)));

        return new PlotView(PlotView.NEWTON, "牛顿迭代求 √2", x, x, fx,
                0, 2.3, -2.5, 3.3,
                Collections.unmodifiableList(curve),
                Collections.unmodifiableList(tangent),
                Collections.unmodifiableList(rootPts), List.of(),
                Collections.unmodifiableList(labels));
    }

    private Trace.TraceMeta meta() {
        return new Trace.TraceMeta(id(), title(), codeResource(), "x²−2=0, x₀=2", MAX_ITER);
    }

    @Override
    public java.util.List<String> dataLines() {
        return java.util.List.of("double x0 = 2.0;   // 求 x*x - 2 = 0");
    }

    @Override
    public Complexity complexity() {
        return new Complexity("O(log log 1/ε) 每步有效数字翻倍", "O(1)",
                "平方收敛：误差 e → e²/(2|x*|)。初值不差时 4~5 步就到双精度极限；"
                        + "这就是 Math.sqrt 的底层思路之一（配合查表/多项式初值）");
    }
}
