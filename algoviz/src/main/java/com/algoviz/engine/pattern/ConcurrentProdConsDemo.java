package com.algoviz.engine.pattern;

import com.algoviz.engine.Algorithm;
import com.algoviz.engine.Loc;
import com.algoviz.engine.OrderedMaps;
import com.algoviz.engine.Step;
import com.algoviz.engine.VizEvent;
import com.algoviz.engine.ds.SeqView;
import com.algoviz.engine.trace.Trace;

import java.util.List;

/**
 * 并发·生产者-消费者（有界队列，容量 2）：队列是两个线程之间唯一的握手点。
 * 脚本化交错（确定性地演示真实竞态的典型时序）：
 * 队列满 → 生产者阻塞（背压）；队列空 → 消费者阻塞；put/take 互相唤醒。
 * 这就是 BlockingQueue 的全部语义。
 */
public final class ConcurrentProdConsDemo implements Algorithm {

    private static final String SCHEMA = "1";
    private static final String INVARIANT =
            "队列满则生产者等、空则消费者等——有界队列用「阻塞」把两个快慢不同的线程解耦";

    private static final int L_PUT = 6;
    private static final int L_FULL = 8;
    private static final int L_TAKE = 12;
    private static final int L_EMPTY = 16;

    private final java.util.List<String> items = new java.util.ArrayList<>();
    private PatternEmitter em;

    @Override public String id() { return "concurrent.prodcons"; }
    @Override public String title() { return "并发·生产者-消费者"; }
    @Override public String codeResource() { return "/code/ProdConsDemo.java"; }
    @Override public String docResource() { return "/markdown/prodcons.md"; }

    @Override
    public Trace produce() {
        em = new PatternEmitter(List.of(
                new SeqView.Participant("p", "生产者线程"),
                new SeqView.Participant("q", "BoundedQueue(2)"),
                new SeqView.Participant("c", "消费者线程")), INVARIANT);
        em.reset();
        items.clear();
        em.addObject("@q", "BoundedQueue");
        syncQueue();
        em.state(3, VizEvent.EventType.INIT, "队列容量 2，当前 []——两个线程只通过它握手", List.of("@q"),
                "生产者消费者解耦的本质：双方不认识对方，只认识队列");
        // 生产快：连放三个，第三个被背压
        em.message(L_PUT, 0, 1, "put(A)", "CALL", "生产节奏快：A 入队");
        put("A");
        em.message(L_PUT, 0, 1, "put(B)", "CALL", "B 入队——队列到容量上限");
        put("B");
        em.message(L_FULL, 0, 1, "put(C)", "CALL", "C 到达——但队列已满");
        em.state(L_FULL, VizEvent.EventType.COMPARE, "队列满 [A,B] → put(C) 阻塞（挂起，等待 notFull）", List.of("@q"),
                "背压：生产者被迫等消费者——有界队列防止内存被撑爆");
        // 消费者拉动
        em.message(L_TAKE, 2, 1, "take()", "CALL", "消费者来取");
        em.message(L_TAKE, 1, 2, "return A", "RETURN", "A 出队 → 消费者拿到；队列 [B]");
        take();
        em.state(L_TAKE, VizEvent.EventType.SET, "队列有空位 → 唤醒被阻塞的 put(C)", List.of("@q"),
                "条件队列：take 释放名额时顺带唤醒等待的生产者");
        em.message(L_FULL, 0, 1, "put(C) 恢复", "RETURN", "C 补位入队 → 队列 [B,C]");
        put("C");
        // 消费快：连取三个，第四个被阻塞
        em.message(L_TAKE, 2, 1, "take()", "CALL", "消费加速：取 B");
        em.message(L_TAKE, 1, 2, "return B", "RETURN", "B 出队，队列 [C]");
        take();
        em.message(L_TAKE, 2, 1, "take()", "CALL", "取 C");
        em.message(L_TAKE, 1, 2, "return C", "RETURN", "C 出队，队列 []");
        take();
        em.message(L_EMPTY, 2, 1, "take()", "CALL", "还想取——但队列空了");
        em.state(L_EMPTY, VizEvent.EventType.COMPARE, "队列空 → take() 阻塞（等待 notEmpty）", List.of("@q"),
                "对称的另一半：消费者也会被空队列挂起");
        em.message(L_EMPTY, 0, 1, "put(D)", "CALL", "生产者送来 D，队列 [D]");
        put("D");
        em.state(L_EMPTY, VizEvent.EventType.SET, "put 唤醒 take → 消费者拿到 D，双双收工", List.of("@q"),
                "put/take 互为对方的唤醒条件——阻塞 + 唤醒就是全部协作协议");
        em.state(20, VizEvent.EventType.RETURN, "全程无一处「共享变量轮询」——阻塞队列把同步细节封装干净", List.of("@q"),
                "Java 直接用 ArrayBlockingQueue/LinkedBlockingQueue；理解了这张图，消息队列（Kafka/RabbitMQ）也是同一张图放大");
        return new Trace(SCHEMA, meta(), em.steps);
    }

    private void put(String v) {
        items.add(v);
        syncQueue();
    }

    private void take() {
        if (!items.isEmpty()) {
            items.remove(0);
        }
        syncQueue();
    }

    private void syncQueue() {
        em.setField("@q", "items", com.algoviz.engine.Value.prim(items.toString()));
        em.setField("@q", "capacity", com.algoviz.engine.Value.prim(2));
    }

    private Trace.TraceMeta meta() {
        return new Trace.TraceMeta(id(), title(), codeResource(), "容量2 A/B/C/D", 4);
    }

    @Override
    public java.util.List<String> dataLines() {
        return List.of("int capacity = 2;   // A/B/C/D 依次流过");
    }

    @Override
    public com.algoviz.engine.Complexity complexity() {
        return new com.algoviz.engine.Complexity("put/take O(1)", "O(capacity) 队列",
                "快的一方被队列拖住（背压），慢的一方永不丢消息——解耦 + 缓冲 + 削峰，一个结构三件事");
    }
}
