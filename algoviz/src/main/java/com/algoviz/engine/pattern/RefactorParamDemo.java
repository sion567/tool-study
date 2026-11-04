package com.algoviz.engine.pattern;

import com.algoviz.engine.Algorithm;
import com.algoviz.engine.Loc;
import com.algoviz.engine.Step;
import com.algoviz.engine.VizEvent;
import com.algoviz.engine.ds.SeqView;
import com.algoviz.engine.trace.Trace;

import java.util.List;

/**
 * 重构·引入参数对象：把总是结伴出现的参数打包成一个对象。
 * 反例：register(name, age, addr, phone, email) 五参齐飞——加个字段，全链签名都要动。
 * 正例：打包成 UserInfo——签名稳定，字段演化只碰对象。
 */
public final class RefactorParamDemo implements Algorithm {

    private static final String SCHEMA = "1";
    private static final String INVARIANT = "总是一起传的参数 = 概念上是一个东西——打包它，签名就稳定了";

    private static final int L_BAD1 = 4;
    private static final int L_BAD2 = 6;
    private static final int L_GOOD = 12;

    private PatternEmitter em;

    @Override public String id() { return "refactor.param"; }
    @Override public String title() { return "重构·引入参数对象"; }
    @Override public String codeResource() { return "/code/RefactorParamDemo.java"; }
    @Override public String docResource() { return "/markdown/refactor-param.md"; }

    @Override
    public Trace produce() {
        em = new PatternEmitter(List.of(
                new SeqView.Participant("client", "client"),
                new SeqView.Participant("svc", "UserService（5 参签名）"),
                new SeqView.Participant("obj", "UserInfo 参数对象")), INVARIANT);
        em.reset();
        // ===== 反例：参数链 =====
        em.message(L_BAD1, 0, 1, "register(name, age, addr, phone, email)", "CALL",
                "反例：五个参数结伴出行——调用方要背顺序，容易把 phone 和 age 传反（都是数字）");
        em.state(L_BAD2, VizEvent.EventType.COMPARE,
                "新增「微信号」字段 → 签名变 6 参 → 全项目调用点逐个改", List.of(),
                "签名是 API 契约：参数越多，契约越脆");
        // ===== 正例：打包 =====
        em.message(L_GOOD, 0, 2, "register(UserInfo u)——签名从此不变", "CALL",
                "正例：结伴参数打包成 UserInfo——加字段只动对象内部，调用点不动");
        em.state(L_GOOD, VizEvent.EventType.RETURN,
                "编译器还顺带管住了「name 和 phone 传反」这类错——类型不对直接不过", List.of(),
                "延伸：参数超 3 个就闻一闻——里面是不是藏着一个没被命名的概念");
        return new Trace(SCHEMA, meta(), em.steps);
    }

    private Trace.TraceMeta meta() {
        return new Trace.TraceMeta(id(), title(), codeResource(), "5 参 → 1 对象", 1);
    }

    @Override
    public java.util.List<String> dataLines() {
        return List.of("// 反例: register(name, age, addr, phone, email) → 正例: register(UserInfo u)");
    }

    @Override
    public com.algoviz.engine.Complexity complexity() {
        return new com.algoviz.engine.Complexity("O(1)", "O(1) 多一个小对象",
                " neighboring 判据：几个参数总是一起出现、总是一起变化——它们就是一个对象被拆散了");
    }
}
