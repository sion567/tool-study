package com.algoviz.engine.pattern;

import com.algoviz.engine.Algorithm;
import com.algoviz.engine.Loc;
import com.algoviz.engine.Step;
import com.algoviz.engine.VizEvent;
import com.algoviz.engine.ds.SeqView;
import com.algoviz.engine.trace.Trace;

import java.util.List;

/**
 * 重构·提取方法（Extract Method）：把一段代码变成名字讲得清它的方法。
 * 反例：20 行方法混「校验 + 计算 + 打印」三件事，读的时候大脑要同时开三条线程。
 * 正例：切成三个小方法——读调用处像读摘要，方法名就是注释。
 */
public final class RefactorExtractDemo implements Algorithm {

    private static final String SCHEMA = "1";
    private static final String INVARIANT = "提取的判据：你能给这段代码起一个**不带「和」**的名字——起得出就提得出";

    private static final int L_BAD = 4;
    private static final int L_PAIN = 6;
    private static final int L_GOOD = 12;
    private static final int L_GAIN = 14;

    private PatternEmitter em;

    @Override public String id() { return "refactor.extract"; }
    @Override public String title() { return "重构·提取方法"; }
    @Override public String codeResource() { return "/code/RefactorExtractDemo.java"; }
    @Override public String docResource() { return "/markdown/refactor-extract.md"; }

    @Override
    public Trace produce() {
        em = new PatternEmitter(List.of(
                new SeqView.Participant("reader", "读代码的人"),
                new SeqView.Participant("blob", "process() 20 行"),
                new SeqView.Participant("small", "validate / compute / print")), INVARIANT);
        em.reset();
        // ===== 反例：一坨 =====
        em.message(L_BAD, 0, 1, "读 process()：校验在哪？算什么？为什么打印？——从头啃到尾", "CALL",
                "反例：一段长方法，三层缩进里混着三件事——读懂一次要花 5 分钟，下次还得重读");
        em.state(L_PAIN, VizEvent.EventType.COMPARE,
                "改打印格式 → 得先在 20 行里翻出「哪几行是打印」——改动半径=整个方法", List.of(),
                "长方法的成本不在写，在**每一次读和改**");
        // ===== 正例：切碎 =====
        em.message(L_GOOD, 0, 2, "读 process()：validate() → compute() → print()（读名字就懂了）", "CALL",
                "正例：调用处 3 行 = 方法名组成的摘要——main 逻辑 5 秒读完");
        em.state(L_GAIN, VizEvent.EventType.RETURN,
                "改打印 → 直奔 print() 一个方法；改规则 → 直奔 validate()", List.of(),
                "行为零变化（重构的定义），但每次阅读/修改的成本从「全方法」缩到「单方法」");
        return new Trace(SCHEMA, meta(), em.steps);
    }

    private Trace.TraceMeta meta() {
        return new Trace.TraceMeta(id(), title(), codeResource(), "20 行 → 3×N 行", 2);
    }

    @Override
    public java.util.List<String> dataLines() {
        return List.of("// 反例: void process(){...20 行...} → 正例: validate(); compute(); print();");
    }

    @Override
    public com.algoviz.engine.Complexity complexity() {
        return new com.algoviz.engine.Complexity("O(1) 每次多一层调用", "O(1) 栈深 +1",
                "重构第一招，其余招式的地基——代码首先得读得懂，才谈得上改得动");
    }
}
