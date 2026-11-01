package com.algoviz.engine.mem;

import com.algoviz.engine.Value;

import java.util.Map;

/** 栈帧：方法名 + 局部变量表（基本类型存值，对象存引用 id——蓝图 §1.2）。 */
public record StackFrame(String method, Map<String, Value> locals, String returnLabel) {
}
