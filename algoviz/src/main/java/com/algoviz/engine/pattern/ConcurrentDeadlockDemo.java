package com.algoviz.engine.pattern;

import com.algoviz.engine.Algorithm;
import com.algoviz.engine.Complexity;
import com.algoviz.engine.Loc;
import com.algoviz.engine.Value;
import com.algoviz.engine.VizEvent;
import com.algoviz.engine.ds.SeqView;
import com.algoviz.engine.trace.Trace;

import java.util.List;

/**
 * 并发·死锁四要件：两线程交叉持锁——脚本化交错到冻结点，再演示破解。
 * 第一幕：T1 持 A 等 B、T2 持 B 等 A——互斥/占有等待/不可剥夺/环路等待四要件全齐 → 死锁。
 * 第二幕：同一时序 + 锁排序（全程序统一 A→B 顺序拿锁）→ 环路消失；tryLock 超时兜底。
 */
public final class ConcurrentDeadlockDemo implements Algorithm {

    private static final String SCHEMA = "1";
    private static final String INVARIANT =
            "死锁四要件：互斥、占有且等待、不可剥夺、环路等待——缺一不可，也只需破坏其一即可破解；"
                    + "锁排序是最便宜的破解（环路结构被全局顺序拆掉）";

    private static final int L_LOCK_A = 4;
    private static final int L_LOCK_B = 5;
    private static final int L_DEADLOCK = 7;
    private static final int L_SORTED = 13;
    private static final int L_TRYLOCK = 15;
    private static final int L_END = 19;

    private PatternEmitter em;

    @Override public String id() { return "concurrent.deadlock"; }
    @Override public String title() { return "线程·死锁四要件"; }
    @Override public String codeResource() { return "/code/DeadlockDemo.java"; }
    @Override public String docResource() { return "/markdown/deadlock.md"; }

    @Override
    public Trace produce() {
        em = new PatternEmitter(List.of(
                new SeqView.Participant("t1", "线程 1"),
                new SeqView.Participant("lx", "锁 A / 锁 B"),
                new SeqView.Participant("t2", "线程 2")), INVARIANT);
        em.reset();
        em.addObject("@a", "lock A");
        em.addObject("@b", "lock B");
        // ---- 第一幕：交叉持锁 → 死锁 ----
        em.message(L_LOCK_A, 0, 1, "T1: lock(A) ✓", "CALL", "T1 先拿 A——同时 T2 拿 B（交错起点）");
        em.setField("@a", "持有者", Value.prim("T1"));
        em.message(L_LOCK_B, 2, 1, "T2: lock(B) ✓", "CALL", "T2 拿 B——双方各握一角");
        em.setField("@b", "持有者", Value.prim("T2"));
        em.message(L_LOCK_B, 0, 1, "T1: lock(B) —— 阻塞", "CALL", "T1 想要 B：但 B 在 T2 手里 → 挂起等待");
        em.state(L_DEADLOCK, VizEvent.EventType.COMPARE, "T1 持 A 等 B；T2 持 B 等 A——环路形成，双双冻结", List.of("@a", "@b"),
                "四要件核对：互斥 ✓ 占有且等待 ✓ 不可剥夺 ✓ 环路等待 ✓——死锁成立");
        em.message(L_LOCK_A, 2, 1, "T2: lock(A) —— 阻塞（永远）", "CALL", "T2 也在等 A——没有任何线程能前进");
        em.state(L_DEADLOCK, VizEvent.EventType.MARK_SORTED,
                "死锁定格：jstack 会看到两个线程互为 BLOCKED——这就是'程序卡死但 CPU 0%'的经典现场", List.of("@a", "@b"),
                "死锁不耗 CPU：线程在锁队列里睡觉——监控上表现为'响应消失'而非'负载爆炸'");
        // ---- 第二幕：破解 ----
        em.state(L_SORTED, VizEvent.EventType.MESSAGE,
                "破解一：锁排序——全程序约定先 A 后 B：T2 拿 B 之前必须先拿 A", List.of("@a", "@b"),
                "全局顺序拆掉'环路'要件：等 A 的线程永远不可能同时持有 B");
        em.message(L_SORTED, 2, 1, "T2: lock(A) ✓（按序）", "CALL", "T2 按序先拿 A——与 T1 竞争 A，必有先后");
        em.setField("@a", "持有者", Value.prim("T2（排序后）"));
        em.message(L_TRYLOCK, 0, 1, "T1: tryLock(A) 超时失败 → 释放已有锁", "CALL",
                "破解二：tryLock 带超时——拿不到就放弃并释放自己持有的锁，退避重试");
        em.setField("@b", "持有者", Value.prim("无（T2 用完即还）"));
        em.message(L_END, 2, 1, "T2 完成 → 释放 A/B → T1 重试成功", "RETURN",
                "四要件破了'占有且等待'与'环路'两条——死锁从结构上不可能");
        em.state(L_END, VizEvent.EventType.RETURN, "排序 + tryLock 是生产代码的标配组合：一个拆环路、一个做兜底", List.of("@a", "@b"),
                "jstack / ThreadMXBean.findDeadlockedThreads 是线上排查死锁的第一工具");
        return new Trace(SCHEMA, meta(), em.steps);
    }

    private Trace.TraceMeta meta() {
        return new Trace.TraceMeta(id(), title(), codeResource(), "锁 A/B 两线程", em.steps.size());
    }

    @Override
    public Complexity complexity() {
        return new Complexity("O(1) 脚本化时序", "O(1) 两把锁",
                "数据库死锁检测、分布式锁续约、Go 的锁排序 lint——"
                        + "死锁四要件是 1968 年 Coffman 的总结，五十年没变过");
    }
}
