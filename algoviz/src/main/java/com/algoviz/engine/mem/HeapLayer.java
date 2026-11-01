package com.algoviz.engine.mem;

import com.algoviz.engine.Layer;

import java.util.Map;

/** 图层 mem.heap：堆快照。 */
public record HeapLayer(Heap heap) implements Layer {

    public Map<String, HeapObject> objects() {
        return heap.objects();
    }
}
