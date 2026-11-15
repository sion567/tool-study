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
 * 每个新项两帧：甲帧 = 新项单独形状（绿虚线，累加曲线不动），乙帧 = 累加后 + 全段误差。
 * 幂次只能按 +2 跳（sin 只有奇次幂项），项数计的是真实累加项数，不是幂次。
 */
public final class TaylorDemo implements Algorithm {

    private static final String SCHEMA = "1";

    private static final int[] SIN_POWERS = {1, 3, 5, 7, 9, 11};   // sin：奇次幂
    private static final int[] COS_POWERS = {0, 2, 4, 6, 8, 10};   // cos：偶次幂
    private static final double X_MAX = 2 * Math.PI;
    private static final double SAMPLES = 40;
    /** 固定显示域 ±1.4：低次多项式在 x→2π 爆冲（1 项 6.3 / 3 项 -35 / 5 项 +46），
     *  域若自适应包住爆冲会把真实曲线压成贴轴线——渲染器把超域段裁剪在边框，sin 永远满幅可读。 */
    private static final double Y_LIM = 1.4;

    private final boolean cosMode;
    private final int[] powers;
    private final String invariant;

    private final List<Step> steps = new ArrayList<>();
    private final List<List<Double>> errHistory = new ArrayList<>();   // [项数, 全段最大误差] 逐乙帧追加（迷你误差图）
    private int nTerms;      // 已累加的真实项数（非幂次！）
    private int maxPower;    // 当前累加的最高次幂（-1 = 尚未开始）
    private double maxError;

    public TaylorDemo() {
        this(false);
    }

    public TaylorDemo(boolean cosMode) {
        this.cosMode = cosMode;
        this.powers = cosMode ? COS_POWERS : SIN_POWERS;
        this.invariant = "项数越多，多项式越贴近 " + fn() + "——贴合段从原点向两端生长；"
                + "全段最大误差先被远端爆冲推高，再被后续项拽回";
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
        nTerms = 0;
        maxPower = -1;
        maxError = 0;
        errHistory.clear();
        emit(3, List.of(VizEvent.of(VizEvent.EventType.INIT,
                "目标：" + fn() + "，x ∈ [0, 2π]——逐项叠加逼近")),
                (cosMode ? "cos(x) = 1 - x²/2! + x⁴/4! - …（偶次幂，常数项起步）"
                         : "sin(x) = x - x³/3! + x⁵/5! - x⁷/7! + …（奇次幂）") + "——无穷多项",
                false);

        for (int i = 0; i < powers.length; i++) {
            int k = powers[i];
            String f = termFormula(i);
            // 甲帧：新项单独亮相——累加曲线保持上一帧，绿虚线 = 该项自身形状
            emit(5, List.of(VizEvent.of(VizEvent.EventType.SET,
                    "新项 " + f + " 亮相：在 x=π 处贡献 " + String.format("%+.2f", termValue(i, Math.PI)))),
                    "新项 " + f + "——幂次越高只在 |x| 越大处显著，原点附近它几乎为零；看它把曲线往哪边拉",
                    true);
            // 乙帧：累加后的近似 + 全段误差
            nTerms = i + 1;
            maxPower = k;
            maxError = computeMaxError(k);
            errHistory.add(List.of((double) nTerms, maxError));
            emit(5, List.of(VizEvent.of(VizEvent.EventType.SET,
                    String.format("累加至 %d 项（最高次幂 x%s）：全段最大误差 %.4f", nTerms, sup(k), maxError))),
                    String.format("%d 项：[0,2π] 全段最大误差 %.4f——%s", nTerms, maxError,
                            maxError < 0.01 ? "几乎重合" : maxError < 0.5 ? "已很接近" : "还差得远"),
                    false);
        }
        return new Trace(SCHEMA, meta(), steps);
    }

    /** n 项泰勒近似在 [0, 2π] 的最大误差（n = 最高次幂）。 */
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

    // ---------- 逐项展示辅助 ----------

    /** 第 idx 个新项的公式串（符号与级数一致：偶数序 +、奇数序 -）。 */
    private String termFormula(int idx) {
        int k = powers[idx];
        if (cosMode && k == 0) {
            return "1";
        }
        if (k == 1) {
            return "x";
        }
        return (idx % 2 == 0 ? "" : "-") + "x" + sup(k) + "/" + k + "!";
    }

