package com.algoviz.engine.mem;

import java.util.Map;

/** 堆：@1 -> 对象。每步深拷贝快照（蓝图 §2.1 铁律 1）。 */
public record Heap(Map<String, HeapObject> objects) {
}
