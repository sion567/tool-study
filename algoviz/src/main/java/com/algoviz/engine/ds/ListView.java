package com.algoviz.engine.ds;

import com.algoviz.engine.Layer;

import java.util.List;

/**
 * 图层 ds.list：链表的算法语义视图（蓝图 §2.2 视图 B）。
 * nodes 按展示顺序（原链顺序）排列；每节点的 nextId 是当前真实指向（null = 指向空）。
 * prevId = 双链表的反向指针（null = 单链表，不画背向箭）。
 * 与 mem.heap 的 prev/next 字段由生产者同源生成，一致性由 TraceAsserts 锁定。
 */
public record ListView(String name, String headId, List<NodeBox> nodes) implements Layer {

    /** 一个节点盒子：@id、值、当前 next 指向（null = null 引用）、prev 指向（null = 单链/无）。 */
    public record NodeBox(String nodeId, int val, String nextId, String prevId) {

        /** 单链表便捷构造。 */
        public static NodeBox singly(String nodeId, int val, String nextId) {
            return new NodeBox(nodeId, val, nextId, null);
        }
    }
}
