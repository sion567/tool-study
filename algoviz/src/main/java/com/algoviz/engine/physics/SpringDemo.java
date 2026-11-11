package com.algoviz.engine.physics;

import com.algoviz.engine.Algorithm;
import com.algoviz.engine.Complexity;
import com.algoviz.engine.Intent;
import com.algoviz.engine.Layer;
import com.algoviz.engine.Loc;
import com.algoviz.engine.OrderedMaps;
import com.algoviz.engine.Step;
import com.algoviz.engine.Value;
import com.algoviz.engine.VizEvent;
import com.algoviz.engine.math.PlotView;
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
 * 力学·弹簧振子：胡克定律 F = −kx 的简谐运动，以及"数值积分会偷能量"的现场。
 * 精确解 x(t) = A·cos(ωt)，周期 T = 2π√(m/k)——能量永远守恒。
 * 显式欧拉每步注入 ~½(ω·dt)² 的相对能量：振幅越摆越大，3 个周期涨 2.5 倍。
 * 画面（CARTESIAN 复用）：蓝线 = 欧拉积分离散点，绿虚线 = 解析解 cos 参考线（对比数值误差）。
 */
public final class SpringDemo implements Algorithm {

    private static final String SCHEMA = "1";
    private static final String INVARIANT =
            "精确解能量 E = ½kx² + ½mv² 严格守恒；显式欧拉每步放大 √(1+(ω·dt)²) 倍——"
                    + "振幅指数增长不是物理，是数值方法的罪证";

    private static final double M = 1.0;
    private static final double K = 1.0;
    private static final double DT = 0.1;
    private static final int TICKS = 189;          // 3 个周期（T = 2π ≈ 6.28s）
    private static final int EMIT_EVERY = 8;

    private final List<Step> steps = new ArrayList<>();
    private final List<List<Double>> eulerTrail = new ArrayList<>();
    private double t, x, v, energyRatio;

    @Override public String id() { return "physics.spring"; }
    @Override public String title() { return "力学·弹簧振子"; }
    @Override public String codeResource() { return "/code/SpringDemo.java"; }
    @Override public String docResource() { return "/markdown/spring.md"; }

    @Override
    public Trace produce() {
        steps.clear();
        eulerTrail.clear();
        t = 0;
        x = 1.0;                                    // 拉到 A=1 静止释放
        v = 0;
        energyRatio = 1;
        emit(5, List.of(VizEvent.of(VizEvent.EventType.INIT,
                "m=1、k=1、A=1 静止释放：初始能量全是势能 ½kx² = 0.5——精确解将永远守住它")),
                "胡克定律 F = −kx 是线性回复力——位移越大，往回拉的劲越大");
        for (int i = 0; i < TICKS; i++) {
            double a = -K / M * x;                  // 胡克定律（用旧位移算力）
            x = x + v * DT;                         // 显式欧拉：旧速度挪位置
            v = v + a * DT;
            t = (i + 1) * DT;
            double e = 0.5 * K * x * x + 0.5 * M * v * v;
            energyRatio = e / 0.5;
            eulerTrail.add(List.of(t, x));
            if (i % EMIT_EVERY == EMIT_EVERY - 1) {
                double exact = Math.cos(t);
                emit(8, List.of(VizEvent.of(VizEvent.EventType.SET,
                        String.format("t=%.1fs：x_欧拉=%+.3f  x_精确=%+.3f  能量比 E/E₀=%.3f", t, x, exact, energyRatio))),
                        "绿虚线是精确解 cos(t)：欧拉蓝点在它外圈越荡越远——每步都在偷偷注入能量");
            }
        }
        emit(16, List.of(VizEvent.of(VizEvent.EventType.MARK_SORTED,
                String.format("3 个周期后能量比 = %.2f（×%.1f 倍！）：显式欧拉每步放大 √(1+(ω·dt)²) ≈ 1.005", energyRatio, energyRatio))),
                "步长减半，注能率平方地变小（½(ω·dt)²）——这是所有数值方法的通则：精度换步数");
        emit(16, List.of(VizEvent.of(VizEvent.EventType.RETURN,
                "换辛积分（半隐式欧拉：先更新 v 再挪 x）能把漂移从'指数增长'变成'有界振荡'——物理仿真引擎的标配")),
                "选积分器 = 选能量守恒的成色：欧拉会爆炸、辛积分守家有界、RK4 精但也会漂");
        return new Trace(SCHEMA, meta(), steps);
    }

