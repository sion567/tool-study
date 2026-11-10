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
 * 傅里叶变换：时域混叠的复杂信号 → 频域一排柱状频谱，峰在哪频率就是谁。
 * 信号 = sin(3·2πt) + 0.5·sin(7·2πt)：肉眼看着是一团乱波，DFT 后只剩两根峰（3 和 7，高度 1.0/0.5）。
 * 画面：RIEMANN 形态画时域曲线 + MathView 一行 16 格频谱（活动格 = 峰值频率）。
 */
public final class FourierDemo implements Algorithm {

    private static final String SCHEMA = "1";
    private static final String INVARIANT =
            "任何信号 = 不同频率正弦波的叠加；DFT 把它拆回频率成分——|X[k]| 峰出现在哪个 k，那个频率就在信号里";

    private static final int N = 64;          // 采样点
    private static final int F1 = 3, F2 = 7;  // 两个分量频率
    private static final double A2 = 0.5;     // 第二分量幅度

    private final List<Step> steps = new ArrayList<>();
    private final double[] amp = new double[N / 2];   // 归一化幅度谱
    private List<List<Double>> curve = new ArrayList<>();
    private String curveName = "";
    private Set<Integer> peaks = new LinkedHashSet<>();

    @Override public String id() { return "math.fourier"; }
    @Override public String title() { return "傅里叶变换"; }
    @Override public String codeResource() { return "/code/FourierDemo.java"; }
    @Override public String docResource() { return "/markdown/fourier.md"; }

    @Override
    public Trace produce() {
        steps.clear();
        peaks.clear();
        // 复合信号
        curve = signal(i -> Math.sin(2 * Math.PI * F1 * i / N)
                + A2 * Math.sin(2 * Math.PI * F2 * i / N));
        curveName = "复合信号";
        emit(5, List.of(VizEvent.of(VizEvent.EventType.INIT,
                "x(t) = sin(3ωt) + 0.5·sin(7ωt)——时域里已经看不出它是两根正弦的叠加")),
                "时域视角：波形纠缠；换个视角（频域）立刻真相大白");
        // DFT 频谱
        for (int k = 0; k < N / 2; k++) {
            double re = 0, im = 0;
            for (int i = 0; i < N; i++) {
                double x = Math.sin(2 * Math.PI * F1 * i / N) + A2 * Math.sin(2 * Math.PI * F2 * i / N);
                double ang = 2 * Math.PI * k * i / N;
                re += x * Math.cos(ang);
                im -= x * Math.sin(ang);
            }
            amp[k] = 2 * Math.sqrt(re * re + im * im) / N;
        }
        peaks.add(F1);
        peaks.add(F2);
        curveName = "频谱";
        emit(8, List.of(VizEvent.of(VizEvent.EventType.MARK_SORTED,
                "DFT 频谱：峰出现在频率 3（高度 1.0）和频率 7（高度 0.5）——与原式逐字吻合")),
                "频域视角：3 行的谱线有 1 份高度，7 行的有 0.5 份——幅度和频率同时读出");
        // 分量还原
        curve = signal(i -> Math.sin(2 * Math.PI * F1 * i / N));
        curveName = "分量 1";
        peaks.clear();
        peaks.add(F1);
        emit(11, List.of(VizEvent.of(VizEvent.EventType.SET,
                "分量 1：sin(3ωt)——从频谱的峰 3 还原出来")),
                "傅里叶的承诺：每个峰都能单独'拎'出一条正弦");
        curve = signal(i -> A2 * Math.sin(2 * Math.PI * F2 * i / N));
        curveName = "分量 2";
        peaks.clear();
        peaks.add(F2);
        emit(12, List.of(VizEvent.of(VizEvent.EventType.SET,
                "分量 2：0.5·sin(7ωt)——峰 7 对应，幅度恰为一半")),
                "两条分量曲线叠加 = 最初的复合信号——分解与还原互为逆过程");
        curve = signal(i -> Math.sin(2 * Math.PI * F1 * i / N)
                + A2 * Math.sin(2 * Math.PI * F2 * i / N));
        curveName = "复合信号";
        peaks.add(F1);
        peaks.add(F2);
        emit(14, List.of(VizEvent.of(VizEvent.EventType.RETURN,
                "FFT：DFT 的 O(n²) 提速到 O(n log n)——音频/图像/通信全部建立在它之上")),
                "MP3 把频谱里听不见的分量扔掉来压缩；JPEG 同理——傅里叶是'换一个坐标系看问题'的典范");
        return new Trace(SCHEMA, meta(), steps);
    }

