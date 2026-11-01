package com.algoviz.engine.ds;

import com.algoviz.engine.Layer;

import java.util.List;
import java.util.Map;

/**
 * 图层 ds.graph：带权图的最短路语义视图（Dijkstra / BFS / DFS）。
 * vertices 带固定布局坐标（相对 0..1）；dist 是当前距离表（null = ∞）；
 * settledIds = 已出圈/已访问节点；activeId = 本步处理的圆心；
 * relaxFrom/relaxTo = 本步正在松弛的边（null = 无）；
 * visitOrderIds = 访问/出圈顺序（用于角标编号，Dijkstra 传空表则不显示）。
 */
public record GraphView(String name, List<GraphVertex> vertices, List<GraphEdge> edges,
                        String activeId, List<String> settledIds,
                        Map<String, Integer> dist,
                        String relaxFrom, String relaxTo,
                        List<String> visitOrderIds) implements Layer {

    /** 顶点：nodeId（"@1"）、显示名（"A"）、相对布局坐标。 */
    public record GraphVertex(String nodeId, String label, double x, double y) {
    }

    /** 无向带权边。 */
    public record GraphEdge(String fromId, String toId, int weight) {
    }
}
