package com.algoviz.engine.pattern;

import com.algoviz.engine.Algorithm;
import com.algoviz.engine.Loc;
import com.algoviz.engine.Step;
import com.algoviz.engine.VizEvent;
import com.algoviz.engine.ds.SeqView;
import com.algoviz.engine.trace.Trace;

import java.util.List;

/**
 * 并发·读写锁：读-读共享，读-写互斥，写-写互斥。
 * 脚本化交错：两个读者并行进 → 写者到达被挂起 → 读者退出 → 写者独占 → 释放。
 * 读多写少的数据（配置、缓存）用它能吃到并发红利。
 */
public final class ConcurrentRwLockDemo implements Algorithm {

    private static final String SCHEMA = "1";
    private static final String INVARIANT =
            "读读共享、读写互斥、写写互斥——锁状态只有三态：空闲 / 读(n) / 写(1)";

    private static final int L_R = 5;
    private static final int L_WAIT = 8;
    private static final int L_W = 12;

    private String mode = "空闲";
    private int readers = 0;
    private PatternEmitter em;

    @Override public String id() { return "concurrent.rwlock"; }
    @Override public String title() { return "并发·读写锁"; }
    @Override public String codeResource() { return "/code/RwLockDemo.java"; }
    @Override public String docResource() { return "/markdown/rwlock.md"; }

    @Override
    public Trace produce() {
        em = new PatternEmitter(List.of(
                new SeqView.Participant("ra", "Reader A"),
                new SeqView.Participant("rb", "Reader B"),
                new SeqView.Participant("w", "Writer"),
                new SeqView.Participant("lock", "ReadWriteLock")), INVARIANT);
        em.reset();
        mode = "空闲";
        readers = 0;
        em.addObject("@lock", "RwLock");
        syncLock();
        em.state(3, VizEvent.EventType.INIT, "锁空闲——读多写少场景：配置表 99% 读 1% 写", List.of("@lock"),
                "普通互斥锁一视同仁；读写锁按「谁真的会冲突」分派并发权");
        // 两读者并行
        em.message(L_R, 0, 3, "acquireRead()", "CALL", "读者 A 进入");
        em.message(L_R, 3, 0, "OK mode=读(1)", "RETURN", "读锁可共享——A 不挡别人");
        acquireRead("A");
        em.message(L_R, 1, 3, "acquireRead()", "CALL", "读者 B 同时进入");
        em.message(L_R, 3, 1, "OK mode=读(2)", "RETURN", "读-读共享：A、B 并行读，互不阻塞");
        acquireRead("B");
        // 写者到达
        em.message(L_WAIT, 2, 3, "acquireWrite()", "CALL", "写者请求——有人在读");
        em.state(L_WAIT, VizEvent.EventType.COMPARE, "读在进行 → 写者挂起等待（读写互斥）", List.of("@lock"),
                "写者必须等所有读者退场——它改的数据不能被人正在读");
        releaseRead("A");
        em.message(L_WAIT, 0, 3, "releaseRead()", "RETURN", "A 读完退出（读计数 2→1）");
        releaseRead("B");
        em.message(L_WAIT, 1, 3, "releaseRead()", "RETURN", "B 退出（读计数归零）→ 唤醒写者");
        // 写者独占
        em.message(L_W, 2, 3, "获得写锁", "RETURN", "mode=写：现在数据只属于写者一个");
        em.state(L_W, VizEvent.EventType.SET, "写者独占——期间任何读/写请求都要等", List.of("@lock"),
                "写-写、写-读全互斥：一致性靠这一步保住");
        em.message(L_W, 2, 3, "releaseWrite()", "CALL", "写完释放");
        em.message(L_W, 3, 2, "OK mode=空闲", "RETURN", "锁回到空闲——被挡的读者们可以进来了");
        releaseWrite();
        em.state(18, VizEvent.EventType.RETURN, "一张状态机：空闲→读(n)→空→写→空——Java 用 ReentrantReadWriteLock", List.of("@lock"),
                "红利算术：99 读 1 写，读锁并行 = 吞吐近似 ×核数；写太多反而不如普通锁（还要维护读计数）");
        return new Trace(SCHEMA, meta(), em.steps);
    }

    private void acquireRead(String who) {
        mode = "读";
        readers++;
        syncLock();
    }

    private void releaseRead(String who) {
        readers--;
        if (readers == 0) {
            mode = "空闲";
        }
        syncLock();
    }

    private void releaseWrite() {
        mode = "空闲";
        syncLock();
    }

    private void syncLock() {
        em.setField("@lock", "mode", com.algoviz.engine.Value.prim(mode));
        em.setField("@lock", "readers", com.algoviz.engine.Value.prim(readers));
    }

    private Trace.TraceMeta meta() {
        return new Trace.TraceMeta(id(), title(), codeResource(), "2 读 + 1 写", 3);
    }

    @Override
    public java.util.List<String> dataLines() {
        return List.of("// 场景：读 99% 写 1%（配置表 / 缓存）");
    }

    @Override
    public com.algoviz.engine.Complexity complexity() {
        return new com.algoviz.engine.Complexity("O(1) 加解锁", "O(等待线程数)",
                "适用判据：读远多于写。写频繁时读计数维护反成开销——JDK8+ 可看 StampedLock 的乐观读");
    }
}
