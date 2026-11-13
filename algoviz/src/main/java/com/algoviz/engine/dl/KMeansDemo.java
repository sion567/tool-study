package com.algoviz.engine.dl;

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
 * 深度学习·K-Means 聚类：无监督的"物以类聚"——质心在点云上滚动收敛。
 * 12 个点（3 簇固定数据）+ 3 个初始质心（固定布局）：分配（就近归属）→ 更新（质心=簇均值）交替迭代。
 * 质心移动距离逐轮递减——收敛可视化。
 * 画面（CARTESIAN 复用）：蓝点 = 数据点，橙大点 = 质心，labels 标注迭代轮次。
 */
public final class KMeansDemo implements Algorithm {

    private static final String SCHEMA = "1";
    private static final String INVARIANT =
            "两步交替：分配（每个点归最近的质心）→ 更新（质心 = 簇内均值）——"
                    + "每轮的目标函数（簇内平方和）单调下降，质心移动距离递减 → 收敛到局部最优";

    private static final double[][] PTS = {
            {1.0, 1.5}, {1.3, 1.1}, {0.8, 1.8}, {1.1, 1.3},       // 簇 1 左下
            {4.0, 4.2}, {4.3, 3.8}, {3.8, 4.5},                   // 簇 2 右上
            {1.2, 4.0}, {0.9, 4.3}, {1.5, 4.5},                   // 簇 3 左上
            {4.1, 1.0}, {4.4, 1.3}};                              // 簇 4 右下（兼作第 3 簇的分散点）
    private static final double[][] INIT_C = {{1.0, 4.0}, {4.0, 1.0}, {4.0, 4.0}};
    private static final int K = 3;

    private final List<Step> steps = new ArrayList<>();
    private final double[][] centroids = new double[K][2];
    private final double[][] moves = new double[4][K];   // 每轮质心移动距离
    private int iter;

    @Override public String id() { return "dl.kmeans"; }
    @Override public String title() { return "K-Means 聚类"; }
    @Override public String codeResource() { return "/code/KMeansDemo.java"; }
    @Override public String docResource() { return "/markdown/kmeans.md"; }

    @Override
    public Trace produce() {
        steps.clear();
        for (int i = 0; i < K; i++) {
            centroids[i] = INIT_C[i].clone();
        }
        iter = 0;
        emit(7, List.of(VizEvent.of(VizEvent.EventType.INIT,
                "12 个点散布四角，3 个初始质心（橙大点）——分配与更新即将交替")),
                "K-Means 是无监督的代表：没人告诉它'答案有几个类'——K 是超参数，类是自己长出来的");
        for (int it = 0; it < 4; it++) {
            iter = it + 1;
            double moved = iterate();
            for (int i = 0; i < K; i++) {
                moves[it][i] = moved;
            }
            emit(12, List.of(VizEvent.of(VizEvent.EventType.SET,
                    String.format("第 %d 轮：分配 + 更新完成——质心最大移动 %.3f", iter, moved))),
                    moved > 0.1 ? "质心在挪：均值把每个质心拉向自己簇的重心——点云的'引力'在起作用"
                            : "移动 < 0.1：收敛——质心找到了各自的簇重心");
        }
        emit(15, List.of(VizEvent.of(VizEvent.EventType.MARK_SORTED,
                "收敛：12 个点归属 3 簇——簇内平方和最小化的局部最优解")),
                "K-Means 只保证局部最优：初始质心决定结局——k-means++ 的聪明在于让初始质心互相远离");
        emit(17, List.of(VizEvent.of(VizEvent.EventType.RETURN,
                "聚类的应用：用户分群、图像压缩（颜色聚类）、异常检测——'物以类聚'的算法化")),
                "与监督学习对照：没有标签，目标函数'簇内平方和'自己定义了什么叫好");
        return new Trace(SCHEMA, meta(), steps);
    }

