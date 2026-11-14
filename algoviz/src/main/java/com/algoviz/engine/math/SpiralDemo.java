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
 * 数学·螺线画廊：三种螺线同屏对照——同一个极坐标画布，三种"生长方式"。
 * 阿基米德 r = 0.15θ（等距）、对数 r = e^(0.2θ)（等比）、黄金 r = φ^(2θ/π)（每圈 ×φ）。
 * 画面（CARTESIAN 三线复用）：蓝线 = 阿基米德，绿虚线 = 对数，trace3 = 黄金。
 */
public final class SpiralDemo implements Algorithm {

    private static final String SCHEMA = "1";
    private static final String INVARIANT =
            "阿基米德 r=aθ：等差生长——每圈半径等距加宽；对数 r=e^(bθ)：等比生长——每圈倍增；"
                    + "黄金 r=φ^(2θ/π)：与斐波那契同源的等比——'生长方式'决定了螺线的性格";

    private static final int POINTS = 150;
    private static final double THETA_MAX = 6 * Math.PI;
    private static final double A_ARCH = 0.13;
    private static final double B_LOG = 0.22;

    private final List<Step> steps = new ArrayList<>();
    private int shown = 0;

    @Override public String id() { return "math.spiral"; }
    @Override public String title() { return "螺线画廊"; }
    @Override public String codeResource() { return "/code/SpiralDemo.java"; }
    @Override public String docResource() { return "/markdown/spiral.md"; }

    @Override
    public Trace produce() {
        steps.clear();
        shown = 0;
        emit(7, List.of(VizEvent.of(VizEvent.EventType.INIT,
                "同一极坐标画布：三条螺线各画各的——'每转一圈长多少'决定一切")),
                "自然界最爱的形状：鹦鹉螺是对数螺线、星系旋臂近对数——等距螺线反而少见于自然");
        shown = 1;
        emit(10, List.of(VizEvent.of(VizEvent.EventType.SET,
                "阿基米德 r = 0.15θ（蓝线）：每圈加 ≈0.94——等差生长，胳膊等宽")),
                "阿基米德螺线的臂宽恒定——唱片纹路、卷纸、蚊香都是它");
        shown = 2;
        emit(13, List.of(VizEvent.of(VizEvent.EventType.SET,
                "对数 r = e^(0.2θ)（绿虚线）：每圈 ×e^1.26 ≈ 3.5——等比生长，越转越宽")),
                "对数螺线的自相似：放大任意倍还是同一条线——鹦鹉螺外壳的秘密");
        shown = 3;
        emit(16, List.of(VizEvent.of(VizEvent.EventType.SET,
                "黄金 r = φ^(2θ/π)（橙线）：每圈 ×φ ≈ 1.618——对数螺线的黄金特例")),
                "黄金螺线的每圈比 = φ：黄金矩形切下去的轨迹就是它");
        emit(18, List.of(VizEvent.of(VizEvent.EventType.MARK_SORTED,
                "对照：阿基米德匀速扩张、对数/黄金加速扩张——'等距'与'等比'的一字之差")),
                "极坐标 r(θ) 的表达力：一个函数画出全部螺线——参数化方程的胜利");
        emit(20, List.of(VizEvent.of(VizEvent.EventType.RETURN,
                "数学史：阿基米德(前 3 世纪)→笛卡尔对数螺线(1638)→伯努利的墓志铭（'纵使改变，依然故我'）")),
                "伯努利把对数螺线刻上墓碑：它的自相似性让'放大缩小'都回到自己——数学的浪漫");
        return new Trace(SCHEMA, meta(), steps);
    }

    private void emit(int line, List<VizEvent> events, String why) {
        StackFrame frame = new StackFrame("spiral",
                OrderedMaps.of("已展出", Value.prim(shown + "/3"),
                        "阿基米德", Value.prim("r = 0.15θ（等距）"),
                        "对数", Value.prim("r = e^(0.2θ)（等比）")), null);
        Map<String, Layer> layers = new LinkedHashMap<>();
        layers.put("mem.stack", new StackLayer(List.of(frame)));
        layers.put("mem.heap", new HeapLayer(snapshotHeap()));
        layers.put("math.plot", snapshotPlot());
        steps.add(new Step(steps.size(), Loc.line(line, why),
                Collections.unmodifiableMap(layers), List.copyOf(events), new Intent(why, INVARIANT)));
    }

    private Heap snapshotHeap() {
        Map<String, Value> fields = new LinkedHashMap<>();
        fields.put("阿基米德", Value.prim("r = 0.15θ（等差）"));
        fields.put("对数", Value.prim("r = e^(0.2θ)（等比）"));
        fields.put("黄金", Value.prim("r = φ^(2θ/π)（每圈 ×φ）"));
        Map<String, HeapObject> objs = new LinkedHashMap<>();
        objs.put("@1", new HeapObject("@1", "spiral", "SpiralGallery",
                Collections.unmodifiableMap(fields), List.of()));
        return new Heap(Collections.unmodifiableMap(objs));
    }

    private PlotView snapshotPlot() {
        List<List<Double>> arch = new ArrayList<>();
        List<List<Double>> log = new ArrayList<>();
        List<List<Double>> golden = new ArrayList<>();
        double phi = (1 + Math.sqrt(5)) / 2;
        double rMax = Math.max(Math.pow(phi, 2 * THETA_MAX / Math.PI), Math.exp(B_LOG * THETA_MAX));
        for (int i = 0; i <= POINTS; i++) {
            double th = THETA_MAX * i / POINTS;
            arch.add(List.of(A_ARCH * th * Math.cos(th), A_ARCH * th * Math.sin(th)));
            double rl = Math.exp(B_LOG * th);
            log.add(List.of(rl * Math.cos(th), rl * Math.sin(th)));
            double rg = Math.pow(phi, 2 * th / Math.PI);
            golden.add(List.of(rg * Math.cos(th), rg * Math.sin(th)));
        }
        double lim = rMax * 1.10;
        List<String> labels = new ArrayList<>();
        labels.add("螺线画廊：阿基米德（蓝，等距）vs 对数（绿虚，等比）vs 黄金（橙）");
        labels.add("等差 vs 等比：每转一圈长多少——生长方式决定性格");
        labels.add("伯努利墓志铭：'纵使改变，依然故我'（对数螺线的自相似）");
        return new PlotView(PlotView.CARTESIAN, "螺线画廊", 0, 0, 0,
                -lim, lim, -lim, lim,
                Collections.unmodifiableList(arch),
                Collections.unmodifiableList(log),
                Collections.unmodifiableList(golden),
                List.of(),
                Collections.unmodifiableList(labels));
    }

    private Trace.TraceMeta meta() {
        return new Trace.TraceMeta(id(), title(), codeResource(),
                "阿基米德/对数/黄金", shown + 3);
    }

    @Override
    public java.util.List<String> dataLines() {
        return java.util.List.of("double aArch = 0.13, bLog = 0.22;   //@DATA 螺线参数");
    }

    @Override
    public Complexity complexity() {
        return new Complexity("O(采样点) 参数化方程", "O(1) 三条曲线",
                "极坐标 r(θ) 一族函数画出全部螺线——'换个公式换条曲线'与玫瑰线同宗");
    }
}
