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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 深度学习·ResNet 残差连接：网络版的"c 传送带"（对照 dl.lstm）。
 * 普通深网：梯度逐层连乘局部导数（每层 ~0.3）→ 10 层后衰减到 10⁻⁵（退化现象）。
 * 残差网：每层 y = F(x) + x——反向 ∂L/∂x = ∂L/∂y·(F'(x)+1)，"+1" 让梯度每层都有一条直通 highway。
 * 数值确定性：每层局部导数取 0.3（sigmoid 量级）。
 * 画面（math.matrix 复用）：双列网格逐层推进——普通梯度列一路衰到底，残差梯度列稳如泰山。
 */
public final class ResNetDemo implements Algorithm {

    private static final String SCHEMA = "1";
    private static final String INVARIANT =
            "残差块 y = F(x) + x 的反向：∂L/∂x = ∂L/∂y·(F'(x)+1)——'+1' 是恒等映射的直通项，"
                    + "梯度不再被 F'(x) 连乘淹没；10 层后普通网梯度 10⁻⁵ vs 残差网 ~1——这就是'退化'的解药";

    private static final int LAYERS = 10;
    private static final double LOCAL = 0.3;   // 每层局部导数（sigmoid 量级）

    private final List<Step> steps = new ArrayList<>();
    private final double[] plain = new double[LAYERS];
    private final double[] res = new double[LAYERS];
    private int curLayer = -1;

    @Override public String id() { return "dl.resnet"; }
    @Override public String title() { return "残差连接"; }
    @Override public String codeResource() { return "/code/ResNetDemo.java"; }
    @Override public String docResource() { return "/markdown/resnet.md"; }

    @Override
    public Trace produce() {
        steps.clear();
        curLayer = -1;
        double p = 1, r = 1;
        emit(7, List.of(VizEvent.of(VizEvent.EventType.INIT,
                String.format("%d 层网络，每层局部导数 %.1f（sigmoid 量级）：普通网连乘 vs 残差网 (F'+1)——梯度衰减赛跑",
                        LAYERS, LOCAL))),
                "退化现象：56 层比 20 层训练误差还高——不是过拟合，是'深层根本训不动'（He 2015）");
        for (int layer = 1; layer <= LAYERS; layer++) {
            curLayer = layer;
            p *= LOCAL;                       // 普通网：连乘
            r *= LOCAL + 1;                   // 残差网：每层乘 (F'+1)——'+1' 直通
            plain[layer - 1] = p;
            res[layer - 1] = r;
            if (layer % 2 == 0 || layer == LAYERS) {
                emit(12, List.of(VizEvent.of(VizEvent.EventType.SET,
                        String.format("第 %d 层：普通网梯度 %.2e vs 残差网 %.2f——差距 %.0f 倍",
                                layer, p, r, r / Math.max(p, 1e-12)))),
                        layer < LAYERS / 2 ? "普通网每层乘 0.3：5 层就缩 200 倍——深层的梯度先头部队已经阵亡"
                                : "残差网的 '+1' 让梯度每层都有一条不衰减的直通线——深层终于训得动");
            }
        }
        curLayer = -1;
        emit(15, List.of(VizEvent.of(VizEvent.EventType.MARK_SORTED,
                String.format("终局：普通网 %.2e（梯度消失）vs 残差网 %.0f（几乎无损）——1 层 skip = 1 条梯度高速路", plain[9], res[9]))),
                "残差连接与 LSTM 的 c 传送带同源：都是给梯度开一条'不乘'的高速路");
        emit(17, List.of(VizEvent.of(VizEvent.EventType.RETURN,
                "ResNet(2015) 让 152 层成为现实，ImageNet 错误率 3.57%——'更深'从此成为免费午餐")),
                "恒等映射的哲学：最坏情况下 F(x)=0，网络退化为恒等——'至少不比浅层差'的下限保证");
        return new Trace(SCHEMA, meta(), steps);
    }

    private void emit(int line, List<VizEvent> events, String why) {
        StackFrame frame = new StackFrame("resnet",
                OrderedMaps.of("层", Value.prim(curLayer >= 0 ? String.valueOf(curLayer) : "-"),
                        "普通梯度", Value.prim(curLayer >= 0 ? String.format("%.2e", plain[curLayer - 1]) : "1"),
                        "残差梯度", Value.prim(curLayer >= 0 ? String.format("%.2f", res[curLayer - 1]) : "1")), null);
        Map<String, Layer> layers = new LinkedHashMap<>();
        layers.put("mem.stack", new StackLayer(List.of(frame)));
        layers.put("mem.heap", new HeapLayer(snapshotHeap()));
        layers.put("math.matrix", snapshotMath());
        steps.add(new Step(steps.size(), Loc.line(line, why),
                Collections.unmodifiableMap(layers), List.copyOf(events), new Intent(why, INVARIANT)));
    }

    private Heap snapshotHeap() {
        Map<String, Value> fields = new LinkedHashMap<>();
        fields.put("层数", Value.prim(String.valueOf(LAYERS)));
        fields.put("普通网终梯度", Value.prim(String.format("%.2e", plain[9])));
        fields.put("残差网终梯度", Value.prim(String.format("%.1f", res[9])));
        Map<String, HeapObject> objs = new LinkedHashMap<>();
        objs.put("@1", new HeapObject("@1", "net", "ResidualNet",
                Collections.unmodifiableMap(fields), List.of()));
        return new Heap(Collections.unmodifiableMap(objs));
    }

    private MathView snapshotMath() {
        List<String> vals = new ArrayList<>();
        Set<Integer> active = new LinkedHashSet<>();
        Set<Integer> done = new LinkedHashSet<>();
        for (int i = 0; i < LAYERS; i++) {
            String pText;
            String rText;
            if (i < curLayer) {
                pText = String.format("%.1e", plain[i]);
                rText = String.format("%.1f", res[i]);
                done.add(i * 3 + 1);
                done.add(i * 3 + 2);
            } else if (i == curLayer) {
                pText = String.format("%.1e", plain[i]);
                rText = String.format("%.1f", res[i]);
                for (int k = 0; k < 3; k++) {
                    active.add(i * 3 + k);
                }
            } else {
                pText = "?";
                rText = "?";
            }
            vals.add("层" + (i + 1));
            vals.add(pText);
            vals.add(rText);
        }
        MathView.Grid grid = new MathView.Grid("grad", "梯度赛跑（普通连乘列 vs 残差直通列）", LAYERS, 3,
                Collections.unmodifiableList(vals), null, null,
                Collections.unmodifiableSet(active), Collections.unmodifiableSet(done));
        return new MathView("ResNet", List.of(grid));
    }

    private Trace.TraceMeta meta() {
        return new Trace.TraceMeta(id(), title(), codeResource(), "10 层 局部导数0.3", LAYERS);
    }

    @Override
    public java.util.List<String> dataLines() {
        return java.util.List.of("int layers = 10;   // 每层局部导数 0.3");
    }

    @Override
    public Complexity complexity() {
        return new Complexity("O(层数) 前向+反向", "O(层数) 残差引用（无额外参数）",
                "残差思想外溢：Transformer 的每个块都是残差块——'加一条直通线'成为深层网络的标准姿势");
    }
}
