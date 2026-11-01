package com.algoviz.engine.pattern;

import com.algoviz.engine.Algorithm;
import com.algoviz.engine.Value;
import com.algoviz.engine.VizEvent;
import com.algoviz.engine.ds.SeqView;
import com.algoviz.engine.trace.Trace;

import java.util.List;

/**
 * 策略模式演示（蓝图 §5.2 第一批；反向引用：本项目 engine.Algorithm 接口）。
 * 画面核心：@1.strategy 的箭头从 @2 换到 @3——同一接口，运行时换实现，调用代码零改动。
 */
public final class StrategyDemo implements Algorithm {

    private static final String SCHEMA = "1";
    private static final String INVARIANT =
            "调用方只认 ctx.run()——运行时换成哪个策略，调用代码一行不改";

    /** 与 /code/StrategyDemo.java 完整清单的行号对齐 */
    private static final int L_ASSEMBLE = 23;
    private static final int L_RUN = 24;
    private static final int L_DELEGATE = 19;   // Context.run() 里的转发行
    private static final int L_SET = 25;
    private static final int L_FLIP = 18;       // setStrategy 的赋值行
    private static final int L_BUBBLE_BODY = 8;
    private static final int L_INSERT_BODY = 12;

    private PatternEmitter em;

    public StrategyDemo() {
        em = new PatternEmitter(List.of(
                new SeqView.Participant("client", "client"),
                new SeqView.Participant("ctx", "ctx : Context"),
                new SeqView.Participant("strategy", "strategy : Strategy")), INVARIANT);
    }

    @Override
    public String id() {
        return "pattern.strategy";
    }

    @Override
    public String title() {
        return "策略模式";
    }

    @Override
    public String codeResource() {
        return "/code/StrategyDemo.java";
    }

    @Override
    public String docResource() {
        return "/markdown/pattern-strategy.md";
    }

    @Override
    public Trace produce() {
        em.reset();
        em.addObject("@1", "Context");
        em.addObject("@2", "BubbleSort");
        em.setField("@1", "strategy", Value.ref("@2"));
        em.mainLocals.put("ctx", Value.ref("@1"));
        em.state(L_ASSEMBLE, VizEvent.EventType.INIT,
                "装配：new Context(new BubbleSort()) —— @1.strategy → @2", List.of("@1"),
                "构造器注入：Context 拿到策略引用，但不知道也不关心具体是谁");
        em.message(L_RUN, 0, 1, "run()", "CALL", "调用方只说 run()：请求交给 Context");
        em.message(L_DELEGATE, 1, 2, "execute()〔冒泡〕", "CALL", "Context 转发给策略对象——多态分发，编译期只认接口");
        em.message(L_BUBBLE_BODY, 2, 1, "逐帧 Step", "RETURN", "策略干完活，结果交回");
        em.addObject("@3", "InsertionSort");
        em.message(L_SET, 0, 1, "setStrategy(插入排序)", "CALL", "运行时换实现：接口不变，实现可换");
        em.setField("@1", "strategy", Value.ref("@3"));   // 箭头真的换位：状态与台词同步
        em.state(L_FLIP, VizEvent.EventType.SET,
                "@1.strategy: @2 → @3（箭头换位——策略模式的本体就这一下）", List.of("@1.strategy"),
                "同一接口、另一实现：行为改变，调用代码结构零改动");
        em.message(L_RUN + 2, 0, 1, "run()", "CALL", "再跑一遍：调用代码一行没改");
        em.message(L_DELEGATE, 1, 2, "execute()〔插入〕", "CALL", "这次转发到的是插入排序——箭头已经换了");
        em.message(L_INSERT_BODY, 2, 1, "逐帧 Step", "RETURN", "返回结果");
        return new Trace(SCHEMA, meta(new int[0]), em.steps);
    }
}
