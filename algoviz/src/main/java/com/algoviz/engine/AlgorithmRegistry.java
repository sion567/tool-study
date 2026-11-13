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
            new Entry("数学", "几何坐标", new com.algoviz.engine.math.UnitCircleDemo()),
            new Entry("数学", "微积分", new com.algoviz.engine.math.TaylorDemo()),
            new Entry("数学", "微积分", new com.algoviz.engine.math.TaylorDemo(true)),   // cos 版
            new Entry("数学", "微积分", new com.algoviz.engine.math.DerivativeDemo()),
            new Entry("数学", "微积分", new com.algoviz.engine.math.RiemannSumDemo()),
            new Entry("数学", "几何坐标", new com.algoviz.engine.math.CardioidDemo()),
            new Entry("数学", "图论离散", new com.algoviz.engine.math.YangHuiDemo()),
            new Entry("数学", "线性代数", new com.algoviz.engine.math.GaussianElimDemo()),
            new Entry("数学", "图论离散", new com.algoviz.engine.math.HanoiDemo()),
            new Entry("数学", "概率统计", new com.algoviz.engine.math.MonteCarloPiDemo()),
            new Entry("数学", "图论离散", new com.algoviz.engine.math.SevenBridgesDemo()),
            new Entry("数学", "数与计算", new com.algoviz.engine.math.RsaMiniDemo()),
            new Entry("数学", "几何坐标", new com.algoviz.engine.math.GoldenSpiralDemo()),
            new Entry("数学", "几何坐标", new com.algoviz.engine.math.RoseCurveGalleryDemo()),
            new Entry("数学", "几何坐标", new com.algoviz.engine.math.Rotation2DDemo()),
            new Entry("数学", "微积分", new com.algoviz.engine.math.PendulumDemo()),
            new Entry("数学", "数与计算", new com.algoviz.engine.math.BisectionDemo()),
            new Entry("数学", "微积分", new com.algoviz.engine.math.CompoundInterestDemo()),
            new Entry("数学", "数与计算", new com.algoviz.engine.math.FibMatrixDemo()),
            new Entry("数学", "概率统计", new com.algoviz.engine.math.DiceBellDemo()),
            new Entry("数学", "概率统计", new com.algoviz.engine.math.GamblersRuinDemo()),
            new Entry("数学", "数与计算", new com.algoviz.engine.math.FastPowerDemo()),
            new Entry("数学", "数与计算", new com.algoviz.engine.math.GcdDemo()),
            new Entry("数学", "图论离散", new com.algoviz.engine.math.BloomFilterDemo()),
            new Entry("数学", "数与计算", new com.algoviz.engine.math.BitOpsDemo()),
            new Entry("数学", "数与计算", new com.algoviz.engine.math.SieveDemo()),
            new Entry("数学", "数与计算", new com.algoviz.engine.math.NewtonDemo()),
            new Entry("数学", "数与计算", new com.algoviz.engine.math.CordicDemo()),
            new Entry("数学", "图论离散", new com.algoviz.engine.math.BucketDemo()),
            new Entry("算法", "回溯", new com.algoviz.engine.backtracking.SudokuDemo()),
            new Entry("算法", "图", new com.algoviz.engine.graph.AStarDemo()),
            new Entry("算法", "树", new com.algoviz.engine.tree.PathSumDemo()),
            new Entry("算法", "树", new com.algoviz.engine.tree.HuffmanDemo()),
            new Entry("算法", "链表", new com.algoviz.engine.list.ListBuildDemo()),
            new Entry("算法", "链表", new com.algoviz.engine.list.ListDeleteDemo()),
            new Entry("算法", "链表", new com.algoviz.engine.list.DoublyListDemo()),
            new Entry("数学", "图论离散", new com.algoviz.engine.math.HashMapDemo()),
            new Entry("数学", "信号处理", new com.algoviz.engine.math.FourierDemo()),
            new Entry("深度学习", null, new com.algoviz.engine.dl.CnnDemo()),
            new Entry("深度学习", null, new com.algoviz.engine.dl.RnnDemo()),
            new Entry("深度学习", null, new com.algoviz.engine.dl.GnnDemo()),
            new Entry("深度学习", null, new com.algoviz.engine.dl.GanDemo()),
            new Entry("深度学习", null, new com.algoviz.engine.dl.AttentionDemo()),
            new Entry("深度学习", null, new com.algoviz.engine.dl.LstmDemo()),
            new Entry("深度学习", null, new com.algoviz.engine.dl.BackpropDemo()),
            new Entry("深度学习", null, new com.algoviz.engine.dl.Word2VecDemo()),
            new Entry("深度学习", null, new com.algoviz.engine.dl.ResNetDemo()),
            new Entry("深度学习", null, new com.algoviz.engine.dl.DropoutDemo()),
            new Entry("深度学习", null, new com.algoviz.engine.dl.BatchNormDemo()),
            new Entry("深度学习", null, new com.algoviz.engine.dl.KMeansDemo()),
            new Entry("深度学习", null, new com.algoviz.engine.dl.GradientDescentDemo()),
            new Entry("机器学习", null, new com.algoviz.engine.ml.QlearnDemo()),
            new Entry("机器学习", null, new com.algoviz.engine.ml.DtreeDemo()),
            new Entry("机器学习", null, new com.algoviz.engine.ml.NaiveBayesDemo()),
            new Entry("物理", "力学", new com.algoviz.engine.physics.ProjectileDemo()),
            new Entry("物理", "力学", new com.algoviz.engine.physics.SpringDemo()),
            new Entry("物理", "力学", new com.algoviz.engine.physics.DoublePendulumDemo()),
            new Entry("物理", "热与分子", new com.algoviz.engine.physics.HeatDemo()),
            new Entry("物理", "热与分子", new com.algoviz.engine.physics.GasDemo()),
            new Entry("物理", "天体", new com.algoviz.engine.physics.NBodyDemo()),
            new Entry("奥数", "组合构造", new com.algoviz.engine.olympiad.KnightsTourDemo()),
            new Entry("奥数", "组合构造", new com.algoviz.engine.olympiad.JosephusDemo()),
            new Entry("奥数", "数论", new com.algoviz.engine.olympiad.CrtDemo()),
            new Entry("奥数", "不变量", new com.algoviz.engine.olympiad.FifteenPuzzleDemo()),
            new Entry("化学", "反应过程", new com.algoviz.engine.chem.KineticsDemo()),
            new Entry("化学", "反应过程", new com.algoviz.engine.chem.EquilibriumDemo()),
            new Entry("化学", "反应过程", new com.algoviz.engine.chem.TitrationDemo()),
            new Entry("化学", "分子结构", new com.algoviz.engine.chem.IsomerDemo()),
            new Entry("密码学", "哈希", new com.algoviz.engine.crypto.Sm3Demo()),
            new Entry("密码学", "对称", new com.algoviz.engine.crypto.Sm4Demo()),
            new Entry("密码学", "非对称", new com.algoviz.engine.crypto.Sm2Demo()),
            new Entry("密码学", "对称", new com.algoviz.engine.crypto.AesDemo()),
            new Entry("密码学", "哈希", new com.algoviz.engine.crypto.Sha256Demo()),
            new Entry("数学", "数与计算", new com.algoviz.engine.math.KaratsubaDemo()),
            new Entry("数学", "微积分", new com.algoviz.engine.math.TrapezoidDemo()),
            new Entry("数学", "数与计算", new com.algoviz.engine.math.BigIntDemo()),
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
            new Entry("并发模式", null, new com.algoviz.engine.pattern.ConcurrentProdConsDemo()),
            new Entry("并发模式", null, new com.algoviz.engine.pattern.ConcurrentRwLockDemo()),
            new Entry("并发模式", null, new com.algoviz.engine.pattern.ConcurrentPoolDemo()),
            new Entry("并发模式", null, new com.algoviz.engine.pattern.ConcurrentVisDemo()),
            new Entry("并发模式", null, new com.algoviz.engine.pattern.ConcurrentDeadlockDemo()),
            new Entry("分布式模式", null, new com.algoviz.engine.pattern.DistBreakerDemo()),
            new Entry("分布式模式", null, new com.algoviz.engine.pattern.DistDiscoveryDemo()),
            new Entry("分布式模式", null, new com.algoviz.engine.pattern.DistCqrsDemo()),
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
            new Entry("数学", "线性代数", new MatrixMultiply()),
            new Entry("数学", "图论离散", new BitmapDemo())
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

    /** 该演示是否支持随机变体（不支持则 🎲/滑杆置灰——固定剧本类没有「换一批数据」）。 */
    public static boolean hasVariant(Algorithm current) {
        return variantOf(current, 8) != null;
    }

    /** 数据量滑杆入口：n = 期望元素个数（算法内部再微调，如二分固定 7、双指针需 7+）。 */
    public static Algorithm randomVariant(Algorithm current, int n) {
        return variantOf(current, n);
    }

    private static Algorithm variantOf(Algorithm current, int n) {
        java.util.Random r = new java.util.Random();
        return switch (current.id()) {
            case "sort.bubble" -> new BubbleSort(randArray(r, n));
            case "sort.selection" -> new SelectionSort(randArray(r, n));
            case "sort.insertion" -> new InsertionSort(randArray(r, n));
            case "sort.quick" -> new QuickSort(randArray(r, n));
            case "search.binary" -> {
                int[] a = randDistinct(r, 7, 2, 200);   // 稀疏取值：间距自然、不像等差（二分前提：有序）
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
            // ---- 打磨批：数据型 demo 变体扩充 ----
            case "dp.knapsack" -> {
                int items = Math.max(3, Math.min(n - 2, 5));
                int[] w = new int[items], v = new int[items];
                for (int i = 0; i < items; i++) {
                    w[i] = 1 + r.nextInt(5);
                    v[i] = 5 + r.nextInt(30);
                }
                yield new KnapsackDemo(w, v, 4 + r.nextInt(5));
            }
            case "dp.lcs" -> {
                String abc = "ABCD";
                StringBuilder a = new StringBuilder(), b = new StringBuilder();
                for (int i = 0; i < 5 + r.nextInt(3); i++) a.append(abc.charAt(r.nextInt(4)));
                for (int i = 0; i < 5 + r.nextInt(3); i++) b.append(abc.charAt(r.nextInt(4)));
                yield new LcsDemo(a.toString(), b.toString());
            }
            case "dp.lis" -> new LisDemo(randArray(r, Math.max(6, Math.min(n + 1, 10))));
            case "dp.climbing" -> new ClimbingStairsDemo(5 + r.nextInt(11));
            case "array.slidingwindow" -> new com.algoviz.engine.array.SlidingWindow(randArray(r, Math.max(6, Math.min(n + 1, 10))));
            case "string.kmp" -> {
                String abc = "AB";
                StringBuilder text = new StringBuilder();
                for (int i = 0; i < 14 + r.nextInt(5); i++) text.append(abc.charAt(r.nextInt(2)));
                String pattern = text.substring(4 + r.nextInt(3), 8 + r.nextInt(3));   // 必在文本中
                yield new KmpDemo(text.toString(), pattern);
            }
            case "math.gcd" -> new GcdDemo(12 + r.nextInt(988), 12 + r.nextInt(988));
            case "math.fastpower" -> new FastPowerDemo(2 + r.nextInt(8), 10 + r.nextInt(21));
            case "math.sieve" -> new SieveDemo(20 + r.nextInt(41));
            case "backtracking.nqueens" -> new com.algoviz.engine.backtracking.NQueensDemo(4 + r.nextInt(3));
            case "backtracking.permutation" -> new com.algoviz.engine.backtracking.PermutationDemo(randDistinct(r, 3 + r.nextInt(3), 1, 99));
            case "math.hashmap" -> {
                int[] keys = new int[5 + r.nextInt(4)];
                for (int i = 0; i < keys.length; i++) keys[i] = 1 + r.nextInt(99);
                yield new com.algoviz.engine.math.HashMapDemo(keys, keys[keys.length - 1]);   // 查询最后一个：走链有戏
            }
            case "math.bucket" -> {
                int[] data = new int[6 + r.nextInt(4)];
                for (int i = 0; i < data.length; i++) data[i] = r.nextInt(1024);
                data[data.length - 1] = data[0];                      // 保证有一个重复
                yield new com.algoviz.engine.math.BucketDemo(data);
            }
            case "physics.projectile" -> new com.algoviz.engine.physics.ProjectileDemo(
                    20 + r.nextInt(11), 30 + r.nextInt(31));   // v₀ 20~30，θ 30°~60°
            case "physics.nbody" -> new com.algoviz.engine.physics.NBodyDemo(1L + r.nextInt(9999));
            case "physics.gas" -> new com.algoviz.engine.physics.GasDemo(1L + r.nextInt(9999));
            // ---- 奥数名题 ----
            case "olympiad.knights" -> {
                int[][] starts = {{0, 0}, {0, 7}, {7, 0}, {7, 7}};   // 四角起点（验证可走完全程）
                int[] s = starts[r.nextInt(starts.length)];
                yield new com.algoviz.engine.olympiad.KnightsTourDemo(s[0], s[1]);
            }
            case "olympiad.josephus" -> new com.algoviz.engine.olympiad.JosephusDemo(
                    8 + r.nextInt(7), 2 + r.nextInt(3));               // n 8~14，k 2~4
            case "olympiad.crt" -> {
                int[] primes = {3, 5, 7, 11};
                int i1 = r.nextInt(primes.length);
                int i2 = r.nextInt(primes.length);
                while (i2 == i1) {
                    i2 = r.nextInt(primes.length);   // 两模数必须不同（互质素数 → CRT 解恒存在）
                }
                int p1 = primes[i1], p2 = primes[i2];
                int aa = 1 + r.nextInt(p1 - 1), bb = 1 + r.nextInt(p2 - 1);
                yield new com.algoviz.engine.olympiad.CrtDemo(aa, p1, bb, p2);
            }
            case "olympiad.fifteen" -> new com.algoviz.engine.olympiad.FifteenPuzzleDemo(1L + r.nextInt(9999));
            // ---- 化学过程类 ----
            case "chem.kinetics" -> new com.algoviz.engine.chem.KineticsDemo(
                    0.05 + 0.05 * r.nextInt(8));                       // k₁ 0.05~0.40
            case "chem.equilibrium" -> new com.algoviz.engine.chem.EquilibriumDemo(
                    0.3 + 0.15 * r.nextInt(4));                        // 扰动注入 0.3~0.75
            case "chem.titration" -> new com.algoviz.engine.chem.TitrationDemo(
                    new double[]{0.05, 0.10, 0.20}[r.nextInt(3)]);     // 酸浓度档位
            case "chem.isomer" -> new com.algoviz.engine.chem.IsomerDemo(
                    1 + r.nextInt(3));                                 // C₅ 三兄弟随机出场
            // ---- 密码学 ----
            case "crypto.sm3" -> new com.algoviz.engine.crypto.Sm3Demo(
                    new String[]{"abc", "国密算法", "SM3 test"}[r.nextInt(3)]);
            case "crypto.sm4" -> {
                int[] kb = new int[16];
                for (int i = 0; i < 16; i++) {
                    kb[i] = r.nextInt(256);                        // 随机 16 字节密钥
                }
                yield new com.algoviz.engine.crypto.Sm4Demo(kb);
            }
            case "crypto.sm2" -> new com.algoviz.engine.crypto.Sm2Demo();
            case "crypto.aes" -> {
                int[] kb = new int[16];
                for (int i = 0; i < 16; i++) {
                    kb[i] = r.nextInt(256);                        // 随机 16 字节密钥
                }
                yield new com.algoviz.engine.crypto.AesDemo(kb);
            }
            case "crypto.sha256" -> new com.algoviz.engine.crypto.Sha256Demo(
                    new String[]{"abc", "SHA-256", "哈希对照"}[r.nextInt(3)]);
            case "dl.lstm" -> new com.algoviz.engine.dl.LstmDemo();
            case "dl.backprop" -> new com.algoviz.engine.dl.BackpropDemo();
            case "dl.word2vec" -> new com.algoviz.engine.dl.Word2VecDemo();
            case "dl.resnet" -> new com.algoviz.engine.dl.ResNetDemo();
            case "dl.dropout" -> new com.algoviz.engine.dl.DropoutDemo();
            case "dl.batchnorm" -> new com.algoviz.engine.dl.BatchNormDemo();
            case "dl.kmeans" -> new com.algoviz.engine.dl.KMeansDemo();
            case "dl.gradient" -> new com.algoviz.engine.dl.GradientDescentDemo();
            case "math.trapezoid" -> new com.algoviz.engine.math.TrapezoidDemo();
            case "math.bigint" -> new com.algoviz.engine.math.BigIntDemo();
            case "ml.qlearn" -> new com.algoviz.engine.ml.QlearnDemo();
            case "ml.dtree" -> new com.algoviz.engine.ml.DtreeDemo();
            case "ml.naivebayes" -> new com.algoviz.engine.ml.NaiveBayesDemo();
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
