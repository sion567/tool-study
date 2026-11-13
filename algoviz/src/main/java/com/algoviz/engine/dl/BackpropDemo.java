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
 * 深度学习·反向传播逐层演算：2 神经元隐层 + 1 输出，一个样本的前向 + 链式法则反向。
 * 数值确定性：x=1.0，w1=[0.5,-0.3]、b1=[0.1,0.2]、w2=[0.8,-0.6]、t=1.0，lr=0.5。
 * 前向：h=σ(w1·x+b1)、y=w2·h；L=(y−t)²。
 * 反向：∂L/∂y=2(y−t) → ∂L/∂w2=∂L/∂y·h → ∂L/∂h=∂L/∂y·w2 → ∂L/∂w1=∂L/∂h·h(1−h)·x——数字逐格算出。
 * 画面：网格一 = 前向值，网格二 = 梯度表逐格填充；更新后二次前向看损失下降。
 */
public final class BackpropDemo implements Algorithm {

    private static final String SCHEMA = "1";
    private static final String INVARIANT =
            "链式法则：∂L/∂w2=∂L/∂y·h、∂L/∂w1=∂L/∂y·w2·h(1−h)·x——每层的梯度由后一层乘局部导数；"
                    + "更新 w ← w − lr·∂L/∂w 后损失必然下降（学习率合适时）";

    private static final double X = 1.0, T = 1.0, LR = 0.5;
    private static final double[] W1 = {0.5, -0.3};
    private static final double[] B1 = {0.1, 0.2};
    private static final double[] W2 = {0.8, -0.6};

    private final List<Step> steps = new ArrayList<>();
    private double loss1, loss2;

    @Override public String id() { return "dl.backprop"; }
    @Override public String title() { return "反向传播逐层演算"; }
    @Override public String codeResource() { return "/code/BackpropDemo.java"; }
    @Override public String docResource() { return "/markdown/backprop.md"; }

    @Override
    public Trace produce() {
        steps.clear();
        // ---- 前向 ----
        double z1a = W1[0] * X + B1[0], z1b = W1[1] * X + B1[1];
        double h1a = sigmoid(z1a), h1b = sigmoid(z1b);
        double y = W2[0] * h1a + W2[1] * h1b;
        loss1 = (y - T) * (y - T);
        emit(8, List.of(VizEvent.of(VizEvent.EventType.INIT,
                String.format("前向：h=σ(w1·x+b1)=[%.3f, %.3f]，y=w2·h=%.3f，目标 t=%.1f → L=(y−t)²=%.4f",
                        h1a, h1b, y, T, loss1))),
                "前向 = 把输入沿权重'推'到输出：每一步只是乘加 + sigmoid");
        // ---- 反向：链式法则逐格算 ----
        double dLdy = 2 * (y - T);
        emit(13, List.of(VizEvent.of(VizEvent.EventType.MESSAGE,
                String.format("反向起步：∂L/∂y = 2(y-t) = %.3f——损失对输出的斜率", dLdy))),
                "链式法则的第一环：平方损失的导数 2(y-t)：错得越多推得越狠");
        double dLdw2a = dLdy * h1a, dLdw2b = dLdy * h1b;
        double dLdh1a = dLdy * W2[0], dLdh1b = dLdy * W2[1];
        emit(16, List.of(VizEvent.of(VizEvent.EventType.SET,
                String.format("∂L/∂w2 = ∂L/∂y·h = [%.3f, %.3f]；∂L/∂h = ∂L/∂y·w2 = [%.3f, %.3f]",
                        dLdw2a, dLdw2b, dLdh1a, dLdh1b))),
                "链式法则第二环：乘上'本步的局部导数'——w2 的梯度乘 h、传给隐层乘 w2");
        double dLdw1a = dLdh1a * h1a * (1 - h1a) * X;
        double dLdw1b = dLdh1b * h1b * (1 - h1b) * X;
        emit(19, List.of(VizEvent.of(VizEvent.EventType.SET,
                String.format("∂L/∂w1 = ∂L/∂h·h(1−h)·x = [%.4f, %.4f]（sigmoid 局部导数 h(1−h)）", dLdw1a, dLdw1b))),
                "sigmoid 的局部导数 h(1−h) ≤ 0.25——乘得越多越小，这就是深层的'梯度消失'");
        // ---- 更新 ----
        double[] nW2 = {W2[0] - LR * dLdw2a, W2[1] - LR * dLdw2b};
        double[] nW1 = {W1[0] - LR * dLdw1a, W1[1] - LR * dLdw1b};
        emit(22, List.of(VizEvent.of(VizEvent.EventType.MARK_SORTED,
                String.format("更新：w ← w − lr·∂L/∂w（lr=%.1f）→ w2=[%.3f, %.3f]，w1=[%.3f, %.3f]", LR, nW2[0], nW2[1], nW1[0], nW1[1]))),
                "梯度下降：沿负梯度挪一小步——lr 是步长，太大震荡太小爬得慢");
        // ---- 二次前向：损失下降 ----
        double h2a = sigmoid(nW1[0] * X + B1[0]), h2b = sigmoid(nW1[1] * X + B1[1]);
        double y2 = nW2[0] * h2a + nW2[1] * h2b;
        loss2 = (y2 - T) * (y2 - T);
        emit(25, List.of(VizEvent.of(VizEvent.EventType.SET,
                String.format("二次前向：y=%.3f → L=%.4f（原 %.4f，降 %.1f%%）", y2, loss2, loss1, 100 * (1 - loss2 / loss1)))),
                "一步更新就有可见的损失下降——千万个这样的小步就是'训练'");
        emit(27, List.of(VizEvent.of(VizEvent.EventType.RETURN,
                "反向传播 = 链式法则 + 动态规划：每层的梯度只依赖后一层——一次反向遍历算完全部梯度")),
                "1986 年 Rumelhart 让反向传播成名：深度学习的'引擎'直到今天仍是它");
        return new Trace(SCHEMA, meta(), steps);
    }

