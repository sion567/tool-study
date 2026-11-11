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
 * 反应过程·反应动力学：一级 vs 二级衰减同屏——半衰期谁恒定谁是"一级"。
 * 一级 [A] = A₀·e^(−kt)：半衰期 t½ = ln2/k 恒定（放射性测年的原理）。
 * 二级 [A] = A₀/(1+kA₀t)：半衰期翻倍增长——消耗越多、走得越慢。
 * 画面（CARTESIAN 复用）：蓝线 = 一级曲线 + 动点，绿虚线 = 二级对照曲线。
 */
public final class KineticsDemo implements Algorithm {

    private static final String SCHEMA = "1";
    private static final String INVARIANT =
            "一级 t½ = ln2/k 与浓度无关——每过 t½ 浓度必减半（指数衰减的记忆缺失）；"
                    + "二级 t½ = 1/(k[A]₀) 随浓度降低翻倍——线性检验（ln[A] vs 1/[A] 谁是直线）是反应级数的实验判据";

    private static final double A0 = 1.0;
    private static final double K2 = 0.2;          // 二级速率常数 L/(mol·s)
    private static final double T_TOTAL = 12.0;
    private static final int TICKS = 24;
    private static final int EMIT_EVERY = 2;

    private final double k1;

    private final List<Step> steps = new ArrayList<>();
    private final List<List<Double>> firstTrail = new ArrayList<>();   // 一级曲线
    private final List<List<Double>> secondTrail = new ArrayList<>();  // 二级对照
    private double t, concFirst, concSecond;

    public KineticsDemo() {
        this(0.2);
    }

    /** 变体：换一级速率常数 k（0.05~0.4）。 */
    public KineticsDemo(double k1) {
        this.k1 = k1;
    }

    @Override public String id() { return "chem.kinetics"; }
    @Override public String title() { return "反应过程·反应动力学"; }
    @Override public String codeResource() { return "/code/KineticsDemo.java"; }
    @Override public String docResource() { return "/markdown/kinetics.md"; }

    @Override
    public Trace produce() {
        steps.clear();
        firstTrail.clear();
        secondTrail.clear();
        t = concFirst = concSecond = 0;
        emit(5, List.of(VizEvent.of(VizEvent.EventType.INIT,
                String.format("两种反应同浓度 A₀=%.1f 起跑：一级 k=%.2f/s（蓝），二级 k=%.2f L/(mol·s)（绿虚）——半衰期见分晓", A0, k1, K2))),
                "浓度只剩 1 个反应物级数的反应：速率 ∝ [A]^n——n 是几，曲线形状就完全不同");
        for (int i = 0; i <= TICKS; i++) {
            t = T_TOTAL * i / TICKS;
            concFirst = A0 * Math.exp(-k1 * t);
            concSecond = A0 / (1 + K2 * A0 * t);
            firstTrail.add(List.of(t, concFirst));
            secondTrail.add(List.of(t, concSecond));
            if (i % EMIT_EVERY == 0 || i == TICKS) {
                emit(9, List.of(VizEvent.of(VizEvent.EventType.SET,
                        String.format("t=%.1fs：[A]一级=%.3f（已过 %d 个半衰期），[A]二级=%.3f",
                                t, concFirst, (int) (t * k1 / Math.log(2)), concSecond))),
                        "一级每过 ln2/k 就雷打不动减半——蓝点沿固定节拍阶梯下行");
            }
        }
        emit(13, List.of(VizEvent.of(VizEvent.EventType.MARK_SORTED,
                String.format("收官：一级 t½=%.2fs（恒定），二级已从 %.0fs 涨到 %.0fs——衰减越慢是二级的指纹",
                        Math.log(2) / k1, 1 / (K2 * A0), 1 / (K2 * concSecond)))),
                "二级反应越消耗越「恋战」：浓度掉一半，速率跟着掉一半以上");
        emit(16, List.of(VizEvent.of(VizEvent.EventType.RETURN,
                "实验判级数：画 ln[A]-t 是直线 → 一级；画 1/[A]-t 是直线 → 二级——谁线性谁上岗")),
                "碳-14 测年（半衰期 5730 年恒定）、药物代谢半衰期——一级衰减是自然界的默认钟表");
        return new Trace(SCHEMA, meta(), steps);
    }

