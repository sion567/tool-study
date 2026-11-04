package com.algoviz.engine;

import com.algoviz.engine.graph.BfsDemo;
import com.algoviz.engine.graph.BellmanFordDemo;
import com.algoviz.engine.graph.Dijkstra;
import com.algoviz.engine.graph.TopoSort;
import com.algoviz.engine.graph.UnionFindDemo;
import com.algoviz.engine.graph.DfsDemo;
import com.algoviz.engine.list.IntersectionDemo;
import com.algoviz.engine.list.LinkedListCycle;
import com.algoviz.engine.list.LruCacheDemo;
import com.algoviz.engine.list.MergeLists;
import com.algoviz.engine.list.ReverseList;
import com.algoviz.engine.array.PrefixSum;
import com.algoviz.engine.array.TwoPointers;
import com.algoviz.engine.tree.RedBlackDemo;
import com.algoviz.engine.math.BitmapDemo;
import com.algoviz.engine.math.FastPowerDemo;
import com.algoviz.engine.dp.ClimbingStairsDemo;
import com.algoviz.engine.dp.KnapsackDemo;
import com.algoviz.engine.dp.LcsDemo;
import com.algoviz.engine.dp.FibonacciDemo;
import com.algoviz.engine.dp.LisDemo;
import com.algoviz.engine.backtracking.PermutationDemo;
import com.algoviz.engine.math.BloomFilterDemo;
import com.algoviz.engine.math.GcdDemo;
import com.algoviz.engine.math.SieveDemo;
import com.algoviz.engine.math.BitmapDemo;
import com.algoviz.engine.math.MatrixMultiply;
import com.algoviz.engine.search.BinarySearch;
import com.algoviz.engine.sort.MergeSort;
import com.algoviz.engine.sort.HeapSort;
import com.algoviz.engine.sort.QuickSort;
import com.algoviz.engine.tree.InvertTree;
import com.algoviz.engine.tree.LcaDemo;
import com.algoviz.engine.tree.SymmetricTree;
import com.algoviz.engine.tree.LevelOrderTraversal;
import com.algoviz.engine.pattern.AbstractFactoryDemo;
import com.algoviz.engine.pattern.AdapterDemo;
import com.algoviz.engine.pattern.BridgeDemo;
import com.algoviz.engine.pattern.BuilderDemo;
import com.algoviz.engine.pattern.ChainDemo;
import com.algoviz.engine.pattern.CommandDemo;
import com.algoviz.engine.pattern.CompositeDemo;
import com.algoviz.engine.pattern.DecoratorDemo;
import com.algoviz.engine.pattern.FacadeDemo;
import com.algoviz.engine.pattern.FactoryMethodDemo;
import com.algoviz.engine.pattern.FlyweightDemo;
import com.algoviz.engine.pattern.InterpreterDemo;
import com.algoviz.engine.pattern.IteratorDemo;
import com.algoviz.engine.pattern.MediatorDemo;
import com.algoviz.engine.pattern.MementoDemo;
import com.algoviz.engine.pattern.ObserverDemo;
import com.algoviz.engine.pattern.PrototypeDemo;
import com.algoviz.engine.pattern.ProxyDemo;
import com.algoviz.engine.pattern.SingletonDemo;
import com.algoviz.engine.pattern.StateDemo;
import com.algoviz.engine.pattern.StrategyDemo;
import com.algoviz.engine.pattern.TemplateMethodDemo;
import com.algoviz.engine.pattern.VisitorDemo;
import com.algoviz.engine.sort.BubbleSort;
import com.algoviz.engine.string.KmpDemo;
import com.algoviz.engine.sort.InsertionSort;
import com.algoviz.engine.sort.SelectionSort;
import com.algoviz.engine.tree.AvlDemo;
import com.algoviz.engine.tree.BstDelete;
import com.algoviz.engine.tree.BstInsert;
import com.algoviz.engine.tree.TreeTraversal;
import com.algoviz.engine.tree.TreeTraversal.Mode;

import java.util.List;

/** 算法注册表：每加一个算法，这里加一行，TreeView 自动多一个叶子。 */
public final class AlgorithmRegistry {

