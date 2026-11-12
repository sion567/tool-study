package com.algoviz.engine.crypto;

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
 * 非对称·SM2 与椭圆曲线：教学曲线 y² = x³+2x+2 (mod 17) 上的弦切法现场。
 * 点加 = 两点连线（倍点 = 切线）交曲线于第三点 → 取反；同一套公式在实数与有限域上成立。
 * mod 17 的小曲线点构成有限循环群——SM2 的 256 位大曲线是同一数学的工业放大。
 * 画面（CARTESIAN 复用）：蓝线 = 实数曲线 y²=x³+2x+2，橙点 = 当前运算点，绿点 = 有限域点云，
 * 绿虚线 = 弦线/切线。
 */
public final class Sm2Demo implements Algorithm {

    private static final String SCHEMA = "1";
    private static final String INVARIANT =
            "点加公式 λ=(y₂−y₁)/(x₂−x₁)（倍点 λ=3x₁²/2y₁）、x₃=λ²−x₁−x₂、y₃=λ(x₁−x₃)−y₁——"
                    + "除法 = 乘模逆元，mod 17 的点构成有限循环群；SM2 的命门 = 大曲线上已知 kG 求 k（离散对数）";

    private static final int P = 17;
    private static final int A = 2;
    private static final int B = 2;
    private static final int GX = 6, GY = 3;       // 生成元（在曲线上：9 = 216+14 mod 17 ✓）

    private final List<Step> steps = new ArrayList<>();
    private double px, py;                          // 当前动点（实数画面坐标）
    private double chordX1, chordY1, chordX2, chordY2;
    private boolean hasChord;
    private String verdict;

    @Override public String id() { return "crypto.sm2"; }
    @Override public String title() { return "非对称·SM2与椭圆曲线"; }
    @Override public String codeResource() { return "/code/Sm2Demo.java"; }
    @Override public String docResource() { return "/markdown/sm2.md"; }

    @Override
    public Trace produce() {
        steps.clear();
        hasChord = false;
        verdict = "";
        px = GX;
        py = GY;
        emit(6, List.of(VizEvent.of(VizEvent.EventType.INIT,
                "教学曲线 y² = x³+2x+2 (mod 17)：蓝线 = 实数曲线的形状，绿点 = mod 17 的全部整数解——同一个方程的两个世界")),
                "SM2 用 256 位大曲线，数字大到画不了——但几何与公式一模一样：小曲线是它的沙盘");
        // 点加演示：P = G, Q = 2G 结果…… 直接演 G+G（倍点，切线最直观）
        int[] r2 = add(GX, GY, GX, GY);
        px = r2[0];
        py = r2[1];
        emit(10, List.of(VizEvent.of(VizEvent.EventType.MESSAGE,
                "2G = G+G（倍点）：切线代替弦线，λ = 3x₁²/(2y₁) = 3·36/6 = 18 ≡ 1 (mod 17)")),
                "同一个点加倍需要'切'——几何直觉在实数曲线上练，算术在有限域上算");
        emit(12, List.of(VizEvent.of(VizEvent.EventType.SET,
                String.format("x₃ = λ²−2x = 1−12 ≡ 6 (mod 17)，y₃ = λ(x−x₃)−y = 3 ≡ 3 → 2G = (%d,%d)", r2[0], r2[1]))),
                "同一个点加倍需要'切'：公式与弦线版同源，只是斜率换成切线斜率");
        int[] acc = {GX, GY};
        for (int k = 3; k <= 8; k++) {
            int[] prev = acc;
            acc = add(acc[0], acc[1], GX, GY);
            px = acc[0];
            py = acc[1];
            chordX1 = prev[0];
            chordY1 = prev[1];
            chordX2 = GX;
            chordY2 = GY;
            hasChord = true;
            emit(15, List.of(VizEvent.of(VizEvent.EventType.SET,
                    String.format("%dG = %dG + G：弦线连接 (%d,%d) 与 (%d,%d) → (%d,%d)",
                            k, k - 1, prev[0], prev[1], GX, GY, acc[0], acc[1]))),
                    "标量乘 = 反复点加：kG 一个公式从 G 走到任何点——椭圆曲线上的'快速幂'");
        }
        hasChord = false;
        int count = 1;   // 含无穷远点 O
        int[] q = {GX, GY};
        for (int i = 0; i < 40 && !(q[0] < 0); i++) {
            q = add(q[0], q[1], GX, GY);
            if (q[0] < 0) {
                break;
            }
            count++;
        }
        verdict = String.format("G 生成 %d 阶循环群（%d 个仿射点 + O，第 %d 步回到无穷远点）——点数有限，运算闭合",
                count + 1, count, count + 1);
        px = 0;
        py = 0;
        emit(19, List.of(VizEvent.of(VizEvent.EventType.MARK_SORTED, verdict)),
                "循环群是椭圆曲线密码的全部资本：私钥 k 是'步数'，公钥 kG 是'终点'——从终点倒推步数 = 离散对数难题");
        emit(22, List.of(VizEvent.of(VizEvent.EventType.RETURN,
                "SM2 真实参数：256 位素数曲线、基点 G 阶 n≈2²⁵⁶——把这里的 (6,3) 换成 78 位十六进制就是工业曲线")),
                "签名 = 用 k 造 r,s；验签 = 公钥 P=kG 反算——正算一毫秒，倒算到宇宙热寂");
        return new Trace(SCHEMA, meta(), steps);
    }

