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
 * 力学·抛体运动：水平匀速 + 竖直上抛的合成。
 * x(t) = v₀cosθ·t，y(t) = v₀sinθ·t − ½gt²——轨迹是一条抛物线。
 * 射程 R = v²sin(2θ)/g：30° 与 60° 同射程（sin2θ 关于 45° 对称），45° 独占最大射程。
 * 画面（CARTESIAN 复用）：蓝线 = 弹丸轨迹，橙点 = 弹丸，绿虚线 = 60° 对照轨迹（同射程）。
 */
public final class ProjectileDemo implements Algorithm {

    private static final String SCHEMA = "1";
    private static final String INVARIANT =
            "水平方向匀速、竖直方向匀加速，两分量互不干扰——合运动是抛物线；"
                    + "射程 R = v²sin(2θ)/g：30°/60° 互为镜像，45° 是峰值";

    private static final double G = 9.8;
    private static final int TICKS = 24;

    private final double v0;
    private final double thetaDeg;

    private final List<Step> steps = new ArrayList<>();
    private final List<List<Double>> trail = new ArrayList<>();
    private double t, x, y, vx, vy;

    public ProjectileDemo() {
        this(25, 45);
    }

    public ProjectileDemo(double v0, double thetaDeg) {
        this.v0 = v0;
        this.thetaDeg = thetaDeg;
    }

    @Override public String id() { return "physics.projectile"; }
    @Override public String title() { return "力学·抛体运动"; }
    @Override public String codeResource() { return "/code/ProjectileDemo.java"; }
    @Override public String docResource() { return "/markdown/projectile.md"; }

    @Override
    public Trace produce() {
        steps.clear();
        trail.clear();
        double theta = Math.toRadians(thetaDeg);
        vx = v0 * Math.cos(theta);
        vy = v0 * Math.sin(theta);
        double tFlight = 2 * vy / G;
        double range = vx * tFlight;
        double apex = vy * vy / (2 * G);
        t = x = y = 0;
        emit(5, List.of(VizEvent.of(VizEvent.EventType.INIT,
                String.format("v₀=%.0fm/s、θ=%.0f°：水平匀速 %.2f + 竖直上抛 %.2f——两条独立定律拼出一条抛物线",
                        v0, thetaDeg, vx, vy))),
                "运动的水平/竖直分量互不干扰——伽利略把「下落」从「前进」里剥离了出来");
        for (int i = 0; i <= TICKS; i++) {
            t = tFlight * i / TICKS;
            x = vx * t;
            y = vy * t - 0.5 * G * t * t;
            trail.add(List.of(x, y));
            if (i % 2 == 0 || i == TICKS) {
                emit(11, List.of(VizEvent.of(VizEvent.EventType.SET,
                        String.format("t=%.2fs → (%.1f, %.1f)m，竖直速度 %+.2f m/s", t, x, y, vy - G * t))),
                        i < TICKS / 2 ? "上升段：竖直速度每秒被 g 打折，水平速度分毫未动"
                                : "下降段：下落与前进各过各的日子——所以轨迹左右对称");
            }
        }
        emit(7, List.of(VizEvent.of(VizEvent.EventType.MARK_SORTED,
                String.format("落地！射程 R = v²sin(2θ)/g = %.2fm——飞行时间只由竖直分量决定：T = 2v₀sinθ/g", range))),
                "飞多远 = 水平速度 × 留在空中的时间：两个竖直量联手决定了射程");
        emit(14, List.of(VizEvent.of(VizEvent.EventType.RETURN,
                String.format("绿虚线 = 60° 对照轨迹：与 %.0f° 射程相同（sin2θ 关于 45° 对称）——45° 独占最大射程 %.1fm",
                        thetaDeg, v0 * v0 / G))),
                "「最优投掷角 45°」不是经验，是 sin(2θ) 对称性的直接推论");
        return new Trace(SCHEMA, meta(), steps);
    }

