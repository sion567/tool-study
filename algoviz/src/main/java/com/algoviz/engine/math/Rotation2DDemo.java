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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 线性代数·2D 旋转变换：三角形 × 旋转矩阵，每步转 30°，六步转半圈。
 * x' = x·cosθ − y·sinθ；y' = x·sinθ + y·cosθ——矩阵乘向量 = 几何变换。
 * 画面：三角形每步的落位（蓝多边形）+ 三个顶点（蓝点）+ 原点。旋转中心 = 原点。
 */
public final class Rotation2DDemo implements Algorithm {

    private static final String SCHEMA = "1";
    private static final String INVARIANT =
            "旋转矩阵 R(θ) 保持长度与角度不变（正交矩阵）——线性代数不只是解方程，更是'变换的语言'";

    private static final double STEP_DEG = 30;
    private static final int ROUNDS = 6;   // 共转 180°
    private static final double[][] TRIANGLE = {{2, 0}, {-1, 1}, {-1, -1}};

    private final List<Step> steps = new ArrayList<>();
    private final double[][] verts = new double[3][2];   // 当前顶点
    private double thetaDeg = 0;

    @Override public String id() { return "math.rotation"; }
    @Override public String title() { return "线性代数·2D 旋转"; }
    @Override public String codeResource() { return "/code/Rotation2DDemo.java"; }
    @Override public String docResource() { return "/markdown/rotation2d.md"; }

    @Override
    public Trace produce() {
        steps.clear();
        for (int i = 0; i < 3; i++) {
            verts[i][0] = TRIANGLE[i][0];
            verts[i][1] = TRIANGLE[i][1];
        }
        thetaDeg = 0;
        emit(4, List.of(VizEvent.of(VizEvent.EventType.INIT,
                "三角形 (2,0)(−1,1)(−1,−1)——绕原点每步逆时针转 30°")),
                "游戏/机器人/图形学里每次'转动'都是一次矩阵乘向量");
        for (int round = 1; round <= ROUNDS; round++) {
            thetaDeg = STEP_DEG * round;
            for (int i = 0; i < 3; i++) {
                double x = verts[i][0], y = verts[i][1];
                double rad = Math.toRadians(STEP_DEG);
                verts[i][0] = x * Math.cos(rad) - y * Math.sin(rad);
                verts[i][1] = x * Math.sin(rad) + y * Math.cos(rad);
            }
            emit(7, List.of(VizEvent.of(VizEvent.EventType.SET,
                    String.format("R(%.0f°)·(2,0) = (%.3f, %.3f)   全部顶点已旋转",
                            thetaDeg, verts[0][0], verts[0][1]))),
                    "R(θ) 矩阵两行就是两个'新基'——cos/sin 恰好保长度：cos²+sin²=1");
        }
        emit(10, List.of(VizEvent.of(VizEvent.EventType.MARK_SORTED,
                "转了 180°：顶点 (2,0) → (−2,0)——两次 90° 旋转 = 一次镜像回转")),
                "边长、面积、形状全程不变——正交变换只动位置不动几何");
        emit(12, List.of(VizEvent.of(VizEvent.EventType.RETURN,
                "延伸：缩放=对角阵、切变=三角阵、组合=矩阵连乘——3D 游戏引擎的每一帧都是这套")),
                "矩阵不是数字表格，是'空间 操作指南'");
        return new Trace(SCHEMA, meta(), steps);
    }

    private void emit(int line, List<VizEvent> events, String why) {
        StackFrame frame = new StackFrame("rotate",
                OrderedMaps.of("累计角", Value.prim(String.format("%.0f°", thetaDeg)),
                        "顶点 A", Value.prim(String.format("(%.3f, %.3f)", verts[0][0], verts[0][1]))), null);
        Map<String, Layer> layers = new LinkedHashMap<>();
        layers.put("mem.stack", new StackLayer(List.of(frame)));
        layers.put("mem.heap", new HeapLayer(snapshotHeap()));
        layers.put("math.plot", snapshotPlot());
        steps.add(new Step(steps.size(), Loc.line(line, why),
                Collections.unmodifiableMap(layers), List.copyOf(events), new Intent(why, INVARIANT)));
    }

    private Heap snapshotHeap() {
        Map<String, Value> fields = new LinkedHashMap<>();
        for (int i = 0; i < 3; i++) {
            fields.put("顶点" + "ABC".charAt(i), Value.prim(String.format("(%.3f, %.3f)", verts[i][0], verts[i][1])));
        }
        Map<String, HeapObject> objs = new LinkedHashMap<>();
        objs.put("@1", new HeapObject("@1", "triangle", "RotatedTriangle",
                Collections.unmodifiableMap(fields), List.of()));
        return new Heap(Collections.unmodifiableMap(objs));
    }

    private PlotView snapshotPlot() {
        List<List<Double>> poly = new ArrayList<>();
        List<List<Double>> dots = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            poly.add(List.of(verts[i][0], verts[i][1]));
            dots.add(List.of(verts[i][0], verts[i][1], 0.06));
        }
        poly.add(List.of(verts[0][0], verts[0][1]));   // 闭合三角形
        List<String> labels = new ArrayList<>();
        labels.add(String.format("累计旋转 %.0f°", thetaDeg));
        labels.add("x' = x·cosθ − y·sinθ");
        labels.add("y' = x·sinθ + y·cosθ");
        return new PlotView(PlotView.CARTESIAN, "2D 旋转变换", Math.toRadians(thetaDeg),
                verts[0][0], verts[0][1],
                -3.2, 3.2, -3.2, 3.2,
                Collections.unmodifiableList(poly),
                List.of(), List.of(),
                Collections.unmodifiableList(dots),
                Collections.unmodifiableList(labels));
    }

    private Trace.TraceMeta meta() {
        return new Trace.TraceMeta(id(), title(), codeResource(), "6×30°=180°", ROUNDS + 2);
    }

    @Override
    public java.util.List<String> dataLines() {
        return java.util.List.of("double stepDeg = " + (int) STEP_DEG + ";   // 6 步 = 180°");
    }

    @Override
    public Complexity complexity() {
        return new Complexity("O(V) 每次变换 = V 次矩阵乘向量", "O(V) 顶点",
                "100 万顶点的模型转一次 = 300 万次乘加——GPU 一毫秒内完成：显卡本质上就是矩阵乘法机器");
    }
}
