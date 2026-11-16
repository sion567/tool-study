package com.algoviz.engine.registry;

import com.algoviz.engine.Algorithm;
import com.algoviz.engine.AlgorithmRegistry;

import java.util.List;

/** 「算法」域：排序/查找/链表/树/图/动态规划/回溯/双指针/前缀和 + 字符串 + 贪心。 */
public final class CoreSection implements AlgorithmSection {

    @Override
    public List<AlgorithmRegistry.Entry> entries() {
        return List.of(
                new AlgorithmRegistry.Entry("算法", "排序", new com.algoviz.engine.sort.BubbleSort(new int[]{5, 2, 9, 1, 5})),
                new AlgorithmRegistry.Entry("算法", "排序", new com.algoviz.engine.sort.SelectionSort(new int[]{5, 2, 9, 1, 5})),
                new AlgorithmRegistry.Entry("算法", "排序", new com.algoviz.engine.sort.InsertionSort(new int[]{5, 2, 9, 1, 5})),
                new AlgorithmRegistry.Entry("算法", "排序", new com.algoviz.engine.sort.QuickSort(new int[]{5, 2, 9, 1, 5})),
                new AlgorithmRegistry.Entry("算法", "查找", new com.algoviz.engine.search.BinarySearch()),
                new AlgorithmRegistry.Entry("算法", "链表", new com.algoviz.engine.list.ReverseList(new int[]{5, 2, 9, 1, 5})),
                new AlgorithmRegistry.Entry("算法", "链表", new com.algoviz.engine.list.LinkedListCycle()),
                new AlgorithmRegistry.Entry("算法", "树", new com.algoviz.engine.tree.TreeTraversal(com.algoviz.engine.tree.TreeTraversal.Mode.PRE, new int[]{5, 3, 8, 1, 4, 7, 9})),
                new AlgorithmRegistry.Entry("算法", "树", new com.algoviz.engine.tree.TreeTraversal(com.algoviz.engine.tree.TreeTraversal.Mode.IN, new int[]{5, 3, 8, 1, 4, 7, 9})),
                new AlgorithmRegistry.Entry("算法", "树", new com.algoviz.engine.tree.TreeTraversal(com.algoviz.engine.tree.TreeTraversal.Mode.POST, new int[]{5, 3, 8, 1, 4, 7, 9})),
                new AlgorithmRegistry.Entry("算法", "树", new com.algoviz.engine.tree.LevelOrderTraversal()),
                new AlgorithmRegistry.Entry("算法", "树", new com.algoviz.engine.tree.BstInsert(new int[]{5, 3, 8}, 4)),
                new AlgorithmRegistry.Entry("算法", "树", new com.algoviz.engine.tree.BstDelete(new int[]{5, 3, 8, 1, 4, 7, 9}, 5)),
                new AlgorithmRegistry.Entry("算法", "树", new com.algoviz.engine.tree.RedBlackDemo()),
                new AlgorithmRegistry.Entry("算法", "树", new com.algoviz.engine.tree.RedBlackDeleteDemo()),
                new AlgorithmRegistry.Entry("算法", "图", new com.algoviz.engine.graph.Dijkstra()),
                new AlgorithmRegistry.Entry("算法", "图", new com.algoviz.engine.graph.BfsDemo()),
                new AlgorithmRegistry.Entry("算法", "图", new com.algoviz.engine.graph.DfsDemo()),
                new AlgorithmRegistry.Entry("字符串", null, new com.algoviz.engine.string.KmpDemo()),
                new AlgorithmRegistry.Entry("算法", "图", new com.algoviz.engine.graph.TopoSort()),
                new AlgorithmRegistry.Entry("算法", "图", new com.algoviz.engine.graph.UnionFindDemo()),
                new AlgorithmRegistry.Entry("算法", "图", new com.algoviz.engine.graph.BellmanFordDemo()),
                new AlgorithmRegistry.Entry("算法", "图", new com.algoviz.engine.graph.PrimDemo()),
                new AlgorithmRegistry.Entry("算法", "图", new com.algoviz.engine.graph.KruskalDemo()),
                new AlgorithmRegistry.Entry("贪心", null, new com.algoviz.engine.graph.IntervalSchedulingDemo()),
                new AlgorithmRegistry.Entry("算法", "链表", new com.algoviz.engine.list.MergeLists()),
                new AlgorithmRegistry.Entry("算法", "链表", new com.algoviz.engine.list.IntersectionDemo()),
                new AlgorithmRegistry.Entry("算法", "链表", new com.algoviz.engine.list.LruCacheDemo()),
                new AlgorithmRegistry.Entry("算法", "动态规划", new com.algoviz.engine.dp.FibonacciDemo()),
                new AlgorithmRegistry.Entry("算法", "动态规划", new com.algoviz.engine.dp.ClimbingStairsDemo()),
                new AlgorithmRegistry.Entry("算法", "动态规划", new com.algoviz.engine.dp.KnapsackDemo()),
                new AlgorithmRegistry.Entry("算法", "动态规划", new com.algoviz.engine.dp.LcsDemo()),
                new AlgorithmRegistry.Entry("算法", "动态规划", new com.algoviz.engine.dp.LisDemo()),
                new AlgorithmRegistry.Entry("算法", "回溯", new com.algoviz.engine.backtracking.PermutationDemo()),
                new AlgorithmRegistry.Entry("算法", "回溯", new com.algoviz.engine.backtracking.NQueensDemo()),
                new AlgorithmRegistry.Entry("算法", "回溯", new com.algoviz.engine.backtracking.SudokuDemo()),
                new AlgorithmRegistry.Entry("算法", "图", new com.algoviz.engine.graph.AStarDemo()),
                new AlgorithmRegistry.Entry("算法", "树", new com.algoviz.engine.tree.PathSumDemo()),
                new AlgorithmRegistry.Entry("算法", "树", new com.algoviz.engine.tree.HuffmanDemo()),
                new AlgorithmRegistry.Entry("算法", "链表", new com.algoviz.engine.list.ListBuildDemo()),
                new AlgorithmRegistry.Entry("算法", "链表", new com.algoviz.engine.list.ListDeleteDemo()),
                new AlgorithmRegistry.Entry("算法", "链表", new com.algoviz.engine.list.DoublyListDemo()),
                new AlgorithmRegistry.Entry("字符串", null, new com.algoviz.engine.string.AcautoDemo()),
                new AlgorithmRegistry.Entry("算法", "树", new com.algoviz.engine.tree.TreeSerializeDemo()),
                new AlgorithmRegistry.Entry("算法", "树", new com.algoviz.engine.tree.BPlusTreeDemo()),
                new AlgorithmRegistry.Entry("算法", "前缀和", new com.algoviz.engine.array.PrefixSum(new int[]{3, 7, 2, 5, 1})),
                new AlgorithmRegistry.Entry("算法", "查找", new com.algoviz.engine.array.SlidingWindow()),
                new AlgorithmRegistry.Entry("算法", "双指针", new com.algoviz.engine.array.TwoPointers(new int[]{1, 2, 5, 5, 9, 12, 15}, 14)),
                new AlgorithmRegistry.Entry("算法", "排序", new com.algoviz.engine.sort.MergeSort(new int[]{5, 2, 9, 1, 5})),
                new AlgorithmRegistry.Entry("算法", "排序", new com.algoviz.engine.sort.HeapSort(new int[]{5, 2, 9, 1, 5})),
                new AlgorithmRegistry.Entry("算法", "树", new com.algoviz.engine.tree.InvertTree()),
                new AlgorithmRegistry.Entry("算法", "树", new com.algoviz.engine.tree.SymmetricTree()),
                new AlgorithmRegistry.Entry("算法", "树", new com.algoviz.engine.tree.LcaDemo()),
                new AlgorithmRegistry.Entry("算法", "树", new com.algoviz.engine.tree.AvlDemo())
        );
    }

