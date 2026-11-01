package com.algoviz.engine.pattern;

import com.algoviz.engine.Algorithm;
import com.algoviz.engine.Value;
import com.algoviz.engine.VizEvent;
import com.algoviz.engine.ds.SeqView;
import com.algoviz.engine.trace.Trace;

import java.util.List;

/** 原型模式演示：从模板复制起步——深/浅拷贝的分界就在 clone 里那两行。 */
public final class PrototypeDemo implements Algorithm {

    private static final String SCHEMA = "1";
    private static final String INVARIANT = "复制现成对象当起点，省掉重建成本——深/浅拷贝的分界就在 clone 里";

    private static final int L_TPL = 13;
    private static final int L_CLONE = 15;
    private static final int L_COPY_MADE = 6;
    private static final int L_MUTATE = 16;
    private static final int L_PROOF = 17;

    private PatternEmitter em;

    @Override public String id() { return "pattern.prototype"; }
    @Override public String title() { return "原型模式"; }
    @Override public String codeResource() { return "/code/PrototypeDemo.java"; }
    @Override public String docResource() { return "/markdown/pattern-prototype.md"; }

    @Override
    public Trace produce() {
        em = new PatternEmitter(List.of(
                new SeqView.Participant("client", "client"),
                new SeqView.Participant("tpl", "tpl : Sheet(模板)"),
                new SeqView.Participant("copy", "copy : Sheet(副本)")), INVARIANT);
        em.addObject("@1", "Sheet");
        em.setField("@1", "owner", Value.prim("模板"));
        em.setField("@1", "cells", Value.prim("[A1]"));
        em.mainLocals.put("tpl", Value.ref("@1"));
        em.state(L_TPL, VizEvent.EventType.INIT, "@1 模板就位（owner=模板, cells=[A1]）", List.of("@1"),
                "昂贵的创建过程只走一次：这个对象从此当模板");
        em.message(L_CLONE, 0, 1, "clone()", "CALL", "要一个新 Sheet——但不想重走初始化");
        em.addObject("@2", "Sheet");
        em.setField("@2", "owner", Value.prim("模板"));
        em.setField("@2", "cells", Value.prim("[A1]"));
        em.state(L_COPY_MADE, VizEvent.EventType.SET, "@2 从模板复制而来（字段同值）", List.of("@2"),
                "clone 里逐字段复制：引用字段再拷一层 = 深拷贝（第 8 行正是这么干的）");
        em.message(L_MUTATE, 0, 2, "改副本：owner/cells", "CALL", "只动副本");
        em.setField("@2", "owner", Value.prim("副本"));
        em.setField("@2", "cells", Value.prim("[改过]"));
        em.state(L_MUTATE, VizEvent.EventType.SET, "@2.owner ← 副本、cells ← [改过]", List.of("@2"),
                "检验深浅拷贝的时刻：模板动没动？");
        em.state(L_PROOF, VizEvent.EventType.RETURN, "模板 @1 纹丝不动——两个对象各自独立", List.of("@1"),
                "若 clone 里直接 copy.cells = cells（浅拷贝），这里模板就被污染了");
        return new Trace(SCHEMA, meta(new int[0]), em.steps);
    }
}
