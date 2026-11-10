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
 * 深度学习·Transformer 自注意力：三个词元（猫/坐/垫）的 self-attention 逐步演算。
 * 得分 = q·kᵀ/√d → softmax → 注意力权重 → 加权求和 V。
 * 本例 W_q=W_k=W_v=I（简化），嵌入 d=2：权重数字干净可手算。
 * 画面：词元嵌入 3×2、注意力权重 3×3（逐行点亮）、输出 3×2。
 */
public final class AttentionDemo implements Algorithm {

    private static final String SCHEMA = "1";
    private static final String INVARIANT =
            "每个词元用 q 查询所有 k，softmax 打分后加权求和 v——'关注谁、关注多少'由数据自己算出来";

    private static final String[] TOKENS = {"猫", "坐", "垫"};
    private static final double[][] E = {{1, 0}, {0, 1}, {1, 1}};   // 嵌入（d=2）

    private final List<Step> steps = new ArrayList<>();
    private final double[][] weights = new double[3][3];
    private int curRow = -1;

    @Override public String id() { return "dl.attention"; }
    @Override public String title() { return "Transformer 自注意力"; }
    @Override public String codeResource() { return "/code/AttentionDemo.java"; }
    @Override public String docResource() { return "/markdown/attention.md"; }

    private double dot(double[] a, double[] b) {
        return a[0] * b[0] + a[1] * b[1];
    }

    private double[] softmax(double[] s) {
        double max = Math.max(s[0], Math.max(s[1], s[2]));
        double e0 = Math.exp(s[0] - max), e1 = Math.exp(s[1] - max), e2 = Math.exp(s[2] - max);
        double sum = e0 + e1 + e2;
        return new double[]{e0 / sum, e1 / sum, e2 / sum};
    }

    @Override
    public Trace produce() {
        steps.clear();
        curRow = -1;
        emit(4, List.of(VizEvent.of(VizEvent.EventType.INIT,
                "词元 猫/坐/垫，嵌入 d=2（W_q=W_k=W_v=I 简化）——每个词既当查询又当键值")),
                "自注意力 = 序列内部互相'打量'：我是猫，动词和地点各该关注多少？");
        for (int q = 0; q < 3; q++) {
            curRow = q;
            double[] scores = new double[3];
            for (int k = 0; k < 3; k++) {
                scores[k] = dot(E[q], E[k]) / Math.sqrt(2);   // 除以 √d 防饱和
            }
            weights[q] = softmax(scores);
            emit(8, List.of(VizEvent.of(VizEvent.EventType.SET,
                    String.format("查询[%s]：得分/√2 = [%.2f, %.2f, %.2f] → softmax → [%.2f, %.2f, %.2f]",
                            TOKENS[q], scores[0], scores[1], scores[2],
                            weights[q][0], weights[q][1], weights[q][2]),
                    "W" + (q + 1))),
                    "softmax = '归一化 + 放大差距'：得分高的词被更多关注");
        }
        curRow = -1;
        // 输出：每行 = 权重 · V（V=E）
        double[][] out = new double[3][2];
        for (int q = 0; q < 3; q++) {
            for (int d = 0; d < 2; d++) {
                out[q][d] = weights[q][0] * E[0][d] + weights[q][1] * E[1][d] + weights[q][2] * E[2][d];
            }
        }
        emit(11, List.of(VizEvent.of(VizEvent.EventType.MARK_SORTED,
                String.format("输出[猫] = %.2f·猫 + %.2f·坐 + %.2f·垫 = (%.2f, %.2f)——'猫'已经吸收了上下文",
                        weights[0][0], weights[0][1], weights[0][2], out[0][0], out[0][1]))),
                "注意力的本质：每个位置的表示 = 全序列的加权混合——信息在词之间流动");
        emit(13, List.of(VizEvent.of(VizEvent.EventType.RETURN,
                "多头 = 并行多组 Q/K/V 各看各的角度；位置编码补上语序；堆叠 N 层 = Transformer")),
                "'Attention Is All You Need'（2017）：抛弃循环与卷积，只靠注意力——GPT/BERT 的地基");
        return new Trace(SCHEMA, meta(), steps);
    }