    @Override
    public Algorithm variantOf(String id, int n, java.util.Random r) {
        return switch (id) {
            case "sort.bubble" -> new com.algoviz.engine.sort.BubbleSort(AlgorithmSection.randArray(r, n));
            case "sort.selection" -> new com.algoviz.engine.sort.SelectionSort(AlgorithmSection.randArray(r, n));
            case "sort.insertion" -> new com.algoviz.engine.sort.InsertionSort(AlgorithmSection.randArray(r, n));
            case "sort.quick" -> new com.algoviz.engine.sort.QuickSort(AlgorithmSection.randArray(r, n));
            case "search.binary" -> {
                int[] a = AlgorithmSection.randDistinct(r, 7, 2, 200);   // 稀疏取值：间距自然、不像等差（二分前提：有序）
                java.util.Arrays.sort(a);
                yield new com.algoviz.engine.search.BinarySearch(a, a[r.nextInt(a.length)]);   // 目标必存在
            }
            case "list.reverse" -> new com.algoviz.engine.list.ReverseList(AlgorithmSection.randArray(r, n));
            case "list.cycle" -> {
                int[] vals = AlgorithmSection.randArray(r, n);
                yield new com.algoviz.engine.list.LinkedListCycle(vals, r.nextInt(n - 1));   // 环尾接回中间某节点
            }
            case "tree.traversal.pre" -> new com.algoviz.engine.tree.TreeTraversal(com.algoviz.engine.tree.TreeTraversal.Mode.PRE, AlgorithmSection.randDistinct(r, 7, 1, 99));
            case "tree.traversal.in" -> new com.algoviz.engine.tree.TreeTraversal(com.algoviz.engine.tree.TreeTraversal.Mode.IN, AlgorithmSection.randDistinct(r, 7, 1, 99));
            case "tree.traversal.post" -> new com.algoviz.engine.tree.TreeTraversal(com.algoviz.engine.tree.TreeTraversal.Mode.POST, AlgorithmSection.randDistinct(r, 7, 1, 99));
            case "tree.levelorder" -> new com.algoviz.engine.tree.LevelOrderTraversal(AlgorithmSection.randDistinct(r, 7, 1, 99));
            case "tree.bst.insert" -> {
                int[] base = AlgorithmSection.randDistinct(r, 6, 1, 99);
                yield new com.algoviz.engine.tree.BstInsert(base, 1 + r.nextInt(99));        // 可能命中去重分支，也是教学点
            }
            case "tree.bst.delete" -> {
                int[] base = AlgorithmSection.randDistinct(r, 7, 1, 99);
                yield new com.algoviz.engine.tree.BstDelete(base, base[r.nextInt(base.length)]);
            }
            case "graph.dijkstra" -> new com.algoviz.engine.graph.Dijkstra(AlgorithmSection.randomWeights(r));
            case "graph.bfs" -> new com.algoviz.engine.graph.BfsDemo(r.nextLong());
            case "graph.dfs" -> new com.algoviz.engine.graph.DfsDemo(r.nextLong());
            case "sort.merge" -> new com.algoviz.engine.sort.MergeSort(AlgorithmSection.randArray(r, n));
            case "sort.heap" -> new com.algoviz.engine.sort.HeapSort(AlgorithmSection.randArray(r, n));
            case "array.prefixsum" -> new com.algoviz.engine.array.PrefixSum(AlgorithmSection.randArray(r, Math.min(n, 8)));
            case "array.twopointers" -> {
                int m = Math.max(n, 7);
                int[] a = AlgorithmSection.randArray(r, m);
                java.util.Arrays.sort(a);
                yield new com.algoviz.engine.array.TwoPointers(a, a[r.nextInt(a.length)] + a[r.nextInt(a.length)]);
            }
            case "tree.bplus" -> new com.algoviz.engine.tree.BPlusTreeDemo(AlgorithmSection.randomKeys(r, Math.min(n + 4, 16)));
            // ---- 打磨批：数据型 demo 变体扩充 ----
            case "dp.knapsack" -> {
                int items = Math.max(3, Math.min(n - 2, 5));
                int[] w = new int[items], v = new int[items];
                for (int i = 0; i < items; i++) {
                    w[i] = 1 + r.nextInt(5);
                    v[i] = 5 + r.nextInt(30);
                }
                yield new com.algoviz.engine.dp.KnapsackDemo(w, v, 4 + r.nextInt(5));
            }
            case "dp.lcs" -> {
                String abc = "ABCD";
                StringBuilder a = new StringBuilder(), b = new StringBuilder();
                for (int i = 0; i < 5 + r.nextInt(3); i++) a.append(abc.charAt(r.nextInt(4)));
                for (int i = 0; i < 5 + r.nextInt(3); i++) b.append(abc.charAt(r.nextInt(4)));
                yield new com.algoviz.engine.dp.LcsDemo(a.toString(), b.toString());
            }
            case "dp.lis" -> new com.algoviz.engine.dp.LisDemo(AlgorithmSection.randArray(r, Math.max(6, Math.min(n + 1, 10))));
            case "dp.climbing" -> new com.algoviz.engine.dp.ClimbingStairsDemo(5 + r.nextInt(11));
            case "array.slidingwindow" -> new com.algoviz.engine.array.SlidingWindow(AlgorithmSection.randArray(r, Math.max(6, Math.min(n + 1, 10))));
            case "string.kmp" -> {
                String abc = "AB";
                StringBuilder text = new StringBuilder();
                for (int i = 0; i < 14 + r.nextInt(5); i++) text.append(abc.charAt(r.nextInt(2)));
                String pattern = text.substring(4 + r.nextInt(3), 8 + r.nextInt(3));   // 必在文本中
                yield new com.algoviz.engine.string.KmpDemo(text.toString(), pattern);
            }
            case "backtracking.nqueens" -> new com.algoviz.engine.backtracking.NQueensDemo(4 + r.nextInt(3));
            case "backtracking.permutation" -> new com.algoviz.engine.backtracking.PermutationDemo(AlgorithmSection.randDistinct(r, 3 + r.nextInt(3), 1, 99));
            default -> null;
        };
    }
}
