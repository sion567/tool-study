package com.algoviz.engine.pattern;

import com.algoviz.engine.Algorithm;
import com.algoviz.engine.Value;
import com.algoviz.engine.VizEvent;
import com.algoviz.engine.ds.SeqView;
import com.algoviz.engine.trace.Trace;

import java.util.List;

/**
 * 模板方法模式演示（蓝图 §5.2 第一批；反向引用：本项目 Algorithm 的执行骨架）。
 * 画面核心：消息顺序 load → compute → render 由 final 骨架锁死，子类只填 compute。
 */
public final class TemplateMethodDemo implements Algorithm {

    private static final String SCHEMA = "1";
    private static final String INVARIANT =
            "骨架顺序由 final run() 锁死：load → compute → render，子类只填变化点";

    /** 与 /code/TemplateMethodDemo.java 完整清单的行号对齐 */
    private static final int L_RUN = 20;
    private static final int L_LOAD = 9;
    private static final int L_COMPUTE = 15;    // 子类的实现
    private static final int L_RENDER = 11;
    private static final int L_FINAL = 4;       // final run() 签名行

    private PatternEmitter em;

    public TemplateMethodDemo() {
        em = new PatternEmitter(List.of(
                new SeqView.Participant("client", "client"),
                new SeqView.Participant("skeleton", "run() : 骨架(final)"),
                new SeqView.Participant("impl", "compute() : 子类(冒泡)")), INVARIANT);
    }

    @Override
    public String id() {
        return "pattern.templatemethod";
    }

    @Override
    public String title() {
        return "模板方法模式";
    }

    @Override
    public String codeResource() {
        return "/code/TemplateMethodDemo.java";
    }

    @Override
    public String docResource() {
        return "/markdown/pattern-templatemethod.md";
    }

    @Override
    public Trace produce() {
        em.reset();
        em.addObject("@1", "BubbleSortRunner");
        em.addObject("@2", "BubbleSortStep");
        em.mainLocals.put("runner", Value.ref("@1"));
        em.message(L_RUN, 0, 1, "run()", "CALL", "入口只有一个：骨架方法（final，谁也改不了顺序）");
        em.message(L_LOAD, 1, 1, "load()〔公共〕", "CALL", "① 公共步骤：骨架自己实现，所有子类共享");
        em.message(L_COMPUTE, 1, 2, "compute()〔冒泡实现〕", "CALL", "② 变化点：钩子方法，子类填自己的算法");
        em.message(L_RENDER, 1, 1, "render()〔公共〕", "CALL", "③ 公共步骤：收尾也定死");
        em.state(L_FINAL, VizEvent.EventType.RETURN,
                "顺序永远 ①②③——子类换的是 compute 的内容，不是 run 的结构", List.of("@1"),
                "模板方法 = 好莱坞原则：别调用我们，我们会调用你");
        return new Trace(SCHEMA, meta(new int[0]), em.steps);
    }
}
