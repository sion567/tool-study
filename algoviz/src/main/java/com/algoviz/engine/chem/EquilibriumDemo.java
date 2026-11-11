package com.algoviz.engine.chem;

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
 * 反应过程·勒夏特列平衡：正逆速率此消彼长夹出平衡，扰动后重新夹——动态平衡不是静止。
 * 反应 A + B ⇌ C：v_f = k_f·[A][B]（蓝线），v_r = k_r·[C]（绿虚线）；K = k_f/k_r。
 * 三幕：趋衡（v_f↓ v_r↑ → 相等）→ 扰动（加入产物，Q>K）→ 再平衡（勒夏特列：向削弱扰动的方向移动）。
 * 画面（CARTESIAN 复用）：两条速率曲线 + 动点 = 当前时刻。
 */
public final class EquilibriumDemo implements Algorithm {

    private static final String SCHEMA = "1";
    private static final String INVARIANT =
            "平衡 = 正逆速率相等（不是浓度相等！）；K = k_f/k_r 只认温度——"
                    + "扰动后 Q≠K 系统自动向「削弱扰动」方向移动，回到 K 的怀抱：勒夏特列原理";

    private static final double KF = 0.5;
    private static final double KR = 0.25;
    private static final double DT = 0.06;
    private static final double PERTURB_AT = 4.0;
    private static final double T_TOTAL = 9.0;
    private static final int EMIT_EVERY = 8;

    private final double perturbC;   // 扰动注入量（变体可换）

    private final List<Step> steps = new ArrayList<>();
    private final List<List<Double>> vfTrail = new ArrayList<>();
    private final List<List<Double>> vrTrail = new ArrayList<>();
    private double t, a, b, c, vf, vr;
    private boolean perturbed;

    public EquilibriumDemo() {
        this(0.6);
    }

    /** 变体：换扰动注入量（Q 冲得越远，回弹越狠）。 */
    public EquilibriumDemo(double perturbC) {
        this.perturbC = perturbC;
    }

    @Override public String id() { return "chem.equilibrium"; }
    @Override public String title() { return "反应过程·勒夏特列平衡"; }
    @Override public String codeResource() { return "/code/EquilibriumDemo.java"; }
    @Override public String docResource() { return "/markdown/equilibrium.md"; }

    @Override
    public Trace produce() {
        steps.clear();
        vfTrail.clear();
        vrTrail.clear();
        t = 0;
        a = b = 1.0;
        c = 0;
        perturbed = false;
        vf = KF * a * b;
        vr = 0;
        emit(6, List.of(VizEvent.of(VizEvent.EventType.INIT,
                String.format("A + B ⇌ C，k_f=%.2f、k_r=%.2f → K=%.1f。起点：v_f=%.3f 而 v_r=0——单向狂奔", KF, KR, KF / KR, vf))),
                "K = k_f/k_r 是速率常数的比值：动力学量一除，热力学量出炉");
        int total = (int) (T_TOTAL / DT);
        for (int i = 0; i < total; i++) {
            vf = KF * a * b;
            vr = KR * c;
            a += (vr - vf) * DT;
            b += (vr - vf) * DT;
            c += (vf - vr) * DT;
            t = (i + 1) * DT;
            vfTrail.add(List.of(t, vf));
            vrTrail.add(List.of(t, vr));
            if (!perturbed && t >= PERTURB_AT) {
                c += perturbC;
                perturbed = true;
                vfTrail.add(List.of(t, vf));
                emit(14, List.of(VizEvent.of(VizEvent.EventType.MESSAGE,
                        String.format("⚡ 扰动：瞬间注入 %.1f mol C → Q=%.2f > K=%.1f，逆反应被迫接管", perturbC, c / (a * b), KF / KR))),
                        "Q 是「此刻的 K」：Q>K 说明产物超编——系统只有一个念头，把 C 消耗回去");
            } else if ((i + 1) % EMIT_EVERY == 0) {
                emit(10, List.of(VizEvent.of(VizEvent.EventType.SET,
                        String.format("t=%.2fs：v_f=%.3f vs v_r=%.3f，Q=%.3f（K=%.1f）", t, vf, vr, c / (a * b), KF / KR))),
                        Math.abs(vf - vr) < 0.02 ? "两线咬合 = 动态平衡：分子还在来回撞，只是净流量为零"
                                : "v_f 与 v_r 的剪刀差就是净反应方向——曲线交叉前后系统换向");
            }
        }
        emit(18, List.of(VizEvent.of(VizEvent.EventType.MARK_SORTED,
                String.format("终态：v_f=%.3f == v_r=%.3f，[C]=%.2f（扰动前 %.2f）——Q 重新逼近 K", vf, vr, c, 0.5))),
                "加入产物后平衡左移：勒夏特列说系统会「反抗」——但反抗的结局仍是 K，只是 Composition 换了");
        emit(21, List.of(VizEvent.of(VizEvent.EventType.RETURN,
                "工业合成氨照样用：不断移走氨（Q<K）逼反应不停右移——勒夏特列原理的 billion-dollar 应用")),
                "平衡是「速率的握手」，不是「数量的对半」——混淆这两者是有机化学第一大坑");
        return new Trace(SCHEMA, meta(), steps);
    }

