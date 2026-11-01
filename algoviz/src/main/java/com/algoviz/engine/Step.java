package com.algoviz.engine;

import com.algoviz.engine.ds.ArrayView;
import com.algoviz.engine.ds.GraphView;
import com.algoviz.engine.ds.ListView;
import com.algoviz.engine.ds.SeqView;
import com.algoviz.engine.ds.TreeView;
import com.algoviz.engine.math.MathView;
import com.algoviz.engine.mem.HeapLayer;
import com.algoviz.engine.mem.StackLayer;

import java.util.List;
import java.util.Map;

/**
 * 通用底层合约（蓝图 §2.1）：
 * 一步 = 各图层的不可变快照 + 结构化事件 + 意图。
 * 算法域的 Frame = mem.stack + mem.heap + ds.array 三个图层的组合。
 */
public record Step(int index, Loc loc, Map<String, Layer> layers, List<VizEvent> events, Intent intent) {

    public StackLayer stack() {
        return (StackLayer) layers.get("mem.stack");
    }

    public HeapLayer heap() {
        return (HeapLayer) layers.get("mem.heap");
    }

    public ArrayView array() {
        return (ArrayView) layers.get("ds.array");
    }

    public ListView list() {
        return (ListView) layers.get("ds.list");
    }

    public TreeView tree() {
        return (TreeView) layers.get("ds.tree");
    }

    public GraphView graph() {
        return (GraphView) layers.get("ds.graph");
    }

    public SeqView seq() {
        return (SeqView) layers.get("seq.main");
    }

    public MathView math() {
        return (MathView) layers.get("math.matrix");
    }

    public com.algoviz.engine.math.BitmapView bitmap() {
        return (com.algoviz.engine.math.BitmapView) layers.get("math.bitmap");
    }
}
