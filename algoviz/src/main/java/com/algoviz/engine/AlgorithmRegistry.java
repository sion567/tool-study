package com.algoviz.engine;

import com.algoviz.engine.graph.BfsDemo;
import com.algoviz.engine.graph.Dijkstra;
import com.algoviz.engine.graph.DfsDemo;
import com.algoviz.engine.list.LinkedListCycle;
import com.algoviz.engine.list.ReverseList;
import com.algoviz.engine.math.MatrixMultiply;
import com.algoviz.engine.search.BinarySearch;
import com.algoviz.engine.sort.QuickSort;
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
import com.algoviz.engine.sort.InsertionSort;
import com.algoviz.engine.sort.SelectionSort;
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
            new Entry("算法", "图", new Dijkstra()),
            new Entry("算法", "图", new BfsDemo()),
            new Entry("算法", "图", new DfsDemo()),
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
            new Entry("数学", null, new MatrixMultiply())
    );

    private AlgorithmRegistry() {
    }

    public static List<Entry> all() {
        return ALL;
    }
}
