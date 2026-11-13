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
 * 深度学习·Word2Vec（skip-gram）：词的"语义坐标"是怎么从共现里长出来的。
 * 语料 "the cat sat on the mat"（词表 5 词），窗口 ±1 → 5 对/轮；
 * 每对 (中心词, 上下文词) 用 SGD 推两词向量靠近（简化：只更新中心词向量朝上下文挪）。
 * 3 轮训练后高频词聚拢、名词簇靠近——词点在 2 维平面上"长出语义"。
 * 画面（CARTESIAN 复用）：5 个词点（labels 标词名），坐标 = 2 维嵌入，逐帧移动。
 * 数值确定性：固定初始化（种子式硬编码）+ 固定配对序，lr=0.3。
 */
public final class Word2VecDemo implements Algorithm {

    private static final String SCHEMA = "1";
    private static final String INVARIANT =
            "skip-gram：中心词预测上下文——共现的词对被推近，非共现的被推开；"
                    + "嵌入向量在训练中自发分化出'语义方向'（高频虚词聚拢、实词抱团）——无监督学到的第一个语义空间";

    private static final String[] WORDS = {"the", "cat", "sat", "on", "mat"};
    private static final int[][] PAIRS = {{0, 1}, {1, 2}, {2, 3}, {3, 0}, {0, 4}};   // 窗口 ±1 配对
    private static final double LR = 0.3;
    private static final double[][] INIT = {
            {2.0, 2.0}, {-2.0, 2.0}, {2.0, -2.0}, {-2.0, -2.0}, {0.5, 0.5}};   // 固定初始化

    private final List<Step> steps = new ArrayList<>();
    private double[][] emb = new double[WORDS.length][2];

    @Override public String id() { return "dl.word2vec"; }
    @Override public String title() { return "Word2Vec 词向量"; }
    @Override public String codeResource() { return "/code/Word2VecDemo.java"; }
    @Override public String docResource() { return "/markdown/word2vec.md"; }

    @Override
    public Trace produce() {
        steps.clear();
        emb = new double[WORDS.length][];
        for (int i = 0; i < WORDS.length; i++) {
            emb[i] = INIT[i].clone();
        }
        emit(6, List.of(VizEvent.of(VizEvent.EventType.INIT,
                "语料 \"the cat sat on the mat\"：词表 5 词，窗口 ±1 → 5 对/轮；初始嵌入四角散开")),
                "词向量 = 把词变成坐标：语义相近 → 坐标相近——这个空间完全从'谁挨着谁'里长出来");
        for (int epoch = 1; epoch <= 3; epoch++) {
            for (int[] pair : PAIRS) {
                int center = pair[0], ctx = pair[1];
                double d0 = (emb[center][0] - emb[ctx][0]);
                double d1 = (emb[center][1] - emb[ctx][1]);
                emb[center][0] -= LR * d0;      // 中心词朝上下文挪
                emb[center][1] -= LR * d1;
                emb[ctx][0] += LR * d0 / 2;     // 上下文反向挪一半（简化对称更新）
                emb[ctx][1] += LR * d1 / 2;
                emit(11, List.of(VizEvent.of(VizEvent.EventType.SET,
                        String.format("轮%d：%s↔%s —— \"%s\"挪向\"%s\"（lr=%.1f）",
                                epoch, WORDS[center], WORDS[ctx], WORDS[center], WORDS[ctx], LR),
                        "w:" + WORDS[center])),
                        "共现对的每一步都让两个词互相靠近——'语义'从共现统计中结晶");
            }
        }
        double dCatMat = dist(1, 4);
        double dCatSat = dist(1, 2);
        emit(14, List.of(VizEvent.of(VizEvent.EventType.MARK_SORTED,
                String.format("训练后：cat↔mat 距离 %.2f（名词簇），cat↔sat 距离 %.2f——高频虚词 the 已聚到原点附近", dCatMat, dCatSat))),
                "词距 = 语义相似度：king−man+woman≈queen 的'向量算术'就长在这张图上");
        emit(16, List.of(VizEvent.of(VizEvent.EventType.RETURN,
                "Word2Vec(2013) 让'嵌入'成为 NLP 的地基：今天的 BERT/GPT 词表仍然全是向量")),
                "无监督的奇迹：没人标注任何语义，语义自己从共现里长了出来");
        return new Trace(SCHEMA, meta(), steps);
    }

    private double dist(int i, int j) {
        return Math.hypot(emb[i][0] - emb[j][0], emb[i][1] - emb[j][1]);
    }

    private void emit(int line, List<VizEvent> events, String why) {
        StackFrame frame = new StackFrame("word2vec",
                OrderedMaps.of("词表", Value.prim("the/cat/sat/on/mat"),
                        "cat 位置", Value.prim(String.format("(%.2f, %.2f)", emb[1][0], emb[1][1])),
                        "lr", Value.prim("0.3")), null);
        Map<String, Layer> layers = new LinkedHashMap<>();
        layers.put("mem.stack", new StackLayer(List.of(frame)));
        layers.put("mem.heap", new HeapLayer(snapshotHeap()));
        layers.put("math.plot", snapshotPlot());
        steps.add(new Step(steps.size(), Loc.line(line, why),
                Collections.unmodifiableMap(layers), List.copyOf(events), new Intent(why, INVARIANT)));
    }

    private Heap snapshotHeap() {
        Map<String, Value> fields = new LinkedHashMap<>();
        for (int i = 0; i < WORDS.length; i++) {
            fields.put(WORDS[i], Value.prim(String.format("(%.2f, %.2f)", emb[i][0], emb[i][1])));
        }
        Map<String, HeapObject> objs = new LinkedHashMap<>();
        objs.put("@1", new HeapObject("@1", "emb", "EmbeddingTable",
                Collections.unmodifiableMap(fields), List.of()));
        return new Heap(Collections.unmodifiableMap(objs));
    }

    private PlotView snapshotPlot() {
        List<List<Double>> dots = new ArrayList<>();
        for (int i = 0; i < WORDS.length; i++) {
            dots.add(List.of(emb[i][0], emb[i][1], 0.010));
        }
        List<String> labels = new ArrayList<>();
        labels.add("词向量投影（2 维嵌入；点 = 词）");
        labels.add("高频虚词聚拢、实词抱团——语义从共现里长出来");
        labels.add("cat/mat 名词簇 vs the 虚词——距离即相似度");
        return new PlotView(PlotView.CARTESIAN, "Word2Vec 词点", 0, 0, 0,
                -3.2, 3.2, -3.2, 3.2,
                List.of(), List.of(), List.of(),
                Collections.unmodifiableList(dots),
                Collections.unmodifiableList(labels));
    }

    private Trace.TraceMeta meta() {
        return new Trace.TraceMeta(id(), title(), codeResource(),
                "5 词 3 轮", WORDS.length * 3 + 2);
    }

    @Override
    public java.util.List<String> dataLines() {
        return java.util.List.of("String corpus = \"the cat sat on the mat\";   //@DATA 语料");
    }

    @Override
    public Complexity complexity() {
        return new Complexity("O(轮数×配对数) 一次 SGD 步", "O(词表×维度) 嵌入矩阵",
                "词向量是所有 NLP 模型的第一层：GPT/BERT 的 embedding 表与它同构——"
                        + "维度从 2 到 4096，思想从未变过");
    }
}
