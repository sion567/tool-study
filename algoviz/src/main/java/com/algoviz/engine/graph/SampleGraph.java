package com.algoviz.engine.graph;

import com.algoviz.engine.ds.GraphView;

import java.util.ArrayList;
import java.util.List;

/** 示例图（6 节点带权无向图）：Dijkstra / BFS / DFS 共用同一张图，便于横向对比。 */
final class SampleGraph {

    static final String[] LABELS = {"A", "B", "C", "D", "E", "F"};
    static final double[][] POS = {
            {0.10, 0.45}, {0.32, 0.16}, {0.32, 0.76}, {0.58, 0.45}, {0.78, 0.78}, {0.90, 0.20}
    };
    static final int[][] EDGES = {
            {0, 1, 4}, {0, 2, 2}, {1, 2, 1}, {1, 3, 5}, {2, 3, 8}, {2, 4, 10}, {3, 4, 2}, {3, 5, 6}, {4, 5, 3}
    };

    private SampleGraph() {
    }

    static String label(int idx) {
        return LABELS[idx];
    }

    static String nodeId(int idx) {
        return "@" + (idx + 1);
    }

    static int indexOf(String label) {
        for (int i = 0; i < LABELS.length; i++) {
            if (LABELS[i].equals(label)) {
                return i;
            }
        }
        return -1;
    }

    /** 邻居（按 EDGES 出现顺序，去自身）。 */
    static List<Integer> neighbors(int idx) {
        List<Integer> out = new ArrayList<>();
        for (int[] e : EDGES) {
            if (e[0] == idx) {
                out.add(e[1]);
            } else if (e[1] == idx) {
                out.add(e[0]);
            }
        }
        return out;
    }

    static int weight(int u, int v) {
        for (int[] e : EDGES) {
            if ((e[0] == u && e[1] == v) || (e[0] == v && e[1] == u)) {
                return e[2];
            }
        }
        return -1;
    }

    static List<GraphView.GraphVertex> vertices() {
        List<GraphView.GraphVertex> out = new ArrayList<>();
        for (int i = 0; i < LABELS.length; i++) {
            out.add(new GraphView.GraphVertex(nodeId(i), LABELS[i], POS[i][0], POS[i][1]));
        }
        return out;
    }

    static List<GraphView.GraphEdge> edges() {
        List<GraphView.GraphEdge> out = new ArrayList<>();
        for (int[] e : EDGES) {
            out.add(new GraphView.GraphEdge(nodeId(e[0]), nodeId(e[1]), e[2]));
        }
        return out;
    }
}
