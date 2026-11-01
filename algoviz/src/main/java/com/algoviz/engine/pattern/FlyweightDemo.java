package com.algoviz.engine.pattern;

import com.algoviz.engine.Algorithm;
import com.algoviz.engine.Value;
import com.algoviz.engine.VizEvent;
import com.algoviz.engine.ds.SeqView;
import com.algoviz.engine.trace.Trace;

import java.util.List;

/** 享元演示：内在状态共享（池），外在状态外置——内存从 N 份降到种类数。 */
public final class FlyweightDemo implements Algorithm {

    private static final String SCHEMA = "1";
    private static final String INVARIANT =
            "内在状态共享（池子），外在状态外置（每棵树的坐标）——内存从 N 份降到种类数";

    private static final int L_POOL = 13;
    private static final int L_GET1 = 14;
    private static final int L_GET2 = 15;
    private static final int L_GET3 = 16;
    private static final int L_COMPUTE = 9;
    private static final int L_PROOF = 17;

    private PatternEmitter em;

    @Override public String id() { return "pattern.flyweight"; }
    @Override public String title() { return "享元模式"; }
    @Override public String codeResource() { return "/code/FlyweightDemo.java"; }
    @Override public String docResource() { return "/markdown/pattern-flyweight.md"; }

    @Override
    public Trace produce() {
        em = new PatternEmitter(List.of(
                new SeqView.Participant("client", "client"),
                new SeqView.Participant("pool", "pool : StylePool"),
                new SeqView.Participant("green", "绿色 : TreeStyle"),
                new SeqView.Participant("blue", "蓝色 : TreeStyle")), INVARIANT);
        em.addObject("@1", "StylePool");
        em.setField("@1", "pool", Value.prim("{}"));
        em.mainLocals.put("pool", Value.ref("@1"));
        em.state(L_POOL, VizEvent.EventType.INIT, "new StylePool()：@1.pool = {}（空池）", List.of("@1"),
                "一百万棵树在门外排队，池子里却只需要「样式」这么几种对象");
        em.message(L_GET1, 0, 1, "get(\"绿色\")", "CALL", "第一次：池里没有");
        em.addObject("@2", "TreeStyle");
        em.setField("@2", "color", Value.prim("绿色"));
        em.setField("@2", "shape", Value.prim("圆锥"));
        em.setField("@1", "pool", Value.prim("{绿色=@2}"));
        em.state(L_COMPUTE, VizEvent.EventType.SET, "miss → 真的造 @2 入池", List.of("@2"),
                "computeIfAbsent：不存在才造");
        em.message(L_GET2, 0, 1, "get(\"绿色\")", "CALL", "第二次要同款——这次有讲究了");
        em.state(L_COMPUTE, VizEvent.EventType.SET, "hit → 直接返回 @2（没有 new！）", List.of("@2"),
                "看堆面板：@2 的引用被第二次交了出去——这就是共享");
        em.message(L_GET3, 0, 1, "get(\"蓝色\")", "CALL", "新 key：池里真没有");
        em.addObject("@3", "TreeStyle");
        em.setField("@3", "color", Value.prim("蓝色"));
        em.setField("@3", "shape", Value.prim("圆锥"));
        em.setField("@1", "pool", Value.prim("{绿色=@2, 蓝色=@3}"));
        em.state(L_COMPUTE, VizEvent.EventType.SET, "miss → 造 @3 入池（现在一共 2 份）", List.of("@3"),
                "外在状态（每棵树的坐标）不进池——交给使用现场各自保存");
        em.state(L_PROOF, VizEvent.EventType.RETURN, "a == b → true：一百万棵树共享 2 个样式对象", List.of("@2", "@3"),
                "反向引用：Integer.valueOf(-128~127) 的缓存就是 JDK 内置的享元");
        return new Trace(SCHEMA, meta(new int[0]), em.steps);
    }
}
