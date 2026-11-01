package com.algoviz.engine.pattern;

import com.algoviz.engine.Algorithm;
import com.algoviz.engine.Value;
import com.algoviz.engine.VizEvent;
import com.algoviz.engine.ds.SeqView;
import com.algoviz.engine.trace.Trace;

import java.util.List;

/** 代理模式演示：同接口转发前先做点别的（控制访问）——与装饰器同形、异在意图。 */
public final class ProxyDemo implements Algorithm {

    private static final String SCHEMA = "1";
    private static final String INVARIANT = "代理与真身同接口：转发之前先做点别的（权限/远程/缓存/懒加载）——区别于装饰器的是意图";

    private static final int L_NEW = 16;
    private static final int L_OPEN1 = 17;
    private static final int L_CHECK = 11;
    private static final int L_LOGIN = 9;
    private static final int L_OPEN2 = 17;
    private static final int L_FORWARD = 12;
    private static final int L_REAL = 4;

    private PatternEmitter em;

    @Override public String id() { return "pattern.proxy"; }
    @Override public String title() { return "代理模式"; }
    @Override public String codeResource() { return "/code/ProxyDemo.java"; }
    @Override public String docResource() { return "/markdown/pattern-proxy.md"; }

    @Override
    public Trace produce() {
        em = new PatternEmitter(List.of(
                new SeqView.Participant("client", "client"),
                new SeqView.Participant("proxy", "v : VaultProxy"),
                new SeqView.Participant("real", "real : RealVault")), INVARIANT);
        em.addObject("@1", "VaultProxy");
        em.addObject("@2", "RealVault");
        em.setField("@1", "real", Value.ref("@2"));
        em.setField("@1", "loggedIn", Value.prim("false"));
        em.mainLocals.put("v", Value.ref("@1"));
        em.state(L_NEW, VizEvent.EventType.INIT, "装配：@1 代理持有真身 @2（同接口）", List.of("@1"),
                "client 拿到的是代理——它以为自己在和金库说话");
        em.message(L_OPEN1, 0, 1, "open()", "CALL", "第一次请求");
        em.state(L_CHECK, VizEvent.EventType.SET, "检查：loggedIn=false → 拒绝（真身 @2 纹丝不动）", List.of("@1"),
                "转发前插一道关卡——这就是代理多干的事");
        em.message(L_OPEN1, 1, 0, "拒绝：请先登录", "RETURN", "没碰到真身就回来了");
        em.state(L_LOGIN, VizEvent.EventType.SET, "login()：@1.loggedIn ← true", List.of("@1"),
                "代理自己的状态变化");
        em.message(L_OPEN2, 0, 1, "open()", "CALL", "第二次请求");
        em.message(L_FORWARD, 1, 2, "real.open()", "CALL", "检查通过，这次真转发——真身终于被碰到了");
        em.message(L_REAL, 2, 1, "金库里的宝藏", "RETURN", "真身干活");
        em.message(L_OPEN2, 1, 0, "金库里的宝藏", "RETURN", "原样交回调用方");
        em.state(L_OPEN2, VizEvent.EventType.RETURN, "对比装饰器：结构一模一样，意图不同——代理控制访问，装饰器叠加行为", List.of("@1"),
                "Spring AOP / RPC stub / 懒加载图片，全是这个骨架");
        return new Trace(SCHEMA, meta(new int[0]), em.steps);
    }
}