    /** sub = 子分组（设计模式用 创建型/结构型/行为型），其他域传 null。 */
    public record Entry(String category, String sub, Algorithm algorithm) {
    }

    /** 缓存的唯一实例列表——调用方（如循环切换）可以做引用比较。 */
    private static final List<Entry> ALL = List.of(
            new Entry("算法", "排序", new BubbleSort(new int[]{5, 2, 9, 1, 5})),
            new Entry("算法", "排序", new SelectionSort(new int[]{5, 2, 9, 1, 5})),
            new Entry("算法", "排序", new InsertionSort(new int[]{5, 2, 9, 1, 5})),
            new Entry("算法", "排序", new QuickSort(new int[]{5, 2, 9, 1, 5})),
            new Entry("算法", "查找", new BinarySearch()),
            new Entry("算法", "链表", new ReverseList(new int[]{5, 2, 9, 1, 5})),
            new Entry("算法", "链表", new LinkedListCycle()),
            new Entry("算法", "树", new TreeTraversal(Mode.PRE, new int[]{5, 3, 8, 1, 4, 7, 9})),
            new Entry("算法", "树", new TreeTraversal(Mode.IN, new int[]{5, 3, 8, 1, 4, 7, 9})),
            new Entry("算法", "树", new TreeTraversal(Mode.POST, new int[]{5, 3, 8, 1, 4, 7, 9})),
            new Entry("算法", "树", new LevelOrderTraversal()),
            new Entry("算法", "树", new BstInsert(new int[]{5, 3, 8}, 4)),
            new Entry("算法", "树", new BstDelete(new int[]{5, 3, 8, 1, 4, 7, 9}, 5)),
            new Entry("算法", "树", new RedBlackDemo()),
            new Entry("算法", "图", new Dijkstra()),
            new Entry("算法", "图", new BfsDemo()),
            new Entry("算法", "图", new DfsDemo()),
            new Entry("字符串", null, new com.algoviz.engine.string.KmpDemo()),
            new Entry("算法", "图", new TopoSort()),
            new Entry("算法", "图", new UnionFindDemo()),
            new Entry("算法", "图", new BellmanFordDemo()),
            new Entry("算法", "图", new com.algoviz.engine.graph.PrimDemo()),
            new Entry("算法", "图", new com.algoviz.engine.graph.KruskalDemo()),
            new Entry("贪心", null, new com.algoviz.engine.graph.IntervalSchedulingDemo()),
            new Entry("算法", "链表", new MergeLists()),
            new Entry("算法", "链表", new IntersectionDemo()),
            new Entry("算法", "链表", new LruCacheDemo()),
            new Entry("算法", "动态规划", new FibonacciDemo()),
            new Entry("算法", "动态规划", new ClimbingStairsDemo()),
            new Entry("算法", "动态规划", new KnapsackDemo()),
            new Entry("算法", "动态规划", new LcsDemo()),
            new Entry("算法", "动态规划", new LisDemo()),
            new Entry("算法", "回溯", new PermutationDemo()),
            new Entry("算法", "回溯", new com.algoviz.engine.backtracking.NQueensDemo()),
            new Entry("数学", null, new com.algoviz.engine.math.UnitCircleDemo()),
            new Entry("数学", null, new com.algoviz.engine.math.TaylorDemo()),
            new Entry("数学", null, new com.algoviz.engine.math.FastPowerDemo()),
            new Entry("数学", null, new com.algoviz.engine.math.GcdDemo()),
            new Entry("数学", null, new com.algoviz.engine.math.BloomFilterDemo()),
            new Entry("数学", null, new com.algoviz.engine.math.BitOpsDemo()),
            new Entry("数学", null, new com.algoviz.engine.math.SieveDemo()),
            new Entry("数学", null, new com.algoviz.engine.math.NewtonDemo()),
            new Entry("数学", null, new com.algoviz.engine.math.CordicDemo()),
            new Entry("数学", null, new com.algoviz.engine.math.BucketDemo()),
            new Entry("算法", "回溯", new com.algoviz.engine.backtracking.SudokuDemo()),
            new Entry("算法", "图", new com.algoviz.engine.graph.AStarDemo()),
            new Entry("算法", "树", new com.algoviz.engine.tree.PathSumDemo()),
            new Entry("算法", "链表", new com.algoviz.engine.list.ListBuildDemo()),
            new Entry("算法", "链表", new com.algoviz.engine.list.ListDeleteDemo()),
            new Entry("算法", "链表", new com.algoviz.engine.list.DoublyListDemo()),
            new Entry("数学", null, new com.algoviz.engine.math.HashMapDemo()),
            new Entry("数学", null, new com.algoviz.engine.math.KaratsubaDemo()),
            new Entry("算法", "树", new com.algoviz.engine.tree.TreeSerializeDemo()),
            new Entry("算法", "树", new com.algoviz.engine.tree.BPlusTreeDemo()),
            new Entry("SOLID 原则", null, new com.algoviz.engine.pattern.SolidSrpDemo()),
            new Entry("SOLID 原则", null, new com.algoviz.engine.pattern.SolidOcpDemo()),
            new Entry("SOLID 原则", null, new com.algoviz.engine.pattern.SolidLspDemo()),
            new Entry("SOLID 原则", null, new com.algoviz.engine.pattern.SolidIspDemo()),
            new Entry("SOLID 原则", null, new com.algoviz.engine.pattern.SolidDipDemo()),
            new Entry("重构手法", null, new com.algoviz.engine.pattern.RefactorExtractDemo()),
            new Entry("重构手法", null, new com.algoviz.engine.pattern.RefactorParamDemo()),
            new Entry("重构手法", null, new com.algoviz.engine.pattern.RefactorPolyDemo()),
            new Entry("算法", "前缀和", new com.algoviz.engine.array.PrefixSum(new int[]{3, 7, 2, 5, 1})),
            new Entry("算法", "查找", new com.algoviz.engine.array.SlidingWindow()),
            new Entry("算法", "双指针", new com.algoviz.engine.array.TwoPointers(new int[]{1, 2, 5, 5, 9, 12, 15}, 14)),
            new Entry("算法", "排序", new MergeSort(new int[]{5, 2, 9, 1, 5})),
            new Entry("算法", "排序", new HeapSort(new int[]{5, 2, 9, 1, 5})),
            new Entry("算法", "树", new InvertTree()),
            new Entry("算法", "树", new SymmetricTree()),
            new Entry("算法", "树", new LcaDemo()),
            new Entry("算法", "树", new AvlDemo()),
            new Entry("设计模式", "创建型", new SingletonDemo()),
            new Entry("设计模式", "创建型", new FactoryMethodDemo()),
            new Entry("设计模式", "创建型", new AbstractFactoryDemo()),
            new Entry("设计模式", "创建型", new BuilderDemo()),
            new Entry("设计模式", "创建型", new PrototypeDemo()),
            new Entry("设计模式", "结构型", new AdapterDemo()),
            new Entry("设计模式", "结构型", new BridgeDemo()),
            new Entry("设计模式", "结构型", new CompositeDemo()),
            new Entry("设计模式", "结构型", new DecoratorDemo()),
            new Entry("设计模式", "结构型", new FacadeDemo()),
            new Entry("设计模式", "结构型", new FlyweightDemo()),
            new Entry("设计模式", "结构型", new ProxyDemo()),
            new Entry("设计模式", "行为型", new ChainDemo()),
            new Entry("设计模式", "行为型", new CommandDemo()),
            new Entry("设计模式", "行为型", new InterpreterDemo()),
            new Entry("设计模式", "行为型", new IteratorDemo()),
            new Entry("设计模式", "行为型", new MediatorDemo()),
            new Entry("设计模式", "行为型", new MementoDemo()),
            new Entry("设计模式", "行为型", new ObserverDemo()),
            new Entry("设计模式", "行为型", new StateDemo()),
            new Entry("设计模式", "行为型", new StrategyDemo()),
            new Entry("设计模式", "行为型", new TemplateMethodDemo()),
            new Entry("设计模式", "行为型", new VisitorDemo()),
            new Entry("数学", null, new MatrixMultiply()),
            new Entry("数学", null, new BitmapDemo())
    );

