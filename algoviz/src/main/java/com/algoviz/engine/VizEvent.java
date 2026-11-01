package com.algoviz.engine;

import java.util.List;

/**
 * 结构化事件（蓝图 §2.1）：字符串驱动不了 DiffHighlighter，
 * 事件必须带类型和目标，例如 type=SWAP, targets=[a[2],a[3]]。
 */
public record VizEvent(EventType type, List<String> targets, String detail) {

    public enum EventType { INIT, SET, COMPARE, SWAP, MARK_SORTED, CALL, RETURN, VISIT, MESSAGE, CUSTOM }

    public static VizEvent of(EventType type, String detail, String... targets) {
        return new VizEvent(type, List.of(targets), detail);
    }
}
