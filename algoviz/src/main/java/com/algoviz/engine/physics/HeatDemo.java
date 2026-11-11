package com.algoviz.engine.physics;

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
 * 热学·热扩散：中心 4 格 100°C 的热块在绝热边界里摊平——傅里叶热传导方程的数值现场。
 * 显式差分 T' = T + C·(四邻和 − 4T)（C=0.2 满足稳定性 C ≤ 0.25），绝热 = 镜像邻居。
 * 总热量严格守恒（镜面通量成对抵消），终态全场均温 = 400/64 = 6.25°C。
 * 画面（math.matrix 复用）：8×8 温度网格，黄圈 = 本帧值发生变化的格子（波前环）——
 * 这是本批唯一支持 🎯 预测点选的物理演示。
 */
public final class HeatDemo implements Algorithm {

    private static final String SCHEMA = "1";
    private static final String INVARIANT =
            "热量只从高温流向低温（负梯度方向），绝热边界让通量无处可逃——"
                    + "总热量严格守恒，终态必然全场均温 6.25°C（=400 单位摊到 64 格）：熵增的数值版";

    private static final int N = 8;
    private static final double C = 0.2;           // 扩散系数（稳定性要求 ≤ 0.25）
    private static final int FRAMES = 25;
    private static final int SWEEPS_PER_FRAME = 7;
    private static final double TOTAL_HEAT = 400.0; // 4 格 × 100°C

    private final List<Step> steps = new ArrayList<>();
    private double[][] field = new double[N][N];
    private int frame, sweeps;
    private double peak, low;

    @Override public String id() { return "physics.heat"; }
    @Override public String title() { return "热学·热扩散"; }
    @Override public String codeResource() { return "/code/HeatDemo.java"; }
    @Override public String docResource() { return "/markdown/heat.md"; }

    @Override
    public Trace produce() {
        steps.clear();
        field = new double[N][N];
        for (int r = 3; r <= 4; r++) {
            for (int c = 3; c <= 4; c++) {
                field[r][c] = 100;                  // 中心 2×2 热块
            }
        }
        frame = sweeps = 0;
        peak = 100;
        low = 0;
        emit(5, List.of(VizEvent.of(VizEvent.EventType.INIT,
                "中心 2×2 热块 100°C，其余 0°C，四周绝热——400 单位热量只能摊平，无处可逃")),
                "傅里叶定律：热流 ∝ 负温度梯度——温差越大，流得越快，摊平是必然结局");
        double[][] prev = snapshot();
        for (int f = 1; f <= FRAMES; f++) {
            frame = f;
            for (int s = 0; s < SWEEPS_PER_FRAME; s++) {
                field = sweep();
                sweeps++;
            }
            peak = 0;
            low = Double.MAX_VALUE;
            for (double[] row : field) {
                for (double v : row) {
                    peak = Math.max(peak, v);
                    low = Math.min(low, v);
                }
            }
            Set<Integer> changed = changedCells(prev, field);
            prev = snapshot();
            emit(14, List.of(VizEvent.of(VizEvent.EventType.SET,
                    String.format("第 %d 帧：峰值 %.1f°C，最低 %.1f°C——波前继续外推，温差在缩小", f, peak, low))),
                    f <= 8 ? "黄圈 = 本帧值变化超过 0.1°C 的格子：热量正沿着波前环向外涌"
                            : "温差驱动的流动越来越慢（梯度变小）——摊平是先快后慢的收敛");
        }
        emit(18, List.of(VizEvent.of(VizEvent.EventType.MARK_SORTED,
                String.format("第 %d 帧：峰值 %.2f°C ≈ 最低 %.2f°C ≈ 6.25°C——全场摊平，总热量 %.1f 分毫不差",
                        frame, peak, low, TOTAL_HEAT))),
                "终态均温 = 总热量 ÷ 格数：守恒律直接锁死了结局，过程只决定怎么走到那里");
        emit(20, List.of(VizEvent.of(VizEvent.EventType.RETURN,
                "同一套方程在别处：咖啡变凉、暖气片烘房间、CPU 散热——∇²T 是热的 universal 搬运工")),
                "扩散方程时间反演不对称：打得碎的鸡蛋拼不回去——热力学第二定律的数学脸孔");
        return new Trace(SCHEMA, meta(), steps);
    }