    private static double sigmoid(double x) {
        return 1 / (1 + Math.exp(-x));
    }

    private void emit(int line, List<VizEvent> events, String why) {
        StackFrame frame = new StackFrame("backprop",
                OrderedMaps.of("L", Value.prim(String.format("%.4f", loss2 > 0 ? Math.min(loss1, loss2) : loss1)),
                        "lr", Value.prim("0.5")), null);
        Map<String, Layer> layers = new LinkedHashMap<>();
        layers.put("mem.stack", new StackLayer(List.of(frame)));
        layers.put("mem.heap", new HeapLayer(snapshotHeap()));
        layers.put("math.matrix", snapshotMath());
        steps.add(new Step(steps.size(), Loc.line(line, why),
                Collections.unmodifiableMap(layers), List.copyOf(events), new Intent(why, INVARIANT)));
    }

    private Heap snapshotHeap() {
        Map<String, Value> fields = new LinkedHashMap<>();
        fields.put("w1", Value.prim("[0.5, -0.3]"));
        fields.put("w2", Value.prim("[0.8, -0.6]"));
        fields.put("学习率", Value.prim("0.5"));
        Map<String, HeapObject> objs = new LinkedHashMap<>();
        objs.put("@1", new HeapObject("@1", "net", "TinyNet",
                Collections.unmodifiableMap(fields), List.of()));
        return new Heap(Collections.unmodifiableMap(objs));
    }

    private MathView snapshotMath() {
        double z1a = W1[0] * X + B1[0], z1b = W1[1] * X + B1[1];
        double h1a = sigmoid(z1a), h1b = sigmoid(z1b);
        double y = W2[0] * h1a + W2[1] * h1b;
        double dLdy = 2 * (y - T);
        List<String> fwd = new ArrayList<>(List.of(
                "x=1.0", String.format("h_a=%.3f", h1a), String.format("h_b=%.3f", h1b),
                String.format("y=%.3f", y), String.format("L=%.4f", loss1)));
        Set<Integer> fwdActive = new LinkedHashSet<>();
        fwdActive.add(3);
        fwdActive.add(4);
        List<String> grads = new ArrayList<>(List.of(
                String.format("dLdy=%.3f", dLdy),
                String.format("dLdw2=[%.3f,%.3f]", dLdy * h1a, dLdy * h1b),
                String.format("dLdw1=[%.4f,%.4f]", dLdy * W2[0] * h1a * (1 - h1a) * X,
                        dLdy * W2[1] * h1b * (1 - h1b) * X)));
        Set<Integer> gradActive = new LinkedHashSet<>();
        gradActive.add(0);
        gradActive.add(1);
        gradActive.add(2);   // 显式插入序：Set.of 迭代序跨 JVM 随机（与 Sm3Demo 同款坑）
        MathView.Grid fwdGrid = new MathView.Grid("fwd", "前向值（黄圈 = 损失与输出）",
                1, 5, fwd, null, null, fwdActive, Set.of());
        MathView.Grid gradGrid = new MathView.Grid("grad", "梯度表（链式法则逐格算出）",
                1, 3, grads, null, null, gradActive, Set.of());
        return new MathView("反向传播", List.of(fwdGrid, gradGrid));
    }

    private Trace.TraceMeta meta() {
        return new Trace.TraceMeta(id(), title(), codeResource(), "x=1 t=1 lr=0.5", 27);
    }

    @Override
    public java.util.List<String> dataLines() {
        return java.util.List.of("double x = 1.0, t = 1.0, lr = 0.5;   //@DATA 样本与学习率");
    }

    @Override
    public Complexity complexity() {
        return new Complexity("O(权重数) 一次反向遍历", "O(层数) 中间梯度",
                "反向传播 = 链式法则在计算图上的动态规划：一次前向 + 一次反向 = 全部梯度——"
                        + "深度学习框架的 autograd 就是它的通用化");
    }
}
