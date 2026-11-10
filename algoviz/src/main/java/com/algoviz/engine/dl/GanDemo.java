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
 * 深度学习·GAN 生成对抗：生成器（橙点）从远处一圈圈逼近真实分布（蓝环），
 * 判别器准确率从 0.88 一路跌到 0.52——当它分不清真假时，生成器就"毕业"了。
 * 画面（SCATTER 复用）：蓝点=真实样本，橙点=生成样本，绿弧=真实分布轮廓。
 */
public final class GanDemo implements Algorithm {

    private static final String SCHEMA = "1";
    private static final String INVARIANT =
            "G 与 D 极小极大博弈：D 越准 G 越要骗，G 越像 D 越瞎——纳什均衡处 D 准确率 = 50%";

    private static final int[] ROUNDS = {1, 2, 3, 4};
    private static final double D_ACC = 0.52;

    private final List<Step> steps = new ArrayList<>();
    private final List<List<Double>> real = new ArrayList<>();
    private final List<List<Double>> gen = new ArrayList<>();
    private int round;
    private double dAcc;

    @Override public String id() { return "dl.gan"; }
    @Override public String title() { return "GAN 生成对抗"; }
    @Override public String codeResource() { return "/code/GanDemo.java"; }
    @Override public String docResource() { return "/markdown/gan.md"; }

    private void emit(int line, List<VizEvent> events, String why) {
        StackFrame frame = new StackFrame("gan",
                OrderedMaps.of("轮", Value.prim(round),
                        "D 准确率", Value.prim(String.format("%.2f", dAcc)),
                        "G 样本均半径", Value.prim(String.format("%.2f", genRadius()))), null);
        Map<String, Layer> layers = new LinkedHashMap<>();
        layers.put("mem.stack", new StackLayer(List.of(frame)));
        layers.put("mem.heap", new HeapLayer(snapshotHeap()));
        layers.put("math.plot", snapshotPlot());
        steps.add(new Step(steps.size(), Loc.line(line, why),
                Collections.unmodifiableMap(layers), List.copyOf(events), new Intent(why, INVARIANT)));
    }

    private double genRadius() {
        double s = 0;
        for (List<Double> p : gen) {
            s += Math.hypot(p.get(0), p.get(1));
        }
        return gen.isEmpty() ? 0 : s / gen.size();
    }

    @Override
    public Trace produce() {
        steps.clear();
        real.clear();
        gen.clear();
        round = 0;
        dAcc = 0;
        // 真实样本：半径 1 的圆环上 8 个点
        for (int i = 0; i < 8; i++) {
            double a = 2 * Math.PI * i / 8;
            real.add(List.of(Math.cos(a), Math.sin(a)));
        }
        // 生成样本起点：半径 3 的外圈
        for (int i = 0; i < 8; i++) {
            double a = 2 * Math.PI * i / 8;
            gen.add(List.of(3 * Math.cos(a), 3 * Math.sin(a)));
        }
        List<List<Double>> boundary = new ArrayList<>();
        for (int i = 0; i <= 64; i++) {
            double a = 2 * Math.PI * i / 64;
            boundary.add(List.of(Math.cos(a), Math.sin(a)));
        }
        emit(5, List.of(VizEvent.of(VizEvent.EventType.INIT,
                "真实数据 = 蓝环（半径 1）；生成器 = 橙点（从半径 3 的外圈出发）")),
                "GAN 两名选手：G 伪造数据骗 D，D 尽力分辨真假——对抗中共同进化");
        for (int rd : ROUNDS) {
            round = rd;
            dAcc = 0.88 - 0.09 * rd;
            // 生成样本向真实环移动：(3-2·rd/4)·方向
            List<List<Double>> moved = new ArrayList<>();
            for (List<Double> p : gen) {
                double r0 = Math.hypot(p.get(0), p.get(1));
                double a = Math.atan2(p.get(1), p.get(0));
                double rn = Math.max(1.0, r0 - (r0 - 1.0) * 0.45);   // 每轮向内收 45%
                moved.add(List.of(rn * Math.cos(a), rn * Math.sin(a)));
            }
            gen.clear();
            gen.addAll(moved);
            emit(9, List.of(VizEvent.of(VizEvent.EventType.SET,
                    String.format("轮 %d：G 样本均半径 %.2f → 1.0（目标）；D 准确率 %.2f",
                            rd, genRadius(), dAcc))),
                    rd < ROUNDS.length ? "D 还能分辨（橙点仍偏外）→ G 按判别器的梯度继续向内挪"
                            : "D 已接近瞎猜（0.5）→ G 的样本混进了真实分布");
        }
        emit(12, List.of(VizEvent.of(VizEvent.EventType.MARK_SORTED,
                String.format("纳什均衡：D 准确率 %.2f ≈ 0.5，生成分布 ≈ 真实分布", D_ACC))),
                "G 学到了'真实数据的分布'——给它一个随机向量，它就能造出以假乱真的样本");
        emit(14, List.of(VizEvent.of(VizEvent.EventType.RETURN,
                "图像版：人脸生成、风格迁移、超分辨率——同一个博弈，换一个像素空间")),
                "生成对抗的思想 everywhere：造假者与鉴定者的军备竞赛，推动彼此进化");
        return new Trace(SCHEMA, meta(), steps);
    }

