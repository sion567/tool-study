package com.algoviz.engine.pattern;

import com.algoviz.engine.Algorithm;
import com.algoviz.engine.Complexity;
import com.algoviz.engine.Value;
import com.algoviz.engine.VizEvent;
import com.algoviz.engine.ds.SeqView;
import com.algoviz.engine.trace.Trace;

import java.util.List;

/**
 * 并发·可见性与 CAS：JMM 的三大现场——脚本化交错（确定性演示竞态时序）。
 * 第一幕：无 volatile——写线程改 flag，读线程永远看不见（JMM 允许工作副本缓存）。
 * 第二幕：volatile——写后强制刷主存、读强制拉取，可见性恢复。
 * 第三幕：非原子 count++ 丢更新 vs compareAndSet 自旋重试——原子性的解药。
 */
public final class ConcurrentVisDemo implements Algorithm {

    private static final String SCHEMA = "1";
    private static final String INVARIANT =
            "JMM：无同步时每个线程可以工作在变量的本地副本上——写别人看不见；volatile 禁用副本；"
                    + "count++ 是读-改-写三步，非原子——CAS 把三步变成一条原子指令 + 自旋重试";

    private static final int L_WRITE = 4;
    private static final int L_READ = 7;
    private static final int L_VOLATILE = 12;
    private static final int L_INC = 17;
    private static final int L_LOST = 19;
    private static final int L_CAS = 22;
    private static final int L_END = 26;

    private PatternEmitter em;

    @Override public String id() { return "concurrent.vis"; }
    @Override public String title() { return "线程·可见性与CAS"; }
    @Override public String codeResource() { return "/code/VisDemo.java"; }
    @Override public String docResource() { return "/markdown/vis.md"; }

    @Override
    public Trace produce() {
        em = new PatternEmitter(List.of(
                new SeqView.Participant("w", "写线程"),
                new SeqView.Participant("m", "主内存"),
                new SeqView.Participant("r", "读线程")), INVARIANT);
        em.reset();
        em.addObject("@flag", "boolean flag");
        em.addObject("@cnt", "int count");
        // ---- 第一幕：无 volatile——可见性消失 ----
        em.message(L_WRITE, 0, 1, "flag = true（写入工作副本）", "CALL", "写线程改了 flag——你以为读线程马上知道");
        em.setField("@flag", "flag(主存)", Value.prim("false（未刷）"));
        em.setField("@flag", "writer 副本", Value.prim("true"));
        em.state(L_WRITE, VizEvent.EventType.SET, "主存仍是 false：JMM 允许写留在工作副本——可见性消失", List.of("@flag"),
                "没有同步措施时，JMM 不承诺何时把写刷回主存——'看不见'是合法行为");
        em.message(L_READ, 2, 1, "读 flag → false", "CALL", "读线程从主存拿到的还是 false");
        em.setField("@flag", "reader 副本", Value.prim("false"));
        em.state(L_READ, VizEvent.EventType.COMPARE, "读线程死循环等不到 true——这就是'可见性 bug'的现场", List.of("@flag"),
                "while (!flag) 空转：单线程直觉在多线程失效的第一课");
        // ---- 第二幕：volatile——可见性恢复 ----
        em.message(L_VOLATILE, 0, 1, "volatile flag = true", "CALL", "加 volatile：写立即刷回主存");
        em.setField("@flag", "flag(主存)", Value.prim("true（volatile 刷新）"));
        em.state(L_VOLATILE, VizEvent.EventType.SET, "volatile 写：强制刷主存 + 禁用副本——主存此刻 = true", List.of("@flag"),
                "volatile = 禁用本地副本：写立即见主存、读绕过副本");
        em.message(L_VOLATILE, 2, 1, "读 flag → true", "CALL", "读线程拉到 true——退出循环");
        em.setField("@flag", "reader 副本", Value.prim("true（volatile 直读）"));
        em.state(L_VOLATILE, VizEvent.EventType.SET, "可见性恢复：volatile 是 JMM 给应用的轻量级同步", List.of("@flag"),
                "比锁便宜：只保证可见性/序，不保证原子性——下一幕看它的边界");
        // ---- 第三幕：count++ 丢更新 vs CAS ----
        em.setField("@cnt", "count", Value.prim(0));
        em.message(L_INC, 0, 1, "count++：读 0", "CALL", "写线程读 count=0，准备 +1");
        em.message(L_INC, 2, 1, "count++：读 0", "CALL", "读线程同时读 count=0——竞态窗口打开");
        em.message(L_INC, 0, 1, "写 1", "CALL", "写线程写回 1");
        em.message(L_INC, 2, 1, "写 1", "CALL", "读线程也写回 1——一次自增凭空蒸发");
        em.state(L_LOST, VizEvent.EventType.COMPARE, "两次 ++ 后 count = 1：'丢失更新'——++ 是读改写三步，非原子", List.of("@cnt"),
                "丢失更新 = 竞态条件的标准形态：两个写撞在同一个旧值上");
        em.message(L_CAS, 0, 1, "CAS(0→1) 成功", "CALL", "compareAndSet(期望0, 新值1)：主存是 0 → 原子交换成功");
        em.setField("@cnt", "count", Value.prim(1));
        em.message(L_CAS, 2, 1, "CAS(0→1) 失败 → 重读", "CALL", "期望 0 但主存已是 1 → CAS 失败——不写回，自旋重试");
        em.message(L_CAS, 2, 1, "CAS(1→2) 成功", "CALL", "重读到 1 → CAS(1,2) 成功 → count=2：一次自增也没丢");
        em.setField("@cnt", "count", Value.prim(2));
        em.state(L_END, VizEvent.EventType.RETURN, "CAS 自旋：失败不写回、重读再试——乐观并发的全部秘密", List.of("@cnt"),
                "synchronized 是悲观等待，CAS 是乐观重试——AtomicInteger 的内核");
        return new Trace(SCHEMA, meta(), em.steps);
    }

    private Trace.TraceMeta meta() {
        return new Trace.TraceMeta(id(), title(), codeResource(), "flag / count 两变量", em.steps.size());
    }

    @Override
    public Complexity complexity() {
        return new Complexity("O(1) 脚本化时序", "O(1) 两三个变量",
                "可见性是并发第一大坑：单线程直觉在多线程失效——volatile 与 CAS 是 Java 暴露给应用的仅有的两个轻量级工具");
    }
}
