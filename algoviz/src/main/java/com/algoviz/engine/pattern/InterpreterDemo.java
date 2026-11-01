package com.algoviz.engine.pattern;

import com.algoviz.engine.Algorithm;
import com.algoviz.engine.Value;
import com.algoviz.engine.VizEvent;
import com.algoviz.engine.ds.SeqView;
import com.algoviz.engine.trace.Trace;

import java.util.List;

/** 解释器演示：每种语法规则一个类，解释 = 语法树的后序求值。 */
public final class InterpreterDemo implements Algorithm {

    private static final String SCHEMA = "1";
    private static final String INVARIANT = "每种语法规则一个类，解释 = 语法树的后序求值——正则/SQL/公式的引擎原型";

    private static final int L_BUILD = 10;
    private static final int L_EVAL = 11;
    private static final int L_PLUS = 7;
    private static final int L_NUM = 4;

    private PatternEmitter em;

    @Override public String id() { return "pattern.interpreter"; }
    @Override public String title() { return "解释器模式"; }
    @Override public String codeResource() { return "/code/InterpreterDemo.java"; }
    @Override public String docResource() { return "/markdown/pattern-interpreter.md"; }

    @Override
    public Trace produce() {
        em = new PatternEmitter(List.of(
                new SeqView.Participant("client", "client"),
                new SeqView.Participant("plus", "e : Plus"),
                new SeqView.Participant("left", "left : Num(1)"),
                new SeqView.Participant("right", "right : Num(2)")), INVARIANT);
        em.addObject("@1", "Plus");
        em.addObject("@2", "Num");
        em.addObject("@3", "Num");
        em.setField("@1", "left", Value.ref("@2"));
        em.setField("@1", "right", Value.ref("@3"));
        em.setField("@2", "v", Value.prim(1));
        em.setField("@3", "v", Value.prim(2));
        em.mainLocals.put("e", Value.ref("@1"));
        em.state(L_BUILD, VizEvent.EventType.INIT, "语法树搭好：@1(Plus){@2, @3}——表达式即对象", List.of("@1"),
                "「1 + 2」不再是字符串，是一棵能求值的树");
        em.message(L_EVAL, 0, 1, "interpret()", "CALL", "求值开始：从根出发");
        em.message(L_PLUS, 1, 2, "left.interpret()", "CALL", "先算左子树");
        em.message(L_NUM, 2, 1, "1", "RETURN", "叶子直接返回自己的值");
        em.message(L_PLUS, 1, 3, "right.interpret()", "CALL", "再算右子树");
        em.message(L_NUM, 3, 1, "2", "RETURN", "叶子返回 2");
        em.message(L_PLUS, 1, 0, "3", "RETURN", "根把两边相加——后序遍历求值");
        em.state(L_EVAL, VizEvent.EventType.RETURN, "树有多深，递归多深——嵌套表达式自然支持", List.of("@1"),
                "嵌套：new Plus(new Plus(new Num(1), new Num(2)), new Num(3)) 也一样跑");
        return new Trace(SCHEMA, meta(new int[0]), em.steps);
    }
}
