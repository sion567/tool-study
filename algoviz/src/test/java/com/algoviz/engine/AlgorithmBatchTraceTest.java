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
                new Object[]{"math_taylor_cos", (java.util.function.Supplier<Algorithm>) () -> new com.algoviz.engine.math.TaylorDemo(true)},
                new Object[]{"math_fastpower", (java.util.function.Supplier<Algorithm>) com.algoviz.engine.math.FastPowerDemo::new},
                new Object[]{"math_gcd", (java.util.function.Supplier<Algorithm>) com.algoviz.engine.math.GcdDemo::new},
                new Object[]{"math_bloom", (java.util.function.Supplier<Algorithm>) com.algoviz.engine.math.BloomFilterDemo::new},
                new Object[]{"math_bitops", (java.util.function.Supplier<Algorithm>) com.algoviz.engine.math.BitOpsDemo::new},
                new Object[]{"math_sieve", (java.util.function.Supplier<Algorithm>) com.algoviz.engine.math.SieveDemo::new},
                // ---- 第六批：牛顿迭代 / CORDIC / 数独 / A* / 分桶 / 路径总和 ----
                new Object[]{"math_newton", (java.util.function.Supplier<Algorithm>) com.algoviz.engine.math.NewtonDemo::new},
                new Object[]{"math_cordic", (java.util.function.Supplier<Algorithm>) com.algoviz.engine.math.CordicDemo::new},
                new Object[]{"math_bucket", (java.util.function.Supplier<Algorithm>) com.algoviz.engine.math.BucketDemo::new},
                new Object[]{"bt_sudoku", (java.util.function.Supplier<Algorithm>) com.algoviz.engine.backtracking.SudokuDemo::new},
                new Object[]{"graph_astar", (java.util.function.Supplier<Algorithm>) com.algoviz.engine.graph.AStarDemo::new},
                new Object[]{"tree_pathsum", (java.util.function.Supplier<Algorithm>) com.algoviz.engine.tree.PathSumDemo::new},
                // ---- 第七批：链表三件套 / 哈希表 / Karatsuba / 序列化 ----
                new Object[]{"list_build", (java.util.function.Supplier<Algorithm>) com.algoviz.engine.list.ListBuildDemo::new},
                new Object[]{"list_delete", (java.util.function.Supplier<Algorithm>) com.algoviz.engine.list.ListDeleteDemo::new},
                new Object[]{"list_doubly", (java.util.function.Supplier<Algorithm>) com.algoviz.engine.list.DoublyListDemo::new},
                new Object[]{"math_hashmap", (java.util.function.Supplier<Algorithm>) com.algoviz.engine.math.HashMapDemo::new},
                new Object[]{"math_karatsuba", (java.util.function.Supplier<Algorithm>) com.algoviz.engine.math.KaratsubaDemo::new},
                new Object[]{"tree_serialize", (java.util.function.Supplier<Algorithm>) com.algoviz.engine.tree.TreeSerializeDemo::new},
                new Object[]{"tree_bplus", (java.util.function.Supplier<Algorithm>) com.algoviz.engine.tree.BPlusTreeDemo::new},
                // ---- 第八批：SOLID 五原则 + 重构三招（正反例对照）----
                new Object[]{"solid_srp", (java.util.function.Supplier<Algorithm>) com.algoviz.engine.pattern.SolidSrpDemo::new},
                new Object[]{"solid_ocp", (java.util.function.Supplier<Algorithm>) com.algoviz.engine.pattern.SolidOcpDemo::new},
                new Object[]{"solid_lsp", (java.util.function.Supplier<Algorithm>) com.algoviz.engine.pattern.SolidLspDemo::new},
                new Object[]{"solid_isp", (java.util.function.Supplier<Algorithm>) com.algoviz.engine.pattern.SolidIspDemo::new},
                new Object[]{"solid_dip", (java.util.function.Supplier<Algorithm>) com.algoviz.engine.pattern.SolidDipDemo::new},
                new Object[]{"refactor_extract", (java.util.function.Supplier<Algorithm>) com.algoviz.engine.pattern.RefactorExtractDemo::new},
                new Object[]{"refactor_param", (java.util.function.Supplier<Algorithm>) com.algoviz.engine.pattern.RefactorParamDemo::new},
                new Object[]{"refactor_poly", (java.util.function.Supplier<Algorithm>) com.algoviz.engine.pattern.RefactorPolyDemo::new},
                // ---- 第九批：并发模式三连（脚本化交错，确定性）----
                new Object[]{"concurrent_prodcons", (java.util.function.Supplier<Algorithm>) com.algoviz.engine.pattern.ConcurrentProdConsDemo::new},
                new Object[]{"concurrent_rwlock", (java.util.function.Supplier<Algorithm>) com.algoviz.engine.pattern.ConcurrentRwLockDemo::new},
                new Object[]{"concurrent_pool", (java.util.function.Supplier<Algorithm>) com.algoviz.engine.pattern.ConcurrentPoolDemo::new},
                // ---- 第十批：分布式模式三连（断路器/服务发现/CQRS）----
                new Object[]{"dist_breaker", (java.util.function.Supplier<Algorithm>) com.algoviz.engine.pattern.DistBreakerDemo::new},
                new Object[]{"dist_discovery", (java.util.function.Supplier<Algorithm>) com.algoviz.engine.pattern.DistDiscoveryDemo::new},
                new Object[]{"dist_cqrs", (java.util.function.Supplier<Algorithm>) com.algoviz.engine.pattern.DistCqrsDemo::new}
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

    /** 行为：牛顿迭代——最后一步误差 < 1e-9（平方收敛，5 步内达 12 位精度）。 */
    @org.junit.jupiter.api.Test
    void newtonConvergesToSqrt2() {
        var steps = new com.algoviz.engine.math.NewtonDemo().produce().steps();
        Step last = steps.get(steps.size() - 1);
        // 堆里 |x−√2| 应为 e 级小数
        String err = last.heap().heap().objects().values().stream()
                .filter(o -> o.fields().containsKey("|x−√2|"))
                .map(o -> o.fields().get("|x−√2|").text())
                .findFirst().orElse("1");
        double e = Double.parseDouble(err.replaceAll("[^0-9.eE\\-]", ""));
        assertTrue2(e < 1e-9, "最终误差应为 1e-9 以下，实际 " + err);
        // 曲线图层是 newton 形态
        assertEquals(com.algoviz.engine.math.PlotView.NEWTON, last.plot().kind());
    }

    /** 行为：CORDIC——9 次迭代后向量角逼近 37°，残差 ≤ atan(2⁻⁸) ≈ 0.225°（理论精度）。 */
    @org.junit.jupiter.api.Test
    void cordicApproachesTarget() {
        var steps = new com.algoviz.engine.math.CordicDemo().produce().steps();
        Step last = steps.get(steps.size() - 1);
        double angle = Math.toDegrees(Math.atan2(last.plot().pointY(), last.plot().pointX()));
        assertEquals(37.0, angle, 0.225, "9 次迭代残差应 ≤ atan(2⁻⁸)，实际偏差 " + (angle - 37.0));
        // 校正后应落回单位圆
        double len = Math.hypot(last.plot().pointX(), last.plot().pointY());
        assertEquals(1.0, len, 1e-9, "×K 后向量长度应恰为 1");
    }

    /** 行为：数独——终局每行/列/宫 1~9 不重不漏，题面数字保留。 */
    @org.junit.jupiter.api.Test
    void sudokuSolutionIsValid() {
        var steps = new com.algoviz.engine.backtracking.SudokuDemo().produce().steps();
        var grid = steps.get(steps.size() - 1).math().grids().get(0);
        int[][] b = new int[9][9];
        for (int r = 0; r < 9; r++) {
            for (int c = 0; c < 9; c++) {
                String v = grid.value(r, c);
                b[r][c] = v.isEmpty() ? 0 : Integer.parseInt(v);
                assertTrue2(b[r][c] > 0, "终局 (" + r + "," + c + ") 仍有空格——没解出");
            }
        }
        java.util.Set<Integer> want = java.util.Set.of(1, 2, 3, 4, 5, 6, 7, 8, 9);
        for (int r = 0; r < 9; r++) {
            assertEquals(want, java.util.Arrays.stream(b[r]).boxed().collect(java.util.stream.Collectors.toSet()),
                    "第 " + r + " 行不是 1~9 排列");
        }
        for (int c = 0; c < 9; c++) {
            java.util.Set<Integer> col = new java.util.HashSet<>();
            for (int r = 0; r < 9; r++) col.add(b[r][c]);
            assertEquals(want, col, "第 " + c + " 列不是 1~9 排列");
        }
        for (int br = 0; br < 3; br++) {
            for (int bc = 0; bc < 3; bc++) {
                java.util.Set<Integer> box = new java.util.HashSet<>();
                for (int i = 0; i < 3; i++) {
                    for (int j = 0; j < 3; j++) box.add(b[br * 3 + i][bc * 3 + j]);
                }
                assertEquals(want, box, "宫 (" + br + "," + bc + ") 不是 1~9 排列");
            }
        }
    }

    /** 行为：A*——终点弹出且路径 ◎ 连成 S→G，步数 = h(S)=18 的可行最短长度。 */
    @org.junit.jupiter.api.Test
    void astarFindsPath() {
        var steps = new com.algoviz.engine.graph.AStarDemo().produce().steps();
        var grid = steps.get(steps.size() - 1).math().grids().get(0);
        // 终局网格里应有 ◎ 路径
        long pathCells = grid.values().stream().filter("◎"::equals).count();
        assertTrue2(pathCells >= 15, "路径格太少：" + pathCells + "——路径没画出来");
        // 事件流里应有"路径长"回溯事件
        boolean hasPath = steps.stream().flatMap(s -> s.events().stream())
                .anyMatch(e -> e.detail() != null && e.detail().contains("路径长"));
        assertTrue2(hasPath, "应有路径回溯事件");
    }

    /** 行为：分桶——发现 700 重复（两遍扫描后 dupNums=[700]）。 */
    @org.junit.jupiter.api.Test
    void bucketDetectsDuplicate() {
        var steps = new com.algoviz.engine.math.BucketDemo().produce().steps();
        boolean dup = steps.stream().flatMap(s -> s.events().stream())
                .anyMatch(e -> e.detail() != null && e.detail().contains("700") && e.detail().contains("重复"));
        assertTrue2(dup, "应检测到 700 重复");
        // 终局：4 桶 + 数据条 共 5 个网格
        assertEquals(5, steps.get(steps.size() - 1).math().grids().size(), "data + 4 桶");
    }

    /** 行为：泰勒 cos 版——终局（10 次幂）在 [0,2π] 逼近 cos，且 heap 公式为偶次幂形式。 */
    @org.junit.jupiter.api.Test
    void taylorCosConvergesToCos() throws Exception {
        var steps = new com.algoviz.engine.math.TaylorDemo(true).produce().steps();
        var last = steps.get(steps.size() - 1);
        String formula = last.heap().heap().objects().get("@1").fields().get("公式").text();
        assertEquals("1 - x²/2! + x⁴/4! - …", formula, "cos 公式应为偶次幂形式");
        // 终局近似 vs 真值：抽样对比（x=1 处 10 次幂近似应极接近 cos(1)）
        double approx = com.algoviz.engine.math.TaylorDemo.taylorCos(1.0, 10);
        assertEquals(Math.cos(1.0), approx, 1e-8, "taylorCos(1,10) 应收敛到 cos(1)（理论误差 x¹²/12!≈2e-9）");
        // sin 版不受影响
        assertEquals(Math.sin(1.0), com.algoviz.engine.math.TaylorDemo.taylorSin(1.0, 11), 1e-9);
    }

    /** 行为：路径总和——5→3→4 = 12 命中，且三个节点保持 visited。 */
    @org.junit.jupiter.api.Test
    void pathSumFindsHit() {
        var steps = new com.algoviz.engine.tree.PathSumDemo().produce().steps();
        boolean hit = steps.stream().flatMap(s -> s.events().stream())
                .anyMatch(e -> e.detail() != null && e.detail().contains("命中"));
        assertTrue2(hit, "应出现命中事件（5+3+4=12）");
        var tree = steps.get(steps.size() - 1).tree();
        List<Integer> visited = tree.nodes().stream()
                .filter(nb -> "visited".equals(nb.state()))
                .map(com.algoviz.engine.ds.TreeView.TreeNodeBox::val)
                .toList();
        assertTrue2(visited.containsAll(List.of(5, 3, 4)), "命中路径 5,3,4 应保持高亮，实际 " + visited);
    }

    /** 行为：头插/尾插——头插终态 [3,2,1]，尾插终态 [1,2,3]。 */
    @org.junit.jupiter.api.Test
    void listBuildReversesWithHeadInsert() {
        var steps = new com.algoviz.engine.list.ListBuildDemo().produce().steps();
        // 找"头插完成"与"尾插完成"两步，校验链序
        var headDone = steps.stream().filter(s -> s.events().stream()
                .anyMatch(e -> e.detail() != null && e.detail().startsWith("头插完成"))).findFirst().orElseThrow();
        var tailDone = steps.stream().filter(s -> s.events().stream()
                .anyMatch(e -> e.detail() != null && e.detail().startsWith("尾插完成"))).findFirst().orElseThrow();
        assertEquals(List.of(3, 2, 1), headDone.list().nodes().stream()
                .map(com.algoviz.engine.ds.ListView.NodeBox::val).toList(), "头插 = 逆序");
        assertEquals(List.of(1, 2, 3), tailDone.list().nodes().stream()
                .map(com.algoviz.engine.ds.ListView.NodeBox::val).toList(), "尾插 = 保序");
    }

    /** 行为：删除节点——7 出链，其余保持连接。 */
    @org.junit.jupiter.api.Test
    void listDeleteBypassesTarget() {
        var steps = new com.algoviz.engine.list.ListDeleteDemo().produce().steps();
        var last = steps.get(steps.size() - 1);
        var nodes = last.list().nodes();
        var delNode = nodes.stream().filter(nb -> nb.val() == 7).findFirst().orElseThrow();
        boolean referenced = nodes.stream()
                .anyMatch(nb -> Integer.toString(4).equals(delNode.nodeId())
                        ? false : delNode.nodeId().equals(nb.nextId()));
        assertTrue2(!referenced, "被删节点不应再被指向");
        // 改道验证：@3(2) 的 next 直指 @5(5)
        var two = nodes.stream().filter(nb -> nb.val() == 2).findFirst().orElseThrow();
        assertEquals("@5", two.nextId(), "2 的 next 应跳过 7 直指 5");
        boolean swapped = steps.stream().flatMap(s -> s.events().stream())
                .anyMatch(e -> e.type() == com.algoviz.engine.VizEvent.EventType.SWAP);
        assertTrue2(swapped, "应有改道事件");
    }

    /** 行为：双链表——镜像处处成立（A.next=B ⟺ B.prev=A），插入后 4↔7↔9↔2，删 9 后 4↔7↔2。 */
    @org.junit.jupiter.api.Test
    void doublyListKeepsMirrorInvariant() {
        var steps = new com.algoviz.engine.list.DoublyListDemo().produce().steps();
        for (Step s : steps) {
            boolean midInsert = s.events().stream()
                    .anyMatch(e -> e.detail() != null
                            && (e.detail().contains("new.prev") || e.detail().contains("右邻.prev")));
            if (midInsert) {
                continue;   // 插入前两步刻意分步动箭——镜像补齐 ② 完成后才恢复
            }
            var nodes = s.list().nodes();
            for (var nb : nodes) {
                if (nb.nextId() != null) {
                    var next = nodes.stream().filter(o -> o.nodeId().equals(nb.nextId())).findFirst().orElseThrow();
                    assertEquals(nb.nodeId(), next.prevId(),
                            "镜像破坏 @" + nb.nodeId() + " → " + nb.nextId() + " @step " + s.index());
                }
            }
        }
        var last = steps.get(steps.size() - 1).list().nodes();
        List<Integer> seq = last.stream().map(com.algoviz.engine.ds.ListView.NodeBox::val).toList();
        assertEquals(List.of(4, 7, 2), seq, "终态应为 4↔7↔2（9 已删，7 已插）");
    }

    /** 行为：哈希表——put 5 键撞 3 次，get(7) 走链 3 步命中。 */
    @org.junit.jupiter.api.Test
    void hashMapCollidesAndWalks() {
        var steps = new com.algoviz.engine.math.HashMapDemo().produce().steps();
        long collisions = steps.stream().flatMap(s -> s.events().stream())
                .filter(e -> e.detail() != null && e.detail().contains("撞车！")).count();
        assertEquals(3, collisions, "12/20 撞桶0，7/3/15 撞桶3 → 3 次撞车");
        boolean hit = steps.stream().flatMap(s -> s.events().stream())
                .anyMatch(e -> e.detail() != null && e.detail().contains("走链第 3 步"));
        assertTrue2(hit, "get(7) 应走链 3 步（链 15→3→7）");
        var last = steps.get(steps.size() - 1).hash();
        assertEquals(4, last.bucketCount());
        assertEquals(5, last.nodes().size(), "5 个键全部入表");
    }

    /** 行为：Karatsuba——结果 = 7006652（与朴素乘一致）。 */
    @org.junit.jupiter.api.Test
    void karatsubaResultMatches() {
        var steps = new com.algoviz.engine.math.KaratsubaDemo().produce().steps();
        var last = steps.get(steps.size() - 1);
        String detail = last.events().get(last.events().size() - 1).detail();
        assertTrue2(detail.contains("7006652"), "1234×5678 = 7006652，实际：" + detail);
        assertTrue2(detail.contains("7006652") && detail.contains("验算"), "应含验算对照");
    }

    /** 行为：分布式三连——断路器三态流转 / 服务发现故障摘除 / CQRS 双库同步。 */
    @org.junit.jupiter.api.Test
    void distributedNarrativesHitKeyMoments() throws Exception {
        // 断路器：必须有 OPEN 与 HALF_OPEN
        var breaker = new com.algoviz.engine.pattern.DistBreakerDemo().produce().steps();
        String breakerText = breaker.stream()
                .flatMap(s -> java.util.stream.Stream.concat(s.events().stream().map(e -> e.detail()), java.util.stream.Stream.of(s.intent().why())))
                .filter(java.util.Objects::nonNull).collect(java.util.stream.Collectors.joining("; "));
        assertTrue2(breakerText.contains("OPEN"), "断路器必须有 OPEN 态");
        assertTrue2(breakerText.contains("HALF_OPEN"), "断路器必须有半开试探");
        // 服务发现：必须有摘除与恢复注册
        var disc = new com.algoviz.engine.pattern.DistDiscoveryDemo().produce().steps();
        String discText = disc.stream()
                .flatMap(s -> java.util.stream.Stream.concat(s.events().stream().map(e -> e.detail()), java.util.stream.Stream.of(s.intent().why())))
                .filter(java.util.Objects::nonNull).collect(java.util.stream.Collectors.joining("; "));
        assertTrue2(discText.contains("摘除"), "服务发现必须有故障摘除");
        assertTrue2(discText.contains("重新注册"), "必须有恢复注册");
        // CQRS：写模型与读模型都在堆里
        var cqrs = new com.algoviz.engine.pattern.DistCqrsDemo().produce().steps();
        var lastHeap = cqrs.get(cqrs.size() - 1).heap().heap().objects();
        assertTrue2(lastHeap.containsKey("@w") && lastHeap.containsKey("@r"), "CQRS 必须双库同屏");
    }

    /** 行为：并发三连——脚本化交错必含「阻塞/等待」时刻与收尾总结。 */
    @org.junit.jupiter.api.Test
    void concurrentNarrativesContainBlocking() throws Exception {
        Class<?>[] demos = {
                com.algoviz.engine.pattern.ConcurrentProdConsDemo.class,
                com.algoviz.engine.pattern.ConcurrentRwLockDemo.class,
                com.algoviz.engine.pattern.ConcurrentPoolDemo.class};
        for (Class<?> c : demos) {
            var alg = (com.algoviz.engine.Algorithm) c.getDeclaredConstructor().newInstance();
            var steps = alg.produce().steps();
            boolean blocking = steps.stream().flatMap(s -> s.events().stream())
                    .anyMatch(e -> e.detail() != null
                            && (e.detail().contains("阻塞") || e.detail().contains("挂起")));
            boolean summary = steps.get(steps.size() - 1).events().stream()
                    .anyMatch(e -> e.type() == com.algoviz.engine.VizEvent.EventType.RETURN);
            assertTrue2(blocking, c.getSimpleName() + " 缺阻塞/挂起时刻（并发教学的核心）");
            assertTrue2(summary, c.getSimpleName() + " 收尾应是总结 RETURN");
        }
    }

    /** 行为：SOLID/重构八连——每个 demo 都有「反例痛点」+「正例收益」两幕。 */
    @org.junit.jupiter.api.Test
    void solidAndRefactorNarrativesComplete() throws Exception {
        Class<?>[] demos = {
                com.algoviz.engine.pattern.SolidSrpDemo.class,
                com.algoviz.engine.pattern.SolidOcpDemo.class,
                com.algoviz.engine.pattern.SolidLspDemo.class,
                com.algoviz.engine.pattern.SolidIspDemo.class,
                com.algoviz.engine.pattern.SolidDipDemo.class,
                com.algoviz.engine.pattern.RefactorExtractDemo.class,
                com.algoviz.engine.pattern.RefactorParamDemo.class,
                com.algoviz.engine.pattern.RefactorPolyDemo.class};
        for (Class<?> c : demos) {
            var alg = (com.algoviz.engine.Algorithm) c.getDeclaredConstructor().newInstance();
            var steps = alg.produce().steps();
            boolean pain = steps.stream().anyMatch(s ->
                    s.events().stream().anyMatch(e -> e.detail() != null
                            && (e.detail().contains("反例") || e.detail().contains("痛")))
                    || s.intent().why().contains("反例") || s.intent().why().contains("痛"));
            boolean gain = steps.get(steps.size() - 1).events().stream()
                    .anyMatch(e -> e.type() == com.algoviz.engine.VizEvent.EventType.RETURN);
            assertTrue2(pain, c.getSimpleName() + " 缺反例痛感事件");
            assertTrue2(gain, c.getSimpleName() + " 收尾应是正例 RETURN");
        }
    }

    /** 行为：B+ 树——数据全在叶子且叶链升序；4 次叶分裂 + 1 次内部分裂；children=keys+1。 */
    @org.junit.jupiter.api.Test
    void bplusTreeInvariantsHold() {
        var steps = new com.algoviz.engine.tree.BPlusTreeDemo().produce().steps();
        var last = steps.get(steps.size() - 1).btree();
        // 叶链 = 全部数据，升序
        java.util.Map<String, com.algoviz.engine.ds.BTreeView.BNode> byId = new java.util.HashMap<>();
        last.nodes().forEach(nd -> byId.put(nd.nodeId(), nd));
        List<Integer> data = new ArrayList<>();
        for (String id : last.leafChainIds()) {
            var nd = byId.get(id);
            assertTrue2(nd.leaf(), "叶链上的必须是叶子: " + id);
            data.addAll(nd.keys());
        }
        assertEquals(12, data.size(), "12 个键全部在叶子上");
        for (int i = 1; i < data.size(); i++) {
            assertTrue2(data.get(i - 1) < data.get(i), "叶链必须升序 @" + i);
        }
        // 分裂次数：SWAP 事件 = 叶/内部分裂，MARK_SORTED = 新根
        long splits = steps.stream().flatMap(s -> s.events().stream())
                .filter(e -> e.type() == com.algoviz.engine.VizEvent.EventType.SWAP).count();
        assertEquals(5, splits, "4 次叶分裂（15/30/6/22）+ 1 次内部分裂（22）");
        // 合约自检：终局（所有分裂完成）内部节点 children = keys+1
        for (var nd : last.nodes()) {
            if (!nd.leaf()) {
                assertEquals(nd.keys().size() + 1, nd.childIds().size());
            }
        }
    }

    /** 行为：序列化——往返一致：终局树 7 节点，流 15 token（7 值 + 8 个 #）。 */
    @org.junit.jupiter.api.Test
    void treeSerializeRoundTrip() {
        var steps = new com.algoviz.engine.tree.TreeSerializeDemo().produce().steps();
        var serDone = steps.stream().filter(s -> s.events().stream()
                .anyMatch(e -> e.detail() != null && e.detail().startsWith("序列化完成"))).findFirst().orElseThrow();
        String stream = serDone.events().stream()
                .filter(e -> e.detail().startsWith("序列化完成")).findFirst().orElseThrow().detail();
        long hashes = stream.chars().filter(c -> c == '#').count();
        assertEquals(8, hashes, "8 个 null 指针 → 8 个 '#'");

        var last = steps.get(steps.size() - 1).tree();
        assertEquals(7, last.nodes().size(), "重建后仍 7 节点");
        // 重建树的前序 = 原树前序
        List<Integer> preOrder = new ArrayList<>();
        java.util.Map<String, com.algoviz.engine.ds.TreeView.TreeNodeBox> byId = new java.util.HashMap<>();
        last.nodes().forEach(nb -> byId.put(nb.nodeId(), nb));
        var stack = new java.util.ArrayDeque<com.algoviz.engine.ds.TreeView.TreeNodeBox>();
        stack.push(byId.get(last.rootId()));
        while (!stack.isEmpty()) {
            var n = stack.pop();
            preOrder.add(n.val());
            if (n.rightId() != null) stack.push(byId.get(n.rightId()));
            if (n.leftId() != null) stack.push(byId.get(n.leftId()));
        }
        assertEquals(List.of(5, 3, 1, 4, 8, 7, 9), preOrder, "重建树前序应与原树一致");
    }
}
