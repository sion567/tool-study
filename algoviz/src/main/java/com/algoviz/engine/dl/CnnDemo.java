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
 * 深度学习·CNN 卷积与池化：6×6 输入 × 3×3 垂直边缘核 → 4×4 特征图 → 2×2 最大池化。
 * 输入左半 1 / 右半 5（一根垂直边缘）：卷积核 [[-1,0,1]×3] 在边界列输出 +12，其余 0——
 * 卷积 = 滑动窗口的点积；池化 = 保留最强响应（平移容忍）。
 */
public final class CnnDemo implements Algorithm {

    private static final String SCHEMA = "1";
    private static final String INVARIANT =
            "卷积 = 核在输入上滑动做点积（权重共享：一个核扫全图）——边缘检测核让边界位置亮、平坦位置归零";

    private static final int IN = 6;     // 输入 6×6
    private static final int K = 3;      // 核 3×3
    private static final int OUT = IN - K + 1;   // 4

    private static final int[][] KERNEL = {{-1, 0, 1}, {-1, 0, 1}, {-1, 0, 1}};

    private final List<Step> steps = new ArrayList<>();
    private final int[][] input = new int[IN][IN];
    private final int[][] fmap = new int[OUT][OUT];
    private int curR = -1, curC = -1;
    private int curP = -1;   // 池化输出下标

    @Override public String id() { return "dl.cnn"; }
    @Override public String title() { return "CNN 卷积与池化"; }
    @Override public String codeResource() { return "/code/CnnDemo.java"; }
    @Override public String docResource() { return "/markdown/cnn.md"; }

    @Override
    public Trace produce() {
        steps.clear();
        curR = curC = curP = -1;
        for (int r = 0; r < IN; r++) {
            for (int c = 0; c < IN; c++) {
                input[r][c] = c < 3 ? 1 : 5;   // 左半 1，右半 5 → 垂直边缘在列 2|3 之间
            }
        }
        emit(5, List.of(VizEvent.of(VizEvent.EventType.INIT,
                "输入 6×6（左半 1 右半 5 = 一根垂直边缘）× 核 3×3（[-1,0,1] 行 ×3）")),
                "核的三个行都是 [-1,0,1]：左减右——窗口跨在边缘上时输出最大");
        // 卷积：16 个输出格
        for (int r = 0; r < OUT; r++) {
            for (int c = 0; c < OUT; c++) {
                int sum = 0;
                for (int kr = 0; kr < K; kr++) {
                    for (int kc = 0; kc < K; kc++) {
                        sum += input[r + kr][c + kc] * KERNEL[kr][kc];
                    }
                }
                fmap[r][c] = sum;
                curR = r;
                curC = c;
                emit(9, List.of(VizEvent.of(VizEvent.EventType.SET,
                        String.format("窗口(%d,%d) 点积 → 特征图(%d,%d) = %d%s", r, c, r, c, sum,
                                sum != 0 ? "  ← 边缘响应！" : ""),
                        "R" + (r + 1) + "C" + (c + 1), "K", "F" + (r + 1) + "C" + (c + 1))),
                        sum != 0 ? "窗口左半值 ≠ 右半值 → 有边缘 → 强响应"
                                : "窗口左右两侧值相等 → 平坦区域 → 归零");
            }
        }
        curR = curC = -1;
        emit(12, List.of(VizEvent.of(VizEvent.EventType.MARK_SORTED,
                "卷积完成：只有边界列 = +12，其余全 0——核把'边缘'这一个概念变成了一个数字")),
                "参数共享：16 个输出共用同一 3×3=9 个权重——对比全连接 36×16=576 个");
        // 最大池化 2×2 步幅 2
        int p = 0;
        for (int pr = 0; pr < OUT / 2; pr++) {
            for (int pc = 0; pc < OUT / 2; pc++) {
                int max = Math.max(Math.max(fmap[2 * pr][2 * pc], fmap[2 * pr][2 * pc + 1]),
                        Math.max(fmap[2 * pr + 1][2 * pc], fmap[2 * pr + 1][2 * pc + 1]));
                curP = p++;
                emit(15, List.of(VizEvent.of(VizEvent.EventType.SET,
                        String.format("池化块(%d,%d)：max{%d,%d,%d,%d} = %d",
                                pr, pc, fmap[2 * pr][2 * pc], fmap[2 * pr][2 * pc + 1],
                                fmap[2 * pr + 1][2 * pc], fmap[2 * pr + 1][2 * pc + 1], max),
                        "P" + (pr + 1) + "C" + (pc + 1))),
                        "最大池化：只留最强响应——尺寸减半、轻微平移不再敏感");
            }
        }
        curP = -1;
        emit(18, List.of(VizEvent.of(VizEvent.EventType.RETURN,
                "流水线：卷积(提特征) → 池化(压缩) → 卷积 → 池化 → … → 全连接(分类)")),
                "CNN 的全部直觉：局部感受野 + 权重共享 + 池化——层层堆叠出'从像素到语义'");
        return new Trace(SCHEMA, meta(), steps);
    }