    private void emit(int line, List<VizEvent> events, String why) {
        StackFrame frame = new StackFrame("spring",
                OrderedMaps.of("t", Value.prim(String.format("%.2fs", t)),
                        "x", Value.prim(String.format("%+.4f", x)),
                        "v", Value.prim(String.format("%+.4f", v)),
                        "E/E₀", Value.prim(String.format("%.4f", energyRatio))), null);
        Map<String, Layer> layers = new LinkedHashMap<>();
        layers.put("mem.stack", new StackLayer(List.of(frame)));
        layers.put("mem.heap", new HeapLayer(snapshotHeap()));
        layers.put("math.plot", snapshotPlot());
        steps.add(new Step(steps.size(), Loc.line(line, why),
                Collections.unmodifiableMap(layers), List.copyOf(events), new Intent(why, INVARIANT)));
    }

    private Heap snapshotHeap() {
        Map<String, Value> fields = new LinkedHashMap<>();
        fields.put("周期 T", Value.prim(String.format("%.3fs", 2 * Math.PI * Math.sqrt(M / K))));
        fields.put("步长 dt", Value.prim(String.format("%.2f", DT)));
        fields.put("欧拉能量比", Value.prim(String.format("%.4f", energyRatio)));
        fields.put("精确解能量比", Value.prim("1.0000（永远）"));
        Map<String, HeapObject> objs = new LinkedHashMap<>();
        objs.put("@1", new HeapObject("@1", "spring", "OscillatorState",
                Collections.unmodifiableMap(fields), List.of()));
        return new Heap(Collections.unmodifiableMap(objs));
    }

    private PlotView snapshotPlot() {
        // 解析解参考线（绿虚线）：全时段 cos(t)
        List<List<Double>> exact = new ArrayList<>();
        for (int i = 0; i <= 120; i++) {
            double tt = TICKS * DT * i / 120;
            exact.add(List.of(tt, Math.cos(tt)));
        }
        List<List<Double>> pts = new ArrayList<>();
        for (List<Double> p : eulerTrail) {
            pts.add(List.copyOf(p));
        }
        List<String> labels = new ArrayList<>();
        labels.add("弹簧振子：显式欧拉 vs 解析解");
        labels.add(String.format("T = 2π√(m/k) = %.3fs，dt = %.2f", 2 * Math.PI * Math.sqrt(M / K), DT));
        labels.add(String.format("t=%.2fs  x_欧拉=%+.3f  能量比=%.3f", t, x, energyRatio));
        return new PlotView(PlotView.CARTESIAN, "弹簧振子：欧拉注能现场", t, t, x,
                0, TICKS * DT + 0.3, -2.7, 2.7,
                Collections.unmodifiableList(pts),
                Collections.unmodifiableList(exact), List.of(), List.of(),
                Collections.unmodifiableList(labels));
    }

    private Trace.TraceMeta meta() {
        return new Trace.TraceMeta(id(), title(), codeResource(),
                String.format("m=%.0f k=%.0f dt=%.1f", M, K, DT), TICKS / EMIT_EVERY + 3);
    }

    @Override
    public java.util.List<String> dataLines() {
        return java.util.List.of("double m = 1.0, k = 1.0, dt = 0.1;");
    }

    @Override
    public Complexity complexity() {
        return new Complexity("O(1) 每步", "O(n) 步数",
                "游戏引擎、分子动力学、气候模式——凡是用步进积分的地方都要选积分器；"
                        + "欧拉省事但注能，辛积分是物理仿真的行业默认");
    }
}
