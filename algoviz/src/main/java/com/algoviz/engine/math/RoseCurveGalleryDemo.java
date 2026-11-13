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
 * 玫瑰线画廊：r = cos(kθ)，一个参数 k 决定花瓣数——k 奇 = k 瓣，k 偶 = 2k 瓣。
 * 五幅展品：四叶（k=2）→ 三叶（k=3）→ 五叶（k=5）→ 八叶（k=4）→ 七叶（k=7）；同一套采样代码，换一个字母换一朵花。
 */
public final class RoseCurveGalleryDemo implements Algorithm {

    private static final String SCHEMA = "1";
    private static final String INVARIANT =
            "r = cos(kθ)：k 为奇数时曲线转一圈画 k 瓣（θ:0..π 即闭合）；k 为偶数时画 2k 瓣——负半径翻到对面成瓣";

    private static final int K_2 = 2, K_3 = 3, K_5 = 5;
    private static final int SAMPLES = 120;

    private final List<Step> steps = new ArrayList<>();
    private final List<List<Double>> points = new ArrayList<>();
    private int k = K_2;
    private double theta, r, x, y;

    @Override public String id() { return "math.rose"; }
    @Override public String title() { return "玫瑰线画廊"; }
    @Override public String codeResource() { return "/code/RoseCurveGalleryDemo.java"; }
    @Override public String docResource() { return "/markdown/rose.md"; }

    @Override
    public Trace produce() {
        steps.clear();
        points.clear();
        theta = r = x = y = 0;
        emit(4, List.of(VizEvent.of(VizEvent.EventType.INIT,
                "同一支笔，三种花：只改方程里的一个数字 k")),
                "极坐标的'旋转对称'优势：玫瑰线在直角坐标下写不出初等表达式");
        show(K_2, 6, "四叶（k=2：偶数 → 2k 瓣）——负半径翻到对面，两两重合成四瓣");
        show(K_3, 8, "三叶（k=3：奇数 → k 瓣）——转 π 就闭合，省了一半路程");
        show(K_5, 10, "五叶（k=5：奇数 → k 瓣）——五重旋转对称，与五边形同宗");
        show(4, 11, "八叶（k=4：偶数 → 2k 瓣）——偶数族再添一员：2×4=8 瓣");
        show(7, 12, "七叶（k=7：奇数 → k 瓣）——七重对称，素数 k 最'纯粹'");
        emit(13, List.of(VizEvent.of(VizEvent.EventType.MARK_SORTED,
                "规律入袋：瓣数 = k（奇）/ 2k（偶）；花瓣长短 = |cos| 的极值 1")),
                "同一个采样循环画遍所有玫瑰线——参数化方程的'一码多花'");
        emit(15, List.of(VizEvent.of(VizEvent.EventType.RETURN,
                "延伸：r=cos(2θ) 若改 sin(2θ) 花瓣转 45°；r=cos(3θ) 改 sin 旋转 30°")),
                "数学的'免费午餐'：旋转 = 方程里加一个相位，不必重算");
        return new Trace(SCHEMA, meta(), steps);
    }

    /** 展出一朵：k 给定，θ 0..2π 采样落点。 */
    private void show(int kk, int line, String why) {
        points.clear();
        k = kk;
        for (int i = 0; i <= SAMPLES; i++) {
            theta = 2 * Math.PI * i / SAMPLES;
            r = Math.cos(k * theta);
            x = r * Math.cos(theta);
            y = r * Math.sin(theta);
            points.add(List.of(x, y));
        }
        x = points.get(points.size() - 1).get(0);
        y = points.get(points.size() - 1).get(1);
        emit(line, List.of(VizEvent.of(VizEvent.EventType.SET,
                "r = cos(" + k + "θ) → " + petalName(k), "@" + k)),
                why);
    }

    private String petalName(int kk) {
        return (kk % 2 == 1 ? kk : 2 * kk) + " 瓣玫瑰";
    }

    private void emit(int line, List<VizEvent> events, String why) {
        StackFrame frame = new StackFrame("roseGallery",
                OrderedMaps.of("k", Value.prim(k),
                        "花瓣", Value.prim(petalName(k)),
                        "采样点", Value.prim(String.valueOf(points.size()))), null);
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
        fields.put("方程", Value.prim("r = cos(" + k + "θ)"));
        fields.put("花瓣数", Value.prim(petalName(k)));
        Map<String, HeapObject> objs = new LinkedHashMap<>();
        objs.put("@1", new HeapObject("@1", "rose", "RoseCurve",
                Collections.unmodifiableMap(fields), List.of()));
        return new Heap(Collections.unmodifiableMap(objs));
    }

    private PlotView snapshotPlot() {
        List<List<Double>> pts = new ArrayList<>();
        for (List<Double> p : points) pts.add(List.copyOf(p));
        List<String> labels = new ArrayList<>();
        labels.add("r = cos(" + k + "θ)");
        labels.add(petalName(k));
        labels.add("θ: 0 → 2π，" + SAMPLES + " 采样");
        return new PlotView(PlotView.CARTESIAN, "玫瑰线画廊", theta, x, y,
                -1.25, 1.25, -1.25, 1.25,
                Collections.unmodifiableList(pts),
                List.of(), List.of(), List.of(),
                Collections.unmodifiableList(labels));
    }

    private Trace.TraceMeta meta() {
        return new Trace.TraceMeta(id(), title(), codeResource(), "k=2,3,5,4,7", 3);
    }

    @Override
    public java.util.List<String> dataLines() {
        return java.util.List.of("int[] ks = {2, 3, 5};   // r = cos(kθ)");
    }

    @Override
    public Complexity complexity() {
        return new Complexity("O(m) m=采样点", "O(m) 点列",
                "一码多花：参数化方程 + 统一采样循环——图形编程的通用范式（matplotlib/GeoGebra 同款思路）");
    }
}
