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

/**
 * 黄金分割·斐波那契螺旋：F(n+1)/F(n) 逐次逼近 φ = 1.6180339887…，
 * 同时画出对数螺线 r = φ^(2θ/π)——每转四分之一圈，半径乘一次 φ（与斐波那契方块同构）。
 */
public final class GoldenSpiralDemo implements Algorithm {

    private static final String SCHEMA = "1";
    private static final String INVARIANT =
            "相邻斐波那契数之比 → φ=(1+√5)/2 ≈ 1.618；r = φ^(2θ/π) 的对数螺线每 1/4 圈放大 φ 倍";

    private static final double PHI = (1 + Math.sqrt(5)) / 2;
    private static final int STEPS = 8;
    private static final double QUARTER = Math.PI / 2;

    private final List<Step> steps = new ArrayList<>();
    private final List<List<Double>> spiral = new ArrayList<>();
    private double thetaMax, pointX, pointY, ratio;

    @Override public String id() { return "math.goldenspiral"; }
    @Override public String title() { return "黄金分割·斐波那契螺旋"; }
    @Override public String codeResource() { return "/code/GoldenSpiralDemo.java"; }
    @Override public String docResource() { return "/markdown/golden-spiral.md"; }

    @Override
    public Trace produce() {
        steps.clear();
        spiral.clear();
        thetaMax = pointX = pointY = ratio = 0;
        emit(4, List.of(VizEvent.of(VizEvent.EventType.INIT,
                "斐波那契数列 1,1,2,3,5,8,13,21——相邻两数之比会走向哪里？")),
                "向日葵、鹦鹉螺、松果的螺纹都在回答同一个数");
        long prev = 1, curr = 1;
        for (int n = 1; n <= STEPS; n++) {
            long next = prev + curr;
            ratio = (double) next / curr;   // F(n+1)/F(n)
            thetaMax = n * QUARTER;
            spiral.clear();
            int samples = 24 * n;
            for (int i = 0; i <= samples; i++) {
                double th = thetaMax * i / samples;
                double r = Math.pow(PHI, 2 * th / Math.PI);
                spiral.add(List.of(r * Math.cos(th), r * Math.sin(th)));
            }
            pointX = spiral.get(spiral.size() - 1).get(0);
            pointY = spiral.get(spiral.size() - 1).get(1);
            double nextRatio = (double) (curr + next) / next;
            emit(7, List.of(VizEvent.of(VizEvent.EventType.SET,
                    String.format("F(%d)/F(%d) = %d/%d ≈ %.4f", n + 2, n + 1, next, curr, ratio))),
                    "比值在 φ 两侧来回摆动，每次摆动幅度约减半");
            prev = curr;
            curr = next;
        }
        emit(10, List.of(VizEvent.of(VizEvent.EventType.MARK_SORTED,
                String.format("比值收敛于 φ = (1+√5)/2 = %.10f", PHI))),
                "黄金矩形切掉一个正方形，剩下的小矩形还是黄金矩形——对折永无止境，轨迹就是这条螺线");
        emit(12, List.of(VizEvent.of(VizEvent.EventType.RETURN,
                "φ 的连分数全是 1：[1;1,1,1,…]——'最无理'的数，却统治着生长")),
                "向日葵籽排列 137.5°（= 360°/φ²）：黄金角让每颗籽获得最多阳光");
        return new Trace(SCHEMA, meta(), steps);
    }

    private void emit(int line, List<VizEvent> events, String why) {
        StackFrame frame = new StackFrame("goldenSpiral",
                OrderedMaps.of("比值", Value.prim(String.format("%.4f", ratio)),
                        "φ", Value.prim("1.6180339887"),
                        "螺线转角", Value.prim(String.format("%.0f°", Math.toDegrees(thetaMax)))), null);
        Map<String, Layer> layers = new LinkedHashMap<>();
        layers.put("mem.stack", new StackLayer(List.of(frame)));
        layers.put("mem.heap", new HeapLayer(snapshotHeap()));
        layers.put("math.plot", snapshotPlot());
        steps.add(new Step(steps.size(), Loc.line(line, why),
                Collections.unmodifiableMap(layers), List.copyOf(events), new Intent(why, invariant())));
    }

    private String invariant() {
        return INVARIANT;
    }

    private Heap snapshotHeap() {
        Map<String, Value> fields = new LinkedHashMap<>();
        fields.put("比值", Value.prim(String.format("%.6f", ratio)));
        fields.put("φ", Value.prim("1.6180339887"));
        fields.put("螺线点数", Value.prim(String.valueOf(spiral.size())));
        Map<String, HeapObject> objs = new LinkedHashMap<>();
        objs.put("@1", new HeapObject("@1", "spiral", "GoldenSpiral",
                Collections.unmodifiableMap(fields), List.of()));
        return new Heap(Collections.unmodifiableMap(objs));
    }

    private PlotView snapshotPlot() {
        List<List<Double>> pts = new ArrayList<>();
        for (List<Double> p : spiral) pts.add(List.copyOf(p));
        List<String> labels = new ArrayList<>();
        labels.add("r = φ^(2θ/π)——对数螺线");
        labels.add(String.format("F(n+1)/F(n) ≈ %.4f", ratio));
        labels.add("每 1/4 圈半径 × φ");
        return new PlotView(PlotView.CARTESIAN, "黄金螺旋", thetaMax, pointX, pointY,
                -7.2, 7.2, -7.2, 7.2,
                Collections.unmodifiableList(pts),
                List.of(), List.of(), List.of(),
                Collections.unmodifiableList(labels));
    }

    private Trace.TraceMeta meta() {
        return new Trace.TraceMeta(id(), title(), codeResource(), "比值→φ", STEPS + 2);
    }

    @Override
    public java.util.List<String> dataLines() {
        return java.util.List.of("double phi = (1 + Math.sqrt(5)) / 2;   // 1.618…");
    }

    @Override
    public Complexity complexity() {
        return new Complexity("O(s) s=采样点数", "O(s) 点列",
                "比值收敛是二阶线性递推的特征根性质：x²=x+1 的两个根里，φ 主导、−1/φ 消失——误差每步乘 1/φ²≈0.38");
    }
}