    private Heap snapshotHeap() {
        Map<String, Value> fields = new LinkedHashMap<>();
        fields.put("轮", Value.prim(round));
        fields.put("D 准确率", Value.prim(String.format("%.2f", dAcc)));
        fields.put("G 均半径", Value.prim(String.format("%.2f", genRadius())));
        Map<String, HeapObject> objs = new LinkedHashMap<>();
        objs.put("@1", new HeapObject("@1", "gan", "GanState",
                Collections.unmodifiableMap(fields), List.of()));
        return new Heap(Collections.unmodifiableMap(objs));
    }

    private PlotView snapshotPlot() {
        List<List<Double>> boundary = new ArrayList<>();
        for (int i = 0; i <= 64; i++) {
            double a = 2 * Math.PI * i / 64;
            boundary.add(List.of(Math.cos(a), Math.sin(a)));
        }
        List<List<Double>> realPts = new ArrayList<>();
        for (List<Double> p : real) {
            realPts.add(List.of(p.get(0), p.get(1), 0.02));
        }
        List<List<Double>> genPts = new ArrayList<>();
        for (List<Double> p : gen) {
            genPts.add(List.of(p.get(0), p.get(1), 0.02));
        }
        List<String> labels = new ArrayList<>();
        labels.add("蓝 = 真实样本（环） 橙 = 生成样本");
        labels.add(String.format("轮 %d：G 均半径 %.2f", round, genRadius()));
        labels.add(String.format("D 准确率 %.2f（0.5 = 瞎猜）", dAcc));
        return new PlotView(PlotView.SCATTER, "GAN：生成分布逼近真实分布", 0, 0, 0,
                -3.4, 3.4, -3.4, 3.4,
                Collections.unmodifiableList(boundary),
                List.of(),
                Collections.unmodifiableList(genPts),
                Collections.unmodifiableList(realPts),
                Collections.unmodifiableList(labels));
    }

    private Trace.TraceMeta meta() {
        return new Trace.TraceMeta(id(), title(), codeResource(), "4 轮", ROUNDS.length + 2);
    }

    @Override
    public java.util.List<String> dataLines() {
        return java.util.List.of("int rounds = 4;   // 真实分布 = 半径 1 的圆环");
    }

    @Override
    public Complexity complexity() {
        return new Complexity("每轮：D 一次前向 + G 一次前向反向", "O(样本数)",
                "训练不稳是出名难题：G/D 任何一方过强都会崩——学习率平衡与'半个 D 步'等技巧是调参玄学");
    }
}
