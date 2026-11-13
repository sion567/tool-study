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
 * 微积分·逐段积分误差对照：同一函数 f(x)=x² on [0,1]（真值 1/3），三种数值积分的误差阶赛跑。
 * 左黎曼 O(1/n)（n 翻倍误差减半）、梯形 O(1/n²)（缩 1/4）、辛普森 O(1/n⁴)（缩 1/16）——
 * "多算几个点"与"怎么算点"是两码事：辛普森用抛物线插值把精度指数级抬升。
 * 画面（math.matrix 复用）：收敛表 4 行（n=4/8/16/32）× 4 列（n/黎曼误差/梯形误差/辛普森误差）逐行填充。
 */
public final class TrapezoidDemo implements Algorithm {

    private static final String SCHEMA = "1";
    private static final String INVARIANT =
            "误差阶：左黎曼 O(1/n)、梯形 O(1/n²)、辛普森 O(1/n⁴)——n 翻倍时误差分别缩 1/2、1/4、1/16；"
                    + "辛普森 = 两点间抛物线插值：'曲线的弯，用抛物线去追'";

    private static final int[] NS = {4, 8, 16, 32};
    private static final double TRUE = 1.0 / 3;   // ∫₀¹x² = 1/3

    private final List<Step> steps = new ArrayList<>();
    private final double[] riemannE = new double[NS.length];
    private final double[] trapE = new double[NS.length];
    private final double[] simpsonE = new double[NS.length];
    private int curN = -1;

    @Override public String id() { return "math.trapezoid"; }
    @Override public String title() { return "逐段积分误差对照"; }
    @Override public String codeResource() { return "/code/TrapezoidDemo.java"; }
    @Override public String docResource() { return "/markdown/trapezoid.md"; }

    @Override
    public Trace produce() {
        steps.clear();
        curN = -1;
        emit(7, List.of(VizEvent.of(VizEvent.EventType.INIT,
                "f(x)=x² on [0,1]，真值 1/3≈0.3333：三种数值积分的误差赛跑——'分多少段' vs '怎么分段'")),
                "数值积分 = 用有限个采样点猜曲线下的面积——误差阶决定你花的算力值不值");
        for (int idx = 0; idx < NS.length; idx++) {
            curN = idx;
            int n = NS[idx];
            double h = 1.0 / n;
            double lr = 0;                       // 左黎曼
            for (int i = 0; i < n; i++) {
                lr += f(i * h) * h;
            }
            double tr = 0;                       // 梯形
            for (int i = 0; i < n; i++) {
                tr += (f(i * h) + f((i + 1) * h)) * h / 2;
            }
            double sp = f(0) + f(1);             // 辛普森（n 偶数：n/2 个二元段）
            for (int i = 1; i < n; i++) {
                sp += f(i * h) * (i % 2 == 0 ? 2 : 4);
            }
            sp = sp * h / 3;
            riemannE[idx] = Math.abs(lr - TRUE);
            trapE[idx] = Math.abs(tr - TRUE);
            simpsonE[idx] = Math.abs(sp - TRUE);
            double half = idx == 0 ? 1 : riemannE[idx - 1] / riemannE[idx];
            emit(12, List.of(VizEvent.of(VizEvent.EventType.SET,
                    String.format("n=%d：黎曼误差 %.5f、梯形 %.5f、辛普森 %.2e（黎曼较上轮缩 %.2f）",
                            n, riemannE[idx], trapE[idx], simpsonE[idx], half))),
                    "三列误差的比值就是'误差阶'：n 翻倍，黎曼减半、梯形缩 1/4、辛普森缩 1/16");
        }
        curN = -1;
        emit(15, List.of(VizEvent.of(VizEvent.EventType.MARK_SORTED,
                "结论：想精度 ×16，黎曼要 16 倍算力、梯形 4 倍、辛普森只要 2 倍——误差阶就是'性价比'")),
                "辛普森的代价：每段 3 个点（2 倍算力）——用插值多项式的阶换收敛速度");
        emit(17, List.of(VizEvent.of(VizEvent.EventType.RETURN,
                "梯形与黎曼的差别 = 把'矩形'换成'梯形'——边界多算一个点，误差阶就升一级")),
                "数值积分家族：中矩形/梯形/辛普森/高斯求积——每一代都在问'同样的点能不能更准'");
        return new Trace(SCHEMA, meta(), steps);
    }

