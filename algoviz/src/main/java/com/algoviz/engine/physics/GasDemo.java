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
 * 分子动理论·理想气体：40 个分子在绝热箱里横冲直撞，弹性碰壁不改速率。
 * 压强的微观本质 = 单位时间单位面积碰壁的动量流——"统计"出来的宏观量。
 * 温度 ∝ 平均动能 ∝ RMS 速度²：碰壁只翻方向不改大小 → RMS 速度恒定 = 温度恒定。
 * 画面（SCATTER 复用）：蓝点 = 分子，边界矩形 = 绝热箱（4×3）。
 */
public final class GasDemo implements Algorithm {

    private static final String SCHEMA = "1";
    private static final String INVARIANT =
            "弹性碰壁只翻转法向速度分量，速率不变——RMS 速度恒定 = 温度恒定（能量守恒）；"
                    + "压强不是每个分子的属性，是亿万次碰壁的统计平均";

    private static final int N = 40;
    private static final double W = 4.0, H = 3.0;   // 箱子尺寸
    private static final double DT = 0.05;
    private static final int TICKS = 60;
    private static final int EMIT_EVERY = 3;

    private final long seed;

    private final List<Step> steps = new ArrayList<>();
    private double[] x, y, vx, vy;
    private int hits;
    private double rms;
    private int tick;

    public GasDemo() {
        this(42);
    }

    /** 种子变体：换一批初始位置/速度——RMS 依旧恒定（守恒律与运气无关）。 */
    public GasDemo(long seed) {
        this.seed = seed;
    }

    @Override public String id() { return "physics.gas"; }
    @Override public String title() { return "分子动理论·理想气体"; }
    @Override public String codeResource() { return "/code/GasDemo.java"; }
    @Override public String docResource() { return "/markdown/gas.md"; }

    @Override
    public Trace produce() {
        steps.clear();
        initParticles();
        hits = 0;
        rms = computeRms();
        tick = 0;
        emit(4, List.of(VizEvent.of(VizEvent.EventType.INIT,
                String.format("%d 个分子自由冲刺（速率 %.1f~%.1f），弹性碰壁只翻方向——温度的微观图像启动",
                        N, 0.8, 2.4))),
                "理想气体假设：分子自身不占体积、碰撞完全弹性——简单到只有位置和速度两条公式");
        for (int i = 0; i < TICKS; i++) {
            advance();
            tick = i + 1;
            if ((i + 1) % EMIT_EVERY == 0 || i == TICKS - 1) {
                emit(13, List.of(VizEvent.of(VizEvent.EventType.SET,
                        String.format("step=%d：累计碰壁 %d 次，RMS=%.3f（纹丝不动）", tick, hits, rms))),
                        "碰壁次数在涨、RMS 不动：方向在洗牌，能量在守恒");
            }
        }
        double pressure = hits / (tick * DT) / (2 * (W + H));
        emit(17, List.of(VizEvent.of(VizEvent.EventType.MARK_SORTED,
                String.format("收官：碰壁 %d 次，压强 ∝ 碰壁频率 ≈ %.3f——宏观的 P 是微观撞击的统计影子",
                        hits, pressure))),
                "把温度计插进这箱分子，读数永远不动——温度就是平均动能的别名");
        emit(19, List.of(VizEvent.of(VizEvent.EventType.RETURN,
                "P = nkT·(2/N)/3：压强公式能从纯力学碰壁推导出来——统计力学把两个世界焊在一起")),
                "布朗运动、扩散、阿伏伽德罗常数——「原子真实存在」的第一个证据全靠这套统计");
        return new Trace(SCHEMA, meta(), steps);
    }

    private void initParticles() {
        Random r = new Random(seed);
        x = new double[N];
        y = new double[N];
        vx = new double[N];
        vy = new double[N];
        for (int i = 0; i < N; i++) {
            x[i] = 0.15 + r.nextDouble() * (W - 0.3);
            y[i] = 0.15 + r.nextDouble() * (H - 0.3);
            double sp = 0.8 + r.nextDouble() * 1.6;
            double th = r.nextDouble() * 2 * Math.PI;
            vx[i] = sp * Math.cos(th);
            vy[i] = sp * Math.sin(th);
        }
    }

