package com.algoviz.engine.ds;

import com.algoviz.engine.Layer;

import java.util.List;

/**
 * 图层 ds.btree：B+ 树的多路节点现场。
 * 每节点 = 一个多槽盒子（keys 1~3 个），内部节点 childIds 与 keys 数满足 children = keys+1，
 * 叶子节点 nextId 串成有序链（范围查询的骨架）。
 * state ∈ {unvisited, onstack, visited, fresh}：下探路径 onstack，新分裂节点 fresh。
 */
public record BTreeView(String name, String rootId, List<BNode> nodes,
                        List<String> leafChainIds) implements Layer {

    /** B+ 树节点：多键槽 + 多孩子 + 叶链 next。 */
    public record BNode(String nodeId, List<Integer> keys, List<String> childIds,
                        boolean leaf, String nextId, String state, Integer activeKey) {
    }
}
