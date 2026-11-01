package com.algoviz.engine.pattern;

import com.algoviz.engine.Algorithm;
import com.algoviz.engine.Value;
import com.algoviz.engine.VizEvent;
import com.algoviz.engine.ds.SeqView;
import com.algoviz.engine.trace.Trace;

import java.util.List;

/** 桥接演示：两个维度各自变化、一根引用相连——组合代替继承的乘法爆炸解药。 */
public final class BridgeDemo implements Algorithm {

    private static final String SCHEMA = "1";
    private static final String INVARIANT =
            "两个维度各自变化、桥接引用相连：m+n 个类覆盖 m×n 种组合（继承要 m×n 个类）";

    private static final int L_NEW1 = 19;
    private static final int L_NEW2 = 20;
    private static final int L_DRAW1 = 21;
    private static final int L_C_DRAW = 12;
    private static final int L_RED = 3;
    private static final int L_S_DRAW = 16;
    private static final int L_BLUE = 4;

    private PatternEmitter em;

    @Override public String id() { return "pattern.bridge"; }
    @Override public String title() { return "桥接模式"; }
    @Override public String codeResource() { return "/code/BridgeDemo.java"; }
    @Override public String docResource() { return "/markdown/pattern-bridge.md"; }

    @Override
    public Trace produce() {
        em = new PatternEmitter(List.of(
                new SeqView.Participant("client", "client"),
                new SeqView.Participant("circle", "s1 : Circle"),
                new SeqView.Participant("red", "Red"),
                new SeqView.Participant("square", "s2 : Square"),
                new SeqView.Participant("blue", "Blue")), INVARIANT);
        em.addObject("@1", "Circle");
        em.addObject("@2", "Red");
        em.addObject("@3", "Square");
        em.addObject("@4", "Blue");
        em.setField("@1", "color", Value.ref("@2"));
        em.setField("@3", "color", Value.ref("@4"));
        em.state(L_NEW1, VizEvent.EventType.INIT, "@1 Circle 桥接 @2 Red（@1.color → @2）", List.of("@1"),
                "形状维与颜色维在这里「桥」在一起——一根引用");
        em.state(L_NEW2, VizEvent.EventType.INIT, "@3 Square 桥接 @4 Blue", List.of("@3"),
                "两个维度互不感知，各自独立继承");
        em.message(L_DRAW1, 0, 1, "draw()", "CALL", "画圆：形状维先干活");
        em.message(L_C_DRAW, 1, 2, "color.apply()", "CALL", "桥到颜色维：要颜色去这里拿");
        em.message(L_RED, 2, 1, "红", "RETURN", "颜色维的答案");
        em.message(L_C_DRAW, 1, 0, "红 的圆", "RETURN", "两维拼接完成");
        em.message(L_DRAW1, 0, 3, "draw()", "CALL", "画方：另一个形状，同一套桥");
        em.message(L_S_DRAW, 3, 4, "color.apply()", "CALL", "Square 只认 Color 接口");
        em.message(L_BLUE, 4, 3, "蓝", "RETURN", "Blue 的答案");
        em.message(L_S_DRAW, 3, 0, "蓝 的方", "RETURN", "完成");
        em.state(L_DRAW1, VizEvent.EventType.RETURN, "2 形状 + 2 颜色 = 4 种组合，只用 4 个类", List.of("@1", "@3"),
                "继承方案要 4 个子类，3×3 就是 9 个——维度一多，乘法爆炸");
        return new Trace(SCHEMA, meta(new int[0]), em.steps);
    }
}
