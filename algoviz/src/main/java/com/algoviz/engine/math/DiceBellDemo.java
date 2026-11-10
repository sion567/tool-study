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
import com.algoviz.engine.math.MathView;
import com.algoviz.engine.mem.Heap;
import com.algoviz.engine.mem.HeapLayer;
import com.algoviz.engine.mem.HeapObject;
import com.algoviz.engine.mem.StackFrame;
import com.algoviz.engine.mem.StackLayer;
import com.algoviz.engine.trace.Trace;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.List;
import java.util.Map;

/**
 * 概率·两枚骰子 → 钟形分布（中心极限定理初见）。
 * 一枚骰子：6 个面均匀；两枚之和：中间高两头低的钟形（7 点概率 6/36 最大）。
 * "独立随机量相加 → 钟形"——这就是正态分布无处不在的原因（中心极限定理）。
 * 画面：RIEMANN 形态复用为直方图（矩形条=概率），两幕：单枚均匀 → 双枚钟形。
 */
public final class DiceBellDemo implements Algorithm {

    private static final String SCHEMA = "1";
    private static final String INVARIANT =
            "一枚骰子均匀分布；两枚之和的分布 = 卷积 → 中间高两头低——再加一枚更像钟形（中心极限定理）";

    private int mode = 1;   // 1=单枚 2=两枚之和
    private double expect;  // 期望值

    private final List<Step> steps = new ArrayList<>();

    @Override public String id() { return "math.dicebell"; }
    @Override public String title() { return "概率·骰子与钟形"; }
    @Override public String codeResource() { return "/code/DiceBellDemo.java"; }
    @Override public String docResource() { return "/markdown/dice-bell.md"; }

    @Override
    public Trace produce() {
        steps.clear();
        mode = 1;
        expect = 3.5;
        emit(4, List.of(VizEvent.of(VizEvent.EventType.INIT,
                "一枚骰子：1~6 每面 1/6——完全平坦（均匀分布）")),
                "单看一枚：没有任何'钟形'的迹象——惊喜在相加之后");
        emit(6, List.of(VizEvent.of(VizEvent.EventType.SET, "均匀分布：期望 3.5，方差 35/12≈2.92")),
                "均匀分布的'平坦'是原料——把两份独立随机相加，形状立刻改变");
        mode = 2;
        expect = 7.0;
        emit(9, List.of(VizEvent.of(VizEvent.EventType.MARK_SORTED,
                "两枚之和（2~12）：直方图拔地而起一座钟——7 点概率 6/36 ≈ 16.7% 最高")),
                "和的分布 = 卷积：3 的写法有 1+2/2+1，7 的写法有 6 种——路径越多概率越大");
        emit(12, List.of(VizEvent.of(VizEvent.EventType.SET,
                "对称性：P(和=s) = P(和=14−s)——直方图左右镜像")),
                "对称来自骰子的公平性：每一组合 (a,b) 与 (7−a, 7−b) 概率相同");
        emit(14, List.of(VizEvent.of(VizEvent.EventType.RETURN,
                "三枚之和更像钟、一百枚之和就是正态——中心极限定理：随机相加必然走向钟形")),
                "为什么现实中处处正态（身高/误差/考试成绩）？——因为它们都是无数独立因素的'和'");
        return new Trace(SCHEMA, meta(), steps);
    }

    private void emit(int line, List<VizEvent> events, String why) {
        StackFrame frame = new StackFrame("diceBell",
                OrderedMaps.of("骰子数", Value.prim(mode),
                        "期望", Value.prim(String.valueOf(expect)),
                        "形状", Value.prim(mode == 1 ? "均匀" : "钟形")), null);
        Map<String, Layer> layers = new LinkedHashMap<>();
        layers.put("mem.stack", new StackLayer(List.of(frame)));
        layers.put("mem.heap", new HeapLayer(snapshotHeap()));
        layers.put("math.plot", snapshotPlot());
        layers.put("math.matrix", snapshotMatrix());
        steps.add(new Step(steps.size(), Loc.line(line, why),
                Collections.unmodifiableMap(layers), List.copyOf(events), new Intent(why, invariant())));
    }

    private String invariant() {
        return INVARIANT;
    }

    private Heap snapshotHeap() {
        Map<String, Value> fields = new LinkedHashMap<>();
        fields.put("骰子数", Value.prim(mode));
        fields.put("期望", Value.prim(String.valueOf(expect)));
        fields.put("形状", Value.prim(mode == 1 ? "均匀分布" : "钟形（卷积）"));
        Map<String, HeapObject> objs = new LinkedHashMap<>();
        objs.put("@1", new HeapObject("@1", "dist", "DiceDistribution",
                Collections.unmodifiableMap(fields), List.of()));
        return new Heap(Collections.unmodifiableMap(objs));
    }

    private MathView snapshotMatrix() {
        List<String> vals = new ArrayList<>();
        Set<Integer> active = new LinkedHashSet<>();
        if (mode == 1) {
            for (int f = 1; f <= 6; f++) {
                vals.add(String.valueOf(f));
                active.add(f - 1);
            }
            MathView.Grid grid = new MathView.Grid("die", "一枚骰子：6 面等概率", 1, 6,
                    Collections.unmodifiableList(vals), null, null,
                    Collections.unmodifiableSet(active), Set.of());
            return new MathView("骰子分布", List.of(grid));
        }
        int[] counts = {1, 2, 3, 4, 5, 6, 5, 4, 3, 2, 1};   // 和 2..12
        int maxIdx = 5;
        for (int c : counts) {
            vals.add(String.valueOf(c));
        }
        active.add(maxIdx);
        MathView.Grid grid = new MathView.Grid("sums", "两枚之和（2~12）的组合数", 1, 11,
                Collections.unmodifiableList(vals), null, null,
                Collections.unmodifiableSet(active), Set.of());
        return new MathView("骰子分布", List.of(grid));
    }

    private PlotView snapshotPlot() {
        List<List<Double>> rects = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        if (mode == 1) {
            for (int f = 1; f <= 6; f++) {
                rects.add(List.of(f - 0.4, f + 0.4, 1.0));
            }
            labels.add("一枚骰子：P(每面) = 1/6 ≈ 0.167");
            labels.add("期望 3.5——完全平坦");
        } else {
            int[] counts = {1, 2, 3, 4, 5, 6, 5, 4, 3, 2, 1};
            for (int i = 0; i < counts.length; i++) {
                int s = i + 2;
                rects.add(List.of(s - 0.42, s + 0.42, (double) counts[i]));
            }
            labels.add("两枚之和：P(7) = 6/36 ≈ 16.7% 最高");
            labels.add("钟形 = 卷积的结果");
            labels.add("再相加一枚 → 更像正态（中心极限定理）");
        }
        return new PlotView(PlotView.RIEMANN, mode == 1 ? "一枚骰子：均匀分布" : "两枚骰子：钟形诞生",
                0, 0, 0, 0.5, 13.5, 0, 7.0,
                List.of(), List.of(), List.of(),
                Collections.unmodifiableList(rects),
                Collections.unmodifiableList(labels));
    }

    private Trace.TraceMeta meta() {
        return new Trace.TraceMeta(id(), title(), codeResource(), "1→2 枚", 2);
    }

    @Override
    public java.util.List<String> dataLines() {
        return java.util.List.of("int dice = 2;   // 每枚 6 面");
    }

    @Override
    public Complexity complexity() {
        return new Complexity("O(36) 枚举所有组合", "O(1)",
                "卷积：两分布之和的分布 = 逐对组合的计数。三枚骰子 216 组合、n 枚 6ⁿ——但钟形不请自来");
    }
}