    private static double f(double x) {
        return x * x;
    }

    private void emit(int line, List<VizEvent> events, String why) {
        StackFrame frame = new StackFrame("trapezoid",
                OrderedMaps.of("n", Value.prim(curN >= 0 ? String.valueOf(NS[curN]) : "-"),
                        "黎曼误差", Value.prim(curN >= 0 ? String.format("%.5f", riemannE[curN]) : "-"),
                        "梯形误差", Value.prim(curN >= 0 ? String.format("%.5f", trapE[curN]) : "-")), null);
        Map<String, Layer> layers = new LinkedHashMap<>();
        layers.put("mem.stack", new StackLayer(List.of(frame)));
        layers.put("mem.heap", new HeapLayer(snapshotHeap()));
        layers.put("math.matrix", snapshotMath());
        steps.add(new Step(steps.size(), Loc.line(line, why),
                Collections.unmodifiableMap(layers), List.copyOf(events), new Intent(why, INVARIANT)));
    }

    private Heap snapshotHeap() {
        Map<String, Value> fields = new LinkedHashMap<>();
        fields.put("真值", Value.prim("1/3 ≈ 0.333333"));
        fields.put("f(x)", Value.prim("x² on [0,1]"));
        fields.put("当前 n", Value.prim(curN >= 0 ? String.valueOf(NS[curN]) : "-"));
        Map<String, HeapObject> objs = new LinkedHashMap<>();
        objs.put("@1", new HeapObject("@1", "quad", "QuadratureState",
                Collections.unmodifiableMap(fields), List.of()));
        return new Heap(Collections.unmodifiableMap(objs));
    }

    private MathView snapshotMath() {
        List<String> vals = new ArrayList<>();
        Set<Integer> active = new LinkedHashSet<>();
        Set<Integer> done = new LinkedHashSet<>();
        vals.add("n");
        vals.add("黎曼误差");
        vals.add("梯形误差");
        vals.add("辛普森误差");
        for (int idx = 0; idx < NS.length; idx++) {
            String rT = "?";
            String tT = "?";
            String sT = "?";
            if (idx < curN) {
                rT = String.format("%.5f", riemannE[idx]);
                tT = String.format("%.5f", trapE[idx]);
                sT = String.format("%.2e", simpsonE[idx]);
                done.add((idx + 1) * 4);
                done.add((idx + 1) * 4 + 1);
                done.add((idx + 1) * 4 + 2);
                done.add((idx + 1) * 4 + 3);
            } else if (idx == curN) {
                rT = String.format("%.5f", riemannE[idx]);
                tT = String.format("%.5f", trapE[idx]);
                sT = String.format("%.2e", simpsonE[idx]);
                for (int k = 0; k < 4; k++) {
                    active.add((idx + 1) * 4 + k);
                }
            }
            vals.add(String.valueOf(NS[idx]));
            vals.add(rT);
            vals.add(tT);
            vals.add(sT);
        }
        MathView.Grid grid = new MathView.Grid("conv", "误差收敛表（每列相邻比值 = 误差阶）", NS.length + 1, 4,
                Collections.unmodifiableList(vals), null, null,
                Collections.unmodifiableSet(active), Collections.unmodifiableSet(done));
        return new MathView("逐段积分误差对照", List.of(grid));
    }

    private Trace.TraceMeta meta() {
        return new Trace.TraceMeta(id(), title(), codeResource(), "x² on [0,1]", NS.length + 2);
    }

    @Override
    public java.util.List<String> dataLines() {
        return java.util.List.of("int[] ns = {4, 8, 16, 32};   // f(x)=x² on [0,1]");
    }

    @Override
    public Complexity complexity() {
        return new Complexity("O(n) 三法各一遍", "O(1) 累加器",
                "误差阶是数值方法的选择语言：辛普森每段 2 倍算力换 1/16 的误差——性价比思维");
    }
}
