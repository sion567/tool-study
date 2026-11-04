package com.algoviz.engine.math;

import com.algoviz.engine.OrderedMaps;
import com.algoviz.engine.Algorithm;
import com.algoviz.engine.Intent;
import com.algoviz.engine.Layer;
import com.algoviz.engine.Loc;
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
 * 单位圆→正弦波演示：圆上动点逆时针转，sin 值被「投影」为右侧正在生长的波。
 * 不变量：sin(θ) = 圆上动点的 y 坐标——sin 不是公式，是几何。
 */
public final class UnitCircleDemo implements Algorithm {

    private static final String SCHEMA = "1";
    private static final String INVARIANT =
            "sin(θ) = 单位圆上角度 θ 对应点的 y 坐标——sin 不是公式，是几何投影";

    /** 步进角度（弧度），2π / 48 = 48 帧 */
    private static final double STEP = Math.PI / 24;

    private final List<Step> steps = new ArrayList<>();
    private final List<List<Double>> sineTrace = new ArrayList<>();
    private final List<List<Double>> cosTrace = new ArrayList<>();
    private double angle;

    @Override public String id() { return "math.unitcircle"; }
    @Override public String title() { return "单位圆→正弦波"; }
    @Override public String codeResource() { return "/code/UnitCircleDemo.java"; }
    @Override public String docResource() { return "/markdown/unit-circle.md"; }

    @Override
    public Trace produce() {
        angle = 0;
        sineTrace.clear();
        cosTrace.clear();

        int totalSteps = (int) (2 * Math.PI / STEP);
        for (int i = 0; i <= totalSteps; i++) {
            angle = i * STEP;
            double sin = Math.sin(angle);
            double cos = Math.cos(angle);
            sineTrace.add(List.of(angle, sin));
            cosTrace.add(List.of(angle, cos));
            emit(i, List.of(VizEvent.of(VizEvent.EventType.SET,
                    String.format("θ = %.2f rad (%.0f°)  sin = %.3f  cos = %.3f",
                            angle, Math.toDegrees(angle), sin, cos))),
                    String.format("θ = %.2f：圆上动点 (%.3f, %.3f) 的 y 坐标就是 sin(θ)",
                            angle, Math.cos(angle), Math.sin(angle)));
        }
        return new Trace(SCHEMA, meta(), steps);
    }

    private void emit(int frame, List<VizEvent> events, String why) {
        StackFrame frame0 = new StackFrame("unitCircle",
                OrderedMaps.of("θ", Value.prim(String.format("%.2f", angle)),
                       "sin", Value.prim(String.format("%.3f", Math.sin(angle))),
                       "cos", Value.prim(String.format("%.3f", Math.cos(angle)))), null);
        Map<String, Layer> layers = new LinkedHashMap<>();
        layers.put("mem.stack", new StackLayer(List.of(frame0)));
        layers.put("mem.heap", new HeapLayer(snapshotHeap()));
        layers.put("math.plot", snapshotPlot());
        steps.add(new Step(steps.size(), Loc.line(3, why),
                Collections.unmodifiableMap(layers), List.copyOf(events),
                new Intent(why, INVARIANT)));
    }

    private Heap snapshotHeap() {
        Map<String, Value> fields = new LinkedHashMap<>();
        fields.put("angle", Value.prim(String.format("%.3f rad", angle)));
        fields.put("sin(θ)", Value.prim(String.format("%.3f", Math.sin(angle))));
        fields.put("cos(θ)", Value.prim(String.format("%.3f", Math.cos(angle))));
        HeapObject obj = new HeapObject("@1", "θ", "UnitCircle",
                Collections.unmodifiableMap(fields), List.of());
        Map<String, HeapObject> objs = new LinkedHashMap<>();
        objs.put("@1", obj);
        return new Heap(Collections.unmodifiableMap(objs));
    }

    private PlotView snapshotPlot() {
        List<List<Double>> sineCopy = new ArrayList<>();
        for (List<Double> p : sineTrace) sineCopy.add(List.copyOf(p));
        List<List<Double>> cosCopy = new ArrayList<>();
        for (List<Double> p : cosTrace) cosCopy.add(List.copyOf(p));
        return PlotView.unitCircle("单位圆→正弦波", angle,
                Math.cos(angle), Math.sin(angle),
                Collections.unmodifiableList(sineCopy),
                Collections.unmodifiableList(cosCopy),
                List.of());
    }

    private Trace.TraceMeta meta() {
        return new Trace.TraceMeta(id(), title(), codeResource(), "θ: 0→2π", 48);
    }
}
