package com.algoviz.engine.pattern;

import com.algoviz.engine.Algorithm;
import com.algoviz.engine.Loc;
import com.algoviz.engine.Step;
import com.algoviz.engine.VizEvent;
import com.algoviz.engine.ds.SeqView;
import com.algoviz.engine.trace.Trace;

import java.util.List;

/**
 * 重构·以多态取代条件：switch/if-else 链是"还没长出来的类层次"。
 * 反例：getPrice(type) switch 三种会员——每加一种会员，方法就长一截。
 * 正例：三个子类各自实现 getPrice()——新增会员 = 新增类，老代码不动（OCP 就地达成）。
 */
public final class RefactorPolyDemo implements Algorithm {

    private static final String SCHEMA = "1";
    private static final String INVARIANT = "同一个 switch 出现两次以上 = 类层次在敲门——把分支变子类";

    private static final int L_BAD1 = 4;
    private static final int L_BAD2 = 6;
    private static final int L_GOOD = 12;

    private PatternEmitter em;

    @Override public String id() { return "refactor.poly"; }
    @Override public String title() { return "重构·以多态取代条件"; }
    @Override public String codeResource() { return "/code/RefactorPolyDemo.java"; }
    @Override public String docResource() { return "/markdown/refactor-poly.md"; }

    @Override
    public Trace produce() {
        em = new PatternEmitter(List.of(
                new SeqView.Participant("client", "client"),
                new SeqView.Participant("svc", "PriceService（switch）"),
                new SeqView.Participant("poly", "Vip/Svip/Normal.getPrice()")), INVARIANT);
        em.reset();
        // ===== 反例：switch 生长 =====
        em.message(L_BAD1, 0, 1, "getPrice(\"vip\") → case VIP: ×0.8", "CALL",
                "反例：会员类型是字符串 + switch——每加一种会员，方法就长一截");
        em.state(L_BAD2, VizEvent.EventType.COMPARE,
                "第二个方法 getPoints(type) 又一份 switch——同样的分支在项目里裂殖", List.of(),
                "switch 裂殖 = 变化原因在扩散：类型一加，处处要改");
        // ===== 正例：分支变子类 =====
        em.message(L_GOOD, 0, 2, "user.getPrice()：Vip×0.8 / Svip×0.6 / Normal×1.0（动态分派）", "CALL",
                "正例：三种会员 = 三个子类，各自实现 getPrice()——分支逻辑就地安家");
        em.state(L_GOOD, VizEvent.EventType.RETURN,
                "新增 SVVip = 新增一个类——PriceService 与 getPoints 全部零修改", List.of(),
                "什么时候**不该**重构：只有两三个分支且几乎不会新增——过度设计比 switch 更难受");
        return new Trace(SCHEMA, meta(), em.steps);
    }

    private Trace.TraceMeta meta() {
        return new Trace.TraceMeta(id(), title(), codeResource(), "switch×2 → 0", 1);
    }

    @Override
    public java.util.List<String> dataLines() {
        return List.of("// 反例: switch(type){VIP/SVIP/NORMAL} → 正例: user.getPrice() 子类各管各");
    }

    @Override
    public com.algoviz.engine.Complexity complexity() {
        return new com.algoviz.engine.Complexity("O(1) 虚方法分派", "O(n) 一个子类一行",
                "和 OCP 的关系：这一招就是达成 OCP 的具体手法——条件链变类层次，扩展点自然浮出");
    }
}
