package com.algoviz.engine.pattern;

import com.algoviz.engine.Algorithm;
import com.algoviz.engine.Value;
import com.algoviz.engine.VizEvent;
import com.algoviz.engine.ds.SeqView;
import com.algoviz.engine.trace.Trace;

import java.util.List;

/** 状态模式演示：同一个按钮，行为随状态对象变——切换由状态自己驱动（对比策略：外部换）。 */
public final class StateDemo implements Algorithm {

    private static final String SCHEMA = "1";
    private static final String INVARIANT =
            "上下文的行为由当前状态对象决定：换状态 = 换引用，但切换由状态自己驱动（策略是外部换，状态是自己换）";

    private static final int L_NEW = 15;
    private static final int L_PRESS1 = 16;
    private static final int L_PRESS2 = 17;
    private static final int L_PRESS3 = 18;
    private static final int L_PLAY = 12;
    private static final int L_PLAYING_SET = 4;
    private static final int L_PAUSED_SET = 7;

    private PatternEmitter em;

    public StateDemo() {
        em = new PatternEmitter(List.of(
                new SeqView.Participant("client", "client"),
                new SeqView.Participant("player", "player : Player"),
                new SeqView.Participant("playing", "Playing"),
                new SeqView.Participant("paused", "Paused")), INVARIANT);
    }

    @Override
    public String id() {
        return "pattern.state";
    }

    @Override
    public String title() {
        return "状态模式";
    }

    @Override
    public String codeResource() {
        return "/code/StateDemo.java";
    }

    @Override
    public String docResource() {
        return "/markdown/pattern-state.md";
    }

    @Override
    public Trace produce() {
        em.reset();
        em.addObject("@1", "Player");
        em.addObject("@2", "Playing");
        em.addObject("@3", "Paused");
        em.setField("@1", "state", Value.ref("@3"));
        em.mainLocals.put("player", Value.ref("@1"));
        em.state(L_NEW, VizEvent.EventType.INIT,
                "new Player()：@1.state = @3（Paused）——初始状态决定第一次按钮的行为", List.of("@1"),
                "Player 里没有一句 if(state==...)：行为全部委托给 state 对象");
        em.message(L_PRESS1, 0, 1, "pressPlay()", "CALL", "同一个按钮被按下——Player 不知道结果会是播放还是暂停");
        em.message(L_PLAY, 1, 3, "state.play(this)", "CALL", "委托给当前状态对象 Paused");
        em.setField("@1", "state", Value.ref("@2"));
        em.state(L_PAUSED_SET, VizEvent.EventType.SET,
                "Paused 说：该切到 Playing 了 → @1.state: @3 → @2", List.of("@1.state"),
                "关键区别：切换由状态对象自己驱动，不是外部指挥");
        em.state(L_PRESS1, VizEvent.EventType.RETURN, "返回：现在是 Playing", List.of("@1"), "按钮没变，行为变了");
        em.message(L_PRESS2, 0, 1, "pressPlay()", "CALL", "再按一次——同一个方法调用");
        em.message(L_PLAY, 1, 2, "state.play(this)", "CALL", "这次委托到的是 Playing——箭头已经换了");
        em.setField("@1", "state", Value.ref("@3"));
        em.state(L_PLAYING_SET, VizEvent.EventType.SET,
                "Playing 说：切回 Paused → @1.state: @2 → @3", List.of("@1.state"),
                "Paused→Playing→Paused：状态机在对象图上转圈");
        em.state(L_PRESS2, VizEvent.EventType.RETURN, "返回：现在是 Paused", List.of("@1"), "行为跟着状态走");
        em.message(L_PRESS3, 0, 1, "pressPlay()", "CALL", "第三次");
        em.message(L_PLAY, 1, 3, "state.play(this)", "CALL", "又回到 Paused 的行为");
        em.setField("@1", "state", Value.ref("@2"));
        em.state(L_PAUSED_SET, VizEvent.EventType.SET,
                "@1.state: @3 → @2（循环往复）", List.of("@1.state"),
                "新状态？加一个 State 实现类即可，Player 一行不改");
        em.state(L_PRESS3, VizEvent.EventType.RETURN, "返回：现在是 Playing", List.of("@1"),
                "对比策略模式：结构一样，区别在「谁来换」——策略外部换，状态自己换");
        return new Trace(SCHEMA, meta(new int[0]), em.steps);
    }
}
