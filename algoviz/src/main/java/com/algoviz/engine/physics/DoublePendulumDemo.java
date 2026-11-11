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
 * 力学·双摆混沌：两个摆锤一根接一根，方程完全确定，命运却完全不可预测。
 * RK4（四阶龙格-库塔）固定 dt=0.002 积分标准双摆方程（m₁=m₂=L₁=L₂=1）。
 * 幽灵双摆：θ₁ 初值只差 0.001 rad——十几秒后两对轨迹完全分道扬镳。
 * 画面（专用 kind doublePendulum）：橙臂橙锤 = 主摆，灰虚点 = 幽灵摆锤 2，蓝线 = 主摆锤 2 轨迹。
 */
public final class DoublePendulumDemo implements Algorithm {

    private static final String SCHEMA = "1";
    private static final String INVARIANT =
            "方程完全确定（同初值必同结局），但初值差 10⁻³ 的误差按指数放大——"
                    + "混沌 = 确定性规律 + 不可长期预测；RK4 的能量漂移 ≈ 0（物理守恒被数值方法保住了）";

    private static final double L = 1.0;
    private static final double G = 9.8;
    private static final double DT = 0.002;
    private static final int TICKS = 6000;         // 12 秒
    private static final int EMIT_EVERY = 200;     // 每 0.4s 一帧
    private static final double GHOST_DELTA = 0.001;

    private final List<Step> steps = new ArrayList<>();
    private final List<List<Double>> trail = new ArrayList<>();       // 主摆锤 2 轨迹
    private final List<List<Double>> ghostTrail = new ArrayList<>();  // 幽灵摆锤 2 轨迹
    private double t, theta1, theta2, separation, energyDrift;

    @Override public String id() { return "physics.doublependulum"; }
    @Override public String title() { return "力学·双摆混沌"; }
    @Override public String codeResource() { return "/code/DoublePendulumDemo.java"; }
    @Override public String docResource() { return "/markdown/doublependulum.md"; }

    @Override
    public Trace produce() {
        steps.clear();
        trail.clear();
        ghostTrail.clear();
        double[] s = {2.0, 0, 0, 0};                // θ₁=2.0 rad（≈115°），θ₂=0，都静止释放
        double[] ghost = {2.0 + GHOST_DELTA, 0, 0, 0};
        t = separation = 0;
        theta1 = s[0];
        theta2 = s[2];
        emit(4, List.of(VizEvent.of(VizEvent.EventType.INIT,
                String.format("θ₁=115° 大角度释放；幽灵双摆的 θ₁ 只多 %.3f rad——按理说它们该永远同步", GHOST_DELTA))),
                "单摆小角度是「可预测的钟」；大角度双摆没了线性化，只能一步一步数值积分");
        double e0 = energy(s);
        energyDrift = 0;
        for (int i = 0; i < TICKS; i++) {
            s = rk4(s, DT);
            ghost = rk4(ghost, DT);
            t = (i + 1) * DT;
            theta1 = s[0];
            theta2 = s[2];
            double x1 = L * Math.sin(s[0]), y1 = -L * Math.cos(s[0]);
            double x2 = x1 + L * Math.sin(s[2]), y2 = y1 - L * Math.cos(s[2]);
            double gx1 = L * Math.sin(ghost[0]), gy1 = -L * Math.cos(ghost[0]);
            double gx2 = gx1 + L * Math.sin(ghost[2]), gy2 = gy1 - L * Math.cos(ghost[2]);
            trail.add(List.of(x2, y2));
            ghostTrail.add(List.of(gx2, gy2));
            separation = Math.hypot(x2 - gx2, y2 - gy2);
            energyDrift = Math.abs(energy(s) - e0) / Math.abs(e0);
            if (i % EMIT_EVERY == EMIT_EVERY - 1) {
                emit(7, List.of(VizEvent.of(VizEvent.EventType.SET,
                        String.format("t=%.1fs：主摆锤(%+.2f, %+.2f)  两解分离 |Δ|=%.3f", t, x2, y2, separation))),
                        separation < 0.05
                                ? "此刻它们还几乎重合——但误差每过一段时间就翻一番，倒计时已经开始"
                                : "分道扬镳！同样的方程、几乎同样的起点，走出了完全不同的历史");
            }
        }
        double drift = energyDrift;
        emit(15, List.of(VizEvent.of(VizEvent.EventType.MARK_SORTED,
                String.format("12 秒后分离 |Δ| = %.3f（初值差的 %.0f 倍）——这就是蝴蝶效应的力学原版",
                        separation, separation / GHOST_DELTA))),
                "初始 0.001 的差距被指数放大了三个数量级：短期可预测（RK4 很准），长期不可能");
        emit(15, List.of(VizEvent.of(VizEvent.EventType.RETURN,
                String.format("RK4 能量漂移 %.1e——数值方法守住了物理守恒律；混沌不是模拟误差，是方程本身的性格", drift))),
                "天气预报 15 天是硬极限：不是算力不够，是初值误差的指数放大把信息吃光了");
        return new Trace(SCHEMA, meta(), steps);
    }

    /** 总机械能（m=L=1）：势能 −2g·cosθ₁ − g·cosθ₂ + 动能项。 */
    private static double energy(double[] s) {
        double th1 = s[0], w1 = s[1], th2 = s[2], w2 = s[3];
        return -2 * G * Math.cos(th1) - G * Math.cos(th2)
                + w1 * w1 + 0.5 * w2 * w2 + w1 * w2 * Math.cos(th1 - th2);
    }

