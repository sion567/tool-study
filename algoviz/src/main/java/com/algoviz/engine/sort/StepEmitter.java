package com.algoviz.engine.sort;

import com.algoviz.engine.Intent;
import com.algoviz.engine.Layer;
import com.algoviz.engine.Loc;
import com.algoviz.engine.Step;
import com.algoviz.engine.Value;
import com.algoviz.engine.VizEvent;
import com.algoviz.engine.ds.ArrayView;
import com.algoviz.engine.mem.Heap;
import com.algoviz.engine.mem.HeapLayer;
import com.algoviz.engine.mem.HeapObject;
import com.algoviz.engine.mem.StackFrame;
import com.algoviz.engine.mem.StackLayer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 排序生产者共用的快照发射器（蓝图 §2.1 铁律 1：每步深拷贝，不存引用）。 */
final class StepEmitter {

    final int[] a;
    final int n;
    final List<Step> steps = new ArrayList<>();
    int sortedFrom;   // 尾部已就位起点（冒泡用），初始 n
    int sortedTo;     // 头部已就位终点（选择/插入用），初始 -1

    private final String invariant;

    StepEmitter(int[] a, String invariant) {
        this.a = a;
        this.n = a.length;
        this.sortedFrom = a.length;
        this.sortedTo = -1;
        this.invariant = invariant;
    }

    void emit(int line, Map<String, Value> locals, List<VizEvent> events,
              ArrayView view, String why) {
        StackFrame frame = new StackFrame("sort",
                Collections.unmodifiableMap(new LinkedHashMap<>(locals)), null);
        Map<String, Layer> layers = new LinkedHashMap<>();
        layers.put("mem.stack", new StackLayer(List.of(frame)));
        layers.put("mem.heap", new HeapLayer(snapshotHeap()));
        if (view != null) {
            layers.put("ds.array", view);
        }
        steps.add(new Step(steps.size(), Loc.line(line, why),
                Collections.unmodifiableMap(layers),
                List.copyOf(events), new Intent(why, invariant)));
    }

    private Heap snapshotHeap() {
        List<Value> elems = new ArrayList<>(n);
        for (int v : a) {
            elems.add(Value.prim(v));
        }
        HeapObject arr = new HeapObject("@1", "a", "int[" + n + "]",
                new LinkedHashMap<>(), List.copyOf(elems));
        Map<String, HeapObject> objs = new LinkedHashMap<>();
        objs.put("@1", arr);
        return new Heap(Collections.unmodifiableMap(objs));
    }

    ArrayView view(Integer cmpA, Integer cmpB, Integer swapA, Integer swapB) {
        List<Integer> vals = new ArrayList<>(n);
        for (int v : a) {
            vals.add(v);
        }
        return new ArrayView("@1", "a", List.copyOf(vals),
                cmpA, cmpB, swapA, swapB, sortedFrom, sortedTo);
    }
}