    /** 一步移动 + 弹性碰壁（翻转法向速度、计数、夹回箱内）。 */
    private void advance() {
        for (int i = 0; i < N; i++) {
            x[i] += vx[i] * DT;
            y[i] += vy[i] * DT;
            if (x[i] < 0 || x[i] > W) {
                vx[i] = -vx[i];
                x[i] = Math.max(0, Math.min(W, x[i]));
                hits++;
            }
            if (y[i] < 0 || y[i] > H) {
                vy[i] = -vy[i];
                y[i] = Math.max(0, Math.min(H, y[i]));
                hits++;
            }
        }
    }

    private double computeRms() {
        double sum = 0;
        for (int i = 0; i < N; i++) {
            sum += vx[i] * vx[i] + vy[i] * vy[i];
        }
        return Math.sqrt(sum / N);
    }

    private void emit(int line, List<VizEvent> events, String why) {
        StackFrame fr = new StackFrame("gas",
                OrderedMaps.of("step", Value.prim(String.valueOf(tick)),
                        "碰壁", Value.prim(hits + " 次"),
                        "RMS", Value.prim(String.format("%.3f", rms))), null);
        Map<String, Layer> layers = new LinkedHashMap<>();
        layers.put("mem.stack", new StackLayer(List.of(fr)));
        layers.put("mem.heap", new HeapLayer(snapshotHeap()));
        layers.put("math.plot", snapshotPlot());
        steps.add(new Step(steps.size(), Loc.line(line, why),
                Collections.unmodifiableMap(layers), List.copyOf(events), new Intent(why, INVARIANT)));
    }

    private Heap snapshotHeap() {
        double pressure = tick > 0 ? hits / (tick * DT) / (2 * (W + H)) : 0;
        Map<String, Value> fields = new LinkedHashMap<>();
        fields.put("分子数", Value.prim(String.valueOf(N)));
        fields.put("RMS 速度", Value.prim(String.format("%.3f（恒定）", rms)));
        fields.put("累计碰壁", Value.prim(hits + " 次"));
        fields.put("压强 ∝", Value.prim(String.format("%.3f", pressure)));
        Map<String, HeapObject> objs = new LinkedHashMap<>();
        objs.put("@1", new HeapObject("@1", "gas", "KineticState",
                Collections.unmodifiableMap(fields), List.of()));
        return new Heap(Collections.unmodifiableMap(objs));
    }

    private PlotView snapshotPlot() {
        List<List<Double>> rects = new ArrayList<>();
        for (int i = 0; i < N; i++) {
            rects.add(List.of(x[i], y[i], 0.004));
        }
        List<String> labels = new ArrayList<>();
        labels.add(String.format("理想气体：%d 分子 × 弹性碰壁", N));
        labels.add(String.format("RMS=%.3f 恒定（=温度恒定），碰壁 %d 次", rms, hits));
        labels.add("压强 ∝ 单位时间碰壁频率——宏观量的微观出身");
        return new PlotView(PlotView.SCATTER, "分子动理论：压强的出身", 0, 0, 0,
                0, W, 0, H,
                List.of(), List.of(), List.of(),
                Collections.unmodifiableList(rects),
                Collections.unmodifiableList(labels));
    }

    private Trace.TraceMeta meta() {
        return new Trace.TraceMeta(id(), title(), codeResource(),
                String.format("%d 分子 种子%d", N, seed), TICKS / EMIT_EVERY + 3);
    }

    @Override
    public java.util.List<String> dataLines() {
        return java.util.List.of(String.format("java.util.Random rnd = new java.util.Random(%d);", seed));
    }

    @Override
    public Complexity complexity() {
        return new Complexity("O(N) 每步（无相互作用）", "O(N) 分子状态",
                "真实气体要算 N² 两两碰撞——百万分子直接爆算力；"
                        + "分子动力学用 cell list / 邻居表砍到近似 O(N)，科研模拟的全是这类优化");
    }
}
