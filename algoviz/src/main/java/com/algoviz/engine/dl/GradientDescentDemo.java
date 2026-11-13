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
 * 深度学习·梯度下降与动量：双谷损失曲面上的两条下降轨迹赛跑。
 * 损失 L(x) = 0.1x⁴ − x² + 0.5（双谷：局部谷 x≈−2.24，全局谷 x≈+2.24，中央小丘）。
 * 普通 GD：x ← x − lr·L'(x)——lr=0.08，谷底来回小幅震荡。
 * 动量 GD：v = β·v + L'(x)；x ← x − lr·v——β=0.9 的惯性冲过中央小丘，直抵全局谷。
 * 画面（CARTESIAN 复用）：蓝线 = 损失曲线，绿虚线 = 动量轨迹历史，蓝点 = 普通 GD 动点。
 */
public final class GradientDescentDemo implements Algorithm {

    private static final String SCHEMA = "1";
    private static final String INVARIANT =
            "GD：x ← x − lr·L'(x)——沿负梯度每步挪一点；动量：v = β·v + L'(x)——历史速度的指数加权和，"
                    + "惯性能冲过小丘逃离局部谷；lr 大则震荡、β 大则冲得远——两个旋钮的取舍";

    private static final double LR = 0.08;
    private static final double BETA = 0.9;
    private static final double X0 = 0.6;          // 起点：中央小丘右侧
    private static final int STEPS = 12;

    private final List<Step> steps = new ArrayList<>();
    private final double[] plainX = new double[STEPS + 1];
    private final double[] momX = new double[STEPS + 1];
    private int step;

    @Override public String id() { return "dl.gradient"; }
    @Override public String title() { return "梯度下降与动量"; }
    @Override public String codeResource() { return "/code/GradientDescentDemo.java"; }
    @Override public String docResource() { return "/markdown/gradient.md"; }

    @Override
    public Trace produce() {
        return build();
    }

    /** 真实实现（produce 引用）。 */
    private Trace build() {
        steps.clear();
        plainX[0] = X0;
        momX[0] = X0;
        step = 0;
        emit(8, List.of(VizEvent.of(VizEvent.EventType.INIT,
                "损失 L(x)=0.1x⁴−x²+0.5 双谷曲面：起点在中央小丘右侧——普通 GD vs 动量 GD 赛跑开始")),
                "梯度下降是深度学习的引擎：所有参数更新都是同一个公式 x ← x − lr·L'(x)");
        double v = 0;
        for (int i = 1; i <= STEPS; i++) {
            step = i;
            double g = grad(plainX[i - 1]);
            plainX[i] = plainX[i - 1] - LR * g;
            v = BETA * v + grad(momX[i - 1]);
            momX[i] = momX[i - 1] - LR * v;
            if (i % 2 == 0 || i == STEPS) {
                emit(13, List.of(VizEvent.of(VizEvent.EventType.SET,
                        String.format("step %d：普通 x=%.2f（L=%.2f） vs 动量 x=%.2f（L=%.2f）",
                                i, plainX[i], loss(plainX[i]), momX[i], loss(momX[i])),
                        "S" + i)),
                        i < STEPS / 2 ? "动量的惯性正在积累速度：普通 GD 每步从零起跑，动量越滚越快"
                                : "动量冲过中央小丘直抵全局谷——普通 GD 还在局部谷里打转");
            }
        }
        emit(16, List.of(VizEvent.of(VizEvent.EventType.MARK_SORTED,
                String.format("终局：普通 GD 停在 x=%.2f（局部谷），动量 GD 停在 x=%.2f（全局谷）",
                        plainX[STEPS], momX[STEPS]))),
                "动量 = 历史梯度的指数加权平均：方向一致时加速、方向反转时抵消——穿越曲面的'惯性'");

        emit(18, List.of(VizEvent.of(VizEvent.EventType.RETURN,
                "Adam = 动量 + 自适应学习率——深度学习框架默认优化器的两个祖先都在本演示里")),
                "局部谷不是终点：lr/β/随机重启/模拟退火——逃离局部最优的武器库");
        return new Trace(SCHEMA, meta(), steps);
    }

