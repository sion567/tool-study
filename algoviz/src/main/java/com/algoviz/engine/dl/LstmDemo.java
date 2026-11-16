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
 * 深度学习·LSTM 门控：RNN 的"记忆强化版"——三门（遗忘/输入/输出）+ 细胞状态 c 传送带。
 * c_t = f⊙c_{t-1} + i⊙tanh(w·[x,h])；h_t = o⊙tanh(c_t)——遗忘门关闭时梯度沿 c 直通，
 * 这就是 RNN 梯度消失的解药（对照 dl.rnn 的 w_h⁴ 衰减）。
 * 数值（确定性小权重）：f=σ(0.5x+0.5h)、i=σ(0.4x+0.4h)、o=σ(0.6x+0.6h)、c̃=tanh(x+h)。
 * 画面（math.matrix 复用）：时间线网格（x/c/h/f/i/o 列）+ 权重网格——三门值 ∈ (0,1) 随步滚动。
 */
public final class LstmDemo implements Algorithm {

    private static final String SCHEMA = "1";
    private static final String INVARIANT =
            "c_t = f⊙c_{t-1} + i⊙c̃：遗忘门决定'留多少旧记忆'、输入门决定'写多少新内容'、输出门决定'露多少给 h'——"
                    + "梯度沿 c 传送带直通，RNN 的消失问题在此被门控解药治愈";

    private static final int[] XS = {1, 0, 1, 1, 0};
    private static final double WF = 0.5, WI = 0.4, WO = 0.6;

    private final List<Step> steps = new ArrayList<>();
    private final double[] cs = new double[XS.length];
    private final double[] hs = new double[XS.length];
    private final double[] fs = new double[XS.length];
    private final double[] ios = new double[XS.length];
    private final double[] os = new double[XS.length];
    private int curT = -1;

    @Override public String id() { return "dl.lstm"; }
    @Override public String title() { return "LSTM 门控"; }
    @Override public String codeResource() { return "/code/LstmDemo.java"; }
    @Override public String docResource() { return "/markdown/lstm.md"; }

    @Override
    public Trace produce() {
        steps.clear();
        curT = -1;
        double c = 0, h = 0;
        emit(7, List.of(VizEvent.of(VizEvent.EventType.INIT,
                "同一序列 [1,0,1,1,0]：三门权重 f=0.5 / i=0.4 / o=0.6，c₀=0——对照 dl.rnn 的普通 h 滚动")),
                "LSTM = RNN + 三道门 + 一条细胞状态传送带 c：门值 ∈ (0,1) 是 sigmoid 出来的'阀门开度'");
        for (int t = 0; t < XS.length; t++) {
            curT = t;
            double x = XS[t];
            double f = sigmoid(WF * x + WF * h);
            double i = sigmoid(WI * x + WI * h);
            double o = sigmoid(WO * x + WO * h);
            double cTilde = Math.tanh(x + h);
            c = f * c + i * cTilde;
            h = o * Math.tanh(c);
            fs[t] = f;
            ios[t] = i;
            os[t] = o;
            cs[t] = c;
            hs[t] = h;
            emit(12, List.of(VizEvent.of(VizEvent.EventType.SET,
                    String.format("t=%d：x=%d → f=%.2f i=%.2f o=%.2f | c=%.3f（f·%.2f+i·%.2f）| h=%.3f",
                            t, XS[t], f, i, o, c, t > 0 ? cs[t - 1] : 0, cTilde, h),
                    "T" + (t + 1))),
                    t < XS.length - 1 ? "三门各司其职：f 决定旧记忆留多少、i 决定新内容写多少、o 决定露出多少"
                            : String.format("c₅=%.3f h₅=%.3f：长期记忆活在 c 传送带上，h 只是被输出门过滤的'读出'", c, h));
        }
        curT = -1;
        emit(15, List.of(VizEvent.of(VizEvent.EventType.MARK_SORTED,
                "梯度直通：∂c₅/∂c₄ = f₅（门值 ~0.5-0.9）——对照 RNN 的 w_h⁴=0.41，衰减被门控「放行」")),
                "遗忘门是可学习的：网络自己决定'记住多久'——长序列不再一键失忆");
        emit(17, List.of(VizEvent.of(VizEvent.EventType.RETURN,
                "LSTM 三门 = 输入Gate/遗忘Gate/输出Gate：GRU 合并其中两门、Transformer 用注意力替代循环")),
                "1997 年提出的架构撑起了 2017 年之前的全部序列建模——机器翻译/语音识别的第一代引擎");
        return new Trace(SCHEMA, meta(), steps);
    }

