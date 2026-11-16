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
 * 深度学习·RNN 循环神经网络：同一个细胞逐时间步滚动，隐藏状态 h 不断被改写。
 * h_t = tanh(w_x·x_t + w_h·h_{t-1})——"当前输入 + 上一步记忆"。
 * 输入序列 [1,0,1,1,0]，h 从 0 开始滚动——最后一步的 h 就是整条序列的"记忆摘要"。
 */
public final class RnnDemo implements Algorithm {

    private static final String SCHEMA = "1";
    private static final String INVARIANT =
            "h_t = tanh(w_x·x_t + w_h·h_{t-1})：同一组权重复用每个时间步——序列信息被压缩进不断更新的 h";

    private static final int[] XS = {1, 0, 1, 1, 0};
    private static final double WX = 1.0, WH = 0.8;

    private final List<Step> steps = new ArrayList<>();
    private final double[] hs = new double[XS.length];
    private int curT = -1;

    @Override public String id() { return "dl.rnn"; }
    @Override public String title() { return "RNN 循环神经网络"; }
    @Override public String codeResource() { return "/code/RnnDemo.java"; }
    @Override public String docResource() { return "/markdown/rnn.md"; }

    @Override
    public Trace produce() {
        steps.clear();
        curT = -1;
        double h = 0;
        emit(5, List.of(VizEvent.of(VizEvent.EventType.INIT,
                "权重 w_x=1, w_h=0.8；h₀ = 0——同一个细胞将滚动 5 个时间步")),
                "RNN 与前馈网络的区别：'循环'——上一步的输出 h 是这一步的输入之一");
        for (int t = 0; t < XS.length; t++) {
            curT = t;
            h = Math.tanh(WX * XS[t] + WH * h);
            hs[t] = h;
            emit(8, List.of(VizEvent.of(VizEvent.EventType.SET,
                    String.format("t=%d：x=%d → h%d = tanh(%.2f + %.2f·%.2f) = %.2f",
                            t, XS[t], t + 1, WX * XS[t], WH, t > 0 ? hs[t - 1] : 0, h),
                    "T" + (t + 1))),
                    t < XS.length - 1 ? "h 既是输出也是下一步的'记忆'——序列被逐步压缩"
                            : String.format("h₅ = %.2f：整条序列被压缩成一个向量——它可以直接接分类器", h));
        }
        curT = -1;
        emit(11, List.of(VizEvent.of(VizEvent.EventType.MARK_SORTED,
                "训练：沿时间展开后反向传播（BPTT）——梯度连乘 w_h 导致消失/爆炸")),
                "h₅ 对 h₁ 的梯度 ∝ w_h⁴ = 0.41——序列越长衰减越狠，这就是 LSTM/GRU 诞生的原因");
        emit(13, List.of(VizEvent.of(VizEvent.EventType.RETURN,
                "RNN 的两个变体：LSTM 用门控保住长期记忆；GRU 是简化版——Transformer 后来用注意力另起炉灶")),
                "RNN 教会了世界'用隐藏状态处理序列'——这个思想被 Transformer 继承并发扬");
        return new Trace(SCHEMA, meta(), steps);
    }

    private void emit(int line, List<VizEvent> events, String why) {
        StackFrame frame = new StackFrame("rnn",
                OrderedMaps.of("t", Value.prim(curT >= 0 ? String.valueOf(curT) : "-"),
                        "h_t", Value.prim(curT >= 0 ? String.format("%.2f", hs[curT]) : "0")), null);
        Map<String, Layer> layers = new LinkedHashMap<>();
        layers.put("mem.stack", new StackLayer(List.of(frame)));
        layers.put("mem.heap", new HeapLayer(snapshotHeap()));
        layers.put("math.matrix", snapshotMatrix());
        steps.add(new Step(steps.size(), Loc.line(line, why),
                Collections.unmodifiableMap(layers), List.copyOf(events), new Intent(why, INVARIANT)));
    }

    private Heap snapshotHeap() {
        Map<String, Value> fields = new LinkedHashMap<>();
        fields.put("w_x / w_h", Value.prim("1.0 / 0.8"));
        fields.put("当前 h", Value.prim(curT >= 0 ? String.format("%.2f", hs[curT]) : "0"));
        Map<String, HeapObject> objs = new LinkedHashMap<>();
        objs.put("@1", new HeapObject("@1", "cell", "RnnCell",
                Collections.unmodifiableMap(fields), List.of()));
        return new Heap(Collections.unmodifiableMap(objs));
    }

    private MathView snapshotMatrix() {
        List<String> timeline = new ArrayList<>();
        Set<Integer> active = new LinkedHashSet<>();
        Set<Integer> done = new LinkedHashSet<>();
        for (int t = 0; t < XS.length; t++) {
            String hText;
            if (t < curT) {
                hText = String.format("%.2f", hs[t]);
                done.add(t * 3);
                done.add(t * 3 + 2);
            } else if (t == curT) {
                hText = curT >= 0 ? String.format("%.2f", hs[t]) : "?";
                active.add(t * 3);
                active.add(t * 3 + 1);
                active.add(t * 3 + 2);
            } else {
                hText = "?";
            }
            timeline.add(String.valueOf(XS[t]));
            timeline.add("h" + (t + 1));
            timeline.add(hText);
        }
        MathView.Grid grid = new MathView.Grid("timeline", "时间线（x → h 滚动）", XS.length, 3,
                Collections.unmodifiableList(timeline), curT >= 0 ? curT : null, null,
                Collections.unmodifiableSet(active), Collections.unmodifiableSet(done));
        MathView.Grid weights = new MathView.Grid("weights", "权重（所有时间步共享）", 1, 2,
                List.of("w_x=1.0", "w_h=0.8"), null, null, Set.of(), Set.of());
        return new MathView("RNN", List.of(grid, weights));
    }

    private Trace.TraceMeta meta() {
        return new Trace.TraceMeta(id(), title(), codeResource(), "5 步", XS.length);
    }

    @Override
    public java.util.List<String> dataLines() {
        return java.util.List.of(Algorithm.joinArray("xs", XS) + "   // w_x=1, w_h=0.8");
    }

    @Override
    public Complexity complexity() {
        return new Complexity("O(T) 每时间步一次细胞计算", "O(1) 细胞参数复用",
                "序列长度任意、参数量不变——'循环'的代价是并行困难（必须逐步滚动），这正是 Transformer 取而代之的突破口");
    }
}
