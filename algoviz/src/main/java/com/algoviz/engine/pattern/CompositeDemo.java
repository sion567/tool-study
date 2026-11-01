package com.algoviz.engine.pattern;

import com.algoviz.engine.Algorithm;
import com.algoviz.engine.Value;
import com.algoviz.engine.VizEvent;
import com.algoviz.engine.ds.SeqView;
import com.algoviz.engine.trace.Trace;

import java.util.List;

/** 组合模式演示：叶子和容器统一对待——对整棵树只调一次 draw。 */
public final class CompositeDemo implements Algorithm {

    private static final String SCHEMA = "1";
    private static final String INVARIANT = "叶子和容器统一对待：递归结构 = 同一个接口反复调用——UI 树、文件系统全是它";

    private static final int L_ADD = 13;
    private static final int L_DRAW = 14;
    private static final int L_LOOP = 9;
    private static final int L_BAR_BODY = 4;

    private PatternEmitter em;

    @Override public String id() { return "pattern.composite"; }
    @Override public String title() { return "组合模式"; }
    @Override public String codeResource() { return "/code/CompositeDemo.java"; }
    @Override public String docResource() { return "/markdown/pattern-composite.md"; }

    @Override
    public Trace produce() {
        em = new PatternEmitter(List.of(
                new SeqView.Participant("client", "client"),
                new SeqView.Participant("chart", "root : Chart(容器)"),
                new SeqView.Participant("bar1", "bar1 : Bar(叶)"),
                new SeqView.Participant("bar2", "bar2 : Bar(叶)")), INVARIANT);
        em.addObject("@1", "Chart");
        em.addObject("@2", "Bar");
        em.addObject("@3", "Bar");
        em.setField("@1", "children", Value.prim("[@2, @3]"));
        em.mainLocals.put("root", Value.ref("@1"));
        em.state(L_ADD, VizEvent.EventType.INIT, "@1.children = [@2, @3]——叶子和容器装在同一个名单里", List.of("@1"),
                "Composite 也实现 Node：容器可以套容器，树就长起来了");
        em.message(L_DRAW, 0, 1, "root.draw()", "CALL", "对整棵树只调一次——调用方不管里面有几个、是什么");
        em.message(L_LOOP, 1, 2, "n.draw()", "CALL", "容器把调用转给第一个孩子");
        em.message(L_BAR_BODY, 2, 1, "画一根柱子", "RETURN", "叶子真正干活");
        em.message(L_LOOP, 1, 3, "n.draw()", "CALL", "第二个孩子");
        em.message(L_BAR_BODY, 3, 1, "画一根柱子", "RETURN", "完成");
        em.message(L_DRAW, 1, 0, "整棵树画完", "RETURN", "若孩子还是容器，这里会递归下去——结构任意深，代码零特判");
        em.state(L_DRAW, VizEvent.EventType.RETURN, "调用方没有写一句 if (x 是叶子) … else …——类型统一就是威力", List.of("@1"),
                "反向引用：本工具左侧的 TreeView 节点树就是组合模式");
        return new Trace(SCHEMA, meta(new int[0]), em.steps);
    }
}
