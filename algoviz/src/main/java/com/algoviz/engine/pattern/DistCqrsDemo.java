package com.algoviz.engine.pattern;

import com.algoviz.engine.Algorithm;
import com.algoviz.engine.Loc;
import com.algoviz.engine.Step;
import com.algoviz.engine.VizEvent;
import com.algoviz.engine.ds.SeqView;
import com.algoviz.engine.trace.Trace;

import java.util.List;

/**
 * 分布式·CQRS：命令（写）与查询（读）走两条路，各优化各的。
 * 剧本：下单命令 → 写模型校验落库 → 事件投影到读模型（宽表）→
 * 查询全走读模型（免 join、可缓存、可复制多副本）。
 * 代价是最终一致——收益是读写各自伸缩。
 */
public final class DistCqrsDemo implements Algorithm {

    private static final String SCHEMA = "1";
    private static final String INVARIANT =
            "写侧保一致（规范化库），读侧保速度（反规范化宽表）——中间用事件同步，接受短暂不一致";

    private PatternEmitter em;

    @Override public String id() { return "dist.cqrs"; }
    @Override public String title() { return "分布式·CQRS 读写分离"; }
    @Override public String codeResource() { return "/code/CqrsDemo.java"; }
    @Override public String docResource() { return "/markdown/cqrs.md"; }

    @Override
    public Trace produce() {
        em = new PatternEmitter(List.of(
                new SeqView.Participant("client", "client"),
                new SeqView.Participant("cmd", "命令侧（写模型）"),
                new SeqView.Participant("evt", "事件流"),
                new SeqView.Participant("qry", "查询侧（读模型宽表）")), INVARIANT);
        em.reset();
        em.addObject("@w", "WriteDb");
        em.addObject("@r", "ReadDb");
        sync("orders: 0", "order_list: []");
        em.state(3, VizEvent.EventType.INIT, "写模型 = 规范化订单库；读模型 = 反规范化的宽表", List.of("@w", "@r"),
                "一个库扛读写时：写要锁行、读要 join 两头为难——CQRS 干脆分成两个世界");
        // 命令：下单
        em.message(5, 0, 1, "command: 创建订单(张三, ¥199)", "CALL", "命令 = 有业务意图的写");
        em.message(5, 1, 0, "校验 ✓ 落库 ✓", "RETURN", "写模型：事务、约束、不变量全在这一侧");
        em.setField("@w", "orders", com.algoviz.engine.Value.prim("orders: 1"));
        em.message(6, 1, 2, "publish OrderCreated", "CALL", "落库后发事件——不直接改读库（解耦）");
        em.message(6, 2, 3, "投影：订单摘要入宽表", "CALL", "读模型按查询的形状存数据（已拼好名字、金额、状态）");
        em.message(6, 3, 2, "宽表 +1 行", "RETURN", "同步完成——通常有毫秒级延迟");
        sync("orders: 1", "order_list: [张三 ¥199 待发货]");
        // 查询：我的订单
        em.message(8, 0, 3, "query: 我的订单", "CALL", "查询全走读模型");
        em.message(8, 3, 0, "return 订单列表（0 join，可缓存）", "RETURN", "宽表即答案：读侧可以任意复制、加缓存、上搜索引擎");
        em.state(9, VizEvent.EventType.SET, "读写比 99:1 的系统：99% 流量在宽表上横行", List.of("@r"),
                "读侧多副本扛流量；写侧单点保一致——各干各的");
        em.state(11, VizEvent.EventType.COMPARE, "下单后立刻查，读侧可能还差一拍（最终一致）", List.of("@r"),
                "CQRS 的唯一代价：读到几毫秒~几秒前的旧数据——多数业务（订单列表）完全可接受");
        em.state(12, VizEvent.EventType.RETURN, "命令/查询分离 + 事件同步 = 各自伸缩", List.of("@w", "@r"),
                "延伸：事件溯源（Event Sourcing）把写模型也变成事件流——CQRS 常与它成对出现");
        return new Trace(SCHEMA, meta(), em.steps);
    }

    private void sync(String w, String r) {
        em.setField("@w", "写模型", com.algoviz.engine.Value.prim(w));
        em.setField("@r", "读模型宽表", com.algoviz.engine.Value.prim(r));
    }

    private Trace.TraceMeta meta() {
        return new Trace.TraceMeta(id(), title(), codeResource(), "1 命令 1 查询", 2);
    }

    @Override
    public java.util.List<String> dataLines() {
        return List.of("// 下单(张三, ¥199) → 查「我的订单」");
    }

    @Override
    public com.algoviz.engine.Complexity complexity() {
        return new com.algoviz.engine.Complexity("写 O(1) / 读 O(1) 宽表直查", "O(数据双份)",
                "空间换吞吐的架构版：数据存两份（规范库 + 宽表），换读写各自无限伸缩");
    }
}
