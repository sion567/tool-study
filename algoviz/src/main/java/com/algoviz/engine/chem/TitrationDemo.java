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
 * 反应过程·酸碱滴定：0.10M NaOH 逐滴滴入 25mL 0.10M 弱酸（pKa=4.76）——pH 曲线三段式。
 * 半等价点 pH=pKa（缓冲区：酸碱各半，抗稀释）、等价点 pH>7（弱酸根水解）、过量区 pH 陡升。
 * 绿虚线 = 强酸强碱对照：等价点 pH=7、无缓冲区——曲线形状就是「弱」的指纹。
 * 画面（CARTESIAN 复用）：蓝线 = 弱酸滴定曲线累积，动点 = 当前滴定位置。
 */
public final class TitrationDemo implements Algorithm {

    private static final String SCHEMA = "1";
    private static final String INVARIANT =
            "缓冲区（半等价点附近）pH=pKa+log(碱/酸)——酸碱各半时 pH=pKa，加碱 pH 纹丝不动；"
                    + "等价点后过量 OH⁻ 接管 pH：弱酸等价点 pH>7 是水解的证据，拐点位置暴露酸的强弱";

    private static final double CB = 0.10;         // 碱浓度恒定
    private static final double V_ACID = 25.0;     // mL
    private static final double PKA = 4.76;
    private static final double STEP_ML = 0.5;
    private static final int DROPS = 60;           // 滴到 30mL
    private static final int EMIT_EVERY = 3;

    private final double ca;                       // 酸浓度（变体可换）

    private final List<Step> steps = new ArrayList<>();
    private final List<List<Double>> weakTrail = new ArrayList<>();
    private final List<List<Double>> strongTrail = new ArrayList<>();
    private double vb, phWeak, phStrong;

    public TitrationDemo() {
        this(0.10);
    }

    /** 变体：换酸浓度（等价点体积随之移动）。 */
    public TitrationDemo(double ca) {
        this.ca = ca;
    }

    @Override public String id() { return "chem.titration"; }
    @Override public String title() { return "反应过程·酸碱滴定"; }
    @Override public String codeResource() { return "/code/TitrationDemo.java"; }
    @Override public String docResource() { return "/markdown/titration.md"; }

    private double conc() {
        return ca;
    }

    private double veq() {
        return V_ACID * ca / CB;   // Ve = Va·Ca/Cb
    }

    @Override
    public Trace produce() {
        steps.clear();
        weakTrail.clear();
        strongTrail.clear();
        vb = 0;
        phWeak = initialPh();
        phStrong = -Math.log10(conc());
        emit(7, List.of(VizEvent.of(VizEvent.EventType.INIT,
                String.format("%.0fmL %.2fM 弱酸（pKa=%.2f）待滴定，0.5mL/滴 0.10M NaOH——等价点 %.1fmL",
                        V_ACID, conc(), PKA, veq()))),
                "滴定的本质是化学计量计数：pH 只是「数完了没有」的显示表");
        for (int d = 1; d <= DROPS; d++) {
            vb = STEP_ML * d;
            phWeak = weakPh(vb);
            phStrong = strongPh(vb);
            weakTrail.add(List.of(vb, phWeak));
            strongTrail.add(List.of(vb, phStrong));
            if (d % EMIT_EVERY == 0 || d == DROPS) {
                String stage = vb < veq() / 2 ? "缓冲前段" : vb < veq() ? "缓冲区（半等价点 pH=pKa）" : vb <= veq() ? "逼近等价点" : "过量区";
                emit(11, List.of(VizEvent.of(VizEvent.EventType.SET,
                        String.format("Vb=%.1fmL：pH=%.2f（弱酸） vs %.2f（强酸对照）——%s", vb, phWeak, phStrong, stage))),
                        vb < veq() && Math.abs(vb - veq() / 2) > STEP_ML * 2 ? "缓冲区：加碱 pH 几乎不动——酸碱库存互为缓冲"
                                : vb > veq() ? "过量 OH⁻ 接管 pH：两条曲线在此重新汇合"
                                : "曲线开始爬坡：缓冲库存见底，pH 失去抵抗力");
            }
        }
        emit(15, List.of(VizEvent.of(VizEvent.EventType.MARK_SORTED,
                String.format("等价点 %.1fmL：弱酸 pH=%.2f > 7（Ac⁻ 水解的证据！），强酸对照恰为 7——拐点一锤定音", veq(), weakPh(veq())),
                String.format("x%d", (int) (veq() / STEP_ML)))),
                "「等价点 pH>7」不是误差，是弱酸根偷走 H₂O 里一个 H⁺ 的直接后果");
        emit(20, List.of(VizEvent.of(VizEvent.EventType.RETURN,
                "指示剂选型就看拐点跨度：酚酞（8.2~10）落在弱酸陡升段——选错指示剂 = 全盘读数漂移")),
                "血液碳酸-碳酸氢钠缓冲对让你的 pH 钉在 7.35~7.45：缓冲区平坦段救你的命");
        return new Trace(SCHEMA, meta(), steps);
    }

