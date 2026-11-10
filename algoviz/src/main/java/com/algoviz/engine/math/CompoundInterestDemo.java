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
 * 复利与 e：(1 + 1/n)ⁿ 随 n 加密结算次数 → 2.71828…
 * 伯努利 1683 年的发现：100% 年利率，半年一结 ×2.25、每季 ×2.44、每秒 ×2.718…——连续复利 = e。
 * 画面（CARTESIAN）：离散点 = 各结算频率的终值，绿线 = e 的极限，蓝线 = 连续函数 (1+1/x)ˣ。
 */
public final class CompoundInterestDemo implements Algorithm {

    private static final String SCHEMA = "1";
    private static final String INVARIANT =
            "结算越频繁收益越高但有上限——(1+1/n)ⁿ 单调递增收敛到 e = 2.71828…，这就是自然底数的出身";

    private static final int[] NS = {1, 2, 4, 8, 16, 32, 64, 128};
    private static final double E = Math.E;

    private final List<Step> steps = new ArrayList<>();
    private int n = 1;
    private double val;

    @Override public String id() { return "math.compound"; }
    @Override public String title() { return "复利与 e"; }
    @Override public String codeResource() { return "/code/CompoundInterestDemo.java"; }
    @Override public String docResource() { return "/markdown/compound.md"; }

    @Override
    public Trace produce() {
        steps.clear();
        n = 1;
        val = 2;
        emit(4, List.of(VizEvent.of(VizEvent.EventType.INIT,
                "本金 1 元、年利率 100%：一年后是多少？——取决于'结几次'")),
                "年结 ×2；半年结 ×1.5² = 2.25——结算越勤，越接近一个神秘上限");
        for (int k : NS) {
            n = k;
            val = Math.pow(1 + 1.0 / n, n);
            emit(7, List.of(VizEvent.of(VizEvent.EventType.SET,
                    String.format("n=%d（每 %s 一结）→ (1+1/n)ⁿ = %.4f",
                            n, n == 1 ? "年" : n + " 份", val))),
                    n >= 32 ? "点子已经贴住绿线——多结一次几乎不再多赚"
                            : "每次加密结算都往上爬一点，但爬升越来越小");
        }
        emit(10, List.of(VizEvent.of(VizEvent.EventType.MARK_SORTED,
                "n→∞ → 连续复利 → e = 2.71828…——这个极限值得一个专属符号")),
                "e 的出身不是几何也不是代数，是'利滚利的上限'——一面记账一面诞生的常数");
        emit(12, List.of(VizEvent.of(VizEvent.EventType.RETURN,
                "连续复利公式 A = P·e^{rt}：e^{rt} 就是 (1+r/n)^{nt} 在 n→∞ 的样子")),
                "同一族曲线：放射性衰变、人口增长、RC 电路——凡'增长速率正比于当前量'者皆姓 e");
        return new Trace(SCHEMA, meta(), steps);
    }

    private void emit(int line, List<VizEvent> events, String why) {
        StackFrame frame = new StackFrame("compound",
                OrderedMaps.of("n", Value.prim(n),
                        "(1+1/n)ⁿ", Value.prim(String.format("%.4f", val)),
                        "e", Value.prim("2.71828")), null);
        Map<String, Layer> layers = new LinkedHashMap<>();
        layers.put("mem.stack", new StackLayer(List.of(frame)));
        layers.put("mem.heap", new HeapLayer(snapshotHeap()));
        layers.put("math.plot", snapshotPlot());
        steps.add(new Step(steps.size(), Loc.line(line, why),
                Collections.unmodifiableMap(layers), List.copyOf(events), new Intent(why, INVARIANT)));
    }

    private Heap snapshotHeap() {
        Map<String, Value> fields = new LinkedHashMap<>();
        fields.put("结算次数 n", Value.prim(n));
        fields.put("终值", Value.prim(String.format("%.4f", val)));
        fields.put("上限 e", Value.prim("2.71828"));
        Map<String, HeapObject> objs = new LinkedHashMap<>();
        objs.put("@1", new HeapObject("@1", "growth", "CompoundState",
                Collections.unmodifiableMap(fields), List.of()));
        return new Heap(Collections.unmodifiableMap(objs));
    }

    private PlotView snapshotPlot() {
        // 连续函数 (1+1/x)^x 采样（蓝线）
        List<List<Double>> curve = new ArrayList<>();
        for (int i = 1; i <= 128; i += 2) {
            double x = i;
            curve.add(List.of(x, Math.pow(1 + 1.0 / x, x)));
        }
        // e 的极限线（绿）
        List<List<Double>> eLine = new ArrayList<>();
        eLine.add(List.of(1.0, E));
        eLine.add(List.of(128.0, E));
        // 离散点（橙）
        List<List<Double>> dots = new ArrayList<>();
        for (int k : NS) {
            dots.add(List.of((double) k, Math.pow(1 + 1.0 / k, k), 0.9));
        }
        List<String> labels = new ArrayList<>();
        labels.add("(1+1/n)ⁿ → e = 2.71828…");
        labels.add(String.format("n=%d → %.4f", n, val));
        labels.add("绿线 = 连续复利的极限");
        return new PlotView(PlotView.CARTESIAN, "复利与 e", n, n, val,
                0, 130, 1.8, 2.85,
                Collections.unmodifiableList(curve),
                Collections.unmodifiableList(eLine),
                List.of(), Collections.unmodifiableList(dots),
                Collections.unmodifiableList(labels));
    }

    private Trace.TraceMeta meta() {
        return new Trace.TraceMeta(id(), title(), codeResource(), "n=1→128", NS.length + 2);
    }

    @Override
    public java.util.List<String> dataLines() {
        return java.util.List.of("int[] ns = {1,2,4,8,16,32,64,128};   // 本金 1 元，利率 100%");
    }

    @Override
    public Complexity complexity() {
        return new Complexity("O(n) 每个结算点直接计算", "O(1)",
                "(1+1/n)ⁿ 单调递增有上界 → 极限存在（单调有界定理）——e 由此定义，导数 (eˣ)'=eˣ 由此生长");
    }
}
