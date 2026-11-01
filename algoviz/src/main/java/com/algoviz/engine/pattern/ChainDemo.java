package com.algoviz.engine.pattern;

import com.algoviz.engine.Algorithm;
import com.algoviz.engine.Value;
import com.algoviz.engine.VizEvent;
import com.algoviz.engine.ds.SeqView;
import com.algoviz.engine.trace.Trace;

import java.util.List;

/** 责任链演示：请求沿链走，处理权在链上各环——发送方与处理方解耦。 */
public final class ChainDemo implements Algorithm {

    private static final String SCHEMA = "1";
    private static final String INVARIANT = "请求不知道会被谁处理：沿链传递，处理权在链上各环——发送方与处理方解耦";

    private static final int L_BUILD = 20;
    private static final int L_HANDLE = 21;
    private static final int L_PASS = 10;
    private static final int L_SOLVED = 15;

    private PatternEmitter em;

    @Override public String id() { return "pattern.chain"; }
    @Override public String title() { return "责任链模式"; }
    @Override public String codeResource() { return "/code/ChainDemo.java"; }
    @Override public String docResource() { return "/markdown/pattern-chain.md"; }

    @Override
    public Trace produce() {
        em = new PatternEmitter(List.of(
                new SeqView.Participant("client", "client"),
                new SeqView.Participant("front", "chain : FrontDesk"),
                new SeqView.Participant("manager", "next : Manager")), INVARIANT);
        em.addObject("@1", "FrontDesk");
        em.addObject("@2", "Manager");
        em.setField("@1", "next", Value.ref("@2"));
        em.setField("@2", "next", Value.NULL);
        em.mainLocals.put("chain", Value.ref("@1"));
        em.state(L_BUILD, VizEvent.EventType.INIT, "@1.link(@2)：链搭好——front 处理不了就给 manager", List.of("@1"),
                "链可以运行时任意拼接：删一环、加一环，发送方无感");
        em.message(L_HANDLE, 0, 1, "handle(\"投诉\")", "CALL", "请求从链头进入——发送方不知道也不关心终点");
        em.message(L_PASS, 1, 2, "next.handle(\"投诉\")", "CALL", "前台一看：不是问路 → 传给下一环");
        em.state(L_SOLVED, VizEvent.EventType.SET, "经理：是投诉，我处理（终点找到了）", List.of("@2"),
                "谁来处理由链上各环自己判断——client 零参与");
        em.message(L_SOLVED, 2, 0, "经理解决", "RETURN", "结果沿链回传");
        em.state(L_HANDLE, VizEvent.EventType.RETURN, "对比 if-else 大 Mig：每环一个类，顺序可配", List.of("@1"),
                "反向引用：Servlet Filter、拦截器、DNS 解析都是责任链");
        return new Trace(SCHEMA, meta(new int[0]), em.steps);
    }
}
