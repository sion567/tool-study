package com.algoviz.engine.pattern;

import com.algoviz.engine.Algorithm;
import com.algoviz.engine.Loc;
import com.algoviz.engine.Step;
import com.algoviz.engine.VizEvent;
import com.algoviz.engine.ds.SeqView;
import com.algoviz.engine.trace.Trace;

import java.util.ArrayList;
import java.util.List;

/**
 * 并发·线程池：任务进队列，常驻 worker 循环 take→执行→再 take。
 * 脚本化交错：提交 3 个任务，2 个 worker 错峰认领，完成后回池待命。
 * 核心：线程是「复用」的——创建/销毁的昂贵成本被摊薄。
 */
public final class ConcurrentPoolDemo implements Algorithm {

    private static final String SCHEMA = "1";
    private static final String INVARIANT =
            "worker 循环 = take → 执行 → 再 take；任务排队，线程复用——池的三个参数都在这张图上";

    private final List<String> queue = new ArrayList<>();
    private final String[] workerBusy = {"-", "-"};
    private PatternEmitter em;

    @Override public String id() { return "concurrent.pool"; }
    @Override public String title() { return "并发·线程池"; }
    @Override public String codeResource() { return "/code/ThreadPoolDemo.java"; }
    @Override public String docResource() { return "/markdown/thread-pool.md"; }

    @Override
    public Trace produce() {
        em = new PatternEmitter(List.of(
                new SeqView.Participant("sub", "submitter"),
                new SeqView.Participant("q", "任务队列"),
                new SeqView.Participant("w1", "worker-1"),
                new SeqView.Participant("w2", "worker-2")), INVARIANT);
        em.reset();
        queue.clear();
        workerBusy[0] = workerBusy[1] = "-";
        em.addObject("@q", "TaskQueue");
        em.addObject("@w", "Workers");
        sync();
        em.state(3, VizEvent.EventType.INIT, "池启动：2 个 worker 常驻循环 take→执行→再 take", List.of("@w"),
                "线程池 = 线程复用 + 任务排队。对比裸 new Thread：每次任务都付一次创建/销毁的开销");
        // 提交三个任务
        em.message(5, 0, 1, "submit(T1)", "CALL", "T1 进队列");
        submit("T1");
        em.message(5, 0, 1, "submit(T2)", "CALL", "T2 进队列（worker 们正忙/待命，队列缓冲）");
        submit("T2");
        em.message(5, 0, 1, "submit(T3)", "CALL", "T3 进队列——峰值被队列削平");
        submit("T3");
        em.state(6, VizEvent.EventType.SET, "队列 [T1,T2,T3]——提交方永远只管扔进队列，毫秒级返回", List.of("@q"),
                "异步的本质：提交与执行解耦——Future 拿回去等结果");
        // worker 认领
        em.message(8, 2, 1, "take()", "CALL", "worker-1 空闲 → 认领");
        em.message(8, 1, 2, "return T1", "RETURN", "T1 出队交给 worker-1");
        assign(0, "T1");
        em.message(8, 3, 1, "take()", "CALL", "worker-2 也来认领");
        em.message(8, 1, 3, "return T2", "RETURN", "T2 出队交给 worker-2");
        assign(1, "T2");
        em.state(10, VizEvent.EventType.COMPARE, "队列只剩 [T3]——两个 worker 并行执行 T1/T2", List.of("@q"),
                "并行度 = worker 数；队列里的任务是「还没轮到的峰值」");
        em.message(12, 2, 3, "T1 完成（回调/结果）", "RETURN", "worker-1 干完活——不放回系统，转身再 take");
        finish(0);
        em.message(12, 2, 1, "take()", "CALL", "worker-1 继续认领 T3");
        em.message(12, 1, 2, "return T3", "RETURN", "T3 出队——队列清空");
        assign(0, "T3");
        em.state(14, VizEvent.EventType.SET, "2 个线程消化了 3 个任务——线程从未重建", List.of("@w"),
                "这就是「池」：线程是资产，任务是消耗品");
        em.state(15, VizEvent.EventType.COMPARE, "队列空 → 两个 worker 阻塞在 take()——池待命", List.of("@w"),
                "空闲线程不是在轮询，是挂在队列的 notEmpty 条件上——零 CPU，来活即醒");
        em.state(16, VizEvent.EventType.RETURN,
                "七个参数对应画面：corePoolSize=2（worker 数）、workQueue=队列、maximum/reject=队列满后的扩容与拒绝策略", List.of("@w"),
                "ThreadPoolExecutor 的本质：一堆 worker 死循环 take 队列——懂这张图，七个参数就不再是咒语");
        return new Trace(SCHEMA, meta(), em.steps);
    }

    private void submit(String t) {
        queue.add(t);
        sync();
    }

    private void assign(int w, String t) {
        queue.remove(t);
        workerBusy[w] = t;
        sync();
    }

    private void finish(int w) {
        workerBusy[w] = "-";
        sync();
    }

    private void sync() {
        em.setField("@q", "队列", com.algoviz.engine.Value.prim(queue.toString()));
        em.setField("@w", "worker-1", com.algoviz.engine.Value.prim(workerBusy[0]));
        em.setField("@w", "worker-2", com.algoviz.engine.Value.prim(workerBusy[1]));
    }

    private Trace.TraceMeta meta() {
        return new Trace.TraceMeta(id(), title(), codeResource(), "2 worker 3 任务", 3);
    }

    @Override
    public java.util.List<String> dataLines() {
        return List.of("int corePoolSize = 2;   // 提交 T1/T2/T3");
    }

    @Override
    public com.algoviz.engine.Complexity complexity() {
        return new com.algoviz.engine.Complexity("提交 O(1)，执行 = 任务本身", "O(corePoolSize + 队列深)",
                "线程创建 ~1ms + MB 级栈：1000 个任务裸开线程 = 1GB 内存；池 = 固定 n 根线程循环吃任务");
    }
}
