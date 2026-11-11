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
import java.util.Random;

/**
 * 天体·N 体引力：太阳定心 + 三行星开普勒轨道——引力只弯折轨迹，不耗一分能量。
 * 半隐式辛积分（kick-drift：先按力改速度，再按新速度挪位置），能量误差有界不发散。
 * 圆轨道速度 v = √(GM/r)，周期 T = 2π√(r³/GM)：内圈跑得快（开普勒第三定律）。
 * 画面（SCATTER 复用）：蓝点 = 天体当前位置（大点=太阳），橙点 = 轨迹历史。
 */
public final class NBodyDemo implements Algorithm {

    private static final String SCHEMA = "1";
    private static final String INVARIANT =
            "引力是保守力：动能 + 势能恒等于总能量，轨道永不螺旋坠毁；"
                    + "v=√(GM/r) 与 T=2π√(r³/GM)（开普勒三定律）都能从它推出来";

    private static final double GM = 60.0;
    private static final double SOFT2 = 0.01;      // 引力软化 ε²（防近距奇点）
    private static final double DT = 0.004;
    private static final int TICKS = 400;
    private static final int EMIT_EVERY = 14;
    private static final double[] RADII = {0.7, 1.1, 1.5};

    private final long seed;

    private final List<Step> steps = new ArrayList<>();
    private final List<List<Double>> trailPts = new ArrayList<>(); // 轨迹历史 [x,y,size]
    private double[][] pos = new double[3][2];
    private double[][] vel = new double[3][2];
    private double[][] initP = new double[3][2];   // 初始位置（🎲 代码页同步用）
    private double t, e0, drift, innerOrbits;

    public NBodyDemo() {
        this(0L);                                   // 0 = 经典固定开局
    }

    /** 种子变体：三行星随机相位 + 轻微椭圆（0.85~1.15 倍圆轨道速度）。 */
    public NBodyDemo(long seed) {
        this.seed = seed;
    }

    @Override public String id() { return "physics.nbody"; }
    @Override public String title() { return "天体·N 体引力"; }
    @Override public String codeResource() { return "/code/NBodyDemo.java"; }
    @Override public String docResource() { return "/markdown/nbody.md"; }

    @Override
    public Trace produce() {
        steps.clear();
        trailPts.clear();
        initPlanets();
        t = 0;
        e0 = totalEnergy();
        drift = 0;
        emit(3, List.of(VizEvent.of(VizEvent.EventType.INIT,
                "太阳 M=60 定心，三行星以圆轨道速度 v=√(GM/r) 启动——引力只弯折轨迹，不耗一分能量")),
                "圆轨道是「引力恰好当向心力」的特例：mv²/r = GMm/r²——质量 m 两边约掉了");
        for (int i = 0; i < TICKS; i++) {
            step();
            t = (i + 1) * DT;
            if ((i + 1) % EMIT_EVERY == 0 || i == TICKS - 1) {
                for (int a = 0; a < 3; a++) {
                    trailPts.add(List.of(pos[a][0], pos[a][1], 0.0025));
                }
                innerOrbits = 13.2 * t / (2 * Math.PI);   // ω_inner ≈ √(GM/r₁³)
                drift = Math.abs((totalEnergy() - e0) / e0);
                emit(16, List.of(VizEvent.of(VizEvent.EventType.SET,
                        String.format("t=%.2fs：内行星已绕 %.1f 圈，能量漂移 %.3f%%", t, innerOrbits, drift * 100))),
                        "轨迹历史全是弧段：椭圆轨道 + 近快远慢（面积速度守恒）——开普勒第二定律在跑");
            }
        }
        emit(18, List.of(VizEvent.of(VizEvent.EventType.MARK_SORTED,
                String.format("收官：内圈 %.1f 圈 / 中圈 %.1f 圈 / 外圈 %.1f 圈——T²∝r³，圈数比 √(r外/r内)³ 的倒数",
                        innerOrbits, 7.39 * t / (2 * Math.PI), 6.32 * t / (2 * Math.PI)))),
                "内行星跑得快不是它努力，是离太阳近：周期只由半径决定（开普勒第三定律）");
        emit(18, List.of(VizEvent.of(VizEvent.EventType.RETURN,
                String.format("全程能量漂移 %.3f%%——辛积分器把误差锁在有界带里，轨道不会假坠落", drift * 100))),
                "三体及以上没有通用闭式解（庞加莱证明）——数值积分是唯一的望远镜");
        return new Trace(SCHEMA, meta(), steps);
    }

    private void initPlanets() {
        if (seed == 0) {
            double[][] p = {{0.7, 0}, {0, 1.1}, {-1.5, 0}};
            double[][] v = {{0, 9.258}, {-7.385, 0}, {0, -6.325}};
            pos = p;
            vel = v;
            initP = new double[][]{{0.7, 0}, {0, 1.1}, {-1.5, 0}};
            return;
        }
        Random r = new Random(seed);
        pos = new double[3][2];
        vel = new double[3][2];
        initP = new double[3][2];
        for (int a = 0; a < 3; a++) {
            double ang = 2 * Math.PI * r.nextDouble();
            double rad = RADII[a];
            double speed = Math.sqrt(GM / rad) * (0.85 + 0.3 * r.nextDouble());
            pos[a][0] = rad * Math.cos(ang);
            pos[a][1] = rad * Math.sin(ang);
            vel[a][0] = -speed * Math.sin(ang);      // 垂直于位矢
            vel[a][1] = speed * Math.cos(ang);
            initP[a][0] = pos[a][0];
            initP[a][1] = pos[a][1];
        }
    }