    private double initialPh() {
        return 0.5 * (PKA - Math.log10(conc()));   // 弱酸近似：pH = ½(pKa − lgC)
    }

    /** 弱酸滴定 pH：缓冲区用 Henderson-Hasselbalch，等价点用水解式，过量区用 OH⁻。 */
    private double weakPh(double v) {
        double ve = veq();
        if (v < 0.01) {
            return initialPh();
        }
        if (v < ve - 0.01) {
            return PKA + Math.log10(v / (ve - v));
        }
        if (v <= ve + 0.01) {
            return 7 + 0.5 * (PKA + Math.log10(conc()));   // 等价点：水解 pH = 7 + ½(pKa + lgC)
        }
        double oh = (v - ve) * conc() / (V_ACID + v);
        return 14 + Math.log10(oh);
    }

    /** 强酸对照：等价点前剩余 H⁺ 决定 pH，等价点后过量 OH⁻。 */
    private double strongPh(double v) {
        double ve = veq();
        if (v < ve - 0.01) {
            double h = (V_ACID * conc() - v * conc()) / (V_ACID + v);
            return -Math.log10(Math.max(h, 1e-10));
        }
        if (v <= ve + 0.01) {
            return 7.0;
        }
        double oh = (v - ve) * conc() / (V_ACID + v);
        return 14 + Math.log10(oh);
    }

    private void emit(int line, List<VizEvent> events, String why) {
        StackFrame fr = new StackFrame("titration",
                OrderedMaps.of("Vb", Value.prim(String.format("%.1fmL", vb)),
                        "pH弱酸", Value.prim(String.format("%.2f", phWeak)),
                        "pH强酸", Value.prim(String.format("%.2f", phStrong)),
                        "pKa", Value.prim(String.valueOf(PKA))), null);
        Map<String, Layer> layers = new LinkedHashMap<>();
        layers.put("mem.stack", new StackLayer(List.of(fr)));
        layers.put("mem.heap", new HeapLayer(snapshotHeap()));
        layers.put("math.plot", snapshotPlot());
        steps.add(new Step(steps.size(), Loc.line(line, why),
                Collections.unmodifiableMap(layers), List.copyOf(events), new Intent(why, INVARIANT)));
    }

    private Heap snapshotHeap() {
        Map<String, Value> fields = new LinkedHashMap<>();
        fields.put("等价点体积", Value.prim(String.format("%.1fmL", veq())));
        fields.put("半等价点 pH", Value.prim(String.format("= pKa = %.2f", PKA)));
        fields.put("等价点 pH（弱酸）", Value.prim(String.format("%.2f（>7 水解）", 7 + 0.5 * (PKA + Math.log10(conc())))));
        Map<String, HeapObject> objs = new LinkedHashMap<>();
        objs.put("@1", new HeapObject("@1", "titration", "TitrantState",
                Collections.unmodifiableMap(fields), List.of()));
        return new Heap(Collections.unmodifiableMap(objs));
    }

    private PlotView snapshotPlot() {
        List<List<Double>> weak = new ArrayList<>();
        for (List<Double> p : weakTrail) {
            weak.add(List.copyOf(p));
        }
        List<List<Double>> strong = new ArrayList<>();
        for (List<Double> p : strongTrail) {
            strong.add(List.copyOf(p));
        }
        List<String> labels = new ArrayList<>();
        labels.add(String.format("滴定曲线：%.2fM 弱酸（蓝） vs 强酸（绿虚），pKa=%.2f", conc(), PKA));
        labels.add(String.format("半等价点 %.1fmL → pH=pKa；等价点 %.1fmL → pH=%.2f>7", veq() / 2, veq(), 7 + 0.5 * (PKA + Math.log10(conc()))));
        labels.add(String.format("Vb=%.1fmL  pH=%.2f", vb, phWeak));
        return new PlotView(PlotView.CARTESIAN, "pH 的三段式旅程", vb, vb, phWeak,
                0, DROPS * STEP_ML + 1, 0, 14,
                Collections.unmodifiableList(weak),
                Collections.unmodifiableList(strong), List.of(), List.of(),
                Collections.unmodifiableList(labels));
    }

    private Trace.TraceMeta meta() {
        return new Trace.TraceMeta(id(), title(), codeResource(),
                String.format("%.2fM %.0fmL pKa=%.2f", conc(), V_ACID, PKA), DROPS / EMIT_EVERY + 3);
    }

    @Override
    public java.util.List<String> dataLines() {
        return java.util.List.of(String.format("double ca = %.2f, cb = 0.10;   //@DATA 酸碱浓度", conc()));
    }

    @Override
    public Complexity complexity() {
        return new Complexity("O(1) 每滴代公式", "O(n) 曲线点数",
                "水质检测、药品含量测定、血液气体分析——"
                        + "滴定把「看不见的浓度」变成「看得见的拐点」：化学最古老的计数器");
    }
}
