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
 * 微积分应用·钟摆：小角度摆动的简谐运动。
 * θ(t) = θ₀·cos(√(g/L)·t)——摆锤沿圆弧往复，轨迹就是一条余弦。
 * 周期 T = 2π√(L/g)：与质量无关、与小角度幅度几乎无关（伽利略的等时性发现）。
 * 画面（CARTESIAN 复用）：橙点=摆锤，橙线=摆臂（原点=悬挂点），蓝弧=摆锤走过的轨迹。
 */
public final class PendulumDemo implements Algorithm {

    private static final String SCHEMA = "1";
    private static final String INVARIANT =
            "小角度时 sinθ≈θ → 方程线性化 → 简谐运动 θ(t)=θ₀cos(ωt)，周期 T=2π√(L/g)——与质量无关";

    private static final double L = 1.0;
    private static final double G = 9.8;
    private static final double THETA0 = Math.toRadians(30);
    private static final double OMEGA = Math.sqrt(G / L);
    private static final double T = 2 * Math.PI / OMEGA;
    private static final int TICKS = 24;   // 覆盖两个周期

    private final List<Step> steps = new ArrayList<>();
    private final List<List<Double>> trace = new ArrayList<>();
    private double t, thetaDeg;
    private double bx, by;

    @Override public String id() { return "math.pendulum"; }
    @Override public String title() { return "微积分应用·钟摆"; }
    @Override public String codeResource() { return "/code/PendulumDemo.java"; }
    @Override public String docResource() { return "/markdown/pendulum.md"; }

    @Override
    public Trace produce() {
        steps.clear();
        trace.clear();
        t = thetaDeg = 0;
        emit(4, List.of(VizEvent.of(VizEvent.EventType.INIT,
                "摆长 L=1m，初始偏角 30°，释放——它将永远重复同一个周期")),
                "微分方程 θ'' = −(g/L)·sinθ 没有初等解；小角度下 sinθ≈θ → 变成简谐运动");
        for (int i = 0; i <= TICKS; i++) {
            t = 2 * T * i / TICKS;
            double theta = THETA0 * Math.cos(OMEGA * t);
            thetaDeg = Math.toDegrees(theta);
            double bxv = L * Math.sin(theta);
            double byv = -L * Math.cos(theta);
            trace.add(List.of(bxv, byv));
            bx = bxv;
            by = byv;
            if (i % 3 == 0 || i == TICKS) {
                emit(7, List.of(VizEvent.of(VizEvent.EventType.SET,
                        String.format("t=%.2fs → θ=%.1f°（周期 %.3fs）", t, thetaDeg, T))),
                        i < TICKS / 2 ? "摆向一侧；cos 控制一切——这是简谐运动的时间表"
                                : "往复对称：第二个周期与第一个逐点相同");
            }
        }
        emit(10, List.of(VizEvent.of(VizEvent.EventType.MARK_SORTED,
                String.format("两个完整周期走完：T = 2π√(L/g) = %.3fs——轨迹重合", T))),
                "钟摆钟靠调摆长校准时间：T 只认 L 和 g——摆钟跟着重锤走遍世界也能对时");
        emit(12, List.of(VizEvent.of(VizEvent.EventType.RETURN,
                "大角度（>15°）时周期开始变长——sinθ≈θ 的近似失效，需要椭圆积分")),
                "线性化的代价与回报：丢掉一点精度，换来一个能写在纸上的公式");
        return new Trace(SCHEMA, meta(), steps);
    }

    private void emit(int line, List<VizEvent> events, String why) {
        StackFrame frame = new StackFrame("pendulum",
                OrderedMaps.of("t", Value.prim(String.format("%.2fs", t)),
                        "θ", Value.prim(String.format("%.1f°", thetaDeg)),
                        "T", Value.prim(String.format("%.3fs", T))), null);
        Map<String, Layer> layers = new LinkedHashMap<>();
        layers.put("mem.stack", new StackLayer(List.of(frame)));
        layers.put("mem.heap", new HeapLayer(snapshotHeap()));
        layers.put("math.plot", snapshotPlot());
        steps.add(new Step(steps.size(), Loc.line(line, why),
                Collections.unmodifiableMap(layers), List.copyOf(events), new Intent(why, INVARIANT)));
    }

    private Heap snapshotHeap() {
        Map<String, Value> fields = new LinkedHashMap<>();
        fields.put("周期 T", Value.prim(String.format("%.3fs", T)));
        fields.put("ω=√(g/L)", Value.prim(String.format("%.3f rad/s", OMEGA)));
        fields.put("当前 θ", Value.prim(String.format("%.1f°", thetaDeg)));
        Map<String, HeapObject> objs = new LinkedHashMap<>();
        objs.put("@1", new HeapObject("@1", "pendulum", "PendulumState",
                Collections.unmodifiableMap(fields), List.of()));
        return new Heap(Collections.unmodifiableMap(objs));
    }

    private PlotView snapshotPlot() {
        List<List<Double>> pts = new ArrayList<>();
        for (List<Double> p : trace) pts.add(List.copyOf(p));
        List<String> labels = new ArrayList<>();
        labels.add("θ(t) = θ₀·cos(ωt)");
        labels.add(String.format("T = 2π√(L/g) = %.3fs", T));
        labels.add(String.format("t = %.2fs, θ = %.1f°", t, thetaDeg));
        return new PlotView(PlotView.CARTESIAN, "钟摆：简谐运动", t, bx, by,
                -1.15, 1.15, -1.15, 0.15,
                Collections.unmodifiableList(pts),
                List.of(), List.of(), List.of(),
                Collections.unmodifiableList(labels));
    }

    private Trace.TraceMeta meta() {
        return new Trace.TraceMeta(id(), title(), codeResource(), "L=1 θ₀=30°", TICKS + 2);
    }

    @Override
    public java.util.List<String> dataLines() {
        return java.util.List.of("double L = 1.0, theta0 = Math.toRadians(30);");
    }

    @Override
    public Complexity complexity() {
        return new Complexity("O(1) 每个时刻直接代公式", "O(n) 轨迹点",
                "弹簧振子/电路振荡/桥梁共振全是同一个方程 x''=−ω²x——简谐运动是物理学的'元音字母'");
    }
}
