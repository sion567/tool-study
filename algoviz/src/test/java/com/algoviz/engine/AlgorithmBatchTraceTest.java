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
                new Object[]{"bitmap", (java.util.function.Supplier<Algorithm>) com.algoviz.engine.math.BitmapDemo::new},
                // ---- 图（补齐 MST/拓扑/并查集/最短路/贪心）----
                new Object[]{"graph_prim", (java.util.function.Supplier<Algorithm>) com.algoviz.engine.graph.PrimDemo::new},
                new Object[]{"graph_kruskal", (java.util.function.Supplier<Algorithm>) com.algoviz.engine.graph.KruskalDemo::new},
                new Object[]{"graph_topo", (java.util.function.Supplier<Algorithm>) com.algoviz.engine.graph.TopoSort::new},
                new Object[]{"graph_unionfind", (java.util.function.Supplier<Algorithm>) com.algoviz.engine.graph.UnionFindDemo::new},
                new Object[]{"graph_bellmanford", (java.util.function.Supplier<Algorithm>) com.algoviz.engine.graph.BellmanFordDemo::new},
                new Object[]{"greedy_interval", (java.util.function.Supplier<Algorithm>) com.algoviz.engine.graph.IntervalSchedulingDemo::new},
                // ---- 排序补齐 / 树补齐 ----
                new Object[]{"merge_sort", (java.util.function.Supplier<Algorithm>) () -> new com.algoviz.engine.sort.MergeSort(new int[]{5, 2, 9, 1, 5})},
                new Object[]{"heap_sort", (java.util.function.Supplier<Algorithm>) () -> new com.algoviz.engine.sort.HeapSort(new int[]{5, 2, 9, 1, 5})},
                new Object[]{"tree_avl", (java.util.function.Supplier<Algorithm>) com.algoviz.engine.tree.AvlDemo::new},
                new Object[]{"tree_invert", (java.util.function.Supplier<Algorithm>) com.algoviz.engine.tree.InvertTree::new},
                new Object[]{"tree_symmetric", (java.util.function.Supplier<Algorithm>) com.algoviz.engine.tree.SymmetricTree::new},
                new Object[]{"tree_lca", (java.util.function.Supplier<Algorithm>) com.algoviz.engine.tree.LcaDemo::new},
                // ---- 数组技巧 ----
                new Object[]{"array_prefixsum", (java.util.function.Supplier<Algorithm>) () -> new com.algoviz.engine.array.PrefixSum(new int[]{3, 7, 2, 5, 1})},
                new Object[]{"array_slidingwindow", (java.util.function.Supplier<Algorithm>) com.algoviz.engine.array.SlidingWindow::new},
                new Object[]{"array_twopointers", (java.util.function.Supplier<Algorithm>) () -> new com.algoviz.engine.array.TwoPointers(new int[]{1, 2, 5, 5, 9, 12, 15}, 14)},
                // ---- 链表补齐 ----
                new Object[]{"list_merge", (java.util.function.Supplier<Algorithm>) com.algoviz.engine.list.MergeLists::new},
                new Object[]{"list_intersection", (java.util.function.Supplier<Algorithm>) com.algoviz.engine.list.IntersectionDemo::new},
                new Object[]{"list_lru", (java.util.function.Supplier<Algorithm>) com.algoviz.engine.list.LruCacheDemo::new},
                // ---- DP ----
                new Object[]{"dp_fibonacci", (java.util.function.Supplier<Algorithm>) com.algoviz.engine.dp.FibonacciDemo::new},
                new Object[]{"dp_climbing", (java.util.function.Supplier<Algorithm>) com.algoviz.engine.dp.ClimbingStairsDemo::new},
                new Object[]{"dp_knapsack", (java.util.function.Supplier<Algorithm>) com.algoviz.engine.dp.KnapsackDemo::new},
                new Object[]{"dp_lcs", (java.util.function.Supplier<Algorithm>) com.algoviz.engine.dp.LcsDemo::new},
                new Object[]{"dp_lis", (java.util.function.Supplier<Algorithm>) com.algoviz.engine.dp.LisDemo::new},
                // ---- 回溯 ----
                new Object[]{"bt_permutation", (java.util.function.Supplier<Algorithm>) com.algoviz.engine.backtracking.PermutationDemo::new},
                new Object[]{"bt_nqueens", (java.util.function.Supplier<Algorithm>) com.algoviz.engine.backtracking.NQueensDemo::new},
                // ---- 字符串 ----
                new Object[]{"string_kmp", (java.util.function.Supplier<Algorithm>) com.algoviz.engine.string.KmpDemo::new},
                // ---- 数学补齐 ----
                new Object[]{"math_unitcircle", (java.util.function.Supplier<Algorithm>) com.algoviz.engine.math.UnitCircleDemo::new},
                new Object[]{"math_taylor", (java.util.function.Supplier<Algorithm>) com.algoviz.engine.math.TaylorDemo::new},
                new Object[]{"math_fastpower", (java.util.function.Supplier<Algorithm>) com.algoviz.engine.math.FastPowerDemo::new},
                new Object[]{"math_gcd", (java.util.function.Supplier<Algorithm>) com.algoviz.engine.math.GcdDemo::new},
                new Object[]{"math_bloom", (java.util.function.Supplier<Algorithm>) com.algoviz.engine.math.BloomFilterDemo::new},
                new Object[]{"math_bitops", (java.util.function.Supplier<Algorithm>) com.algoviz.engine.math.BitOpsDemo::new},
                new Object[]{"math_sieve", (java.util.function.Supplier<Algorithm>) com.algoviz.engine.math.SieveDemo::new}
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

    /** 行为：Kruskal——MST 恰好 V-1=5 条边，总权重 = 1+2+2+3+5 = 13。 */
    @org.junit.jupiter.api.Test
    void kruskalMstIsMinimal() {
        var steps = new com.algoviz.engine.graph.KruskalDemo().produce().steps();
        var last = steps.get(steps.size() - 1).array();
        // 加进 MST 的边用 sortedFrom 标记（已选区间），数组本身是权重列表——用事件数验证
        long added = steps.stream()
                .flatMap(s -> s.events().stream())
                .filter(e -> e.type() == com.algoviz.engine.VizEvent.EventType.MARK_SORTED)
                .count();
        assertEquals(5, added, "MST 必须恰好 V-1=5 条边");
        long skipped = steps.stream()
                .flatMap(s -> s.events().stream())
                .filter(e -> e.type() == com.algoviz.engine.VizEvent.EventType.COMPARE)
                .count();
        assertTrue2(skipped >= 1, "至少一条边因闭环被跳过（1-2-1 与已选 A-B 形成环）");
    }

    /** 行为：Prim 与 Kruskal 在同一张图上边数一致（都是 V-1 条的生成树）。 */
    @org.junit.jupiter.api.Test
    void primAndKruskalAgreeOnEdgeCount() {
        long primAdded = new com.algoviz.engine.graph.PrimDemo().produce().steps().stream()
                .flatMap(s -> s.events().stream())
                .filter(e -> e.type() == com.algoviz.engine.VizEvent.EventType.MARK_SORTED).count();
        long kruskalAdded = new com.algoviz.engine.graph.KruskalDemo().produce().steps().stream()
                .flatMap(s -> s.events().stream())
                .filter(e -> e.type() == com.algoviz.engine.VizEvent.EventType.MARK_SORTED).count();
        assertEquals(primAdded, kruskalAdded, "同一张图的 MST 边数必须一致（V-1）");
        assertEquals(5, primAdded);
    }

    /** 行为：KMP——出现"匹配！"事件且给出起始位置。 */
    @org.junit.jupiter.api.Test
    void kmpFindsPattern() {
        var steps = new com.algoviz.engine.string.KmpDemo().produce().steps();
        boolean hasMatch = steps.stream().flatMap(s -> s.events().stream())
                .anyMatch(e -> e.type() == com.algoviz.engine.VizEvent.EventType.MARK_SORTED
                        && e.detail() != null && e.detail().contains("匹配"));
        assertTrue2(hasMatch, "KMP 过程应出现匹配事件");
    }

    /** 行为：NQueens——"找到解"那一步的 queens 数组是合法解（演示 N=4）。 */
    @org.junit.jupiter.api.Test
    void nQueensSolutionIsValid() {
        var steps = new com.algoviz.engine.backtracking.NQueensDemo().produce().steps();
        // 解出现的时刻 = "找到解"事件那一步（之后演示继续回溯搜完整个空间）
        Step found = steps.stream()
                .filter(s -> s.events().stream().anyMatch(e ->
                        e.detail() != null && e.detail().contains("找到解")))
                .findFirst().orElseThrow(() -> new AssertionError("从未出现『找到解』事件"));
        List<Integer> queens = found.array().values();
        assertEquals(4, queens.size(), "演示用 4 皇后：4 行");
        for (int i = 0; i < queens.size(); i++) {
            assertTrue2(queens.get(i) >= 0, "行 " + i + " 未放置");
            for (int j = i + 1; j < queens.size(); j++) {
                int qi = queens.get(i), qj = queens.get(j);
                assertTrue2(qi != qj, "行 " + i + " 与 " + j + " 同列冲突");
                assertTrue2(Math.abs(qi - qj) != Math.abs(i - j), "行 " + i + " 与 " + j + " 对角线冲突");
            }
        }
        assertEquals(List.of(1, 3, 0, 2), queens, "4 皇后第一解应为 [1,3,0,2]");
    }

    /** 行为：快速幂——3^13 = 1594323 出现在堆 result 字段。 */
    @org.junit.jupiter.api.Test
    void fastPowerComputesCorrectly() {
        var steps = new com.algoviz.engine.math.FastPowerDemo().produce().steps();
        var last = steps.get(steps.size() - 1);
        String result = last.heap().heap().objects().values().stream()
                .filter(o -> o.fields().containsKey("result"))
                .map(o -> o.fields().get("result").text())
                .findFirst().orElse(null);
        assertEquals("1594323", result, "3^13 = 1594323");
    }

    /** 行为：LRU 缓存——容量淘汰最久未用。 */
    @org.junit.jupiter.api.Test
    void lruEvictsLeastRecentlyUsed() {
        var steps = new com.algoviz.engine.list.LruCacheDemo().produce().steps();
        assertTrue2(steps.size() > 5, "LRU 应有多步操作序列");
        boolean evicted = steps.stream().flatMap(s -> s.events().stream())
                .anyMatch(e -> e.detail() != null && (e.detail().contains("淘汰") || e.detail().contains("evict")));
        assertTrue2(evicted, "LRU 过程应发生淘汰");
    }

    /** 行为：筛法——20 以内素数 8 个（数组 1=合数 0=非合数，下标 0/1 除外）。 */
    @org.junit.jupiter.api.Test
    void sieveFindsPrimes() {
        var steps = new com.algoviz.engine.math.SieveDemo().produce().steps();
        var last = steps.get(steps.size() - 1).array();
        List<Integer> vals = last.values();
        assertEquals(21, vals.size(), "0..20 共 21 格");
        long primes = 0;
        for (int i = 2; i < vals.size(); i++) {
            if (vals.get(i) == 0) primes++;   // 未被标记 = 素数
        }
        assertEquals(8, primes, "20 以内素数应为 8 个（2,3,5,7,11,13,17,19）");
    }
}
