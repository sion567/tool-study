package com.algoviz.engine.math;

import com.algoviz.engine.OrderedMaps;
import com.algoviz.engine.Algorithm;
import com.algoviz.engine.Intent;
import com.algoviz.engine.Layer;
import com.algoviz.engine.Loc;
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
 * 泰勒展开逼近 sin(x) / cos(x)（双模式）：逐项叠加多项式，看它一步步「变成」目标函数。
 * sin(x) = x - x³/3! + x⁵/5! - x⁷/7! + …（奇次幂）
 * cos(x) = 1 - x²/2! + x⁴/4! - x⁶/6! + …（偶次幂，从常数 1 起步）
 * 每帧 = 最高次幂 +2，画面上显示近似曲线与真实曲线的差距。
 */
public final class TaylorDemo implements Algorithm {

    private static final String SCHEMA = "1";

    private static final int[] SIN_POWERS = {1, 3, 5, 7, 9, 11};   // sin：奇次幂
    private static final int[] COS_POWERS = {0, 2, 4, 6, 8, 10};   // cos：偶次幂
    private static final double X_MAX = 2 * Math.PI;
    private static final double SAMPLES = 40;

    private final boolean cosMode;
    private final int[] powers;
    private final String invariant;

    private final List<Step> steps = new ArrayList<>();
    private int termCount;
    private double maxError;

    public TaylorDemo() {
        this(false);
    }

    public TaylorDemo(boolean cosMode) {
        this.cosMode = cosMode;
        this.powers = cosMode ? COS_POWERS : SIN_POWERS;
        this.invariant = "项数越多，多项式越贴近 " + fn() + "——每加两项，误差按数量级缩小";
    }

    private String fn() {
        return cosMode ? "cos(x)" : "sin(x)";
    }

    @Override public String id() { return cosMode ? "math.taylor.cos" : "math.taylor"; }
    @Override public String title() { return cosMode ? "泰勒展开逼近 cos" : "泰勒展开逼近 sin"; }
    @Override public String codeResource() { return "/code/TaylorDemo.java"; }
    @Override public String docResource() { return "/markdown/taylor.md"; }

    @Override
    public Trace produce() {
        termCount = 0;
        maxError = 0;
        emit(3, List.of(VizEvent.of(VizEvent.EventType.INIT,
                "目标：" + fn() + "，x ∈ [0, 2π]——逐项叠加逼近")),
                (cosMode ? "cos(x) = 1 - x²/2! + x⁴/4! - …（偶次幂，常数项起步）"
                         : "sin(x) = x - x³/3! + x⁵/5! - x⁷/7! + …（奇次幂）") + "——无穷多项");

        for (int terms : powers) {
            termCount = terms;
            maxError = computeMaxError(terms);
            emit(5, List.of(VizEvent.of(VizEvent.EventType.SET,
                    String.format("%d 项近似：最大误差 %.4f", terms, maxError))),
                    String.format("%d 项：在 [0,2π] 全段最大误差 %.4f——%s",
                            terms, maxError, maxError < 0.01 ? "几乎重合" : maxError < 0.5 ? "已很接近" : "还差得远"));
        }
        return new Trace(SCHEMA, meta(), steps);
    }

    /** n 项泰勒近似在 [0, 2π] 的最大误差。 */
    private double computeMaxError(int n) {
        double max = 0;
        for (int i = 0; i <= SAMPLES; i++) {
            double x = i / SAMPLES * X_MAX;
            double real = cosMode ? Math.cos(x) : Math.sin(x);
            max = Math.max(max, Math.abs(approx(x, n) - real));
        }
        return max;
    }

    /** n = 最高次幂的近似值（按模式分派 sin/cos）。 */
    private double approx(double x, int maxPower) {
        return cosMode ? taylorCos(x, maxPower) : taylorSin(x, maxPower);
    }

    /** n 项泰勒近似：x - x³/3! + x⁵/5! - …（共 n 项，奇数次幂）。 */
    public static double taylorSin(double x, int n) {
        double sum = 0, power = x;
        int sign = 1;
        for (int k = 1; k <= n; k += 2) {
            sum += sign * power / factorial(k);
            power *= x * x;
            sign = -sign;
        }
        return sum;
    }

    /** cos 的 n 项泰勒近似：1 - x²/2! + x⁴/4! - …（偶次幂，含常数项）。 */
    public static double taylorCos(double x, int maxPower) {
        double sum = 1;               // k=0：常数项 1
        double power = 1;
        double sign = -1;
        for (int k = 2; k <= maxPower; k += 2) {
            power *= x * x;           // x² → x⁴ → …
            sum += sign * power / factorial(k);
            sign = -sign;
        }
        return sum;
    }

    private static double factorial(int n) {
        double f = 1;
        for (int i = 2; i <= n; i++) f *= i;
        return f;
    }

    private void emit(int line, List<VizEvent> events, String why) {
        StackFrame frame = new StackFrame("taylor",
                OrderedMaps.of("项数", Value.prim(termCount),
                       "最大误差", Value.prim(String.format("%.4f", maxError))), null);
        Map<String, Layer> layers = new LinkedHashMap<>();
        layers.put("mem.stack", new StackLayer(List.of(frame)));
        layers.put("mem.heap", new HeapLayer(snapshotHeap()));
        layers.put("math.plot", snapshotPlot());
        steps.add(new Step(steps.size(), Loc.line(line, why),
                Collections.unmodifiableMap(layers), List.copyOf(events), new Intent(why, invariant)));
    }

    private Heap snapshotHeap() {
        Map<String, Value> fields = new LinkedHashMap<>();
        fields.put("项数", Value.prim(termCount));
        fields.put("最大误差", Value.prim(String.format("%.4f", maxError)));
        fields.put("公式", Value.prim(cosMode ? "1 - x²/2! + x⁴/4! - …" : "x - x³/3! + x⁵/5! - …"));
        HeapObject obj = new HeapObject("@1", fn() + "≈", "Taylor",
                Collections.unmodifiableMap(fields), List.of());
        Map<String, HeapObject> objs = new LinkedHashMap<>();
        objs.put("@1", obj);
        return new Heap(Collections.unmodifiableMap(objs));
    }

    private PlotView snapshotPlot() {
        // 真实 sin 曲线 + 当前项数的近似曲线
        List<List<Double>> real = new ArrayList<>();
        List<List<Double>> approx = new ArrayList<>();
        for (int i = 0; i <= (int) SAMPLES; i++) {
            double x = i / SAMPLES * X_MAX;
            real.add(List.of(x, cosMode ? Math.cos(x) : Math.sin(x)));
            approx.add(List.of(x, approx(x, termCount)));
        }
        return new PlotView(PlotView.UNIT_CIRCLE, "泰勒展开逼近 " + fn(), termCount,
                termCount, maxError,
                0, X_MAX, -1.4, 1.4,
                Collections.unmodifiableList(real),      // 真实曲线
                Collections.unmodifiableList(approx),    // 近似曲线
                List.of(), List.of("真实 " + fn(), termCount + " 次幂近似"));
    }

    private Trace.TraceMeta meta() {
        return new Trace.TraceMeta(id(), title(), codeResource(), "1→11 次幂", powers.length);
    }
}
