package com.algoviz.engine.pattern;

import com.algoviz.engine.Algorithm;
import com.algoviz.engine.Value;
import com.algoviz.engine.VizEvent;
import com.algoviz.engine.ds.SeqView;
import com.algoviz.engine.trace.Trace;

import java.util.List;

/** 命令模式演示：请求打包成对象——排队、撤销、日志都成了对对象的操作。 */
public final class CommandDemo implements Algorithm {

    private static final String SCHEMA = "1";
    private static final String INVARIANT =
            "请求打包成对象 → 排队/撤销/日志都成了对对象的操作；调用者与执行者彻底解耦";

    private static final int L_NEW = 14;
    private static final int L_SUB1 = 15;
    private static final int L_SUB2 = 16;
    private static final int L_FLUSH = 17;
    private static final int L_ADD = 10;
    private static final int L_LOOP = 11;
    private static final int L_CMD_BODY = 6;

    private PatternEmitter em;

    public CommandDemo() {
        em = new PatternEmitter(List.of(
                new SeqView.Participant("client", "client"),
                new SeqView.Participant("invoker", "invoker : Invoker"),
                new SeqView.Participant("cmd1", "cmd1 : Command"),
                new SeqView.Participant("cmd2", "cmd2 : Command")), INVARIANT);
    }

    @Override
    public String id() {
        return "pattern.command";
    }

    @Override
    public String title() {
        return "命令模式";
    }

    @Override
    public String codeResource() {
        return "/code/CommandDemo.java";
    }

    @Override
    public String docResource() {
        return "/markdown/pattern-command.md";
    }

    @Override
    public Trace produce() {
        em.reset();
        em.addObject("@1", "Invoker");
        em.setField("@1", "queue", Value.prim("[]"));
        em.addObject("@2", "AddStepCommand");
        em.setField("@2", "step", Value.prim("step1"));
        em.addObject("@3", "AddStepCommand");
        em.setField("@3", "step", Value.prim("step2"));
        em.mainLocals.put("invoker", Value.ref("@1"));
        em.state(L_NEW, VizEvent.EventType.INIT,
                "new Invoker()：@1.queue = []——两个请求已经打包成对象 @2、@3", List.of("@1"),
                "关键动作在 new 的时候完成了：请求 → 对象");
        em.message(L_SUB1, 0, 1, "submit(cmd1)", "CALL", "请求入队——注意：只是排队，没人执行");
        em.setField("@1", "queue", Value.prim("[@2]"));
        em.state(L_ADD, VizEvent.EventType.SET, "@1.queue += @2（排队中）", List.of("@1"),
                "排队 = 往列表放对象，和执行彻底分离");
        em.message(L_SUB2, 0, 1, "submit(cmd2)", "CALL", "第二条也入队");
        em.setField("@1", "queue", Value.prim("[@2, @3]"));
        em.state(L_ADD, VizEvent.EventType.SET, "@1.queue += @3（排队中）", List.of("@1"),
                "Invoker 只看到 Command 接口——里面装的是什么，它不知道");
        em.message(L_FLUSH, 0, 1, "flush()", "CALL", "统一执行：遍历队列逐个调 execute()");
        em.message(L_LOOP, 1, 2, "execute()", "CALL", "第一条命令被执行");
        em.message(L_CMD_BODY, 2, 1, "播放 step1", "RETURN", "命令对象自己知道要干什么");
        em.message(L_LOOP, 1, 3, "execute()", "CALL", "第二条");
        em.message(L_CMD_BODY, 3, 1, "播放 step2", "RETURN", "完成");
        em.state(L_FLUSH, VizEvent.EventType.RETURN,
                "两帧排队、一次执行——撤销 = 记历史栈，日志 = 序列化，宏 = 命令列表", List.of("@1"),
                "把请求变成对象之后，一切针对「请求」的花活都只是对对象的操作");
        return new Trace(SCHEMA, meta(new int[0]), em.steps);
    }
}
