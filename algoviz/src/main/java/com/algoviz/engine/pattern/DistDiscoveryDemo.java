package com.algoviz.engine.pattern;

import com.algoviz.engine.Algorithm;
import com.algoviz.engine.Loc;
import com.algoviz.engine.Step;
import com.algoviz.engine.VizEvent;
import com.algoviz.engine.ds.SeqView;
import com.algoviz.engine.trace.Trace;

import java.util.List;

/**
 * 分布式·服务发现：注册表是电话簿，实例上线注册、心跳续约、死亡摘除、恢复回归。
 * 剧本：两实例注册 → 客户端查询 → 负载均衡选一个 → 实例心跳超时被摘除 →
 * 客户端再查（无感知切换）→ 实例恢复重新注册。
 * 客户端对故障无感知 = 服务自愈的关键拼图。
 */
public final class DistDiscoveryDemo implements Algorithm {

    private static final String SCHEMA = "1";
    private static final String INVARIANT =
            "注册表 = 电话簿：注册/心跳/摘除/回归四个动作——调用方只认名字，永远不写死 IP";

    private final java.util.List<String> registry = new java.util.ArrayList<>();
    private PatternEmitter em;

    @Override public String id() { return "dist.discovery"; }
    @Override public String title() { return "分布式·服务发现"; }
    @Override public String codeResource() { return "/code/ServiceDiscoveryDemo.java"; }
    @Override public String docResource() { return "/markdown/service-discovery.md"; }

    @Override
    public Trace produce() {
        em = new PatternEmitter(List.of(
                new SeqView.Participant("i1", "user-svc@10.0.0.1"),
                new SeqView.Participant("i2", "user-svc@10.0.0.2"),
                new SeqView.Participant("reg", "注册表"),
                new SeqView.Participant("client", "客户端")), INVARIANT);
        em.reset();
        registry.clear();
        em.addObject("@reg", "Registry");
        sync();
        em.state(3, VizEvent.EventType.INIT, "注册表空——写死 IP 的时代：一台挂了全网报错", List.of("@reg"),
                "云上实例随扩缩容随时生灭——IP 不可靠，名字才可靠");
        // 注册
        em.message(5, 0, 2, "register(user-svc @.1)", "CALL", "实例 1 启动 → 上报自己的地址");
        em.message(5, 2, 0, "OK", "RETURN", "注册表 [10.0.0.1]");
        register("@10.0.0.1");
        em.message(6, 1, 2, "register(user-svc @.2)", "CALL", "实例 2 也上线");
        em.message(6, 2, 0, "OK", "RETURN", "注册表 [@.1, @.2]——每 10s 心跳续约");
        register("@10.0.0.2");
        // 查询 + 调用
        em.message(8, 3, 2, "lookup(user-svc)", "CALL", "客户端按名字查——不关心几台、在哪");
        em.message(8, 2, 3, "return [@.1, @.2]", "RETURN", "拿到存活列表 → 负载均衡挑一个");
        em.message(9, 3, 1, "call @.2", "CALL", "轮询选中实例 2");
        em.message(9, 1, 3, "200 ✓", "RETURN", "调用成功");
        // 故障 + 摘除
        em.state(11, VizEvent.EventType.COMPARE, "@.1 心跳超时（90s 没续约）→ 注册表摘除它", List.of("@reg"),
                "摘除靠心跳，不靠谁上报死亡——宕机方自己是不报丧的");
        remove("@10.0.0.1");
        em.message(12, 3, 2, "lookup(user-svc)", "CALL", "客户端再查");
        em.message(12, 2, 3, "return [@.2]", "RETURN", "只剩 @.2——流量自动全切，客户端代码零改动");
        // 恢复
        em.message(14, 0, 2, "re-register @.1（进程被守护拉起）", "CALL", "实例 1 恢复 → 重新注册");
        em.message(14, 2, 0, "OK", "RETURN", "回到池子——下一轮查询就能分到流量");
        register("@10.0.0.1");
        em.state(16, VizEvent.EventType.RETURN, "全程客户端无感知：挂、切、愈全自动", List.of("@reg"),
                "服务发现 + 健康检查 = 自愈系统的电话簿；Kubernetes 的 Service/DNS 就是这套机制的平台化");
        return new Trace(SCHEMA, meta(), em.steps);
    }

    private void register(String addr) {
        if (!registry.contains(addr)) {
            registry.add(addr);
        }
        sync();
    }

    private void remove(String addr) {
        registry.remove(addr);
        sync();
    }

    private void sync() {
        em.setField("@reg", "user-svc 实例", com.algoviz.engine.Value.prim(registry.toString()));
    }

    private Trace.TraceMeta meta() {
        return new Trace.TraceMeta(id(), title(), codeResource(), "2 实例 1 故障", 2);
    }

    @Override
    public java.util.List<String> dataLines() {
        return List.of("// 心跳 10s 续约，90s 未续约摘除");
    }

    @Override
    public com.algoviz.engine.Complexity complexity() {
        return new com.algoviz.engine.Complexity("lookup O(1)（注册表内存）", "O(实例数)",
                "CAP 提示：注册表自身也要多副本（Eureka AP / ZooKeeper CP）——电话簿挂了比一台服务挂了更糟");
    }
}