    private void emit(int line, List<VizEvent> events, String why) {
        StackFrame frame = new StackFrame("cnn",
                OrderedMaps.of("阶段", Value.prim(curP < 0 ? "卷积" : "池化"),
                        "输出特征图(2,2)", Value.prim("12 在边界列")), null);
        Map<String, Layer> layers = new LinkedHashMap<>();
        layers.put("mem.stack", new StackLayer(List.of(frame)));
        layers.put("mem.heap", new HeapLayer(snapshotHeap()));
        layers.put("math.matrix", snapshotMatrix());
        steps.add(new Step(steps.size(), Loc.line(line, why),
                Collections.unmodifiableMap(layers), List.copyOf(events), new Intent(why, INVARIANT)));
    }

    private Heap snapshotHeap() {
        Map<String, Value> fields = new LinkedHashMap<>();
        fields.put("输入", Value.prim(IN + "×" + IN));
        fields.put("核", Value.prim(K + "×" + K + " = 9 个权重（共享）"));
        fields.put("特征图", Value.prim(OUT + "×" + OUT));
        Map<String, HeapObject> objs = new LinkedHashMap<>();
        objs.put("@1", new HeapObject("@1", "cnn", "CnnState",
                Collections.unmodifiableMap(fields), List.of()));
        return new Heap(Collections.unmodifiableMap(objs));
    }

    private MathView snapshotMatrix() {
        List<MathView.Grid> grids = new ArrayList<>();
        // 输入
        List<String> iv = new ArrayList<>();
        Set<Integer> iActive = new LinkedHashSet<>();
        for (int r = 0; r < IN; r++) {
            for (int c = 0; c < IN; c++) {
                iv.add(String.valueOf(input[r][c]));
            }
        }
        for (int kr = 0; kr < K; kr++) {
            for (int kc = 0; kc < K; kc++) {
                int r = curR + kr, c = curC + kc;
                if (curR >= 0 && r < IN && c < IN) {
                    iActive.add(r * IN + c);
                }
            }
        }
        grids.add(new MathView.Grid("input", "输入 6×6", IN, IN,
                Collections.unmodifiableList(iv), null, null,
                Collections.unmodifiableSet(iActive), Set.of()));
        // 核
        List<String> kv = new ArrayList<>();
        for (int[] row : KERNEL) {
            for (int v : row) kv.add(String.valueOf(v));
        }
        grids.add(new MathView.Grid("kernel", "核 3×3", K, K,
                Collections.unmodifiableList(kv), null, null, Set.of(), Set.of()));
        // 特征图
        List<String> fv = new ArrayList<>();
        Set<Integer> fActive = new LinkedHashSet<>();
        Set<Integer> fDone = new LinkedHashSet<>();
        for (int r = 0; r < OUT; r++) {
            for (int c = 0; c < OUT; c++) {
                int idx = r * OUT + c;
                fv.add(curR >= r || (curR == -1) ? String.valueOf(fmap[r][c]) : "?");
                if (curR >= r || curR == -1) fDone.add(idx);
                if (curR == r && curC == c) fActive.add(idx);
            }
        }
        grids.add(new MathView.Grid("fmap", "特征图 4×4", OUT, OUT,
                Collections.unmodifiableList(fv), null, null,
                Collections.unmodifiableSet(fActive), Collections.unmodifiableSet(fDone)));
        // 池化
        int[][] pooled = new int[2][2];
        for (int pr = 0; pr < 2; pr++) {
            for (int pc = 0; pc < 2; pc++) {
                pooled[pr][pc] = Math.max(Math.max(fmap[2 * pr][2 * pc], fmap[2 * pr][2 * pc + 1]),
                        Math.max(fmap[2 * pr + 1][2 * pc], fmap[2 * pr + 1][2 * pc + 1]));
            }
        }
        List<String> pv = new ArrayList<>();
        Set<Integer> pActive = new LinkedHashSet<>();
        Set<Integer> pDone = new LinkedHashSet<>();
        for (int r = 0; r < 2; r++) {
            for (int c = 0; c < 2; c++) {
                int idx = r * 2 + c;
                pv.add(curP >= idx || curP == -1 ? String.valueOf(pooled[r][c]) : "?");
                if (curP >= idx || curP == -1) pDone.add(idx);
                if (curP == idx) pActive.add(idx);
            }
        }
        grids.add(new MathView.Grid("pool", "池化 2×2", 2, 2,
                Collections.unmodifiableList(pv), null, null,
                Collections.unmodifiableSet(pActive), Collections.unmodifiableSet(pDone)));
        return new MathView("CNN", Collections.unmodifiableList(grids));
    }

    private Trace.TraceMeta meta() {
        return new Trace.TraceMeta(id(), title(), codeResource(), "6×6 核3×3", 16);
    }

    @Override
    public java.util.List<String> dataLines() {
        return java.util.List.of("int in = 6, k = 3;   // 左半 1 右半 5（垂直边缘）");
    }

    @Override
    public Complexity complexity() {
        return new Complexity("卷积 O(k²) 每输出格（总 O(n²k²)）", "O(9) 核权重共享",
                "参数量 9 个 vs 全连接 576 个——共享+局部性让 CNN 在图像上又小又准");
    }
}
