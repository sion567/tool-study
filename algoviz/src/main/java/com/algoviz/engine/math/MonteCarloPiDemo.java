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
import java.util.Random;

/**
 * 蒙特卡洛求 π：单位正方形里撒点，圆内点数 / 总点数 ≈ π/4。
 * 固定种子随机（golden 确定性），点数 100 → 6300 分批展示——估计值从粗糙到收敛。
 * 画面（SCATTER）：蓝点=圆内，橙点=圆外，绿色圆弧=四分之一圆。
 */
public final class MonteCarloPiDemo implements Algorithm {

    private static final String SCHEMA = "1";
    private static final String INVARIANT =
            "点在正方形内均匀分布 → P(落入四分之一圆) = π/4 → π ≈ 4×圆内点数/总点数——用随机性算确定值";

    private static final int[] BATCH_TOTALS = {100, 300, 700, 1500, 3100, 6300};
    private static final long SEED = 42;   // 固定种子：golden 逐字节稳定

    private final List<Step> steps = new ArrayList<>();
    private final List<List<Double>> inside = new ArrayList<>();
    private final List<List<Double>> outside = new ArrayList<>();
    private int n, inCount;

    @Override public String id() { return "math.montecarlo"; }
    @Override public String title() { return "蒙特卡洛求 π"; }
    @Override public String codeResource() { return "/code/MonteCarloPiDemo.java"; }
    @Override public String docResource() { return "/markdown/montecarlo.md"; }

    @Override
    public Trace produce() {
        steps.clear();
        inside.clear();
        outside.clear();
        n = inCount = 0;
        emit(4, List.of(VizEvent.of(VizEvent.EventType.INIT,
                "单位正方形撒随机点：圆内=蓝，圆外=橙（种子 42，可复现）")),
                "概率的桥梁：面积比 = 点数比——随机性变成了测量工具");
        Random r = new Random(SEED);
        List<List<Double>> boundary = new ArrayList<>();
        for (int i = 0; i <= 64; i++) {   // 四分之一圆弧（边界参考线）
            double t = Math.PI / 2 * i / 64;
            boundary.add(List.of(Math.cos(t), Math.sin(t)));
        }
        for (int total : BATCH_TOTALS) {
            while (n < total) {
                n++;
                double x = r.nextDouble(), y = r.nextDouble();
                if (x * x + y * y <= 1.0) {
                    inCount++;
                    inside.add(List.of(x, y, 0.008));
                } else {
                    outside.add(List.of(x, y, 0.008));
                }
            }
            double pi = 4.0 * inCount / n;
            emit(8, List.of(VizEvent.of(VizEvent.EventType.SET,
                    String.format("n=%d：圆内 %d → π ≈ %.4f", n, inCount, pi))),
                    "误差 ~1/√n：点数翻 4 倍精度才翻 1 倍——蒙特卡洛收敛慢但永不出错");
        }
        emit(11, List.of(VizEvent.of(VizEvent.EventType.MARK_SORTED,
                String.format("n=%d → π ≈ %.4f（真值 3.14159）", n, 4.0 * inCount / n))),
                "确定性问题的随机解法——Buffon 投针 1777 年起源，今天管 GPU 大规模采样都叫蒙特卡洛方法");
        emit(13, List.of(VizEvent.of(VizEvent.EventType.RETURN,
                "同一思想解决：积分、物理模拟、AI 强化学习——随机不是妥协，是算法")),
                "固定种子是工程要点：可复现 = 可调试 = 可测试");
        return new Trace(SCHEMA, meta(), steps);
    }

    private void emit(int line, List<VizEvent> events, String why) {
        StackFrame frame = new StackFrame("montecarlo",
                OrderedMaps.of("n", Value.prim(n),
                        "圆内", Value.prim(inCount),
                        "π 估计", Value.prim(String.format("%.4f", 4.0 * inCount / n))), null);
        Map<String, Layer> layers = new LinkedHashMap<>();
        layers.put("mem.stack", new StackLayer(List.of(frame)));
        layers.put("mem.heap", new HeapLayer(snapshotHeap()));
        layers.put("math.plot", snapshotPlot(boundary()));
        steps.add(new Step(steps.size(), Loc.line(line, why),
                Collections.unmodifiableMap(layers), List.copyOf(events), new Intent(why, INVARIANT)));
    }

    private List<List<Double>> boundary() {
        List<List<Double>> boundary = new ArrayList<>();
        for (int i = 0; i <= 64; i++) {
            double t = Math.PI / 2 * i / 64;
            boundary.add(List.of(Math.cos(t), Math.sin(t)));
        }
        return boundary;
    }

    private Heap snapshotHeap() {
        Map<String, Value> fields = new LinkedHashMap<>();
        fields.put("n", Value.prim(n));
        fields.put("圆内", Value.prim(inCount));
        fields.put("π 估计", Value.prim(String.format("%.4f", 4.0 * inCount / n)));
        fields.put("种子", Value.prim(String.valueOf(SEED)));
        Map<String, HeapObject> objs = new LinkedHashMap<>();
        objs.put("@1", new HeapObject("@1", "state", "MonteCarloState",
                Collections.unmodifiableMap(fields), List.of()));
        return new Heap(Collections.unmodifiableMap(objs));
    }

    private PlotView snapshotPlot(List<List<Double>> boundary) {
        List<String> labels = new ArrayList<>();
        labels.add("π ≈ 4 × 圆内 / 总数");
        labels.add(String.format("n = %d", n));
        labels.add(String.format("π ≈ %.4f", 4.0 * inCount / n));
        return new PlotView(PlotView.SCATTER, "蒙特卡洛求 π", 0, 0, 0,
                0, 1.02, 0, 1.02,
                Collections.unmodifiableList(boundary),      // 四分之一圆弧
                List.of(),
                Collections.unmodifiableList(outside),       // 圆外点（橙）
                Collections.unmodifiableList(inside),        // 圆内点（蓝）
                Collections.unmodifiableList(labels));
    }

    private Trace.TraceMeta meta() {
        return new Trace.TraceMeta(id(), title(), codeResource(), "种子42 n≤6300", BATCH_TOTALS.length + 2);
    }

    @Override
    public java.util.List<String> dataLines() {
        return java.util.List.of("long seed = 42;   // n: 100 → 6300");
    }

    @Override
    public Complexity complexity() {
        return new Complexity("O(n) n=撒点数，误差 O(1/√n)", "O(n) 存点",
                "维度无关性是它的杀手锏：100 维积分数值网格爆炸，蒙特卡洛照常工作——AI/物理模拟的底层引擎");
    }
}
