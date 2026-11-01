package com.algoviz.engine.mem;

import com.algoviz.engine.Layer;

import java.util.List;

/** 图层 mem.stack：调用栈，从底到顶。 */
public record StackLayer(List<StackFrame> frames) implements Layer {
}
