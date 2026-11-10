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
 * CORDIC 旋转模式：只用加法和移位逼近 sin/cos——计算器/FPGA 里没有乘法器时的标准做法。
 * 从向量 (1,0) 出发，每步按残差角 z 的符号决定 ±atan(2^-i) 微旋转（伪旋转，长度会涨），
 * 最后统一乘校正因子 K = Π√(1+2^-2i) ≈ 0.60725。
 * 不变量：z_{i+1} = z_i − d_i·atan(2^-i)，d_i = sign(z_i)——残差角每步至少减半逼近 0。
 */
public final class CordicDemo implements Algorithm {

    private static final String SCHEMA = "1";
    private static final String INVARIANT =
            "每步 ±atan(2^-i) 微旋转，d = sign(z)：残差 |z| 单调缩到 0 → 向量方向逼近目标角";

    private static final double TARGET_DEG = 37.0;
    private static final double TARGET = Math.toRadians(TARGET_DEG);
    private static final int ITERS = 9;
    /** 微旋转角表（度）：45, 26.565, ... */
    private static final double[] ATAN2MI = new double[ITERS];
    /** 校正因子 K = Π√(1+2^-2i) */
    private static final double K;

    static {
        double k = 1.0;
        for (int i = 0; i < ITERS; i++) {
            ATAN2MI[i] = Math.atan(Math.pow(2, -i));
            k *= Math.sqrt(1 + Math.pow(2, -2 * i));
        }
        K = 1 / k;   // 伪旋转把向量拉长了 1/k 倍 → 收尾乘 k 修正 ≈ 0.60725
    }

    private final List<Step> steps = new ArrayList<>();
    private final List<List<Double>> history = new ArrayList<>();   // 历代向量端点
    private double x = 1, y = 0, z = TARGET;
    private int iter = -1;

    @Override public String id() { return "math.cordic"; }
    @Override public String title() { return "CORDIC 算法"; }
    @Override public String codeResource() { return "/code/CordicDemo.java"; }
    @Override public String docResource() { return "/markdown/cordic.md"; }

    @Override
    public Trace produce() {
        steps.clear();
        history.clear();
        x = 1; y = 0; z = TARGET; iter = -1;
        emit(3, List.of(VizEvent.of(VizEvent.EventType.INIT,
                        "目标角 37°，向量从 (1,0) 出发，残差 z = 37°")),
                "CORDIC：只有加法和移位——把任意角拆成一串固定微角的加减");
        for (int i = 0; i < ITERS; i++) {
            iter = i;
            double d = z >= 0 ? 1 : -1;
            double aDeg = Math.toDegrees(ATAN2MI[i]);
            emit(5, List.of(VizEvent.of(VizEvent.EventType.COMPARE,
                            "z = " + String.format("%+.2f", Math.toDegrees(z)) + "° → d = " + (d > 0 ? "+1 顺时针逼近" : "−1 逆时针回退"))),
                    "残差符号决定旋转方向：z>0 就加 atan(2^-" + i + ")=" + String.format("%.3f", aDeg) + "° 的反向旋转");
            // 伪旋转（省掉 cos 因子，只留移位乘 2^-i）
            history.add(List.of(x, y));   // 旋转前方向的快照
            double factor = Math.pow(2, -i);
            double nx = x - d * y * factor;
            double ny = y + d * x * factor;
            x = nx; y = ny;
            z -= d * ATAN2MI[i];
            emit(6, List.of(VizEvent.of(VizEvent.EventType.SET,
                            "旋转后角度 = " + String.format("%+.3f", Math.toDegrees(Math.atan2(y, x)))
                                    + "°（残差 " + String.format("%+.3f", Math.toDegrees(z)) + "°）")),
                    "x ← x − d·y·2⁻ⁱ，y ← y + d·x·2⁻ⁱ：乘 2⁻ⁱ 就是二进制移位，没有真乘法");
        }
        // 收尾：乘校正因子 K 落回单位圆
        double unx = x * K, uny = y * K;
        double cosReal = Math.cos(TARGET), sinReal = Math.sin(TARGET);
        x = unx; y = uny;   // 先归一再拍终局快照
        emit(9, List.of(VizEvent.of(VizEvent.EventType.RETURN,
                        String.format("×K 后 x=cos(37°)=%.6f（真值 %.6f），y=sin(37°)=%.6f（真值 %.6f）",
                                unx, cosReal, uny, sinReal))),
                String.format("伪旋转把向量拉长了 1/K 倍——收尾统一乘 K=%.5f 归一，" +
                        "%d 次迭代只用了加减和移位", K, ITERS));
        return new Trace(SCHEMA, meta(), steps);
    }