    /** 一轮迭代：分配 + 更新，返回质心最大移动距离。 */
    private double iterate() {
        double maxMove = 0;
        // 更新前先按当前质心分配（分配逻辑隐含在均值计算里：每轮直接按最近质心分簇求均值）
        double[][] sums = new double[K][3];   // x 和、y 和、计数
        for (double[] p : PTS) {
            int best = 0;
            double bestD = Double.MAX_VALUE;
            for (int i = 0; i < K; i++) {
                double d = (p[0] - centroids[i][0]) * (p[0] - centroids[i][0])
                        + (p[1] - centroids[i][1]) * (p[1] - centroids[i][1]);
                if (d < bestD) {
                    bestD = d;
                    best = i;
                }
            }
            sums[best][0] += p[0];
            sums[best][1] += p[1];
            sums[best][2]++;
        }
        for (int i = 0; i < K; i++) {
            if (sums[i][2] == 0) {
                continue;
            }
            double nx = sums[i][0] / sums[i][2];
            double ny = sums[i][1] / sums[i][2];
            double move = Math.hypot(nx - centroids[i][0], ny - centroids[i][1]);
            maxMove = Math.max(maxMove, move);
            centroids[i][0] = nx;
            centroids[i][1] = ny;
        }
        return maxMove;
    }

    private void emit(int line, List<VizEvent> events, String why) {
        StackFrame frame = new StackFrame("kmeans",
                OrderedMaps.of("迭代", Value.prim(String.valueOf(iter)),
                        "质心1", Value.prim(String.format("(%.2f, %.2f)", centroids[0][0], centroids[0][1])),
                        "质心2", Value.prim(String.format("(%.2f, %.2f)", centroids[1][0], centroids[1][1])),
                        "质心3", Value.prim(String.format("(%.2f, %.2f)", centroids[2][0], centroids[2][1]))), null);
        Map<String, Layer> layers = new LinkedHashMap<>();
        layers.put("mem.stack", new StackLayer(List.of(frame)));
        layers.put("mem.heap", new HeapLayer(snapshotHeap()));
        layers.put("math.plot", snapshotPlot());
        steps.add(new Step(steps.size(), Loc.line(line, why),
                Collections.unmodifiableMap(layers), List.copyOf(events), new Intent(why, INVARIANT)));
    }

    private Heap snapshotHeap() {
        Map<String, Value> fields = new LinkedHashMap<>();
        fields.put("K", Value.prim("3"));
        fields.put("迭代轮", Value.prim(String.valueOf(iter)));
        for (int i = 0; i < K; i++) {
            fields.put("质心" + (i + 1), Value.prim(String.format("(%.2f, %.2f)", centroids[i][0], centroids[i][1])));
        }
        Map<String, HeapObject> objs = new LinkedHashMap<>();
        objs.put("@1", new HeapObject("@1", "kmeans", "ClusterState",
                Collections.unmodifiableMap(fields), List.of()));
        return new Heap(Collections.unmodifiableMap(objs));
    }

    private PlotView snapshotPlot() {
        List<List<Double>> dots = new ArrayList<>();
        for (double[] p : PTS) {
            dots.add(List.of(p[0], p[1], 0.006));
        }
        for (int i = 0; i < K; i++) {
            dots.add(List.of(centroids[i][0], centroids[i][1], 0.016));   // 质心大点
        }
        List<String> labels = new ArrayList<>();
        labels.add(String.format("K-Means：12 点 3 质心（橙大点），第 %d 轮", iter));
        labels.add("蓝点=数据，橙大点=质心——每轮质心被拉向簇重心");
        labels.add("分配→更新交替：目标函数（簇内平方和）单调下降");
        return new PlotView(PlotView.CARTESIAN, "K-Means 质心收敛", 0, 0, 0,
                -0.5, 5.0, -0.5, 5.0,
                List.of(), List.of(), List.of(),
                Collections.unmodifiableList(dots),
                Collections.unmodifiableList(labels));
    }

    private Trace.TraceMeta meta() {
        return new Trace.TraceMeta(id(), title(), codeResource(),
                "12 点 3 簇", 8);
    }

    @Override
    public java.util.List<String> dataLines() {
        return java.util.List.of("int k = 3;   //@DATA 聚类数");
    }

    @Override
    public Complexity complexity() {
        return new Complexity("O(轮数×K×N) 分配主导", "O(K×N) 点归属",
                "图像压缩里 K=16 的颜色聚类能把真彩图压成 4bit——'聚类'是最古老的无监督思想");
    }
}
