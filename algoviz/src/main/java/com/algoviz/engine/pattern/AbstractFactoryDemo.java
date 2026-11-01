package com.algoviz.engine.pattern;

import com.algoviz.engine.Algorithm;
import com.algoviz.engine.Value;
import com.algoviz.engine.VizEvent;
import com.algoviz.engine.ds.SeqView;
import com.algoviz.engine.trace.Trace;

import java.util.List;

/** 抽象工厂演示：选一次工厂，整族一致——不会混搭出 Mac 按钮 + Win 复选。 */
public final class AbstractFactoryDemo implements Algorithm {

    private static final String SCHEMA = "1";
    private static final String INVARIANT = "工厂方法造一种，抽象工厂造一族：选一次工厂，整族一致";

    private static final int L_PICK = 21;
    private static final int L_BTN = 22;
    private static final int L_MAKE_BTN = 13;
    private static final int L_CHK = 23;
    private static final int L_MAKE_CHK = 14;
    private static final int L_DONE = 24;

    private PatternEmitter em;

    @Override public String id() { return "pattern.abstractfactory"; }
    @Override public String title() { return "抽象工厂"; }
    @Override public String codeResource() { return "/code/AbstractFactoryDemo.java"; }
    @Override public String docResource() { return "/markdown/pattern-abstractfactory.md"; }

    @Override
    public Trace produce() {
        em = new PatternEmitter(List.of(
                new SeqView.Participant("client", "client"),
                new SeqView.Participant("winf", "f : WinFactory"),
                new SeqView.Participant("button", "b : Button"),
                new SeqView.Participant("checkbox", "c : Checkbox")), INVARIANT);
        em.addObject("@1", "WinFactory");
        em.mainLocals.put("f", Value.ref("@1"));
        em.state(L_PICK, VizEvent.EventType.INIT, "new WinFactory()：风格只选这一次", List.of("@1"),
                "族的「血统」由工厂对象携带——之后造什么都带着它");
        em.message(L_BTN, 0, 1, "createButton()", "CALL", "要一个按钮");
        em.addObject("@2", "WinButton");
        em.state(L_MAKE_BTN, VizEvent.EventType.SET, "@2 WinButton 造出（Win 族）", List.of("@2"),
                "工厂承诺：我造的按钮和等下造的复选框必然配套");
        em.message(L_CHK, 0, 1, "createCheckbox()", "CALL", "再要一个复选框");
        em.addObject("@3", "WinCheckbox");
        em.state(L_MAKE_CHK, VizEvent.EventType.SET, "@3 WinCheckbox 造出（同族）", List.of("@3"),
                "两个产品来自同一个工厂 → 保证同族，混搭在结构上不可能");
        em.state(L_DONE, VizEvent.EventType.RETURN, "b 和 c 必然配套——换 Mac？换一个工厂对象即可", List.of("@1"),
                "对比工厂方法：多产品配套时用它，防止 A1+B2 的错配");
        return new Trace(SCHEMA, meta(new int[0]), em.steps);
    }
}
