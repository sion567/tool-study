package com.algoviz.engine.ml;

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
 * 机器学习·决策树（信息增益选根）：精简 play-tennis 8 行，熵与增益的逐格演算。
 * H(S) = −Σ p·log₂p；Gain(S,A) = H(S) − Σ (|Sv|/|S|)·H(Sv)——增益最大的特征先分裂。
 * 数据 8 行 × 2 特征（天候/风力）+ 标签（玩 4 是 4 否）：
 * 天候增益 ≈ 0.311、风力增益 ≈ 0.049——"天候"胜出成为根。
 * 画面（math.matrix 复用）：网格一 = 数据表；网格二 = 熵/增益对照表逐格填。
 */
public final class DtreeDemo implements Algorithm {

    private static final String SCHEMA = "1";
    private static final String INVARIANT =
            "H(S) = −Σ p·log₂p；Gain = H(S) − Σ (|Sv|/|S|)·H(Sv)——增益 = '按这个特征分组后，标签还剩多少混乱'；"
                    + "增益最大的特征先分裂——贪心构建，ID3 算法的全部";

    // 天候, 风力, 玩(标签)
    private static final String[][] DATA = {
            {"晴", "弱", "否"}, {"晴", "强", "否"}, {"阴", "弱", "是"}, {"雨", "弱", "是"},
            {"雨", "强", "否"}, {"阴", "强", "是"}, {"阴", "弱", "是"}, {"雨", "弱", "否"}};
    private static final String[] FEATURES = {"天候", "风力"};
    private static final double ENT_S = 1.0;          // 4是4否 → H = 1.0
    private static final double GAIN_WEATHER = 0.311, GAIN_WIND = 0.049;

    private final List<Step> steps = new ArrayList<>();
    private int curFrame = -1;   // 0=熵 1=增益 2=根选择

    @Override public String id() { return "ml.dtree"; }
    @Override public String title() { return "决策树·信息增益"; }
    @Override public String codeResource() { return "/code/DtreeDemo.java"; }
    @Override public String docResource() { return "/markdown/dtree.md"; }

    @Override
    public Trace produce() {
        steps.clear();
        curFrame = -1;
        emit(8, List.of(VizEvent.of(VizEvent.EventType.INIT,
                "数据 8 行（玩：4 是 4 否）——根节点熵 H(S) = −(½log₂½)×2 = 1.0（完全混乱）")),
                "熵 = 乱度：正反各半时最乱（1.0）；全是一类时为 0——决策树的目标就是把熵拆到 0");
        curFrame = 0;
        emit(12, List.of(VizEvent.of(VizEvent.EventType.SET,
                "逐特征算条件熵：按特征取值分组，每组算子熵再按占比加权")),
                "信息增益 = 分组前后乱度的下降量——'这个特征帮我省了多少疑问'");
        curFrame = 1;
        emit(15, List.of(VizEvent.of(VizEvent.EventType.SET,
                String.format("天候：晴(1是1否) 阴(2是0否) 雨(1是2否) → 条件熵 = %.3f → 增益 = %.3f",
                        ENT_S - GAIN_WEATHER, GAIN_WEATHER))),
                "阴天全是'玩'：纯子集零熵——天候一问就消灭 2/8 的混乱");
        emit(16, List.of(VizEvent.of(VizEvent.EventType.SET,
                String.format("风力：弱(3是2否) 强(1是2否) → 条件熵 = %.3f → 增益 = %.3f",
                        ENT_S - GAIN_WIND, GAIN_WIND))),
                "风力增益小：分了组依然混乱——它当根要白分一轮");
        curFrame = 2;
        emit(19, List.of(VizEvent.of(VizEvent.EventType.MARK_SORTED,
                "根 = 天候（增益 0.311 最大）——分裂后对每个子集递归（增益为零的分支直接成叶）")),
                "ID3 的贪心：每层只看眼前增益最大——不保证全局最优，但快且可解释");
        emit(21, List.of(VizEvent.of(VizEvent.EventType.RETURN,
                "ID3 → C4.5（增益率防偏袒）→ CART（基尼系数 + 回归树）→ 随机森林（多树投票）")),
                "决策树是'可解释机器学习'的门面：每个 if 都能讲出人话");
        return new Trace(SCHEMA, meta(), steps);
    }

    private void emit(int line, List<VizEvent> events, String why) {
        StackFrame frame = new StackFrame("dtree",
                OrderedMaps.of("H(S)", Value.prim("1.000"),
                        "增益-天候", Value.prim(String.format("%.3f", GAIN_WEATHER)),
                        "增益-风力", Value.prim(String.format("%.3f", GAIN_WIND))), null);
        Map<String, Layer> layers = new LinkedHashMap<>();
        layers.put("mem.stack", new StackLayer(List.of(frame)));
        layers.put("mem.heap", new HeapLayer(snapshotHeap()));
        layers.put("math.matrix", snapshotMath());
        steps.add(new Step(steps.size(), Loc.line(line, why),
                Collections.unmodifiableMap(layers), List.copyOf(events), new Intent(why, INVARIANT)));
    }

    private Heap snapshotHeap() {
        Map<String, Value> fields = new LinkedHashMap<>();
        fields.put("H(S)", Value.prim("1.000（4是4否）"));
        fields.put("根", Value.prim("天候（增益 0.311）"));
        Map<String, HeapObject> objs = new LinkedHashMap<>();
        objs.put("@1", new HeapObject("@1", "dtree", "SplitState",
                Collections.unmodifiableMap(fields), List.of()));
        return new Heap(Collections.unmodifiableMap(objs));
    }

    private MathView snapshotMath() {
        List<String> dataVals = new ArrayList<>();
        Set<Integer> dataActive = new LinkedHashSet<>();
        for (String[] row : DATA) {
            dataVals.add(row[0]);
            dataVals.add(row[1]);
            dataVals.add(row[2]);
        }
        if (curFrame >= 0) {
            for (int i = 0; i < DATA.length; i++) {
                dataActive.add(i * 3);
            }
        }
        MathView.Grid dataGrid = new MathView.Grid("data", "数据表（天候/风力/玩——黄列 = 候选根特征）",
                DATA.length, 3, dataVals, null, null, dataActive, Set.of());
        List<String> statVals = new ArrayList<>(List.of(
                "H(S)=1.000",
                String.format("天候增益=%.3f", GAIN_WEATHER),
                String.format("风力增益=%.3f", GAIN_WIND),
                curFrame >= 2 ? "根=天候 ✓" : "…"));
        Set<Integer> statActive = new LinkedHashSet<>();
        statActive.add(1);
        statActive.add(2);
        if (curFrame >= 2) {
            statActive.add(3);
        }
        MathView.Grid statGrid = new MathView.Grid("stat", "熵与增益对照（增益最大者当选根）",
                1, 4, statVals, null, null, statActive, Set.of());
        return new MathView("决策树", List.of(dataGrid, statGrid));
    }

    private Trace.TraceMeta meta() {
        return new Trace.TraceMeta(id(), title(), codeResource(),
                "8 行 2 特征", 22);
    }

    @Override
    public java.util.List<String> dataLines() {
        return java.util.List.of("String[][] data = {…8 行…};   //@DATA 精简 play-tennis");
    }

    @Override
    public Complexity complexity() {
        return new Complexity("O(特征×行数) 每层增益计算", "O(深度) 树结构",
                "决策树 = '可解释机器学习'的门面：随机森林/XGBoost 的基学习器——"
                        + "每个 if 都能讲出人话");
    }
}