    /** 标准 RK4 一步。 */
    private static double[] rk4(double[] s, double h) {
        double[] k1 = accel(s);
        double[] k2 = accel(mix(s, k1, h / 2));
        double[] k3 = accel(mix(s, k2, h / 2));
        double[] k4 = accel(mix(s, k3, h));
        double[] o = new double[4];
        for (int i = 0; i < 4; i++) {
            o[i] = s[i] + h / 6 * (k1[i] + 2 * k2[i] + 2 * k3[i] + k4[i]);
        }
        return o;
    }

    private static double[] mix(double[] s, double[] k, double f) {
        double[] r = new double[4];
        for (int i = 0; i < 4; i++) {
            r[i] = s[i] + k[i] * f;
        }
        return r;
    }

    /** m₁=m₂=L₁=L₂=1 的标准双摆角加速度方程。 */
    private static double[] accel(double[] s) {
        double th1 = s[0], w1 = s[1], th2 = s[2], w2 = s[3];
        double d = th1 - th2;
        double den = 3 - Math.cos(2 * d);
        double a1 = (-G * 3 * Math.sin(th1) - G * Math.sin(th1 - 2 * th2)
                - 2 * Math.sin(d) * (w2 * w2 + w1 * w1 * Math.cos(d))) / den;
        double a2 = 2 * Math.sin(d) * (2 * w1 * w1 + 2 * G * Math.cos(th1)
                + w2 * w2 * Math.cos(d)) / den;
        return new double[]{w1, a1, w2, a2};
    }

    private void emit(int line, List<VizEvent> events, String why) {
        StackFrame frame = new StackFrame("doublePendulum",
                OrderedMaps.of("t", Value.prim(String.format("%.2fs", t)),
                        "θ₁", Value.prim(String.format("%+.2f rad", theta1)),
                        "θ₂", Value.prim(String.format("%+.2f rad", theta2)),
                        "|Δ|", Value.prim(String.format("%.4f", separation))), null);
        Map<String, Layer> layers = new LinkedHashMap<>();
        layers.put("mem.stack", new StackLayer(List.of(frame)));
        layers.put("mem.heap", new HeapLayer(snapshotHeap()));
        layers.put("math.plot", snapshotPlot());
        steps.add(new Step(steps.size(), Loc.line(line, why),
                Collections.unmodifiableMap(layers), List.copyOf(events), new Intent(why, INVARIANT)));
    }

    private Heap snapshotHeap() {
        Map<String, Value> fields = new LinkedHashMap<>();
        fields.put("时间 t", Value.prim(String.format("%.2fs", t)));
        fields.put("初值差", Value.prim(String.format("%.3f rad", GHOST_DELTA)));
        fields.put("两解分离 |Δ|", Value.prim(String.format("%.4f", separation)));
        fields.put("放大倍数", Value.prim(String.format("×%.0f", separation / GHOST_DELTA)));
        fields.put("能量漂移", Value.prim(String.format("%.2e", energyDrift)));
        Map<String, HeapObject> objs = new LinkedHashMap<>();
        objs.put("@1", new HeapObject("@1", "doublePendulum", "ChaosState",
                Collections.unmodifiableMap(fields), List.of()));
        return new Heap(Collections.unmodifiableMap(objs));
    }

    private PlotView snapshotPlot() {
        List<List<Double>> pts = new ArrayList<>();
        for (List<Double> p : trail) {
            pts.add(List.copyOf(p));
        }
        List<List<Double>> ghosts = new ArrayList<>();
        for (List<Double> p : ghostTrail) {
            ghosts.add(List.copyOf(p));
        }
        double x1 = L * Math.sin(theta1), y1 = -L * Math.cos(theta1);
        double x2 = x1 + L * Math.sin(theta2), y2 = y1 - L * Math.cos(theta2);
        List<String> labels = new ArrayList<>();
        labels.add(String.format("双摆混沌：t=%.1fs，分离 |Δ|=%.3f", t, separation));
        labels.add(String.format("θ₁=%+.2f rad，θ₂=%+.2f rad", theta1, theta2));
        labels.add("蓝线=主摆锤轨迹，灰点=幽灵（初值差 0.001）");
        return new PlotView(PlotView.DOUBLE_PENDULUM, "双摆：确定性 + 不可预测", theta1, x2, y2,
                -2.2, 2.2, -2.2, 0.3,
                Collections.unmodifiableList(pts),
                Collections.unmodifiableList(ghosts), List.of(), List.of(),
                Collections.unmodifiableList(labels));
    }

    private Trace.TraceMeta meta() {
        return new Trace.TraceMeta(id(), title(), codeResource(),
                "θ₁=2.0 θ₂=0 Δθ₁=0.001", TICKS / EMIT_EVERY + 3);
    }

    @Override
    public java.util.List<String> dataLines() {
        return java.util.List.of("double th1 = 2.0, th2 = 0.0, w1 = 0, w2 = 0;");
    }

    @Override
    public Complexity complexity() {
        return new Complexity("O(1) 每步（RK4 四次求值）", "O(n) 步数",
                "混沌系统无处不在：三体问题、天气、湍流、心电图——短期可算、长期认命；"
                        + "能算 ≠ 能预测，是对「确定论」的世纪修正");
    }
}