    private void emit(int line, List<VizEvent> events, String why) {
        StackFrame frame = new StackFrame("cordic",
                OrderedMaps.of("x", Value.prim(String.format("%.6f", x)),
                        "y", Value.prim(String.format("%.6f", y)),
                        "z(°)", Value.prim(String.format("%+.3f", Math.toDegrees(z)))), null);
        Map<String, Layer> layers = new LinkedHashMap<>();
        layers.put("mem.stack", new StackLayer(List.of(frame)));
        layers.put("mem.heap", new HeapLayer(snapshotHeap()));
        layers.put("math.plot", snapshotPlot());
        steps.add(new Step(steps.size(), Loc.line(line, why),
                Collections.unmodifiableMap(layers), List.copyOf(events), new Intent(why, INVARIANT)));
    }

    private Heap snapshotHeap() {
        Map<String, Value> fields = new LinkedHashMap<>();
        fields.put("迭代 i", Value.prim(iter < 0 ? "-" : String.valueOf(iter)));
        fields.put("本步微角", Value.prim(iter < 0 ? "-"
                : String.format("atan(2^-%d)=%.3f°", iter, Math.toDegrees(ATAN2MI[iter]))));
        fields.put("当前向量", Value.prim(String.format("(%.4f, %.4f)", x, y)));
        fields.put("|z|", Value.prim(String.format("%.4f°", Math.abs(Math.toDegrees(z)))));
        Map<String, HeapObject> objs = new LinkedHashMap<>();
        objs.put("@1", new HeapObject("@1", "state", "CordicState",
                Collections.unmodifiableMap(fields), List.of()));
        return new Heap(Collections.unmodifiableMap(objs));
    }

    private PlotView snapshotPlot() {
        List<List<Double>> hist = new ArrayList<>();
        for (List<Double> p : history) hist.add(List.copyOf(p));
        hist.add(List.of(x, y));
        List<String> labels = new ArrayList<>();
        labels.add("CORDIC 旋转模式（目标 37°）");
        if (iter >= 0) {
            labels.add("微角表: atan(2⁻ⁱ)");
            labels.add(String.format("第 %d 步 ±%.3f°", iter, Math.toDegrees(ATAN2MI[iter])));
        }
        labels.add(String.format("|残差| = %.4f°", Math.abs(Math.toDegrees(z))));
        return new PlotView(PlotView.CORDIC, "CORDIC 向量旋转", TARGET, x, y,
                -1, 1, -1, 1, List.of(), List.of(), List.of(),
                Collections.unmodifiableList(hist),
                Collections.unmodifiableList(labels));
    }

    private Trace.TraceMeta meta() {
        return new Trace.TraceMeta(id(), title(), codeResource(), "目标角 37°", ITERS);
    }

    @Override
    public java.util.List<String> dataLines() {
        return java.util.List.of("double target = Math.toRadians(37);   // 目标角");
    }

    @Override
    public Complexity complexity() {
        return new Complexity("O(n) n=迭代次数（每步 1°/精度位）", "O(1)",
                "n 次微旋转，每次 2 加法 + 2 移位；n=24 就到单精度极限。"
                        + "没有乘法器（FPGA/计算器）也能算三角函数、开方、取对数");
    }
}
