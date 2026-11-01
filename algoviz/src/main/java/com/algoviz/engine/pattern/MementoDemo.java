package com.algoviz.engine.pattern;

import com.algoviz.engine.Algorithm;
import com.algoviz.engine.Value;
import com.algoviz.engine.VizEvent;
import com.algoviz.engine.ds.SeqView;
import com.algoviz.engine.trace.Trace;

import java.util.List;

/** 备忘录演示：快照不可变，恢复 = 引用换回——Editor 的历史由 Snapshot 全权保管。 */
public final class MementoDemo implements Algorithm {

    private static final String SCHEMA = "1";
    private static final String INVARIANT = "快照不可变，恢复 = 引用换回——Editor 的历史由 Snapshot 全权保管（封装：内容不外泄）";

    private static final int L_NEW = 9;
    private static final int L_SAVE = 10;
    private static final int L_SNAP = 4;
    private static final int L_EDIT = 11;
    private static final int L_RESTORE = 12;
    private static final int L_RESTORED = 5;
    private static final int L_PROOF = 13;

    private PatternEmitter em;

    @Override public String id() { return "pattern.memento"; }
    @Override public String title() { return "备忘录模式"; }
    @Override public String codeResource() { return "/code/MementoDemo.java"; }
    @Override public String docResource() { return "/markdown/pattern-memento.md"; }

    @Override
    public Trace produce() {
        em = new PatternEmitter(List.of(
                new SeqView.Participant("client", "client"),
                new SeqView.Participant("editor", "ed : Editor"),
                new SeqView.Participant("snapshot", "save1 : Snapshot")), INVARIANT);
        em.addObject("@1", "Editor");
        em.setField("@1", "text", Value.prim(""));
        em.mainLocals.put("ed", Value.ref("@1"));
        em.state(L_NEW, VizEvent.EventType.INIT, "new Editor()：@1.text = ''（初始状态）", List.of("@1"),
                "原发器知道自己该被存什么——但快照长什么样由它说了算");
        em.message(L_SAVE, 0, 1, "save()", "CALL", "存档：把当前状态打包成一个对象交出去");
        em.addObject("@2", "Snapshot");
        em.setField("@2", "text", Value.prim(""));
        em.state(L_SNAP, VizEvent.EventType.SET, "@2 备忘录诞生（text=''）——record：天生不可变", List.of("@2"),
                "不可变 = 存多久都不会被偷改；存一串快照 = 撤销栈");
        em.state(L_EDIT, VizEvent.EventType.SET, "@1.text ← \"画到一半\"（继续编辑，世界变了）", List.of("@1"),
                "现在的状态将一去不复返——除非有快照");
        em.message(L_RESTORE, 0, 1, "restore(save1)", "CALL", "读档：把快照交回给原发器");
        em.setField("@1", "text", Value.prim(""));
        em.state(L_RESTORED, VizEvent.EventType.SET, "@1.text ← 从 @2 读回 ''（一键回滚）", List.of("@1"),
                "恢复 = 字段从快照抄回——引用一换，时间倒流");
        em.state(L_PROOF, VizEvent.EventType.RETURN, "打印 ''：回到存档时刻", List.of("@1"),
                "封装的漂亮之处：快照内容只有 Editor 能读懂（text 字段不外泄给 client）");
        return new Trace(SCHEMA, meta(new int[0]), em.steps);
    }
}
