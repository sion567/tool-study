package com.algoviz.engine.pattern;

import com.algoviz.engine.Algorithm;
import com.algoviz.engine.Value;
import com.algoviz.engine.VizEvent;
import com.algoviz.engine.ds.SeqView;
import com.algoviz.engine.trace.Trace;

import java.util.List;

/** 外观模式演示：子系统收进一个门牌——client 与三个类的依赖变成一个。 */
public final class FacadeDemo implements Algorithm {

    private static final String SCHEMA = "1";
    private static final String INVARIANT = "子系统收进一个门牌：client 与 3 个子系统的依赖 → 与 Facade 的 1 个依赖";

    private static final int L_NEW = 14;
    private static final int L_OPEN = 10;
    private static final int L_ENGINE = 2;
    private static final int L_PANELS = 3;
    private static final int L_PLAYER = 4;

    private PatternEmitter em;

    @Override public String id() { return "pattern.facade"; }
    @Override public String title() { return "外观模式"; }
    @Override public String codeResource() { return "/code/FacadeDemo.java"; }
    @Override public String docResource() { return "/markdown/pattern-facade.md"; }

    @Override
    public Trace produce() {
        em = new PatternEmitter(List.of(
                new SeqView.Participant("client", "client"),
                new SeqView.Participant("facade", "app : AppFacade"),
                new SeqView.Participant("engine", "engine"),
                new SeqView.Participant("panels", "panels"),
                new SeqView.Participant("player", "player")), INVARIANT);
        em.addObject("@1", "AppFacade");
        em.addObject("@2", "Engine");
        em.addObject("@3", "Panels");
        em.addObject("@4", "Player");
        em.setField("@1", "engine", Value.ref("@2"));
        em.setField("@1", "panels", Value.ref("@3"));
        em.setField("@1", "player", Value.ref("@4"));
        em.mainLocals.put("app", Value.ref("@1"));
        em.state(L_NEW, VizEvent.EventType.INIT, "@1 门牌就位：内部握着三个子系统（@2 @3 @4）", List.of("@1"),
                "子系统照常工作，Facade 只是站在它们前面");
        em.message(L_NEW, 0, 1, "open()", "CALL", "client 只认识 Facade 的一个方法——三步编排藏在里面");
        em.message(L_OPEN, 1, 2, "engine.start()", "CALL", "编排①：引擎启动");
        em.message(L_ENGINE, 2, 1, "引擎启动", "RETURN", "完成");
        em.message(L_OPEN, 1, 3, "panels.refresh()", "CALL", "编排②：面板刷新");
        em.message(L_PANELS, 3, 1, "面板刷新", "RETURN", "完成");
        em.message(L_OPEN, 1, 4, "player.play()", "CALL", "编排③：开始播放");
        em.message(L_PLAYER, 4, 1, "开始播放", "RETURN", "完成");
        em.message(L_NEW, 1, 0, "open() 完成", "RETURN", "顺序错了会坏——所以编排权收在 Facade 一处");
        em.state(L_NEW, VizEvent.EventType.RETURN, "client 的依赖数：3 → 1——门面不改变子系统，只挡住复杂度", List.of("@1"),
                "反向引用：本工具的「打开一个算法」背后也是一串子系统（建图/渲染/面板），UI 只调一层");
        return new Trace(SCHEMA, meta(new int[0]), em.steps);
    }
}
