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
                new Object[]{"graph_dfs", (java.util.function.Supplier<Algorithm>) DfsDemo::new}
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
}