    private static double sigmoid(double x) {
        return 1 / (1 + Math.exp(-x));
    }

    private void emit(int line, List<VizEvent> events, String why) {
        StackFrame frame = new StackFrame("lstm",
                OrderedMaps.of("t", Value.prim(curT >= 0 ? String.valueOf(curT) : "-"),
                        "c_t", Value.prim(curT >= 0 ? String.format("%.3f", cs[curT]) : "0"),
                        "h_t", Value.prim(curT >= 0 ? String.format("%.3f", hs[curT]) : "0")), null);
        Map<String, Layer> layers = new LinkedHashMap<>();
        layers.put("mem.stack", new StackLayer(List.of(frame)));
        layers.put("mem.heap", new HeapLayer(snapshotHeap()));
        layers.put("math.matrix", snapshotMatrix());
        steps.add(new Step(steps.size(), Loc.line(line, why),
                Collections.unmodifiableMap(layers), List.copyOf(events), new Intent(why, INVARIANT)));
    }

    private Heap snapshotHeap() {
        Map<String, Value> fields = new LinkedHashMap<>();
        fields.put("门权重 f/i/o", Value.prim("0.5 / 0.4 / 0.6"));
        fields.put("当前 c", Value.prim(curT >= 0 ? String.format("%.3f", cs[curT]) : "0"));
        fields.put("当前 h", Value.prim(curT >= 0 ? String.format("%.3f", hs[curT]) : "0"));
        Map<String, HeapObject> objs = new LinkedHashMap<>();
        objs.put("@1", new HeapObject("@1", "cell", "LstmCell",
                Collections.unmodifiableMap(fields), List.of()));
        return new Heap(Collections.unmodifiableMap(objs));
    }

    private MathView snapshotMatrix() {
        List<String> timeline = new ArrayList<>();
        Set<Integer> active = new LinkedHashSet<>();
        Set<Integer> done = new LinkedHashSet<>();
        for (int t = 0; t < XS.length; t++) {
            String cText;
            String hText;
            String fText = "?";
            String iText = "?";
            String oText = "?";
            if (t < curT) {
                cText = String.format("%.2f", cs[t]);
                hText = String.format("%.2f", hs[t]);
                fText = String.format("%.2f", fs[t]);
                iText = String.format("%.2f", ios[t]);
                oText = String.format("%.2f", os[t]);
                done.add(t * 6 + 2);
                done.add(t * 6 + 3);
                done.add(t * 6 + 4);
                done.add(t * 6 + 5);
            } else if (t == curT) {
                cText = String.format("%.2f", cs[t]);
                hText = String.format("%.2f", hs[t]);
                fText = String.format("%.2f", fs[t]);
                iText = String.format("%.2f", ios[t]);
                oText = String.format("%.2f", os[t]);
                for (int k = 0; k < 6; k++) {
                    active.add(t * 6 + k);
                }
            } else {
                cText = "?";
                hText = "?";
            }
            timeline.add(String.valueOf(XS[t]));
            timeline.add("c" + (t + 1));
            timeline.add(cText);
            timeline.add("h" + (t + 1));
            timeline.add(hText);
            timeline.add("门" + fText + "/" + iText + "/" + oText);
        }
        MathView.Grid grid = new MathView.Grid("timeline", "时间线（x → 三门 → c 传送带 → h 读出）", XS.length, 6,
                Collections.unmodifiableList(timeline), null, null,
                Collections.unmodifiableSet(active), Collections.unmodifiableSet(done));
        MathView.Grid weights = new MathView.Grid("weights", "门权重（sigmoid 出 0~1 开度）", 1, 3,
                List.of("wf=0.5", "wi=0.4", "wo=0.6"), null, null, Set.of(), Set.of());
        return new MathView("LSTM", List.of(grid, weights));
    }

    private Trace.TraceMeta meta() {
        return new Trace.TraceMeta(id(), title(), codeResource(), "5 步 三门", XS.length);
    }

    @Override
    public java.util.List<String> dataLines() {
        return java.util.List.of(Algorithm.joinArray("xs", XS) + "   // 门权重 0.5/0.4/0.6");
    }

    @Override
    public Complexity complexity() {
        return new Complexity("O(T) 每时间步三门户计算", "O(4×h) 细胞参数（约 RNN 的 4 倍）",
                "LSTM 用 4 倍参数换长期记忆：机器翻译、语音识别、语法纠错的第一代引擎——GRU 是它的性价比版");
    }
}
