package com.algoviz.engine.pattern;

import com.algoviz.engine.Algorithm;
import com.algoviz.engine.Value;
import com.algoviz.engine.VizEvent;
import com.algoviz.engine.ds.SeqView;
import com.algoviz.engine.trace.Trace;

import java.util.List;

/** 中介者演示：N 个对象两两通信 = N(N-1) 根依赖；经中介 = N 根——网状变星形。 */
public final class MediatorDemo implements Algorithm {

    private static final String SCHEMA = "1";
    private static final String INVARIANT = "N 个对象两两通信 = N(N-1) 根依赖；经中介 = N 根——网状变星形";

    private static final int L_JOIN = 16;
    private static final int L_SEND = 17;
    private static final int L_COLLEAGUE = 12;
    private static final int L_BROADCAST = 6;

    private PatternEmitter em;

    @Override public String id() { return "pattern.mediator"; }
    @Override public String title() { return "中介者模式"; }
    @Override public String codeResource() { return "/code/MediatorDemo.java"; }
    @Override public String docResource() { return "/markdown/pattern-mediator.md"; }

    @Override
    public Trace produce() {
        em = new PatternEmitter(List.of(
                new SeqView.Participant("client", "client"),
                new SeqView.Participant("a", "a : Colleague(画布)"),
                new SeqView.Participant("mediator", "m : ChatMediator"),
                new SeqView.Participant("b", "b : Colleague(变量面板)")), INVARIANT);
        em.addObject("@1", "ChatMediator");
        em.addObject("@2", "Colleague");
        em.addObject("@3", "Colleague");
        em.setField("@2", "mediator", Value.ref("@1"));
        em.setField("@3", "mediator", Value.ref("@1"));
        em.setField("@1", "members", Value.prim("[画布, 变量面板]"));
        em.mainLocals.put("a", Value.ref("@2"));
        em.state(L_JOIN, VizEvent.EventType.INIT, "两个同事入群：都只认识 @1（互不认识）", List.of("@1"),
                "依赖画出来是星形——中介在中心，同事在辐条上");
        em.message(L_SEND, 0, 2, "a.send(\"我刷新了\")", "CALL", "A 要通知 B——但 A 手里根本没有 B 的引用");
        em.message(L_COLLEAGUE, 2, 1, "mediator.send(...)", "CALL", "A 只把消息交给中介");
        em.message(L_BROADCAST, 1, 3, "广播给 b", "CALL", "中介认识所有人：B 收到了——A 从没见过 B");
        em.message(L_BROADCAST, 3, 1, "收到", "RETURN", "B 的回执");
        em.message(L_COLLEAGUE, 1, 2, "完成", "RETURN", "A 的调用结束");
        em.state(L_SEND, VizEvent.EventType.RETURN, "加第 N 个同事：join 一行；同事两两之间的 N(N-1) 根线全消失", List.of("@1"),
                "代价：中介者容易变成上帝对象——它一坏全坏");
        return new Trace(SCHEMA, meta(new int[0]), em.steps);
    }
}
