package com.algoviz.engine.pattern;

import com.algoviz.engine.Algorithm;
import com.algoviz.engine.Value;
import com.algoviz.engine.VizEvent;
import com.algoviz.engine.ds.SeqView;
import com.algoviz.engine.trace.Trace;

import java.util.List;

/** 单例模式演示：两次 getInstance，同一根引用——看堆面板 @id 不变。 */
public final class SingletonDemo implements Algorithm {

    private static final String SCHEMA = "1";
    private static final String INVARIANT = "无论调多少次 getInstance，全局只有一根引用——第一次创建，之后只读";

    private static final int L_FIRST = 9;
    private static final int L_RET = 6;
    private static final int L_SECOND = 10;
    private static final int L_CHECK = 11;

    private PatternEmitter em;

    @Override public String id() { return "pattern.singleton"; }
    @Override public String title() { return "单例模式"; }
    @Override public String codeResource() { return "/code/SingletonDemo.java"; }
    @Override public String docResource() { return "/markdown/pattern-singleton.md"; }

    @Override
    public Trace produce() {
        em = new PatternEmitter(List.of(
                new SeqView.Participant("client", "client"),
                new SeqView.Participant("holder", "Holder : 类加载器")), INVARIANT);
        em.reset();
        em.message(L_FIRST, 0, 1, "getInstance()", "CALL", "第一次：触发 Holder 类加载，实例在这时诞生");
        em.addObject("@1", "Config");
        em.state(L_RET, VizEvent.EventType.SET, "造出 @1 并返回（全局唯一的那份）", List.of("@1"),
                "静态初始化由 JVM 保证只跑一次、线程安全");
        em.message(L_RET, 1, 0, "return @1", "RETURN", "把同一根引用交给调用方");
        em.message(L_SECOND, 0, 1, "getInstance()", "CALL", "第二次：类已加载，直接拿现成的");
        em.message(L_RET, 1, 0, "return @1（同一个！）", "RETURN", "没有第二次 new——堆面板里还是 @1");
        em.state(L_CHECK, VizEvent.EventType.RETURN, "a == b → true：两根引用指向同一个对象", List.of("@1"),
                "单例的本质：把「只造一次」写进类结构，而不是靠调用方自觉");
        return new Trace(SCHEMA, meta(new int[0]), em.steps);
    }
}
