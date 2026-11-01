package com.algoviz.engine.pattern;

import com.algoviz.engine.Algorithm;
import com.algoviz.engine.Value;
import com.algoviz.engine.VizEvent;
import com.algoviz.engine.ds.SeqView;
import com.algoviz.engine.trace.Trace;

import java.util.List;

/**
 * 观察者模式演示（蓝图 §5.2 第一批；反向引用：本项目引擎吐 Step → 各 UI 面板刷新）。
 * 画面核心：一条 emitStep 广播给两个订阅者——Subject 不关心谁在听、有多少个。
 */
public final class ObserverDemo implements Algorithm {

    private static final String SCHEMA = "1";
    private static final String INVARIANT =
            "Subject 只管广播：一次 emit，所有订阅者按订阅顺序被通知，双向解耦";

    /** 与 /code/ObserverDemo.java 完整清单的行号对齐 */
    private static final int L_NEW = 24;
    private static final int L_SUB1 = 25;
    private static final int L_ADD = 9;         // subscribe 的名单加一行
    private static final int L_SUB2 = 26;
    private static final int L_EMIT = 27;
    private static final int L_LOOP = 11;       // 广播的那一行 for

    private PatternEmitter em;

    public ObserverDemo() {
        em = new PatternEmitter(List.of(
                new SeqView.Participant("client", "client"),
                new SeqView.Participant("engine", "engine : Subject"),
                new SeqView.Participant("canvas", "canvas : Observer"),
                new SeqView.Participant("vars", "vars : Observer")), INVARIANT);
    }

    @Override
    public String id() {
        return "pattern.observer";
    }

    @Override
    public String title() {
        return "观察者模式";
    }

    @Override
    public String codeResource() {
        return "/code/ObserverDemo.java";
    }

    @Override
    public String docResource() {
        return "/markdown/pattern-observer.md";
    }

    @Override
    public Trace produce() {
        em.reset();
        em.addObject("@1", "Engine");
        em.setField("@1", "observers", Value.prim("[]"));
        em.mainLocals.put("engine", Value.ref("@1"));
        em.state(L_NEW, VizEvent.EventType.INIT,
                "new Engine()：@1.observers = []（还没有人订阅）", List.of("@1"),
                "被观察者只维护一个订阅名单，别的什么都不管");
        em.addObject("@2", "CanvasPanel");
        em.message(L_SUB1, 0, 1, "subscribe(canvas)", "CALL", "订阅：观察者把自己登记进名单");
        em.setField("@1", "observers", Value.prim("[@2]"));
        em.state(L_ADD, VizEvent.EventType.SET, "@1.observers += @2（现在 1 个）", List.of("@1"),
                "名单变长——Subject 与 Observer 从此只通过名单发生关系");
        em.addObject("@3", "VarsPanel");
        em.message(L_SUB2, 0, 1, "subscribe(vars)", "CALL", "再订阅一个：随时可以加");
        em.setField("@1", "observers", Value.prim("[@2, @3]"));
        em.state(L_ADD, VizEvent.EventType.SET, "@1.observers += @3（现在 2 个）", List.of("@1"),
                "加观察者不需要改 Subject 的一行代码");
        em.message(L_EMIT, 0, 1, "emitStep(step42)", "CALL", "事件来了：Subject 只做一件事——遍历名单广播");
        em.message(L_LOOP, 1, 2, "update(step42)", "CALL", "多方响应①：动画面板刷新");
        em.message(L_LOOP, 1, 3, "update(step42)", "CALL", "多方响应②：变量面板刷新（顺序 = 订阅顺序）");
        em.state(L_EMIT, VizEvent.EventType.RETURN,
                "广播完毕：同一事件，N 个观察者各自响应——这就是「一发，多方响应」", List.of("@1"),
                "广播结束：新增第三种面板？subscribe 一行，其他不变");
        return new Trace(SCHEMA, meta(new int[0]), em.steps);
    }
}