    /** 半隐式辛积分一步：先 kick（改速度）再 drift（挪位置），太阳固定。 */
    private void step() {
        for (int a = 0; a < 3; a++) {
            double dx = -pos[a][0], dy = -pos[a][1];
            double r2 = dx * dx + dy * dy + SOFT2;
            double f = GM / (r2 * Math.sqrt(r2));
            vel[a][0] += f * dx * DT;
            vel[a][1] += f * dy * DT;
        }
        for (int a = 0; a < 3; a++) {
            pos[a][0] += vel[a][0] * DT;
            pos[a][1] += vel[a][1] * DT;
        }
    }

    /** E = Σ½v² − Σ GM/√(r²+ε²)：动能 + 引力势能。 */
    private double totalEnergy() {
        double e = 0;
        for (int a = 0; a < 3; a++) {
            e += 0.5 * (vel[a][0] * vel[a][0] + vel[a][1] * vel[a][1]);
            e -= GM / Math.sqrt(pos[a][0] * pos[a][0] + pos[a][1] * pos[a][1] + SOFT2);
        }
        return e;
    }

    private void emit(int line, List<VizEvent> events, String why) {
        StackFrame fr = new StackFrame("nbody",
                OrderedMaps.of("t", Value.prim(String.format("%.2fs", t)),
                        "r₁/r₂/r₃", Value.prim(String.format("%.2f/%.2f/%.2f",
                                Math.hypot(pos[0][0], pos[0][1]),
                                Math.hypot(pos[1][0], pos[1][1]),
                                Math.hypot(pos[2][0], pos[2][1]))),
                        "能量漂移", Value.prim(String.format("%.3f%%", drift * 100))), null);
        Map<String, Layer> layers = new LinkedHashMap<>();
        layers.put("mem.stack", new StackLayer(List.of(fr)));
        layers.put("mem.heap", new HeapLayer(snapshotHeap()));
        layers.put("math.plot", snapshotPlot());
        steps.add(new Step(steps.size(), Loc.line(line, why),
                Collections.unmodifiableMap(layers), List.copyOf(events), new Intent(why, INVARIANT)));
    }

    private Heap snapshotHeap() {
        Map<String, Value> fields = new LinkedHashMap<>();
        fields.put("时间 t", Value.prim(String.format("%.2fs", t)));
        fields.put("内行星圈数", Value.prim(String.format("%.2f", innerOrbits)));
        fields.put("能量漂移", Value.prim(String.format("%.4f%%", drift * 100)));
        fields.put("软化 ε²", Value.prim("0.01"));
        Map<String, HeapObject> objs = new LinkedHashMap<>();
        objs.put("@1", new HeapObject("@1", "nbody", "OrbitalState",
                Collections.unmodifiableMap(fields), List.of()));
        return new Heap(Collections.unmodifiableMap(objs));
    }

    private PlotView snapshotPlot() {
        List<List<Double>> rects = new ArrayList<>();
        rects.add(List.of(0.0, 0.0, 0.012));         // 太阳
        for (int a = 0; a < 3; a++) {
            rects.add(List.of(pos[a][0], pos[a][1], 0.006));
        }
        List<List<Double>> trails = new ArrayList<>();
        for (List<Double> p : trailPts) {
            trails.add(List.copyOf(p));
        }
        List<String> labels = new ArrayList<>();
        labels.add(String.format("N 体轨道：GM=%.0f，t=%.2fs", GM, t));
        labels.add(String.format("内圈 %.2f 圈，能量漂移 %.3f%%", innerOrbits, drift * 100));
        labels.add("蓝点=天体，橙点=轨迹历史（近快远慢）");
        return new PlotView(PlotView.SCATTER, "三行星开普勒轨道", t, 0, 0,
                -1.8, 1.8, -1.8, 1.8,
                List.of(), List.of(),
                Collections.unmodifiableList(trails),
                Collections.unmodifiableList(rects),
                Collections.unmodifiableList(labels));
    }

    private Trace.TraceMeta meta() {
        return new Trace.TraceMeta(id(), title(), codeResource(),
                seed == 0 ? "太阳+3行星 圆轨道" : "种子 " + seed, TICKS / EMIT_EVERY + 3);
    }

    @Override
    public java.util.List<String> dataLines() {
        StringBuilder sb = new StringBuilder("double[][] p = {");
        for (int a = 0; a < 3; a++) {
            sb.append(String.format("{%.4f, %.4f}", initP[a][0], initP[a][1]));
            if (a < 2) {
                sb.append(", ");
            }
        }
        return java.util.List.of(sb.append("};").toString());
    }

    @Override
    public Complexity complexity() {
        return new Complexity("O(N²) 每步两两引力", "O(N) 天体状态",
                "太阳系行星仪、星系合并模拟、三体——N² 是直接法的命门；"
                        + "百万体用 Barnes-Hut 树把 O(N²) 砍到 O(N log N)——算法在天文学里救算力");
    }
}
