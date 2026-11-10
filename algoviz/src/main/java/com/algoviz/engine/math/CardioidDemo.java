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
 * 笛卡尔·心形线：极坐标方程 r = 1 − cosθ 逐点落到直角坐标系。
 * θ 从 0 转到 2π，每步算 r = 1 − cosθ，再转换 x = r·cosθ, y = r·sinθ——
 * 一行极坐标方程在笛卡尔坐标系里画出一颗心。
 * 这就是两种坐标系的分工：旋转对称的曲线用极坐标写极简，落笔画图用直角坐标。
 */
public final class CardioidDemo implements Algorithm {

    private static final String SCHEMA = "1";
    private static final String INVARIANT =
            "r = 1 − cosθ 的每个 θ 给出一个 (r, θ)，换算 x = r·cosθ、y = r·sinθ 落进笛卡尔坐标系——曲线是点的集合";

    private static final int STEPS = 48;

    private final List<Step> steps = new ArrayList<>();
    private final List<List<Double>> points = new ArrayList<>();
    private double theta, r, x, y;

    @Override public String id() { return "math.cardioid"; }
    @Override public String title() { return "笛卡尔·心形线"; }
    @Override public String codeResource() { return "/code/CardioidDemo.java"; }
    @Override public String docResource() { return "/markdown/cardioid.md"; }

    @Override
    public Trace produce() {
        steps.clear();
        points.clear();
        theta = r = x = y = 0;
        emit(4, List.of(VizEvent.of(VizEvent.EventType.INIT,
                "r = 1 − cosθ，θ: 0 → 2π——极坐标一行，笛卡尔坐标一个心")),
                "笛卡尔坐标 (x,y)：好画不好写；极坐标 (r,θ)：旋转对称的曲线一行写完——两种坐标系各司其职");
        for (int i = 0; i <= STEPS; i++) {
            theta = 2 * Math.PI * i / STEPS;
            r = 1 - Math.cos(theta);
            x = r * Math.cos(theta);
            y = r * Math.sin(theta);
            points.add(List.of(x, y));
            if (i % 6 == 0 || i == STEPS) {
                emit(7, List.of(VizEvent.of(VizEvent.EventType.SET,
                        String.format("θ=%3.0f° → r=%.2f → (x,y) = (%.2f, %.2f)",
                                Math.toDegrees(theta), r, x, y))),
                        "换算三步走：极径 r、极角 θ、直角坐标 (r·cosθ, r·sinθ)");
            }
        }
        emit(12, List.of(VizEvent.of(VizEvent.EventType.MARK_SORTED,
                "心形线完成：x ∈ [−2, 0.25]，关于 x 轴对称——尖点在原点，圆弧朝左")),
                "趣闻：传说笛卡尔用 r = a(1−cosθ) 向瑞典公主表白（心形线情书）——故事是后人编的，曲线是真的");
        emit(14, List.of(VizEvent.of(VizEvent.EventType.RETURN,
                "同一个曲线族：调参 a 得大小，cosθ 换 sinθ 得朝向——参数化让'画一个心'变成'改一个字母'")),
                "这正是坐标系的威力：几何形状 ↔ 代数方程双向翻译——解析几何的奠基之作");
        return new Trace(SCHEMA, meta(), steps);
    }

    private void emit(int line, List<VizEvent> events, String why) {
        StackFrame frame = new StackFrame("cardioid",
                OrderedMaps.of("θ(°)", Value.prim(String.format("%.0f", Math.toDegrees(theta))),
                        "r", Value.prim(String.format("%.2f", r)),
                        "(x,y)", Value.prim(String.format("(%.2f, %.2f)", x, y))), null);
        Map<String, Layer> layers = new LinkedHashMap<>();
        layers.put("mem.stack", new StackLayer(List.of(frame)));
        layers.put("mem.heap", new HeapLayer(snapshotHeap()));
        layers.put("math.plot", snapshotPlot());
        steps.add(new Step(steps.size(), Loc.line(line, why),
                Collections.unmodifiableMap(layers), List.copyOf(events), new Intent(why, invariant())));
    }

    private String invariant() {
        return INVARIANT;
    }

    private Heap snapshotHeap() {
        Map<String, Value> fields = new LinkedHashMap<>();
        fields.put("方程", Value.prim("r = 1 − cosθ"));
        fields.put("已落点", Value.prim(points.size() + " / " + (STEPS + 1)));
        Map<String, HeapObject> objs = new LinkedHashMap<>();
        objs.put("@1", new HeapObject("@1", "curve", "Cardioid",
                Collections.unmodifiableMap(fields), List.of()));
        return new Heap(Collections.unmodifiableMap(objs));
    }

    private PlotView snapshotPlot() {
        List<List<Double>> pts = new ArrayList<>();
        for (List<Double> p : points) pts.add(List.copyOf(p));
        List<String> labels = new ArrayList<>();
        labels.add("r = 1 − cosθ（极坐标）");
        labels.add(String.format("θ = %.0f°  r = %.2f", Math.toDegrees(theta), r));
        labels.add(String.format("(x, y) = (%.2f, %.2f)", x, y));
        return new PlotView(PlotView.CARTESIAN, "心形线：极坐标 → 笛卡尔坐标", theta, x, y,
                -2.2, 0.6, -1.5, 1.5,
                Collections.unmodifiableList(pts),
                List.of(), List.of(), List.of(),
                Collections.unmodifiableList(labels));
    }

    private Trace.TraceMeta meta() {
        return new Trace.TraceMeta(id(), title(), codeResource(), "r=1−cosθ", STEPS + 1);
    }

    @Override
    public java.util.List<String> dataLines() {
        return java.util.List.of("double a = 1.0;   // r = a*(1 - cos(t)), t: 0..2π");
    }

    @Override
    public Complexity complexity() {
        return new Complexity("O(n) n=采样点数", "O(n) 点列",
                "参数方程的通用画法：θ 步进 → 算 (x,y) → 连线。任何极坐标曲线（玫瑰线、螺线、摆线）同一套代码");
    }
}