    private void emit(int line, List<VizEvent> events, String why) {
        StackFrame fr = new StackFrame("equilibrium",
                OrderedMaps.of("t", Value.prim(String.format("%.2fs", t)),
                        "[A]/[C]", Value.prim(String.format("%.2f / %.2f", a, c)),
                        "v_f/v_r", Value.prim(String.format("%.3f / %.3f", vf, vr)),
                        "Q", Value.prim(String.format("%.3f", a * b == 0 ? 0 : c / (a * b)))), null);
        Map<String, Layer> layers = new LinkedHashMap<>();
        layers.put("mem.stack", new StackLayer(List.of(fr)));
        layers.put("mem.heap", new HeapLayer(snapshotHeap()));
        layers.put("math.plot", snapshotPlot());
        steps.add(new Step(steps.size(), Loc.line(line, why),
                Collections.unmodifiableMap(layers), List.copyOf(events), new Intent(why, INVARIANT)));
    }

    private Heap snapshotHeap() {
        Map<String, Value> fields = new LinkedHashMap<>();
        fields.put("K = k_f/k_r", Value.prim(String.format("%.2f", KF / KR)));
        fields.put("Q = [C]/[A][B]", Value.prim(String.format("%.3f", a * b == 0 ? 0 : c / (a * b))));
        fields.put("扰动", Value.prim(perturbed ? String.format("已注入 +%.1f C", perturbC) : "未扰动"));
        fields.put("判据", Value.prim(Math.abs(vf - vr) < 0.005 ? "v_f ≈ v_r（平衡）" : "净反应进行中"));
        Map<String, HeapObject> objs = new LinkedHashMap<>();
        objs.put("@1", new HeapObject("@1", "equilibrium", "BalanceState",
                Collections.unmodifiableMap(fields), List.of()));
        return new Heap(Collections.unmodifiableMap(objs));
    }

    private PlotView snapshotPlot() {
        List<List<Double>> vfPts = new ArrayList<>();
        for (List<Double> p : vfTrail) {
            vfPts.add(List.copyOf(p));
        }
        List<List<Double>> vrPts = new ArrayList<>();
        for (List<Double> p : vrTrail) {
            vrPts.add(List.copyOf(p));
        }
        List<String> labels = new ArrayList<>();
        labels.add(String.format("勒夏特列：v_f=%.3f（蓝） vs v_r=%.3f（绿虚），K=%.1f", vf, vr, KF / KR));
        labels.add(perturbed ? "扰动后：Q>K → 逆反应主导 → 再平衡" : "趋衡中：v_f↓、v_r↑，剪刀差收窄");
        labels.add(String.format("t=%.2fs  [A]=%.2f  [C]=%.2f", t, a, c));
        return new PlotView(PlotView.CARTESIAN, "正逆速率的拔河", t, t, vf,
                0, T_TOTAL + 0.3, 0, 0.62,
                Collections.unmodifiableList(vfPts),
                Collections.unmodifiableList(vrPts), List.of(), List.of(),
                Collections.unmodifiableList(labels));
    }

    private Trace.TraceMeta meta() {
        return new Trace.TraceMeta(id(), title(), codeResource(),
                String.format("k_f=%.1f k_r=%.2f 扰动@%.0fs", KF, KR, PERTURB_AT), totalSteps());
    }

    private int totalSteps() {
        return (int) (T_TOTAL / DT / EMIT_EVERY) + 4;
    }

    @Override
    public java.util.List<String> dataLines() {
        return java.util.List.of(String.format("double kf = %.1f, kr = %.2f, pulse = %.1f;   //@DATA 速率与扰动",
                KF, KR, perturbC));
    }

    @Override
    public Complexity complexity() {
        return new Complexity("O(1) 每步速率演算", "O(n) 积分步数",
                "合成氨移走产物、缓冲溶液抗 pH、血红蛋白氧合——"
                        + "凡是「系统抵抗改变」，背后都是 Q 与 K 的拉锯");
    }
}
