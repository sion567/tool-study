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
import java.util.Random;

/**
 * 随机游走·赌徒破产：本金 50 元、目标 100 元、每次 ±1 元（公平硬币）。
 * 理论：公平游戏中 P(先到 100) = 50/100 = 50%（与起点成比例）；期望游戏时长 = 50×50 = 2500 步。
 * 轨迹 = 一维随机游走 = 布朗运动的雏形（爱因斯坦 1905 解释花粉运动用的就是它）。
 * 固定种子 42 保证剧本可复现。
 */
public final class GamblersRuinDemo implements Algorithm {

    private static final String SCHEMA = "1";
    private static final String INVARIANT =
            "公平硬币：P(先到 100 | 本金 50) = 50/100 = 1/2；游走的期望时长 = 起点×终点差——'庄家优势'把这两个数都推向破产";

    private static final long SEED = 42;
    private static final int START = 50, GOAL = 100, MAX_STEPS = 1500;
    private static final int EMIT_EVERY = 100;

    private final List<Step> steps = new ArrayList<>();
    private final List<List<Double>> trace = new ArrayList<>();
    private int fortune, walkSteps;
    private String outcome = "进行中";

    @Override public String id() { return "math.gambler"; }
    @Override public String title() { return "随机游走·赌徒破产"; }
    @Override public String codeResource() { return "/code/GamblersRuinDemo.java"; }
    @Override public String docResource() { return "/markdown/gambler.md"; }

    @Override
    public Trace produce() {
        steps.clear();
        trace.clear();
        fortune = START;
        walkSteps = 0;
        outcome = "进行中";
        Random r = new Random(SEED);
        emit(4, List.of(VizEvent.of(VizEvent.EventType.INIT,
                "本金 " + START + " 元，目标翻倍到 " + GOAL + "，每局 ±1 元（公平硬币，种子 42）")),
                "理论预告：P(暴富) = 50%，期望时长 2500 局——轨迹会怎么走？");
        trace.add(List.of((double) walkSteps, (double) fortune));
        while (fortune > 0 && fortune < GOAL && walkSteps < MAX_STEPS) {
            walkSteps++;
            fortune += r.nextBoolean() ? 1 : -1;
            trace.add(List.of((double) walkSteps, (double) fortune));
            if (walkSteps % EMIT_EVERY == 0 || fortune == 0 || fortune == GOAL) {
                emit(8, List.of(VizEvent.of(VizEvent.EventType.SET,
                        String.format("第 %d 局：本金 %d 元%s", walkSteps, fortune,
                                fortune > START ? "（领先）" : fortune < START ? "（落后）" : ""))),
                        "随机游走没有记忆——过去领先/落后对未来零信息（赌徒谬误的解药）");
            }
            if (fortune == GOAL) {
                outcome = "暴富（达到 " + GOAL + "）";
            } else if (fortune == 0) {
                outcome = "破产（归零）";
            } else if (walkSteps >= MAX_STEPS) {
                outcome = MAX_STEPS + " 局未分胜负（仍在游走）";
            }
        }
        emit(12, List.of(VizEvent.of(VizEvent.EventType.MARK_SORTED,
                String.format("终局（第 %d 局）：%s——本金 %d 元", walkSteps, outcome, fortune))),
                "公平游戏里'先到 100'的概率 = 本金/目标 = 1/2；有庄家优势时它暴跌（见下）");
        emit(14, List.of(VizEvent.of(VizEvent.EventType.RETURN,
                "庄家优势 2% → P(暴富) = (1−(19/21)^50)/(1−(19/21)^100) ≈ 2%——绝望公式")),
                "凯利公式与止损线的数学基础：赌场不需要出千，'略占优 + 你破产即出局'就赢了全部");
        return new Trace(SCHEMA, meta(), steps);
    }

    private void emit(int line, List<VizEvent> events, String why) {
        StackFrame frame = new StackFrame("randomWalk",
                OrderedMaps.of("局数", Value.prim(walkSteps),
                        "本金", Value.prim(fortune),
                        "结局", Value.prim(outcome)), null);
        Map<String, Layer> layers = new LinkedHashMap<>();
        layers.put("mem.stack", new StackLayer(List.of(frame)));
        layers.put("mem.heap", new HeapLayer(snapshotHeap()));
        layers.put("math.plot", snapshotPlot());
        steps.add(new Step(steps.size(), Loc.line(line, why),
                Collections.unmodifiableMap(layers), List.copyOf(events), new Intent(why, INVARIANT)));
    }

    private Heap snapshotHeap() {
        Map<String, Value> fields = new LinkedHashMap<>();
        fields.put("本金", Value.prim(fortune));
        fields.put("局数", Value.prim(walkSteps));
        fields.put("结局", Value.prim(outcome));
        fields.put("理论 P(暴富)", Value.prim("50%"));
        Map<String, HeapObject> objs = new LinkedHashMap<>();
        objs.put("@1", new HeapObject("@1", "walk", "RandomWalk",
                Collections.unmodifiableMap(fields), List.of()));
        return new Heap(Collections.unmodifiableMap(objs));
    }

    private PlotView snapshotPlot() {
        List<List<Double>> pts = new ArrayList<>();
        for (List<Double> p : trace) pts.add(List.copyOf(p));
        List<String> labels = new ArrayList<>();
        labels.add("一维随机游走（种子 42）");
        labels.add(String.format("局数 %d：本金 %d", walkSteps, fortune));
        labels.add("结局：" + outcome);
        return new PlotView(PlotView.CARTESIAN, "赌徒破产：随机游走", walkSteps, fortune, walkSteps,
                0, Math.max(MAX_STEPS, walkSteps) * 1.02, -5, 105,
                Collections.unmodifiableList(pts),
                List.of(), List.of(), List.of(),
                Collections.unmodifiableList(labels));
    }

    private Trace.TraceMeta meta() {
        return new Trace.TraceMeta(id(), title(), codeResource(), "50 元 种子42", 1194);
    }

    @Override
    public java.util.List<String> dataLines() {
        return java.util.List.of("long seed = 42;   // 本金 50，目标 100");
    }

    @Override
    public Complexity complexity() {
        return new Complexity("O(期望时长) = 起点×终点差 = 2500 局", "O(t) 轨迹点",
                "随机游走 = 布朗运动 = 股价对数模型的原型——扩散、期权定价全在这一条锯齿线上");
    }
}