    /** 有限域点加：O 用 (-1,-1) 表示；切线（倍点）走 2y₁ 逆元。 */
    private int[] add(int x1, int y1, int x2, int y2) {
        if (x1 < 0) {
            return new int[]{x2, y2};
        }
        if (x2 < 0) {
            return new int[]{x1, y1};
        }
        if (x1 == x2 && y1 == y2) {
            int den = ((2 * y1) % P + P) % P;
            if (den == 0) {
                return new int[]{-1, -1};   // 切线垂直 → 无穷远点
            }
            int lam = (3 * x1 * x1 + A) % P * modInverse(den) % P;   // λ = (3x₁²+a)/(2y₁)
            int x3 = ((lam * lam - 2 * x1) % P + P) % P;
            int y3 = ((lam * (x1 - x3) - y1) % P + P) % P;
            return new int[]{x3, y3};
        }
        if (x1 == x2) {
            return new int[]{-1, -1};       // 同 x 不同 y → 互逆 → 和为 O
        }
        int den = ((x2 - x1) % P + P) % P;
        int lam = ((y2 - y1) % P + P) % P * modInverse(den) % P;
        int x3 = ((lam * lam - x1 - x2) % P + P) % P;
        int y3 = ((lam * (x1 - x3) - y1) % P + P) % P;
        return new int[]{x3, y3};
    }

    private int modInverse(int a) {
        a = ((a % P) + P) % P;
        int[] r = {a, P};
        int[] s = {1, 0};
        while (r[1] != 0) {
            int q = r[0] / r[1];
            int tr = r[0] - q * r[1];
            int ts = s[0] - q * s[1];
            r[0] = r[1];
            r[1] = tr;
            s[0] = s[1];
            s[1] = ts;
        }
        return ((s[0] % P) + P) % P;
    }

    private void emit(int line, List<VizEvent> events, String why) {
        StackFrame fr = new StackFrame("sm2",
                OrderedMaps.of("当前点", Value.prim(String.format("(%d,%d)", (int) px, (int) py)),
                        "曲线", Value.prim("y²=x³+2x+2 (mod 17)")), null);
        Map<String, Layer> layers = new LinkedHashMap<>();
        layers.put("mem.stack", new StackLayer(List.of(fr)));
        layers.put("mem.heap", new HeapLayer(snapshotHeap()));
        layers.put("math.plot", snapshotPlot());
        steps.add(new Step(steps.size(), Loc.line(line, why),
                Collections.unmodifiableMap(layers), List.copyOf(events), new Intent(why, INVARIANT)));
    }

    private Heap snapshotHeap() {
        Map<String, Value> fields = new LinkedHashMap<>();
        fields.put("教学曲线", Value.prim("y² = x³+2x+2 (mod 17)"));
        fields.put("生成元 G", Value.prim(String.format("(%d,%d)", GX, GY)));
        fields.put("判决", Value.prim(verdict.isEmpty() ? "演算中" : verdict));
        Map<String, HeapObject> objs = new LinkedHashMap<>();
        objs.put("@1", new HeapObject("@1", "sm2", "CurveState",
                Collections.unmodifiableMap(fields), List.of()));
        return new Heap(Collections.unmodifiableMap(objs));
    }

    private PlotView snapshotPlot() {
        // 实数曲线 y² = x³+2x+2（x ≥ -0.77 段），上半支 + 下半支一条折线
        List<List<Double>> curve = new ArrayList<>();
        for (int i = 0; i <= 120; i++) {
            double xx = -0.77 + 6.77 * i / 120;
            double yy = Math.sqrt(Math.max(0, xx * xx * xx + 2 * xx + 2));
            curve.add(List.of(xx, yy));
        }
        for (int i = 120; i >= 0; i--) {
            double xx = -0.77 + 6.77 * i / 120;
            double yy = Math.sqrt(Math.max(0, xx * xx * xx + 2 * xx + 2));
            curve.add(List.of(xx, -yy));
        }
        // mod 17 点云
        List<List<Double>> dots = new ArrayList<>();
        for (int xx = 0; xx < P; xx++) {
            int rhs = (xx * xx * xx + A * xx + B) % P;
            for (int yy = 0; yy < P; yy++) {
                if (yy * yy % P == rhs) {
                    dots.add(List.of((double) xx, (double) yy, 0.008));
                }
            }
        }
        List<List<Double>> chord = new ArrayList<>();
        if (hasChord) {
            chord.add(List.of(chordX1, chordY1));
            chord.add(List.of(chordX2, chordY2));
        }
        List<String> labels = new ArrayList<>();
        labels.add("椭圆曲线：实数形状（蓝） vs mod 17 解点（绿）");
        labels.add(String.format("G=(%d,%d)——当前点 (%d,%d)", GX, GY, (int) px, (int) py));
        labels.add(hasChord ? "绿虚线 = 弦线：交第三点后取反" : "SM2 = 256 位大曲线上的同一套公式");
        return new PlotView(PlotView.CARTESIAN, "椭圆曲线弦切法", 0, px, py,
                -1.2, 6.8, -9.5, 9.5,
                Collections.unmodifiableList(curve),
                Collections.unmodifiableList(chord), List.of(),
                Collections.unmodifiableList(dots),
                Collections.unmodifiableList(labels));
    }

    private Trace.TraceMeta meta() {
        return new Trace.TraceMeta(id(), title(), codeResource(),
                "y²=x³+2x+2 (mod 17)", 23);
    }

    @Override
    public java.util.List<String> dataLines() {
        return java.util.List.of("int p = 17, a = 2, b = 2;   //@DATA 教学曲线参数");
    }

    @Override
    public Complexity complexity() {
        return new Complexity("点加 O(log p) 模逆元，标量乘 O(log k) 倍加", "O(1) 一个点",
                "SM2 签名/密钥交换、TLS 国密套件、电子签章——"
                        + "RSA 靠'分解大数难'，SM2 靠'曲线离散对数难'：同一安全目标的两条数学路");
    }
}
