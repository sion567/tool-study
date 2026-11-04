package com.algoviz.engine.pattern;

import com.algoviz.engine.Algorithm;
import com.algoviz.engine.Loc;
import com.algoviz.engine.Step;
import com.algoviz.engine.VizEvent;
import com.algoviz.engine.ds.SeqView;
import com.algoviz.engine.trace.Trace;

import java.util.List;

/**
 * 分布式·断路器：三态机 CLOSED → OPEN → HALF_OPEN → CLOSED。
 * 剧本：三连败触发熔断 → OPEN 快速失败（保护已崩溃的服务自愈）→ 冷却后半开试探 → 成功回 CLOSED。
 * 故意"变笨"换系统性存活——Hystrix/Resilience4j 的全部核心。
 */
public final class DistBreakerDemo implements Algorithm {

    private static final String SCHEMA = "1";
    private static final String INVARIANT =
            "CLOSED 放行、OPEN 快速失败、HALF_OPEN 试探——三态机让故障的代价从「拖死全网」变成「快速拒绝」";

    private String state = "CLOSED";
    private int failures = 0;
    private PatternEmitter em;

    @Override public String id() { return "dist.breaker"; }
    @Override public String title() { return "分布式·断路器"; }
    @Override public String codeResource() { return "/code/CircuitBreakerDemo.java"; }
    @Override public String docResource() { return "/markdown/circuit-breaker.md"; }

    @Override
    public Trace produce() {
        em = new PatternEmitter(List.of(
                new SeqView.Participant("client", "client"),
                new SeqView.Participant("breaker", "断路器"),
                new SeqView.Participant("svc", "支付服务")), INVARIANT);
        em.reset();
        state = "CLOSED";
        failures = 0;
        em.addObject("@cb", "CircuitBreaker");
        sync();
        em.state(3, VizEvent.EventType.INIT, "CLOSED：放行一切，同时统计连续失败数", List.of("@cb"),
                "断路器 = 电路保险丝：正常时它是透明的一层");
        em.message(5, 0, 2, "req#1 支付", "CALL", "请求穿过断路器打到服务");
        em.message(5, 2, 0, "200 ✓", "RETURN", "正常——计数不动");
        em.message(6, 0, 2, "req#2", "CALL", "服务开始抖动");
        em.message(6, 2, 0, "500 ✗（失败 1）", "RETURN", "连续失败 +1");
        fail();
        em.message(7, 0, 2, "req#3", "CALL", "继续打");
        em.message(7, 2, 0, "500 ✗（失败 2）", "RETURN", "失败 2——还没到阈值");
        fail();
        em.message(8, 0, 2, "req#4", "CALL", "第 4 发");
        em.message(8, 2, 0, "500 ✗（失败 3 = 阈值）", "RETURN", "三连败 → 触发熔断");
        fail();
        em.state(10, VizEvent.EventType.MARK_SORTED, "阈值到 → 熔断 OPEN：后续请求不再打服务", List.of("@cb"),
                "这时服务可能已经崩了——继续打只会把它按在地上，还拖住调用方的线程");
        em.message(12, 0, 1, "req#5", "CALL", "又来一个请求");
        em.state(12, VizEvent.EventType.COMPARE, "OPEN → 直接拒绝（fail fast），服务端零压力", List.of("@cb"),
                "调用方 1ms 拿到失败，马上走降级——比 30s 超时好一万倍");
        em.state(14, VizEvent.EventType.SET, "冷却期（如 5s）过 → HALF_OPEN：只放行一个试探请求", List.of("@cb"),
                "半开 = 谨慎乐观：给服务一次自愈的机会，但不放任流量冲击");
        em.message(16, 0, 2, "req#6（试探）", "CALL", "唯一放行的探测请求");
        em.message(16, 2, 0, "200 ✓", "RETURN", "试探成功 → 服务缓过来了");
        reset();
        em.state(17, VizEvent.EventType.RETURN, "试探成功 → 回 CLOSED，失败计数清零——全链路恢复", List.of("@cb"),
                "若试探失败则直接回 OPEN 继续等——三态机让故障代价从「拖死全网」变成「快速拒绝」");
        return new Trace(SCHEMA, meta(), em.steps);
    }

    private void fail() {
        failures++;
        sync();
    }

    private void reset() {
        state = "CLOSED";
        failures = 0;
        sync();
    }

    private void sync() {
        em.setField("@cb", "state", com.algoviz.engine.Value.prim(state));
        em.setField("@cb", "连续失败", com.algoviz.engine.Value.prim(failures));
    }

    private Trace.TraceMeta meta() {
        return new Trace.TraceMeta(id(), title(), codeResource(), "阈值3 冷却5s", 6);
    }

    @Override
    public java.util.List<String> dataLines() {
        return List.of("int threshold = 3;   // 冷却 5s，半开试探 1 发");
    }

    @Override
    public com.algoviz.engine.Complexity complexity() {
        return new com.algoviz.engine.Complexity("O(1) 每请求一次状态判断", "O(1) 计数器",
                "配套：失败回退（降级读缓存）、监控打点。Resilience4j 就是这套三态机 + 滑动窗口统计");
    }
}
