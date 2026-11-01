package com.algoviz.engine.ds;

import com.algoviz.engine.Layer;

import java.util.List;

/**
 * 图层 ds.list：链表的算法语义视图（蓝图 §2.2 视图 B）。
 * nodes 按展示顺序（原链顺序）排列；每节点的 nextId 是当前真实指向（null = 指向空）。
 * 与 mem.heap 的 next 字段由生产者同源生成，一致性由 TraceAsserts 锁定。
 */
public record ListView(String name, String headId, List<NodeBox> nodes) implements Layer {

    /** 一个节点盒子：@id、值、当前 next 指向的节点 id（null = null 引用）。 */
    public record NodeBox(String nodeId, int val, String nextId) {
    }
}
