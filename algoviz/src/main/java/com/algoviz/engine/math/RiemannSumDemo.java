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
 * 微积分·定积分：黎曼和逼近曲线下面积。
 * ∫₀¹ x² dx = 1/3 ≈ 0.3333。n = 1→32 个左端点矩形，和从 0 → 0.3203 → … 无限细分即面积。
 * 画面：曲线 + 矩形条随 n 翻倍逐渐"贴"满曲线——∫ 符号本义就是拉长的 S(um)。
 */
public final class RiemannSumDemo implements Algorithm {

    private static final String SCHEMA = "1";
    private static final String INVARIANT =
            "n 个矩形之和随 n 翻倍逼近真实面积——定积分 = 黎曼和在 n→∞ 的极限";

    private static final int[] NS = {1, 2, 4, 8, 16, 32};
    private static final double TRUE = 1.0 / 3;

    private final List<Step> steps = new ArrayList<>();
    private int n = 1;
    private double sum;

    @Override public String id() { return "math.integral"; }
    @Override public String title() { return "微积分·定积分"; }
    @Override public String codeResource() { return "/code/RiemannSumDemo.java"; }
    @Override public String docResource() { return "/markdown/riemann.md"; }

    @Override
    public Trace produce() {
        steps.clear();
        n = 1;
        sum = 0;
        emit(4, List.of(VizEvent.of(VizEvent.EventType.INIT,
                "求 ∫₀¹ x² dx（曲线下面积）——真值 1/3 ≈ 0.3333")),
                "思路：不会求面积 → 先切成矩形，用加法近似");
        for (int k : NS) {
            n = k;
            sum = 0;
            for (int i = 0; i < n; i++) {
                double x = (double) i / n;
                sum += x * x * (1.0 / n);
            }
            emit(7, List.of(VizEvent.of(VizEvent.EventType.SET,
                    String.format("n=%d：矩形和 = %.4f（差真值 %.4f）", n, sum, TRUE - sum))),
                    n >= 16 ? "矩形已经几乎贴住曲线——误差由矩形顶部与弧之间的'小三角'贡献"
                            : "矩形少 → 台阶粗 → 低估明显（左端点矩形都在曲线下方）");
        }
        emit(10, List.of(VizEvent.of(VizEvent.EventType.MARK_SORTED,
                "n→∞ → 黎曼和的极限 = 1/3：这就是定积分 ∫₀¹ x² dx")),
                "∫ 是拉长的字母 S（Sum）；牛顿-莱布尼茨公式说：面积 = 原函数在端点的差 F(1)−F(0) = 1/3 − 0");
        emit(12, List.of(VizEvent.of(VizEvent.EventType.RETURN,
                "积分与导数互为逆运算——(x³/3)' = x²，所以面积 = F(1)−F(0)")),
                "这一步把'无限求和'变成'代入求差'——微积分基本定理，人类数学的高光时刻");
        return new Trace(SCHEMA, meta(), steps);
    }

    private void emit(int line, List<VizEvent> events, String why) {
        StackFrame frame = new StackFrame("riemann",
                OrderedMaps.of("n", Value.prim(n),
                        "矩形和", Value.prim(String.format("%.4f", sum)),
                        "真值 1/3", Value.prim("0.3333")), null);
        Map<String, Layer> layers = new LinkedHashMap<>();
        layers.put("mem.stack", new StackLayer(List.of(frame)));
        layers.put("mem.heap", new HeapLayer(snapshotHeap()));
        layers.put("math.plot", snapshotPlot());
        steps.add(new Step(steps.size(), Loc.line(line, why),
                Collections.unmodifiableMap(layers), List.copyOf(events), new Intent(why, INVARIANT)));
    }

    private Heap snapshotHeap() {
        Map<String, Value> fields = new LinkedHashMap<>();
        fields.put("n", Value.prim(n));
        fields.put("黎曼和", Value.prim(String.format("%.4f", sum)));
        fields.put("真值", Value.prim("0.3333"));
        Map<String, HeapObject> objs = new LinkedHashMap<>();
        objs.put("@1", new HeapObject("@1", "state", "RiemannState",
                Collections.unmodifiableMap(fields), List.of()));
        return new Heap(Collections.unmodifiableMap(objs));
    }

    private PlotView snapshotPlot() {
        List<List<Double>> curve = new ArrayList<>();
        for (int i = 0; i <= 40; i++) {
            double x = (double) i / 40;
            curve.add(List.of(x, x * x));
        }
        List<List<Double>> rects = new ArrayList<>();
        double w = 1.0 / n;
        for (int i = 0; i < n; i++) {
            double x0 = i * w;
            rects.add(List.of(x0, x0 + w, x0 * x0));   // [x0, x1, 高度=左端点]
        }
        List<String> labels = new ArrayList<>();
        labels.add("∫₀¹ x² dx = 1/3 ≈ 0.3333");
        labels.add(String.format("n = %d 个左端点矩形", n));
        labels.add(String.format("黎曼和 = %.4f", sum));
        return new PlotView(PlotView.RIEMANN, "定积分：黎曼和逼近面积", n, 0, 0,
                0, 1.0, 0, 1.05,
                Collections.unmodifiableList(curve),
                List.of(), List.of(),
                Collections.unmodifiableList(rects),
                Collections.unmodifiableList(labels));
    }

    private Trace.TraceMeta meta() {
        return new Trace.TraceMeta(id(), title(), codeResource(), "n=1→32", NS.length + 2);
    }

    @Override
    public java.util.List<String> dataLines() {
        return java.util.List.of("int[] ns = {1, 2, 4, 8, 16, 32};   // f(x) = x*x, 区间 [0,1]");
    }

    @Override
    public Complexity complexity() {
        return new Complexity("O(n) 每轮 n 个矩形求和", "O(n) 矩形",
                "数值积分的祖先：误差 O(1/n)。工程上用梯形法则（O(1/n²)）或辛普森法则（O(1/n⁴)）——思想全在'细分'二字");
    }
}
