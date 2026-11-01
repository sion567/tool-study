package com.algoviz.engine.pattern;

import com.algoviz.engine.Algorithm;
import com.algoviz.engine.Value;
import com.algoviz.engine.VizEvent;
import com.algoviz.engine.ds.SeqView;
import com.algoviz.engine.trace.Trace;

import java.util.List;

/** 迭代器模式演示：怎么走封装在迭代器里，调用方只认 hasNext/next（游标可视化）。 */
public final class IteratorDemo implements Algorithm {

    private static final String SCHEMA = "1";
    private static final String INVARIANT =
            "怎么走封装在迭代器里，调用方只问 hasNext/next——for-each 是它的语法糖";

    private static final int L_IT = 4;
    private static final int L_HAS = 5;
    private static final int L_NEXT = 6;
    private static final int L_BODY = 7;
    private static final int L_END = 8;

    private PatternEmitter em;
    private int cursor;

    public IteratorDemo() {
        em = new PatternEmitter(List.of(
                new SeqView.Participant("client", "client"),
                new SeqView.Participant("iterator", "it : Iterator")), INVARIANT);
    }

    @Override
    public String id() {
        return "pattern.iterator";
    }

    @Override
    public String title() {
        return "迭代器模式";
    }

    @Override
    public String codeResource() {
        return "/code/IteratorDemo.java";
    }

    @Override
    public String docResource() {
        return "/markdown/pattern-iterator.md";
    }

    @Override
    public Trace produce() {
        em.reset();
        em.addObject("@1", "List");
        em.setField("@1", "elements", Value.prim("[step1, step2, step3]"));
        em.addObject("@2", "Iterator");
        em.setField("@2", "cursor", Value.prim(0));
        em.mainLocals.put("steps", Value.ref("@1"));
        em.mainLocals.put("it", Value.ref("@2"));
        em.state(L_IT, VizEvent.EventType.INIT,
                "iterator()：@2.cursor = 0（游标站在第一个元素之前）", List.of("@2"),
                "集合把「怎么走」的职责整个交给了迭代器对象");
        for (int i = 1; i <= 3; i++) {
            em.message(L_HAS, 0, 1, "hasNext() → true", "CALL", "先问还有没有——游标没到头就继续");
            em.message(L_NEXT, 0, 1, "next() → step" + i, "CALL", "取当前元素，游标右移");
            cursor = i;
            em.setField("@2", "cursor", Value.prim(cursor));
            em.state(L_NEXT, VizEvent.EventType.SET,
                    "@2.cursor: " + (cursor - 1) + " → " + cursor + "（游标右移）", List.of("@2"),
                    "游标状态活在迭代器内部——调用方永远不用操心下标");
            em.message(L_BODY, 0, 1, "播放 step" + i, "CALL", "拿到元素，干活");
        }
        em.message(L_HAS, 0, 1, "hasNext() → false", "CALL", "游标到头，循环退出");
        em.state(L_END, VizEvent.EventType.RETURN,
                "遍历结束：3 个元素，一个没漏一个没重", List.of("@2"),
                "换一种集合（链表/树），调用方代码不变——变的只是迭代器内部");
        return new Trace(SCHEMA, meta(new int[0]), em.steps);
    }
}
