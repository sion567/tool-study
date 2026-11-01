package com.algoviz.engine.ds;

import com.algoviz.engine.Layer;

import java.util.List;
import java.util.Map;

/**
 * 图层 ds.tree：二叉树的算法语义视图。
 * nodes 按创建顺序（@id 顺序）排列；leftId/rightId 是当前真实结构（null = 无孩子）；
 * state ∈ {unvisited, onstack, visited}；visitOrder = 访问序号（遍历用，null = 未访问）；
 * callStackIds = 当前调用栈上的节点（bottom→top，null 调用不含）；
 * varTags = 画布上的指针标签（如 min → "@6"），只放不在调用栈上的指针。
 * 与 mem.heap 的 left/right/val 字段同源，一致性由 TraceAsserts 锁定。
 */
public record TreeView(String name, String rootId, List<TreeNodeBox> nodes,
                       List<String> callStackIds, Map<String, String> varTags) implements Layer {

    public record TreeNodeBox(String nodeId, int val, String leftId, String rightId,
                              String state, Integer visitOrder, String rbColor) {
    }
}
