package com.algoviz.engine.ds;

import com.algoviz.engine.Layer;

import java.util.List;

/**
 * 图层 ds.hash：哈希表的「桶 + 冲突链」现场。
 * bucketCount 个桶（下标 0..bucketCount-1），每个桶挂一条单链——
 * 链节点与链表图层同构（@id + val + nextId），bucket 字段标注归属。
 * 冲突 = 同一桶出现 ≥2 个节点 → 链条生长一目了然。
 */
public record HashView(String name, int bucketCount, List<HashNode> nodes) implements Layer {

    /** 链节点：@id、键值、所属桶、链内 next（null = 链尾）。 */
    public record HashNode(String nodeId, int val, int bucket, String nextId) {
    }
}
