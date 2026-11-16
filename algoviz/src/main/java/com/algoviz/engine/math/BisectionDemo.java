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
 * 数值方法·二分法求根：f(x) = x³ − x − 2 在 [1,2] 上变号 → 根被区间夹住，每次砍一半。
 * 10 步把区间缩到 1/1024 → 根 ≈ 1.5214。与牛顿迭代（平方收敛）对比：鲁棒 vs 快速。
 * 画面（NEWTON 形态复用）：曲线 + 当前中点竖线 + 橙点=中点处函数值。
 */
public final class BisectionDemo implements Algorithm {

    private static final String SCHEMA = "1";
    private static final String INVARIANT =
            "f(a)·f(b) < 0 → 根在 [a,b] 内；取中点按符号留一半——每步区间减半，误差预算 1/2ⁿ";

    private static final double A0 = 1, B0 = 2;
    private static final int STEPS = 10;

    private final List<Step> steps = new ArrayList<>();
    private boolean intervalOn = true;
    private double a, b, mid, fMid;

    @Override public String id() { return "math.bisection"; }
    @Override public String title() { return "数值方法·二分法"; }
    @Override public String codeResource() { return "/code/BisectionDemo.java"; }
    @Override public String docResource() { return "/markdown/bisection.md"; }

    @Override
    public Trace produce() {
        steps.clear();
        a = A0;
        b = B0;
        emit(4, List.of(VizEvent.of(VizEvent.EventType.INIT,
                "f(x)=x³−x−2：f(1)=−2 < 0，f(2)=4 > 0 → 根被夹在 [1,2]")),
                "介值定理：连续函数从负走到正，必经过零——这就是二分法的合法性", false);
        for (int i = 1; i <= STEPS; i++) {
            mid = (a + b) / 2;
            fMid = f(mid);
            boolean goRight = fMid < 0;
            emit(7, List.of(VizEvent.of(VizEvent.EventType.COMPARE,
                    String.format("第 %d 步：中点 x=%.4f，f(x)=%+.4f → 根在 %s",
                            i, mid, fMid, goRight ? "[" + fmt(mid) + ", " + fmt(b) + "]" : "[" + fmt(a) + ", " + fmt(mid) + "]"))),
                    "按符号扔掉不含根的一半——信息量每步 +1（log₂ 收敛）");
            if (goRight) {
                a = mid;
            } else {
                b = mid;
            }
            emit(8, List.of(VizEvent.of(VizEvent.EventType.SET,
                    String.format("区间 → [%s, %s]（宽 %.5f，误差 ≤ %.5f）", fmt(a), fmt(b), b - a, (b - a) / 2))),
                    "区间宽度的预算：1/2ⁿ——要 k 位精度就跑 k 步");
        }
        emit(11, List.of(VizEvent.of(VizEvent.EventType.MARK_SORTED,
                String.format("根 ≈ %.4f（真值 1.5213797…）——10 步拿到 3 位小数", mid))),
                "对比牛顿迭代：牛顿平方收敛 5 步顶二分 15 步——但二分只要求'变号'，不需要导数");
        emit(13, List.of(VizEvent.of(VizEvent.EventType.RETURN,
                "工程组合拳：二分兜底保鲁棒，牛顿冲刺抢速度——很多库就是这么混搭的")),
                "本例真值 1.5213797…正是'蜻蜓方程' x³=x+2 的唯一实根");
        return new Trace(SCHEMA, meta(), steps);
    }

    private double f(double x) {
        return x * x * x - x - 2;
    }

    private static String fmt(double v) {
        return String.format("%.4f", v);
    }

    private void emit(int line, List<VizEvent> events, String why) {
        emit(line, events, why, true);
    }

    private void emit(int line, List<VizEvent> events, String why, boolean withInterval) {
        intervalOn = withInterval;
        StackFrame frame = new StackFrame("bisection",
                OrderedMaps.of("区间", Value.prim("[" + fmt(a) + ", " + fmt(b) + "]"),
                        "中点 f(x)", Value.prim(String.format("%+.4f", fMid))),
                null);
        Map<String, Layer> layers = new LinkedHashMap<>();
        layers.put("mem.stack", new StackLayer(List.of(frame)));
        layers.put("mem.heap", new HeapLayer(snapshotHeap()));
        layers.put("math.plot", snapshotPlot());
        steps.add(new Step(steps.size(), Loc.line(line, why),
                Collections.unmodifiableMap(layers), List.copyOf(events), new Intent(why, INVARIANT)));
    }

    private Heap snapshotHeap() {
        Map<String, Value> fields = new LinkedHashMap<>();
        fields.put("a", Value.prim(fmt(a)));
        fields.put("b", Value.prim(fmt(b)));
        fields.put("区间宽", Value.prim(String.format("%.5f", b - a)));
        fields.put("根估计", Value.prim(fmt(mid)));
        Map<String, HeapObject> objs = new LinkedHashMap<>();
        objs.put("@1", new HeapObject("@1", "bracket", "BisectionState",
                Collections.unmodifiableMap(fields), List.of()));
        return new Heap(Collections.unmodifiableMap(objs));
    }

    private PlotView snapshotPlot() {
        return snapshotPlot(intervalOn);
    }

    private PlotView snapshotPlot(boolean withInterval) {
        List<List<Double>> curve = new ArrayList<>();
        for (int i = 0; i <= 60; i++) {
            double x = A0 + (B0 - A0) * i / 60;
            curve.add(List.of(x, f(x)));
        }
        List<List<Double>> midLine = new ArrayList<>();
        midLine.add(List.of(mid, 0.0));
        midLine.add(List.of(mid, f(mid)));
        List<String> labels = new ArrayList<>();
        labels.add("f(x) = x³ − x − 2");
        labels.add(String.format("区间 [%s, %s]", fmt(a), fmt(b)));
        labels.add(String.format("中点 x=%.4f, f=%+.4f", mid, fMid));
        return new PlotView(PlotView.NEWTON, "二分法：区间减半", mid, mid, fMid,
                1.0, 2.0, -2.5, 4.5,
                Collections.unmodifiableList(curve),
                Collections.unmodifiableList(midLine),
                List.of(), withInterval ? List.of(java.util.List.of(a, mid, b)) : List.of(),   // 🎯 rects[0] = [a, mid, b]（INIT 帧不编——mid 未定）
                Collections.unmodifiableList(labels));
    }

    private Trace.TraceMeta meta() {
        return new Trace.TraceMeta(id(), title(), codeResource(), "根≈1.5214", STEPS + 2);
    }

    @Override
    public java.util.List<String> dataLines() {
        return java.util.List.of("double a = 1, b = 2;   // f(x) = x*x*x - x - 2");
    }

    @Override
    public Complexity complexity() {
        return new Complexity("O(log(1/ε)) 每步砍一半", "O(1)",
                "与牛顿的分工：二分只要变号+连续（鲁棒、线性收敛），牛顿要导数（快、平方收敛但可能发散）");
    }
}
