package com.algoviz.engine.ds;

import com.algoviz.engine.Layer;

import java.util.List;

/**
 * 图层 seq.*：设计模式的时序语义视图（蓝图 §5.1）。
 * participants = 泳道（角色）；messages = 消息流（from/to 为泳道下标，kind: CALL/RETURN）；
 * activeIndex = 本步激活的消息（之前的全部保留在画面上，形成时间轴）。
 * 对象图（谁持有谁的引用）由 mem.heap 承载——策略换实现时 @1.strategy 的箭头变化在那边可见。
 */
public record SeqView(String name, List<Participant> participants, List<Message> messages,
                      int activeIndex) implements Layer {

    /** 泳道：id（稳定标识，如 "ctx"）、显示名。 */
    public record Participant(String id, String label) {
    }

    /** 消息：from/to = 泳道下标；kind = CALL（实线）/ RETURN（虚线）。 */
    public record Message(int from, int to, String label, String kind) {
    }
}
