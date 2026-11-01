package com.algoviz.engine.mem;

import com.algoviz.engine.Value;

import java.util.List;
import java.util.Map;

/** 堆对象：@id 盒子。数组看 elements，普通对象看 fields（M3 链表用）。 */
public record HeapObject(String id, String label, String type,
                         Map<String, Value> fields, List<Value> elements) {
}