    private void emit(int line, List<VizEvent> events, String why) {
        StackFrame frame = new StackFrame("attention",
                OrderedMaps.of("查询行", Value.prim(curRow >= 0 ? TOKENS[curRow] : "全部"),
                        "权重行", Value.prim(curRow >= 0
                                ? String.format("[%.2f, %.2f, %.2f]", weights[curRow][0], weights[curRow][1], weights[curRow][2])
                                : "-")), null);
        Map<String, Layer> layers = new LinkedHashMap<>();
        layers.put("mem.stack", new StackLayer(List.of(frame)));
        layers.put("mem.heap", new HeapLayer(snapshotHeap()));
        layers.put("math.matrix", snapshotMatrix());
        steps.add(new Step(steps.size(), Loc.line(line, why),
                Collections.unmodifiableMap(layers), List.copyOf(events), new Intent(why, INVARIANT)));
    }

    private Heap snapshotHeap() {
        Map<String, Value> fields = new LinkedHashMap<>();
        fields.put("词元", Value.prim("猫 / 坐 / 垫"));
        fields.put("d", Value.prim("2（缩放 √2）"));
        fields.put("权重行 1", Value.prim(String.format("[%.2f, %.2f, %.2f]", weights[0][0], weights[0][1], weights[0][2])));
        Map<String, HeapObject> objs = new LinkedHashMap<>();
        objs.put("@1", new HeapObject("@1", "attention", "SelfAttention",
                Collections.unmodifiableMap(fields), List.of()));
        return new Heap(Collections.unmodifiableMap(objs));
    }

    private MathView snapshotMatrix() {
        List<MathView.Grid> grids = new ArrayList<>();
        // 嵌入
        List<String> ev = new ArrayList<>();
        for (double[] row : E) {
            ev.add(String.valueOf(row[0]));
            ev.add(String.valueOf(row[1]));
        }
        grids.add(new MathView.Grid("emb", "嵌入（猫/坐/垫 × d=2）", 3, 2,
                Collections.unmodifiableList(ev), null, null, Set.of(), Set.of()));
        // 注意力权重
        List<String> wv = new ArrayList<>();
        Set<Integer> active = new LinkedHashSet<>();
        Set<Integer> done = new LinkedHashSet<>();
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 3; c++) {
                int idx = r * 3 + c;
                if (r < curRow || curRow == -1) {
                    wv.add(String.format("%.2f", weights[r][c]));
                    done.add(idx);
                } else if (r == curRow) {
                    wv.add(String.format("%.2f", weights[r][c]));
                    active.add(idx);
                } else {
                    wv.add("?");
                }
            }
        }
        grids.add(new MathView.Grid("attn", "注意力权重（行=查询 猫/坐/垫）", 3, 3,
                Collections.unmodifiableList(wv), null, null,
                Collections.unmodifiableSet(active), Collections.unmodifiableSet(done)));
        // 输出
        List<String> ov = new ArrayList<>();
        for (int q = 0; q < 3; q++) {
            ov.add(String.format("%.2f", out(q, 0)));
            ov.add(String.format("%.2f", out(q, 1)));
        }
        grids.add(new MathView.Grid("out", "输出（加权和）", 3, 2,
                Collections.unmodifiableList(ov), null, null, Set.of(), Set.of()));
        return new MathView("自注意力", Collections.unmodifiableList(grids));
    }

    private double out(int q, int d) {
        return weights[q][0] * E[0][d] + weights[q][1] * E[1][d] + weights[q][2] * E[2][d];
    }


    private Trace.TraceMeta meta() {
        return new Trace.TraceMeta(id(), title(), codeResource(), "3 词元 d=2", 3);
    }

    @Override
    public java.util.List<String> dataLines() {
        return java.util.List.of("String[] tokens = {\"猫\", \"坐\", \"垫\"};   // d = 2");
    }

    @Override
    public Complexity complexity() {
        return new Complexity("O(n²·d) 每层（n² 个两两得分）", "O(n²) 注意力矩阵",
                "RNN 逐步滚动 → Transformer 全并行；序列 4K 时注意力矩阵 1600 万格——稀疏注意力/FlashAttention 都在治这个");
    }
}