    /** 采样曲线：x ∈ [0,1)，y 由给定函数生成。 */
    private List<List<Double>> signal(java.util.function.DoubleUnaryOperator f) {
        List<List<Double>> out = new ArrayList<>();
        for (int i = 0; i <= N; i++) {
            double t = (double) i / N;
            out.add(List.of(t, f.applyAsDouble(i)));
        }
        return out;
    }

    private void emit(int line, List<VizEvent> events, String why) {
        StackFrame frame = new StackFrame("fourier",
                OrderedMaps.of("视角", Value.prim(curveName),
                        "峰", Value.prim(peaks.toString())), null);
        Map<String, Layer> layers = new LinkedHashMap<>();
        layers.put("mem.stack", new StackLayer(List.of(frame)));
        layers.put("mem.heap", new HeapLayer(snapshotHeap()));
        layers.put("math.plot", snapshotPlot());
        layers.put("math.matrix", snapshotSpectrum());
        steps.add(new Step(steps.size(), Loc.line(line, why),
                Collections.unmodifiableMap(layers), List.copyOf(events), new Intent(why, INVARIANT)));
    }

    private Heap snapshotHeap() {
        Map<String, Value> fields = new LinkedHashMap<>();
        fields.put("当前视角", Value.prim(curveName));
        fields.put("峰频率", Value.prim(peaks.toString()));
        fields.put("采样数", Value.prim(String.valueOf(N)));
        Map<String, HeapObject> objs = new LinkedHashMap<>();
        objs.put("@1", new HeapObject("@1", "spectrum", "FourierState",
                Collections.unmodifiableMap(fields), List.of()));
        return new Heap(Collections.unmodifiableMap(objs));
    }

    private PlotView snapshotPlot() {
        List<String> labels = new ArrayList<>();
        labels.add("视角：" + curveName);
        labels.add("峰频率：" + peaks);
        return new PlotView(PlotView.RIEMANN, "傅里叶：" + curveName, 0, 0, 0,
                0, 1.0, -1.6, 1.6,
                Collections.unmodifiableList(curve),
                List.of(), List.of(), List.of(),
                Collections.unmodifiableList(labels));
    }

    private MathView snapshotSpectrum() {
        List<String> vals = new ArrayList<>();
        for (int k = 0; k < N / 2; k++) {
            vals.add(String.format("%.1f", amp[k]));
        }
        Set<Integer> active = new LinkedHashSet<>(peaks);
        MathView.Grid grid = new MathView.Grid("spectrum", "频谱（x 轴 = 频率 0~15，格值 = 幅度）", 1, N / 2,
                Collections.unmodifiableList(vals), null, null,
                Collections.unmodifiableSet(active), Set.of());
        return new MathView("DFT 频谱", List.of(grid));
    }

    private Trace.TraceMeta meta() {
        return new Trace.TraceMeta(id(), title(), codeResource(), "N=64 两个分量", 6);
    }

    @Override
    public java.util.List<String> dataLines() {
        return java.util.List.of("int N = 64;   // sin(3ωt) + 0.5·sin(7ωt)");
    }

    @Override
    public Complexity complexity() {
        return new Complexity("DFT O(n²) → FFT O(n log n)", "O(n) 频谱",
                "FFT（1965 Cooley-Tukey）把 64 点从 4096 次乘法降到 ~384 次——n 越大提速越惊人");
    }
}
