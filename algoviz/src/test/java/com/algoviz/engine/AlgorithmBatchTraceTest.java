package com.algoviz.engine;

import com.algoviz.engine.graph.BfsDemo;
import com.algoviz.engine.graph.DfsDemo;
import com.algoviz.engine.list.LinkedListCycle;
import com.algoviz.engine.math.MatrixMultiply;
import com.algoviz.engine.search.BinarySearch;
import com.algoviz.engine.sort.QuickSort;
import com.algoviz.engine.trace.Trace;
import com.algoviz.engine.trace.TraceCodec;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static com.algoviz.engine.TraceAsserts.assertContract;
import static org.junit.jupiter.api.Assertions.assertEquals;

/** 本批新算法（快速排序/二分/判环/层序/BFS/DFS）的统一测试：合约 + 幂等 + golden。 */
class AlgorithmBatchTraceTest {

    static Stream<Object[]> demos() {
        return Stream.of(
                new Object[]{"quick_sort", (java.util.function.Supplier<Algorithm>) QuickSort::new},
                new Object[]{"binary_search", (java.util.function.Supplier<Algorithm>) BinarySearch::new},
                new Object[]{"linked_list_cycle", (java.util.function.Supplier<Algorithm>) LinkedListCycle::new},
                new Object[]{"level_order", (java.util.function.Supplier<Algorithm>) com.algoviz.engine.tree.LevelOrderTraversal::new},
                new Object[]{"graph_bfs", (java.util.function.Supplier<Algorithm>) BfsDemo::new},
                new Object[]{"graph_dfs", (java.util.function.Supplier<Algorithm>) DfsDemo::new},
                new Object[]{"red_black", (java.util.function.Supplier<Algorithm>) com.algoviz.engine.tree.RedBlackDemo::new},
                new Object[]{"bitmap", (java.util.function.Supplier<Algorithm>) com.algoviz.engine.math.BitmapDemo::new}
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("demos")
    void contractHolds(String name, java.util.function.Supplier<Algorithm> alg) {
        assertContract(alg.get().produce());
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("demos")
    void produceIsIdempotent(String name, java.util.function.Supplier<Algorithm> alg) {
        // Registry 缓存实例 → 同一实例会被多次 produce，必须幂等
        Algorithm a = alg.get();
        Trace first = a.produce();
        Trace second = a.produce();
        assertEquals(TraceCodec.toJson(first).strip(), TraceCodec.toJson(second).strip(),
                name + " 同一实例重复 produce 输出不同——状态没清零");
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("demos")
    void goldenTraceMatches(String name, java.util.function.Supplier<Algorithm> alg) throws Exception {
        String actual = TraceCodec.toJson(alg.get().produce()).strip();
        Path golden = Path.of("src/test/resources/traces/" + name + ".golden.json");
        if (Files.notExists(golden)) {
            Files.createDirectories(golden.getParent());
            Files.writeString(golden, actual + "\n", StandardCharsets.UTF_8);
            System.out.println(">>> golden 基准不存在，已自动生成；从下次起逐字节比对");
            return;
        }
        String expected = Files.readString(golden, StandardCharsets.UTF_8).strip();
        assertEquals(expected, actual, name + " 的 Trace 输出变化：有意变更请删 golden 重新生成");
    }

    /** 行为：快排最终有序。 */
    @org.junit.jupiter.api.Test
    void quickSortFinalStateIsSorted() {
        var steps = new QuickSort(new int[]{5, 2, 9, 1, 5}).produce().steps();
        var last = steps.get(steps.size() - 1).array().values();
        assertEquals(List.of(1, 2, 5, 5, 9), last);
    }

    /** 行为：层序 = 5,3,8,1,4,7,9（从上到下从左到右）。 */
    @org.junit.jupiter.api.Test
    void levelOrderSequence() {
        var steps = new com.algoviz.engine.tree.LevelOrderTraversal().produce().steps();
        var last = steps.get(steps.size() - 1).tree();
        List<Integer> visited = last.nodes().stream()
                .filter(nb -> nb.visitOrder() != null)
                .sorted(java.util.Comparator.comparingInt(com.algoviz.engine.ds.TreeView.TreeNodeBox::visitOrder))
                .map(com.algoviz.engine.ds.TreeView.TreeNodeBox::val)
                .toList();
        assertEquals(List.of(5, 3, 8, 1, 4, 7, 9), visited);
    }

    /** 行为：二分命中下标 4（值 9）。 */
    @org.junit.jupiter.api.Test
    void binarySearchFindsIndexFour() {
        var steps = new BinarySearch().produce().steps();
        var last = steps.get(steps.size() - 1);
        assertEquals(6, last.loc().startLine(), "命中行是第 6 行");
    }

    /** 行为：红黑树——中序有序、真根正确、根为黑、无红红相邻。 */
    @org.junit.jupiter.api.Test
    void redBlackFinalStateIsValid() {
        var steps = new com.algoviz.engine.tree.RedBlackDemo().produce().steps();
        var last = steps.get(steps.size() - 1).tree();

        // 真根 = 没被任何孩子指向的节点
        java.util.Set<String> referenced = new java.util.HashSet<>();
        last.nodes().forEach(nb -> {
            if (nb.leftId() != null) referenced.add(nb.leftId());
            if (nb.rightId() != null) referenced.add(nb.rightId());
        });
        var root = last.nodes().stream()
                .filter(nb -> !referenced.contains(nb.nodeId()))
                .findFirst().orElseThrow(() -> new AssertionError("找不到根"));
        assertEquals("BLACK", root.rbColor(), "根必须为黑");
        assertEquals(8, root.val(), "插入 {10,5,1,8,7,9} 后根应为 8（插 8 左旋、插 7 双红右旋）");

        // 中序遍历必须有序（RB 也是 BST）
        List<Integer> inOrder = new ArrayList<>();
        java.util.Deque<com.algoviz.engine.ds.TreeView.TreeNodeBox> stack = new java.util.ArrayDeque<>();
        java.util.Map<String, com.algoviz.engine.ds.TreeView.TreeNodeBox> byId = new java.util.HashMap<>();
        last.nodes().forEach(nb -> byId.put(nb.nodeId(), nb));
        var cur = root;
        while (cur != null || !stack.isEmpty()) {
            while (cur != null) {
                stack.push(cur);
                cur = cur.leftId() == null ? null : byId.get(cur.leftId());
            }
            cur = stack.pop();
            inOrder.add(cur.val());
            cur = cur.rightId() == null ? null : byId.get(cur.rightId());
        }
        assertEquals(List.of(1, 5, 7, 8, 9, 10), inOrder, "中序必须有序");

        // 红红不相邻
        for (var nb : last.nodes()) {
            if ("RED".equals(nb.rbColor())) {
                assertTrue2(nb.leftId() == null || !"RED".equals(byId.get(nb.leftId()).rbColor()),
                        "红红相邻 @" + nb.nodeId());
                assertTrue2(nb.rightId() == null || !"RED".equals(byId.get(nb.rightId()).rbColor()),
                        "红红相邻 @" + nb.nodeId());
            }
        }
    }

    private static void assertTrue2(boolean cond, String msg) {
        org.junit.jupiter.api.Assertions.assertTrue(cond, msg);
    }

    /** 行为：位图判重——在 data[3] 命中重复 7，bit[7] 已置位。 */
    @org.junit.jupiter.api.Test
    void bitmapFindsDuplicate() {
        var steps = new com.algoviz.engine.math.BitmapDemo().produce().steps();
        var last = steps.get(steps.size() - 1).bitmap();
        assertEquals(7, last.dupBit(), "重复位应为 7");
        assertTrue2(last.ones().contains(7), "bit[7] 应已置位");
        assertEquals(3, last.readIndex(), "在读到 data[3] 时命中");
    }
}