    /** 第 idx 个新项在 x 处的值：±x^k / k!（符号与 termFormula 一致）。 */
    private double termValue(int idx, double x) {
        int k = powers[idx];
        return (idx % 2 == 0 ? 1 : -1) * Math.pow(x, k) / factorial(k);
    }

    /** 上标数字：11 → ¹¹。 */
    private static String sup(int n) {
        String[] d = {"⁰", "¹", "²", "³", "⁴", "⁵", "⁶", "⁷", "⁸", "⁹"};
        StringBuilder sb = new StringBuilder();
        for (char c : String.valueOf(n).toCharArray()) {
            sb.append(d[c - '0']);
        }
        return sb.toString();
    }

    private void emit(int line, List<VizEvent> events, String why, boolean isTermFrame) {
        int cumPower = maxPower < 0 ? 0 : maxPower;        // 累加曲线的幂次（未开始 = 0：sin 平 0 / cos 平 1）
        int termIdx = isTermFrame ? nTerms : -1;           // 甲帧正在亮相的项 = 即将累加的那一项
        StackFrame frame = new StackFrame("taylor",
                OrderedMaps.of("已累加项数", Value.prim(nTerms),
                       "最高次幂", Value.prim(maxPower < 0 ? "—" : "x" + sup(maxPower)),
                       "最大误差", Value.prim(String.format("%.4f", maxError))), null);
        Map<String, Layer> layers = new LinkedHashMap<>();
        layers.put("mem.stack", new StackLayer(List.of(frame)));
        layers.put("mem.heap", new HeapLayer(snapshotHeap()));
        layers.put("math.plot", snapshotPlot(cumPower, termIdx));
        steps.add(new Step(steps.size(), Loc.line(line, why),
                Collections.unmodifiableMap(layers), List.copyOf(events), new Intent(why, invariant)));
    }

    private Heap snapshotHeap() {
        Map<String, Value> fields = new LinkedHashMap<>();
        fields.put("已累加项数", Value.prim(nTerms));
        fields.put("最高次幂", Value.prim(maxPower < 0 ? "—" : "x" + sup(maxPower)));
        fields.put("最大误差", Value.prim(String.format("%.4f", maxError)));
        fields.put("公式", Value.prim(cosMode ? "1 - x²/2! + x⁴/4! - …" : "x - x³/3! + x⁵/5! - …"));
        HeapObject obj = new HeapObject("@1", fn() + "≈", "Taylor",
                Collections.unmodifiableMap(fields), List.of());
        Map<String, HeapObject> objs = new LinkedHashMap<>();
        objs.put("@1", obj);
        return new Heap(Collections.unmodifiableMap(objs));
    }

    private PlotView snapshotPlot(int cumPower, int termIdx) {
        // 真实曲线 + 累加近似曲线（域固定 ±Y_LIM，超域爆冲段由渲染器裁剪）
        List<List<Double>> real = new ArrayList<>();
        List<List<Double>> cum = new ArrayList<>();
        List<List<Double>> single = new ArrayList<>();
        boolean hasSingle = termIdx >= 0 && termIdx < powers.length;
        for (int i = 0; i <= (int) SAMPLES; i++) {
            double x = i / SAMPLES * X_MAX;
            real.add(List.of(x, cosMode ? Math.cos(x) : Math.sin(x)));
            cum.add(List.of(x, approx(x, cumPower)));
            if (hasSingle) {
                single.add(List.of(x, termValue(termIdx, x)));
            }
        }
        String second = nTerms == 0 ? "起始状态" : nTerms + " 项累加近似";
        List<String> labels = hasSingle
                ? List.of("真实 " + fn(), second, termFormula(termIdx) + "（本帧新项）")
                : List.of("真实 " + fn(), second);
        return new PlotView(PlotView.TAYLOR, "泰勒展开逼近 " + fn(), Math.max(maxPower, 0),
                0, maxError,
                0, X_MAX, -Y_LIM, Y_LIM,
                Collections.unmodifiableList(real),      // 真实曲线
                Collections.unmodifiableList(cum),       // 累加近似
                Collections.unmodifiableList(single),    // 甲帧：新项单独形状
                List.copyOf(errHistory),                 // 误差历史 [项数, 全段最大误差]——必须逐帧拷贝，
                                                         // 活引用会让"后面的追加"改写"前面帧的快照"
                labels);
    }

    private Trace.TraceMeta meta() {
        return new Trace.TraceMeta(id(), title(), codeResource(), "1→11 次幂", powers.length);
    }
}
