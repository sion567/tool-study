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
 * 微积分·导数：割线 → 切线的极限。
 * f(x) = x²，定点 P(1.5, 2.25)。Δx 逐次减半：割线斜率 4 → 3.5 → 3.25 → 3.125 → … → 3 = f'(1.5) = 2x₀。
 * 画面：曲线 + 当前割线/切线 + 斜率读数——导数不是"斜率公式"，是"割线斜率的极限"。
 */
public final class DerivativeDemo implements Algorithm {

    private static final String SCHEMA = "1";
    private static final String INVARIANT =
            "割线斜率 = (f(x+Δx)−f(x))/Δx；Δx→0 时割线的极限 = 切线 = 导数——导数是一个极限过程，不是一个公式";

    private static final double X0 = 1.5;          // 定点
    private static final double[] DELTAS = {1, 0.5, 0.25, 0.125, 0.0625};

    private final List<Step> steps = new ArrayList<>();
    private double dx = DELTAS[0];
    private double slope;

    @Override public String id() { return "math.derivative"; }
    @Override public String title() { return "微积分·导数"; }
    @Override public String codeResource() { return "/code/DerivativeDemo.java"; }
    @Override public String docResource() { return "/markdown/derivative.md"; }

    @Override
    public Trace produce() {
        steps.clear();
        dx = DELTAS[0];
        emit(4, List.of(VizEvent.of(VizEvent.EventType.INIT,
                "f(x)=x²，定点 P(1.5, 2.25)——割线的另一个点在 P+Δx")),
                "问题：P 这一点的瞬时变化率是多少？——平均变化率（割线）逐次逼近");
        for (double d : DELTAS) {
            dx = d;
            slope = (f(X0 + d) - f(X0)) / d;
            emit(7, List.of(VizEvent.of(VizEvent.EventType.COMPARE,
                    String.format("Δx=%.4f → 割线斜率 = %.4f", d, slope))),
                    "Δx 减半，割线绕着 P 转——斜率在收敛");
        }
        dx = 0;
        slope = 2 * X0;
        emit(10, List.of(VizEvent.of(VizEvent.EventType.MARK_SORTED,
                "Δx→0 → 割线的极限 = 切线，斜率 = 3 = f'(1.5) = 2×1.5")),
                "导数定义：f'(x₀) = lim(Δx→0) (f(x₀+Δx)−f(x₀))/Δx——刚才每一步都是这个极限的近似值");
        emit(12, List.of(VizEvent.of(VizEvent.EventType.RETURN,
                "f'(x)=2x：每一点的斜率都能这样求——这就是导数表能背出来的原因")),
                "牛顿与莱布尼茨把'无限过程'压缩成了'可计算的公式'——微积分的全部威力就在这一步");
        return new Trace(SCHEMA, meta(), steps);
    }

    private double f(double x) {
        return x * x;
    }

    private void emit(int line, List<VizEvent> events, String why) {
        StackFrame frame = new StackFrame("derivative",
                OrderedMaps.of("Δx", Value.prim(String.format("%.4f", dx)),
                        "斜率", Value.prim(String.format("%.4f", slope)),
                        "f'(x₀)=2x₀", Value.prim(String.valueOf(2 * X0))), null);
        Map<String, Layer> layers = new LinkedHashMap<>();
        layers.put("mem.stack", new StackLayer(List.of(frame)));
        layers.put("mem.heap", new HeapLayer(snapshotHeap()));
        layers.put("math.plot", snapshotPlot());
        steps.add(new Step(steps.size(), Loc.line(line, why),
                Collections.unmodifiableMap(layers), List.copyOf(events), new Intent(why, INVARIANT)));
    }

    private Heap snapshotHeap() {
        Map<String, Value> fields = new LinkedHashMap<>();
        fields.put("定点 x₀", Value.prim(X0));
        fields.put("Δx", Value.prim(String.format("%.4f", dx)));
        fields.put("割线/切线斜率", Value.prim(String.format("%.4f", slope)));
        Map<String, HeapObject> objs = new LinkedHashMap<>();
        objs.put("@1", new HeapObject("@1", "state", "DerivativeState",
                Collections.unmodifiableMap(fields), List.of()));
        return new Heap(Collections.unmodifiableMap(objs));
    }

    private PlotView snapshotPlot() {
        // 曲线 f(x)=x² 采样
        List<List<Double>> curve = new ArrayList<>();
        for (int i = 0; i <= 40; i++) {
            double x = 3.0 * i / 40;
            curve.add(List.of(x, f(x)));
        }
        // 割线（Δx>0）或切线（Δx=0）：过 P 的直线，向两端各延伸一段
        List<List<Double>> line = new ArrayList<>();
        double px = X0, py = f(X0);
        double m;
        if (dx > 0) {
            m = (f(px + dx) - py) / dx;
        } else {
            m = 2 * px;
        }
        line.add(List.of(Math.max(0, px - 1.2), py - 1.2 * m));
        line.add(List.of(px, py));
        line.add(List.of(Math.min(3, px + 1.2), py + 1.2 * m));

        List<String> labels = new ArrayList<>();
        labels.add("f(x) = x²   f'(x) = 2x");
        labels.add(dx > 0 ? String.format("割线 Δx = %.4f", dx) : "切线（Δx→0 的极限）");
        labels.add(String.format("斜率 = %.4f", slope));

        return new PlotView(PlotView.NEWTON, "导数：割线逼近切线", 0, px, py,
                0, 3.0, 0, 9.5,
                Collections.unmodifiableList(curve),
                Collections.unmodifiableList(line),
                List.of(), List.of(),
                Collections.unmodifiableList(labels));
    }

    private Trace.TraceMeta meta() {
        return new Trace.TraceMeta(id(), title(), codeResource(), "f=x² x₀=1.5", DELTAS.length + 2);
    }

    @Override
    public java.util.List<String> dataLines() {
        return java.util.List.of("double x0 = 1.5;   // f(x) = x*x");
    }

    @Override
    public Complexity complexity() {
        return new Complexity("O(1) 每点一次极限过程", "O(1)",
                "导数 = 瞬时变化率 = 割线斜率的极限。数值上就是本演示做的事：Δx 越小越准（但舍入误差会反噬，工程上用中心差分）");
    }
}