    /** 损失 L(x) = 0.1x⁴ − x² + 0.5。 */
    private static double loss(double x) {
        return 0.1 * x * x * x * x - x * x + 0.5;
    }

    /** 导数 L'(x) = 0.4x³ − 2x。 */
    private static double grad(double x) {
        return 0.4 * x * x * x - 2 * x;
    }

    private void emit(int line, List<VizEvent> events, String why) {
        StackFrame frame = new StackFrame("gd",
                OrderedMaps.of("step", Value.prim(String.valueOf(step)),
                        "普通 x", Value.prim(String.format("%.2f", plainX[step])),
                        "动量 x", Value.prim(String.format("%.2f", momX[step]))), null);
        Map<String, Layer> layers = new LinkedHashMap<>();
        layers.put("mem.stack", new StackLayer(List.of(frame)));
        layers.put("mem.heap", new HeapLayer(snapshotHeap()));
        layers.put("math.plot", snapshotPlot());
        steps.add(new Step(steps.size(), Loc.line(line, why),
                Collections.unmodifiableMap(layers), List.copyOf(events), new Intent(why, INVARIANT)));
    }

    private Heap snapshotHeap() {
        Map<String, Value> fields = new LinkedHashMap<>();
        fields.put("普通 GD 终点", Value.prim(String.format("x=%.2f", plainX[STEPS])));
        fields.put("动量 GD 终点", Value.prim(String.format("x=%.2f", momX[STEPS])));
        fields.put("lr / β", Value.prim("0.08 / 0.9"));
        Map<String, HeapObject> objs = new LinkedHashMap<>();
        objs.put("@1", new HeapObject("@1", "gd", "DescentState",
                Collections.unmodifiableMap(fields), List.of()));
        return new Heap(Collections.unmodifiableMap(objs));
    }

    private PlotView snapshotPlot() {
        List<List<Double>> curve = new ArrayList<>();
        for (int i = 0; i <= 120; i++) {
            double x = -3.0 + 6.0 * i / 120;
            curve.add(List.of(x, loss(x)));
        }
        // 普通 GD 轨迹历史（trace3：x 值画在曲线上对应 y）
        List<List<Double>> plainTrail = new ArrayList<>();
        for (int i = 0; i <= step; i++) {
            plainTrail.add(List.of(plainX[i], loss(plainX[i])));
        }
        // 动量轨迹（cosineTrace 绿虚线）
        List<List<Double>> momTrail = new ArrayList<>();
        for (int i = 0; i <= step; i++) {
            momTrail.add(List.of(momX[i], loss(momX[i])));
        }
        List<String> labels = new ArrayList<>();
        labels.add(String.format("双谷损失曲面：L(x)=0.1x⁴−x²+0.5（step %d）", step));
        labels.add(String.format("普通 GD x=%.2f vs 动量 x=%.2f——动量冲过中央小丘", plainX[step], momX[step]));
        labels.add("蓝点=普通 GD 轨迹，绿虚线=动量轨迹");
        return new PlotView(PlotView.CARTESIAN, "梯度下降赛跑", 0, plainX[step], loss(plainX[step]),
                -3.0, 3.0, -1.2, 5.5,
                Collections.unmodifiableList(curve),                    // 曲线
                Collections.unmodifiableList(momTrail),                 // 动量轨迹（绿虚线）
                Collections.unmodifiableList(plainTrail),               // 普通 GD 轨迹历史
                List.of(),
                Collections.unmodifiableList(labels));
    }

    private Trace.TraceMeta meta() {
        return new Trace.TraceMeta(id(), title(), codeResource(),
                "双谷 lr=0.08 β=0.9", STEPS);
    }

    @Override
    public java.util.List<String> dataLines() {
        return java.util.List.of("double lr = 0.08, beta = 0.9;   //@DATA 学习率与动量系数");
    }

    @Override
    public Complexity complexity() {
        return new Complexity("O(步数) 一次梯度求值", "O(1) 参数量",
                "优化器进化史：GD → 动量 → Adam——'惯性 + 自适应步长'两条改进主线本演示各占一条");
    }
}