    private AlgorithmRegistry() {
    }

    public static List<Entry> all() {
        return ALL;
    }

    /** 🎲 换一批随机数据：按 id 返回换了输入的新实例；不支持的演示返回 null（按钮置灰）。 */
    public static Algorithm randomVariant(Algorithm current) {
        return randomVariant(current, 5 + new java.util.Random().nextInt(4));
    }

    /** 数据量滑杆入口：n = 期望元素个数（算法内部再微调，如二分固定 7、双指针需 7+）。 */
    public static Algorithm randomVariant(Algorithm current, int n) {
        java.util.Random r = new java.util.Random();
        return switch (current.id()) {
            case "sort.bubble" -> new BubbleSort(randArray(r, n));
            case "sort.selection" -> new SelectionSort(randArray(r, n));
            case "sort.insertion" -> new InsertionSort(randArray(r, n));
            case "sort.quick" -> new QuickSort(randArray(r, n));
            case "search.binary" -> {
                int[] a = randArray(r, 7);
                java.util.Arrays.sort(a);
                yield new BinarySearch(a, a[r.nextInt(a.length)]);   // 目标必存在
            }
            case "list.reverse" -> new ReverseList(randArray(r, n));
            case "list.cycle" -> {
                int[] vals = randArray(r, n);
                yield new LinkedListCycle(vals, r.nextInt(n - 1));   // 环尾接回中间某节点
            }
            case "tree.traversal.pre" -> new TreeTraversal(Mode.PRE, randDistinct(r, 7, 1, 99));
            case "tree.traversal.in" -> new TreeTraversal(Mode.IN, randDistinct(r, 7, 1, 99));
            case "tree.traversal.post" -> new TreeTraversal(Mode.POST, randDistinct(r, 7, 1, 99));
            case "tree.levelorder" -> new LevelOrderTraversal(randDistinct(r, 7, 1, 99));
            case "tree.bst.insert" -> {
                int[] base = randDistinct(r, 6, 1, 99);
                yield new BstInsert(base, 1 + r.nextInt(99));        // 可能命中去重分支，也是教学点
            }
            case "tree.bst.delete" -> {
                int[] base = randDistinct(r, 7, 1, 99);
                yield new BstDelete(base, base[r.nextInt(base.length)]);
            }
            case "graph.dijkstra" -> new Dijkstra(randomWeights(r));
            case "graph.bfs" -> new BfsDemo(r.nextLong());
            case "graph.dfs" -> new DfsDemo(r.nextLong());
            case "sort.merge" -> new MergeSort(randArray(r, n));
            case "sort.heap" -> new HeapSort(randArray(r, n));
            case "array.prefixsum" -> new PrefixSum(randArray(r, Math.min(n, 8)));
            case "array.twopointers" -> {
                int m = Math.max(n, 7);
                int[] a = randArray(r, m);
                java.util.Arrays.sort(a);
                yield new com.algoviz.engine.array.TwoPointers(a, a[r.nextInt(a.length)] + a[r.nextInt(a.length)]);
            }
            case "tree.bplus" -> new com.algoviz.engine.tree.BPlusTreeDemo(randomKeys(r, Math.min(n + 4, 16)));
            default -> null;
        };
    }

    private static int[] randArray(java.util.Random r, int n) {
        int[] a = new int[n];
        for (int i = 0; i < n; i++) {
            a[i] = 1 + r.nextInt(99);
        }
        return a;
    }

    private static int[] randDistinct(java.util.Random r, int n, int lo, int hi) {
        java.util.Set<Integer> s = new java.util.LinkedHashSet<>();
        while (s.size() < n) {
            s.add(lo + r.nextInt(hi - lo + 1));
        }
        return s.stream().mapToInt(Integer::intValue).toArray();
    }

    private static int[] randomKeys(java.util.Random r, int n) {
        return randDistinct(r, n, 1, 99);
    }

    private static int[] randomWeights(java.util.Random r) {
        int[] w = new int[9];   // 示例图固定 9 条边
        for (int i = 0; i < w.length; i++) {
            w[i] = 1 + r.nextInt(20);
        }
        return w;
    }
}