    private void emit(int line, List<VizEvent> events, String why) {
        StackFrame frame = new StackFrame("projectile",
                OrderedMaps.of("t", Value.prim(String.format("%.2fs", t)),
                        "x", Value.prim(String.format("%.1fm", x)),
                        "y", Value.prim(String.format("%.1fm", y)),
                        "vx", Value.prim(String.format("%.2f", vx)),
                        "vy", Value.prim(String.format("%.2f", vy - G * t))), null);
        Map<String, Layer> layers = new LinkedHashMap<>();
        layers.put("mem.stack", new StackLayer(List.of(frame)));
        layers.put("mem.heap", new HeapLayer(snapshotHeap()));
        layers.put("math.plot", snapshotPlot());
        steps.add(new Step(steps.size(), Loc.line(line, why),
                Collections.unmodifiableMap(layers), List.copyOf(events), new Intent(why, INVARIANT)));
    }

    private Heap snapshotHeap() {
        double theta = Math.toRadians(thetaDeg);
        double tFlight = 2 * v0 * Math.sin(theta) / G;
        Map<String, Value> fields = new LinkedHashMap<>();
        fields.put("射程 R", Value.prim(String.format("%.2fm", v0 * Math.cos(theta) * tFlight)));
        fields.put("顶点高度 H", Value.prim(String.format("%.2fm", Math.pow(v0 * Math.sin(theta), 2) / (2 * G))));
        fields.put("飞行时间 T", Value.prim(String.format("%.2fs", tFlight)));
        fields.put("当前速率", Value.prim(String.format("%.2fm/s", Math.hypot(vx, vy - G * t))));
        Map<String, HeapObject> objs = new LinkedHashMap<>();
        objs.put("@1", new HeapObject("@1", "projectile", "BallisticState",
                Collections.unmodifiableMap(fields), List.of()));
        return new Heap(Collections.unmodifiableMap(objs));
    }

    private PlotView snapshotPlot() {
        double theta = Math.toRadians(thetaDeg);
        double tFlight = 2 * v0 * Math.sin(theta) / G;
        double range = v0 * Math.cos(theta) * tFlight;
        double apex = Math.pow(v0 * Math.sin(theta), 2) / (2 * G);
        // 60° 对照轨迹：sin(2θ) 对称性的另一半（仅默认 45° 档严格同射程，变体档做形状对照）
        double tRef = 2 * v0 * Math.sin(Math.toRadians(60)) / G;
        List<List<Double>> ref = new ArrayList<>();
        for (int i = 0; i <= TICKS; i++) {
            double tt = tRef * i / TICKS;
            ref.add(List.of(v0 * Math.cos(Math.toRadians(60)) * tt,
                    v0 * Math.sin(Math.toRadians(60)) * tt - 0.5 * G * tt * tt));
        }
        List<List<Double>> pts = new ArrayList<>();
        for (List<Double> p : trail) {
            pts.add(List.copyOf(p));
        }
        List<String> labels = new ArrayList<>();
        labels.add(String.format("抛体运动：v₀=%.0fm/s，θ=%.0f°", v0, thetaDeg));
        labels.add(String.format("射程 R=%.1fm  顶点 H=%.1fm  飞行 T=%.2fs", range, apex, tFlight));
        labels.add(String.format("t=%.2fs  (x,y)=(%.1f, %.1f)m", t, x, y));
        return new PlotView(PlotView.CARTESIAN, "抛体：抛物线", t, x, y,
                -range * 0.04, range * 1.08, -apex * 0.10, apex * 1.40,
                Collections.unmodifiableList(pts),
                Collections.unmodifiableList(ref), List.of(), List.of(),
                Collections.unmodifiableList(labels));
    }

    private Trace.TraceMeta meta() {
        return new Trace.TraceMeta(id(), title(), codeResource(),
                String.format("v₀=%.0f θ=%.0f°", v0, thetaDeg), TICKS + 2);
    }

    @Override
    public java.util.List<String> dataLines() {
        return java.util.List.of(String.format(
                "double v0 = %.0f, theta = Math.toRadians(%.0f);", v0, thetaDeg));
    }

    @Override
    public Complexity complexity() {
        return new Complexity("O(1) 每时刻直接代公式", "O(n) 轨迹采样点",
                "炮兵射表、篮球远投、蹦床入水——全是同一条抛物线；空气阻力会把最优角压低（标枪 ≈36°）");
    }
}
