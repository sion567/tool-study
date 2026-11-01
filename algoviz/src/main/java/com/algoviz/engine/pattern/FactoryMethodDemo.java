package com.algoviz.engine.pattern;

import com.algoviz.engine.Algorithm;
import com.algoviz.engine.Value;
import com.algoviz.engine.VizEvent;
import com.algoviz.engine.ds.SeqView;
import com.algoviz.engine.trace.Trace;

import java.util.List;

/** 工厂方法演示：new 的决定权下放给子类——父类骨架用产品，造什么由 create() 说。 */
public final class FactoryMethodDemo implements Algorithm {

    private static final String SCHEMA = "1";
    private static final String INVARIANT = "工厂方法 = 把 new 的决定权下放给子类：父类骨架用产品，造什么由 create() 说";

    private static final int L_NEW = 20;
    private static final int L_PLAN = 21;
    private static final int L_CREATE = 14;
    private static final int L_DELIVER = 11;
    private static final int L_BODY = 4;

    private PatternEmitter em;

    @Override public String id() { return "pattern.factorymethod"; }
    @Override public String title() { return "工厂方法"; }
    @Override public String codeResource() { return "/code/FactoryMethodDemo.java"; }
    @Override public String docResource() { return "/markdown/pattern-factorymethod.md"; }

    @Override
    public Trace produce() {
        em = new PatternEmitter(List.of(
                new SeqView.Participant("client", "client"),
                new SeqView.Participant("sea", "sea : SeaLogistics"),
                new SeqView.Participant("ship", "ship : Ship")), INVARIANT);
        em.addObject("@1", "SeaLogistics");
        em.mainLocals.put("sea", Value.ref("@1"));
        em.state(L_NEW, VizEvent.EventType.INIT, "new SeaLogistics()：选定了「海运」这条产品线", List.of("@1"),
                "Logistics 骨架已就位，但造什么产品由子类决定");
        em.message(L_PLAN, 0, 1, "plan()", "CALL", "骨架方法：用 create() 拿产品，全程不出现 Ship 字样");
        em.message(L_CREATE, 1, 2, "create() → new Ship()", "CALL", "子类的工厂方法被回调——决定权在这里");
        em.addObject("@2", "Ship");
        em.state(L_CREATE, VizEvent.EventType.SET, "@2 Ship 造出来了——由 SeaLogistics 说了算", List.of("@2"),
                "换成 RoadLogistics，这里就是 Truck：骨架零改动");
        em.message(L_DELIVER, 1, 2, "t.deliver()", "CALL", "骨架拿产品干活——只认 Transport 接口");
        em.message(L_BODY, 2, 1, "海运", "RETURN", "具体行为在产品类里");
        em.message(L_DELIVER, 1, 0, "plan() 完成", "RETURN", "交付：新增产品线 = 新增一对（产品+工厂）");
        return new Trace(SCHEMA, meta(new int[0]), em.steps);
    }
}
