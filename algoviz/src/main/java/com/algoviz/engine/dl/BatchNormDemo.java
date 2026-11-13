package com.algoviz.engine.dl;

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
import java.util.List;
import java.util.Map;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 深度学习·BatchNorm：把每层的激活"拉回正态"，训练从此不踩独轮车。
 * 一个 batch 4 样本：mean=3.75、var=9.19 → 标准化 (x−μ)/√(σ²+ε) → γ·x̂+β 缩放平移。
 * γ/β 是可学习参数：网络自己决定"要多标准、偏多少"。
 * 数值确定性：batch [1,2,3,9]（含离群值 9——归一化前后对比鲜明）。
 * 画面（math.matrix 复用）：三行网格（原值 / 标准化 / γβ 后）逐行填充。
 */
public final class BatchNormDemo implements Algorithm {

    private static final String SCHEMA = "1";
    private static final String INVARIANT =
            "x̂ = (x−μ)/√(σ²+ε)：均值归 0 方差归 1——下一层不再面对漂移的分布（内部协变量偏移）；"
                    + "γ·x̂+β：缩放平移可学习——'要多标准、偏多少'网络自己定；推理用训练期滑动平均，单个样本也能算";

    private static final double[] BATCH = {1.0, 2.0, 3.0, 9.0};
    private static final double GAMMA = 1.2, BETA = 0.5, EPS = 1e-5;

    private final List<Step> steps = new ArrayList<>();
    private final double[] normed = new double[BATCH.length];
    private final double[] scaled = new double[BATCH.length];
    private double mean, var;

    @Override public String id() { return "dl.batchnorm"; }
    @Override public String title() { return "BatchNorm 归一化"; }
    @Override public String codeResource() { return "/code/BatchNormDemo.java"; }
    @Override public String docResource() { return "/markdown/batchnorm.md"; }

    @Override
    public Trace produce() {
        steps.clear();
        mean = 0;
        for (double v : BATCH) {
            mean += v;
        }
        mean /= BATCH.length;
        var = 0;
        for (double v : BATCH) {
            var += (v - mean) * (v - mean);
        }
        var /= BATCH.length;
        emit(7, List.of(VizEvent.of(VizEvent.EventType.INIT,
                String.format("batch=[1,2,3,9]（离群 9 特意保留）：μ=%.2f，σ²=%.2f——不归一化就喂给下一层", mean, var))),
                "内部协变量偏移：上一层参数一动，下一层的输入分布就漂——训练像踩独轮车");
        for (int i = 0; i < BATCH.length; i++) {
            normed[i] = (BATCH[i] - mean) / Math.sqrt(var + EPS);
            scaled[i] = GAMMA * normed[i] + BETA;
            emit(11, List.of(VizEvent.of(VizEvent.EventType.SET,
                    String.format("x%d=%.1f → x̂=%.3f（标准化）→ γβ 后=%.3f", i + 1, BATCH[i], normed[i], scaled[i]))),
                    "标准化把每个值换算成'偏离均值几个标准差'——离群 9 也被拉回正常刻度");
        }
        emit(14, List.of(VizEvent.of(VizEvent.EventType.MARK_SORTED,
                String.format("归一后均值=%.3f（≈0）——γ=%.1f β=%.1f 可学习：网络自己决定要多标准、偏多少", batchMeanOf(normed), GAMMA, BETA))),
                "γβ 让归一化不是硬约束：网络可以学回'我就需要偏一点'");
        emit(16, List.of(VizEvent.of(VizEvent.EventType.RETURN,
                "训练/推理差异：推理用训练期的滑动平均 μ/σ——单个样本也能归一化")),
                "BatchNorm(2015) 让深层网络收敛快一个数量级——残差连接是它的黄金搭档");
        return new Trace(SCHEMA, meta(), steps);
    }

    private double batchMeanOf(double[] a) {
        double m = 0;
        for (double v : a) {
            m += v;
        }
        return m / a.length;
    }

    private void emit(int line, List<VizEvent> events, String why) {
        StackFrame frame = new StackFrame("batchnorm",
                OrderedMaps.of("μ", Value.prim(String.format("%.2f", mean)),
                        "σ²", Value.prim(String.format("%.2f", var)),
                        "γ/β", Value.prim("1.2 / 0.5")), null);
        Map<String, Layer> layers = new LinkedHashMap<>();
        layers.put("mem.stack", new StackLayer(List.of(frame)));
        layers.put("mem.heap", new HeapLayer(snapshotHeap()));
        layers.put("math.matrix", snapshotMath());
        steps.add(new Step(steps.size(), Loc.line(line, why),
                Collections.unmodifiableMap(layers), List.copyOf(events), new Intent(why, INVARIANT)));
    }

    private Heap snapshotHeap() {
        Map<String, Value> fields = new LinkedHashMap<>();
        fields.put("μ / σ²", Value.prim(String.format("%.2f / %.2f", mean, var)));
        fields.put("γ / β", Value.prim("1.2 / 0.5（可学习）"));
        fields.put("ε", Value.prim("1e-5（防除零）"));
        Map<String, HeapObject> objs = new LinkedHashMap<>();
        objs.put("@1", new HeapObject("@1", "bn", "BatchNormLayer",
                Collections.unmodifiableMap(fields), List.of()));
        return new Heap(Collections.unmodifiableMap(objs));
    }

    private MathView snapshotMath() {
        List<String> vals = new ArrayList<>();
        Set<Integer> active = new LinkedHashSet<>();
        for (int i = 0; i < BATCH.length; i++) {
            vals.add(String.format("%.1f", BATCH[i]));
        }
        for (int i = 0; i < BATCH.length; i++) {
            vals.add(String.format("%.3f", normed[i]));
            active.add(BATCH.length + i);
        }
        for (int i = 0; i < BATCH.length; i++) {
            vals.add(String.format("%.3f", scaled[i]));
        }
        MathView.Grid grid = new MathView.Grid("bn", "三行：原值 / 标准化 x̂（均值≈0）/ γβ 缩放后",
                3, BATCH.length, vals, null, null, active, Set.of());
        return new MathView("BatchNorm", List.of(grid));
    }

    private Trace.TraceMeta meta() {
        return new Trace.TraceMeta(id(), title(), codeResource(), "batch=4 离群保留", BATCH.length);
    }

    @Override
    public java.util.List<String> dataLines() {
        return java.util.List.of("double[] batch = {1, 2, 3, 9};   //@DATA 离群 9 特意保留");
    }

    @Override
    public Complexity complexity() {
        return new Complexity("O(batch) 统计量计算", "O(1) γ/β 两个参数",
                "BatchNorm 让'训得动'变成'训得快'：残差解决能训多深，BN 解决训得多快——两者是 2015 年的双子星");
    }
}
