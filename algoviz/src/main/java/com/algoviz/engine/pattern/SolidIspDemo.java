package com.algoviz.engine.pattern;

import com.algoviz.engine.Algorithm;
import com.algoviz.engine.Loc;
import com.algoviz.engine.Step;
import com.algoviz.engine.VizEvent;
import com.algoviz.engine.ds.SeqView;
import com.algoviz.engine.trace.Trace;

import java.util.List;

/**
 * SOLID·I 接口隔离：客户端不该被迫依赖自己用不到的方法。
 * 反例：胖接口 Worker{work();eat();}——Robot 被迫实现 eat()（抛异常/空实现）。
 * 正例：拆成 Workable/Eatable，各取所需。
 */
public final class SolidIspDemo implements Algorithm {

    private static final String SCHEMA = "1";
    private static final String INVARIANT = "接口 = 客户端视角的能力清单——为「用的人」切，不为「实现的人」合";

    private static final int L_BAD1 = 5;
    private static final int L_BAD2 = 7;
    private static final int L_GOOD1 = 13;
    private static final int L_GOOD2 = 15;

    private PatternEmitter em;

    @Override public String id() { return "solid.isp"; }
    @Override public String title() { return "SOLID·接口隔离 ISP"; }
    @Override public String codeResource() { return "/code/SolidIspDemo.java"; }
    @Override public String docResource() { return "/markdown/solid-isp.md"; }

    @Override
    public Trace produce() {
        em = new PatternEmitter(List.of(
                new SeqView.Participant("client", "client"),
                new SeqView.Participant("worker", "Worker 胖接口"),
                new SeqView.Participant("robot", "Robot（被迫实现 eat()）"),
                new SeqView.Participant("split", "Workable / Eatable")), INVARIANT);
        em.reset();
        // ===== 反例：胖接口 =====
        em.message(L_BAD1, 0, 1, "robot.work()（Worker 接口：work + eat）", "CALL",
                "反例：一个接口管所有能力——人类员工和机器人被塞进同一张能力清单");
        em.state(L_BAD2, VizEvent.EventType.COMPARE,
                "Robot 实现 eat() 只能抛异常或空着——接口在逼实现者撒谎", List.of(),
                "被迫空实现 = 接口在设计上就错了，不是实现者的错");
        // ===== 正例：能力切片 =====
        em.message(L_GOOD1, 0, 3, "robot 只实现 Workable——清单上没有 eat", "CALL",
                "正例：按「谁在用」切接口——用人方只看到自己调的那几个方法");
        em.message(L_GOOD2, 0, 3, "hiring.eat() 走 Eatable，与 Robot 无关", "CALL",
                "人类员工两个都实现，Robot 只实现一个——各取所需，无人被迫");
        em.state(L_GOOD2, VizEvent.EventType.RETURN,
                "接口从 1 个胖的变 2 个瘦的——依赖面随之瘦身", List.of(),
                "ISP 与 SRP 的分工：SRP 拆类（实现侧），ISP 拆接口（使用侧）");
        return new Trace(SCHEMA, meta(), em.steps);
    }

    private Trace.TraceMeta meta() {
        return new Trace.TraceMeta(id(), title(), codeResource(), "1 胖接口 → 2 瘦接口", 2);
    }

    @Override
    public java.util.List<String> dataLines() {
        return List.of("// 反例: interface Worker{work();eat();} → 正例: Workable / Eatable");
    }

    @Override
    public com.algoviz.engine.Complexity complexity() {
        return new com.algoviz.engine.Complexity("O(1) 结构原则", "O(接口数) 略增",
                "识别信号：实现类里出现 UnsupportedOperationException 或空方法体——那就是被迫吃的亏");
    }
}
