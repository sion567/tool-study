package com.algoviz.engine.pattern;

import com.algoviz.engine.Algorithm;
import com.algoviz.engine.Value;
import com.algoviz.engine.VizEvent;
import com.algoviz.engine.ds.SeqView;
import com.algoviz.engine.trace.Trace;

import java.util.List;

/** 访问者演示：结构稳定、操作多变——双重分发把「按类型分派」从 if-else 搬进方法调用。 */
public final class VisitorDemo implements Algorithm {

    private static final String SCHEMA = "1";
    private static final String INVARIANT = "结构稳定、操作多变：双车分发（accept + visit 重载）把「按类型分派」搬进方法调用";

    private static final int L_NEW = 19;
    private static final int L_LOOP = 20;
    private static final int L_ACCEPT_C = 4;
    private static final int L_ACCEPT_S = 7;
    private static final int L_VISIT_C = 14;
    private static final int L_VISIT_S = 15;

    private PatternEmitter em;

    @Override public String id() { return "pattern.visitor"; }
    @Override public String title() { return "访问者模式"; }
    @Override public String codeResource() { return "/code/VisitorDemo.java"; }
    @Override public String docResource() { return "/markdown/pattern-visitor.md"; }

    @Override
    public Trace produce() {
        em = new PatternEmitter(List.of(
                new SeqView.Participant("client", "client"),
                new SeqView.Participant("visitor", "v : DrawVisitor"),
                new SeqView.Participant("circle", "s1 : Circle"),
                new SeqView.Participant("square", "s2 : Square")), INVARIANT);
        em.addObject("@1", "DrawVisitor");
        em.addObject("@2", "Circle");
        em.addObject("@3", "Square");
        em.mainLocals.put("v", Value.ref("@1"));
        em.state(L_NEW, VizEvent.EventType.INIT, "结构（@2 @3）与操作（@1）分家：元素类里没有一行绘制代码", List.of("@1"),
                "想加「导出 SVG」？新写一个 Visitor——元素类一行不改");
        em.message(L_LOOP, 0, 2, "accept(v)", "CALL", "第一跳：元素接受访问者");
        em.message(L_ACCEPT_C, 2, 1, "visitCircle(this)", "CALL", "第二跳：按元素类型分派到对应方法（this 带着类型来了）");
        em.message(L_VISIT_C, 1, 2, "画圆", "RETURN", "操作在 Visitor 侧完成");
        em.message(L_ACCEPT_C, 2, 0, "完成", "RETURN", "Circle 的任务只是把自己交给访问者");
        em.message(L_LOOP, 0, 3, "accept(v)", "CALL", "轮到 Square");
        em.message(L_ACCEPT_S, 3, 1, "visitSquare(this)", "CALL", "又落到另一个重载——类型分派完成");
        em.message(L_VISIT_S, 1, 3, "画方", "RETURN", "各走各的方法");
        em.message(L_ACCEPT_S, 3, 0, "完成", "RETURN", "循环结束");
        em.state(L_LOOP, VizEvent.EventType.RETURN, "结构不变加操作，或操作不变加结构—— Visitor 管前者，子类管后者", List.of("@1"),
                "反向引用：编译器的 AST 处理、ASM 字节码框架，全是访问者");
        return new Trace(SCHEMA, meta(new int[0]), em.steps);
    }
}