    private void emit(int line, List<VizEvent> events, String why) {
        StackFrame fr = new StackFrame("kinetics",
                OrderedMaps.of("t", Value.prim(String.format("%.1fs", t)),
                        "[A]一级", Value.prim(String.format("%.3f M", concFirst)),
                        "[A]二级", Value.prim(String.format("%.3f M", concSecond)),
                        "t½一级", Value.prim(String.format("%.2fs（恒定）", Math.log(2) / k1))), null);
        Map<String, Layer> layers = new LinkedHashMap<>();
        layers.put("mem.stack", new StackLayer(List.of(fr)));
        layers.put("mem.heap", new HeapLayer(snapshotHeap()));
        layers.put("math.plot", snapshotPlot());
        steps.add(new Step(steps.size(), Loc.line(line, why),
                Collections.unmodifiableMap(layers), List.copyOf(events), new Intent(why, INVARIANT)));
    }

    private Heap snapshotHeap() {
        Map<String, Value> fields = new LinkedHashMap<>();
        fields.put("一级 t½", Value.prim(String.format("%.2fs（恒定）", Math.log(2) / k1)));
        fields.put("二级 t½ 当前", Value.prim(String.format("%.2fs（涨中）", 1 / (K2 * concSecond))));
        fields.put("二级 t½ 初值", Value.prim(String.format("%.2fs", 1 / (K2 * A0))));
        Map<String, HeapObject> objs = new LinkedHashMap<>();
        objs.put("@1", new HeapObject("@1", "kinetics", "DecayState",
                Collections.unmodifiableMap(fields), List.of()));
        return new Heap(Collections.unmodifiableMap(objs));
    }

    private PlotView snapshotPlot() {
        List<List<Double>> first = new ArrayList<>();
        for (List<Double> p : firstTrail) {
            first.add(List.copyOf(p));
        }
        List<List<Double>> second = new ArrayList<>();
        for (List<Double> p : secondTrail) {
            second.add(List.copyOf(p));
        }
        List<String> labels = new ArrayList<>();
        labels.add(String.format("动力学：一级 k=%.2f（蓝） vs 二级 k=%.2f（绿虚）", k1, K2));
        labels.add(String.format("一级 t½=%.2fs 恒定；二级 t½ 已涨至 %.2fs", Math.log(2) / k1, 1 / (K2 * concSecond)));
        labels.add(String.format("t=%.1fs  [A]=%.3f / %.3f M", t, concFirst, concSecond));
        return new PlotView(PlotView.CARTESIAN, "浓度衰减：一级 vs 二级", t, t, concFirst,
                0, T_TOTAL + 0.3, 0, A0 * 1.15,
                Collections.unmodifiableList(first),
                Collections.unmodifiableList(second), List.of(), List.of(),
                Collections.unmodifiableList(labels));
    }

    private Trace.TraceMeta meta() {
        return new Trace.TraceMeta(id(), title(), codeResource(),
                String.format("A₀=%.1f k₁=%.2f k₂=%.2f", A0, k1, K2), TICKS / EMIT_EVERY + 3);
    }

    @Override
    public java.util.List<String> dataLines() {
        return java.util.List.of(String.format("double k1 = %.2f, k2 = 0.2;   //@DATA 速率常数", k1));
    }

    @Override
    public Complexity complexity() {
        return new Complexity("O(1) 每时刻代公式", "O(n) 采样点",
                "药物半衰期给药方案、碳-14 测年、核废料存储期——"
                        + "判断衰减是几级，就是判断时间常数属不属于浓度本身");
    }
}
