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
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * 深度学习·Dropout：训练时随机"杀死"一半神经元，推理时全员上岗。
 * inverted dropout：被保留的值除以 keep-prob（训练时预缩放）→ 推理时直接全开无需缩放。
 * 训练/推理行为差异是本演示的核心：同一层，三种状态（两次不同 mask 的训练帧 + 推理帧）。
 * 数值确定性：Random(7) 固定种子——mask 序列可复现。
 * 画面（math.matrix 复用）：神经元一行（值 + 置零格灰显）。
 */
public final class DropoutDemo implements Algorithm {

    private static final String SCHEMA = "1";
    private static final String INVARIANT =
            "训练：每个神经元以 p=0.5 概率置零，保留值 ÷keep（期望不变）——'不能依赖任何特定邻居'；"
                    + "推理：全员上岗、无缩放（inverted 已在训练补）——随机性是正则化，不是噪声";

    private static final double KEEP = 0.5;
    private static final double[] VALUES = {1.0, 2.0, 3.0, 4.0, 5.0, 6.0};   // 6 神经元

    private final List<Step> steps = new ArrayList<>();
    private final boolean[][] masks = new boolean[3][VALUES.length];   // 3 帧训练 mask
    private int curFrame = -1;   // 0..2 = 训练帧，3 = 推理帧

    @Override public String id() { return "dl.dropout"; }
    @Override public String title() { return "Dropout 正则化"; }
    @Override public String codeResource() { return "/code/DropoutDemo.java"; }
    @Override public String docResource() { return "/markdown/dropout.md"; }

    @Override
    public Trace produce() {
        steps.clear();
        Arrays.fill(masks[0], true);
        Random r = new Random(7);
        for (boolean[] m : masks) {
            for (int i = 0; i < m.length; i++) {
                m[i] = r.nextDouble() < KEEP;
            }
        }
        curFrame = -1;
        emit(6, List.of(VizEvent.of(VizEvent.EventType.INIT,
                "6 神经元层，keep=0.5：训练时每帧随机杀一半（灰=置零），保留值 ÷0.5（期望不变）")),
                "Dropout(2012) 从防过拟合起家：让神经元不能抱团——'协同适应'被随机性拆散");
        for (int f = 0; f < 3; f++) {
            curFrame = f;
            int alive = 0;
            for (boolean b : masks[f]) {
                if (b) {
                    alive++;
                }
            }
            emit(10, List.of(VizEvent.of(VizEvent.EventType.SET,
                    String.format("训练帧 %d：mask=%s → 活 %d 个，保留值 ×%.0f 倍（inverted 缩放）",
                            f + 1, maskStr(masks[f]), alive, 1 / KEEP))),
                    "每帧不同组合：网络被迫学'冗余表征'——任何一个特征都不能只依赖单一路径");
        }
        curFrame = 3;
        emit(14, List.of(VizEvent.of(VizEvent.EventType.MARK_SORTED,
                "推理帧：全员上岗、无缩放——训练时的 ÷keep 让期望恒等，推理无需任何调整")),
                "inverted dropout 的聪明处：缩放提前做进训练，推理路径零开销");
        emit(16, List.of(VizEvent.of(VizEvent.EventType.RETURN,
                "Dropout 与 BatchNorm 是正则化双子星：一个随机杀神经元、一个把分布拉回正态")),
                "过拟合 = 背诵训练集；Dropout = 每帧换半个考官——逼你学会规律而不是背题");
        return new Trace(SCHEMA, meta(), steps);
    }

    private String maskStr(boolean[] m) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < m.length; i++) {
            sb.append(m[i] ? 1 : 0);
            if (i < m.length - 1) {
                sb.append(',');
            }
        }
        return sb.append(']').toString();
    }

    private void emit(int line, List<VizEvent> events, String why) {
        StackFrame frame = new StackFrame("dropout",
                OrderedMaps.of("keep", Value.prim("0.5"),
                        "当前帧", Value.prim(curFrame == 3 ? "推理" : "训练 " + (curFrame + 1))), null);
        Map<String, Layer> layers = new LinkedHashMap<>();
        layers.put("mem.stack", new StackLayer(List.of(frame)));
        layers.put("mem.heap", new HeapLayer(snapshotHeap()));
        layers.put("math.matrix", snapshotMath());
        steps.add(new Step(steps.size(), Loc.line(line, why),
                Collections.unmodifiableMap(layers), List.copyOf(events), new Intent(why, INVARIANT)));
    }

    private Heap snapshotHeap() {
        Map<String, Value> fields = new LinkedHashMap<>();
        fields.put("keep", Value.prim("0.5"));
        fields.put("策略", Value.prim(curFrame == 3 ? "推理：全开" : "训练：随机置零 + 预缩放"));
        Map<String, HeapObject> objs = new LinkedHashMap<>();
        objs.put("@1", new HeapObject("@1", "drop", "DropoutLayer",
                Collections.unmodifiableMap(fields), List.of()));
        return new Heap(Collections.unmodifiableMap(objs));
    }

    private MathView snapshotMath() {
        List<String> vals = new ArrayList<>();
        Set<Integer> active = new LinkedHashSet<>();
        for (int i = 0; i < VALUES.length; i++) {
            if (curFrame == 3) {
                vals.add(String.format("%.1f", VALUES[i]));
                active.add(i);
            } else if (curFrame >= 0) {
                boolean alive = masks[curFrame][i];
                vals.add(alive ? String.format("%.1f", VALUES[i] / KEEP) : "0");
                if (alive) {
                    active.add(i);
                }
            } else {
                vals.add(String.format("%.1f", VALUES[i]));   // INIT 帧：原始值未 mask
            }
        }
        MathView.Grid grid = new MathView.Grid("neurons", "神经元（灰 0 = 被置零；值已 ÷keep 预缩放）",
                1, VALUES.length, vals, null, null, active, Set.of());
        return new MathView("Dropout", List.of(grid));
    }

    private Trace.TraceMeta meta() {
        return new Trace.TraceMeta(id(), title(), codeResource(), "keep=0.5 6 神经元", VALUES.length);
    }

    @Override
    public java.util.List<String> dataLines() {
        return java.util.List.of("double keep = 0.5;   // 置零概率 = 1-keep");
    }

    @Override
    public Complexity complexity() {
        return new Complexity("O(神经元) 一次 mask", "O(1) 无参数",
                "Dropout 零参数却改变整个网络的训练动力学——Transformer 里 attention dropout 仍是标配");
    }
}