    /** 一轮显式差分：绝热边界 = 越界处取自身（镜面反射 → 通量为零 → 总热量守恒）。 */
    private double[][] sweep() {
        double[][] nt = new double[N][N];
        for (int r = 0; r < N; r++) {
            for (int c = 0; c < N; c++) {
                double up = field[r == 0 ? r : r - 1][c];
                double dn = field[r == N - 1 ? r : r + 1][c];
                double lf = field[r][c == 0 ? c : c - 1];
                double rt = field[r][c == N - 1 ? c : c + 1];
                nt[r][c] = field[r][c] + C * (up + dn + lf + rt - 4 * field[r][c]);
            }
        }
        return nt;
    }

    private double[][] snapshot() {
        double[][] copy = new double[N][N];
        for (int r = 0; r < N; r++) {
            copy[r] = field[r].clone();
        }
        return copy;
    }

    /** 显示值（0.1°C 精度）发生变化的格子 = 波前环（🎯 预测点选的真值也基于它）。 */
    private static Set<Integer> changedCells(double[][] before, double[][] after) {
        Set<Integer> out = new LinkedHashSet<>();
        for (int r = 0; r < N; r++) {
            for (int c = 0; c < N; c++) {
                if (!display(before[r][c]).equals(display(after[r][c]))) {
                    out.add(r * N + c);
                }
            }
        }
        return out;
    }

    private static String display(double v) {
        return String.format("%.1f", v);
    }

    private void emit(int line, List<VizEvent> events, String why) {
        StackFrame fr = new StackFrame("heat",
                OrderedMaps.of("帧", Value.prim(String.valueOf(frame)),
                        "峰值", Value.prim(String.format("%.1f°C", peak)),
                        "最低", Value.prim(String.format("%.1f°C", low)),
                        "均温", Value.prim(String.format("%.1f°C", TOTAL_HEAT / (N * N)))), null);
        Map<String, Layer> layers = new LinkedHashMap<>();
        layers.put("mem.stack", new StackLayer(List.of(fr)));
        layers.put("mem.heap", new HeapLayer(snapshotHeap()));
        layers.put("math.matrix", snapshotMath());
        steps.add(new Step(steps.size(), Loc.line(line, why),
                Collections.unmodifiableMap(layers), List.copyOf(events), new Intent(why, INVARIANT)));
    }

    private Heap snapshotHeap() {
        Map<String, Value> fields = new LinkedHashMap<>();
        fields.put("总热量", Value.prim(String.format("%.1f 单位", TOTAL_HEAT)));
        fields.put("终态均温", Value.prim(String.format("%.1f°C", TOTAL_HEAT / (N * N))));
        fields.put("当前峰值", Value.prim(String.format("%.2f°C", peak)));
        fields.put("当前最低", Value.prim(String.format("%.2f°C", low)));
        fields.put("扩散轮数", Value.prim(String.valueOf(sweeps)));
        Map<String, HeapObject> objs = new LinkedHashMap<>();
        objs.put("@1", new HeapObject("@1", "heat", "ThermalState",
                Collections.unmodifiableMap(fields), List.of()));
        return new Heap(Collections.unmodifiableMap(objs));
    }

    private MathView snapshotMath() {
        List<String> values = new ArrayList<>();
        for (int r = 0; r < N; r++) {
            for (int c = 0; c < N; c++) {
                values.add(display(field[r][c]));
            }
        }
        Set<Integer> hot = new LinkedHashSet<>();
        for (int r = 0; r < N; r++) {
            for (int c = 0; c < N; c++) {
                if (field[r][c] - TOTAL_HEAT / (N * N) > 0.5) {
                    hot.add(r * N + c);
                }
            }
        }
        MathView.Grid grid = new MathView.Grid("T", "温度场 °C（黄圈=本帧变化格，绿=已高于均温 0.5）",
                N, N, values, null, null, hot, Set.of());
        return new MathView("热扩散温度场", List.of(grid));
    }

    private Trace.TraceMeta meta() {
        return new Trace.TraceMeta(id(), title(), codeResource(),
                "8×8 绝热 中心2×2=100°C", FRAMES + 2);
    }

    @Override
    public java.util.List<String> dataLines() {
        return java.util.List.of("double C = 0.2;   // 扩散系数（稳定要求 ≤ 0.25）");
    }

    @Override
    public Complexity complexity() {
        return new Complexity("O(cells) 每轮扫描全网格", "O(cells) 两份温度场",
                "热扩散 = 一切「摊平」过程的原型：浓度扩散、污染物弥散、PageRank 的能量分发——"
                        + "同一个拉普拉斯算子，同一个均化结局");
    }
}
