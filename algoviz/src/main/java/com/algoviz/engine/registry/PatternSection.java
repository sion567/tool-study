package com.algoviz.engine.registry;

import com.algoviz.engine.Algorithm;
import com.algoviz.engine.AlgorithmRegistry;

import java.util.List;

/** 「模式与工程」域：设计模式 + SOLID 原则 + 重构手法 + 并发/分布式模式。 */
public final class PatternSection implements AlgorithmSection {

    @Override
    public List<AlgorithmRegistry.Entry> entries() {
        return List.of(
                new AlgorithmRegistry.Entry("SOLID 原则", null, new com.algoviz.engine.pattern.SolidSrpDemo()),
                new AlgorithmRegistry.Entry("SOLID 原则", null, new com.algoviz.engine.pattern.SolidOcpDemo()),
                new AlgorithmRegistry.Entry("SOLID 原则", null, new com.algoviz.engine.pattern.SolidLspDemo()),
                new AlgorithmRegistry.Entry("SOLID 原则", null, new com.algoviz.engine.pattern.SolidIspDemo()),
                new AlgorithmRegistry.Entry("SOLID 原则", null, new com.algoviz.engine.pattern.SolidDipDemo()),
                new AlgorithmRegistry.Entry("重构手法", null, new com.algoviz.engine.pattern.RefactorExtractDemo()),
                new AlgorithmRegistry.Entry("重构手法", null, new com.algoviz.engine.pattern.RefactorParamDemo()),
                new AlgorithmRegistry.Entry("重构手法", null, new com.algoviz.engine.pattern.RefactorPolyDemo()),
                new AlgorithmRegistry.Entry("并发模式", null, new com.algoviz.engine.pattern.ConcurrentProdConsDemo()),
                new AlgorithmRegistry.Entry("并发模式", null, new com.algoviz.engine.pattern.ConcurrentRwLockDemo()),
                new AlgorithmRegistry.Entry("并发模式", null, new com.algoviz.engine.pattern.ConcurrentPoolDemo()),
                new AlgorithmRegistry.Entry("并发模式", null, new com.algoviz.engine.pattern.ConcurrentVisDemo()),
                new AlgorithmRegistry.Entry("并发模式", null, new com.algoviz.engine.pattern.ConcurrentDeadlockDemo()),
                new AlgorithmRegistry.Entry("分布式模式", null, new com.algoviz.engine.pattern.DistBreakerDemo()),
                new AlgorithmRegistry.Entry("分布式模式", null, new com.algoviz.engine.pattern.DistDiscoveryDemo()),
                new AlgorithmRegistry.Entry("分布式模式", null, new com.algoviz.engine.pattern.DistCqrsDemo()),
                new AlgorithmRegistry.Entry("设计模式", "创建型", new com.algoviz.engine.pattern.SingletonDemo()),
                new AlgorithmRegistry.Entry("设计模式", "创建型", new com.algoviz.engine.pattern.FactoryMethodDemo()),
                new AlgorithmRegistry.Entry("设计模式", "创建型", new com.algoviz.engine.pattern.AbstractFactoryDemo()),
                new AlgorithmRegistry.Entry("设计模式", "创建型", new com.algoviz.engine.pattern.BuilderDemo()),
                new AlgorithmRegistry.Entry("设计模式", "创建型", new com.algoviz.engine.pattern.PrototypeDemo()),
                new AlgorithmRegistry.Entry("设计模式", "结构型", new com.algoviz.engine.pattern.AdapterDemo()),
                new AlgorithmRegistry.Entry("设计模式", "结构型", new com.algoviz.engine.pattern.BridgeDemo()),
                new AlgorithmRegistry.Entry("设计模式", "结构型", new com.algoviz.engine.pattern.CompositeDemo()),
                new AlgorithmRegistry.Entry("设计模式", "结构型", new com.algoviz.engine.pattern.DecoratorDemo()),
                new AlgorithmRegistry.Entry("设计模式", "结构型", new com.algoviz.engine.pattern.FacadeDemo()),
                new AlgorithmRegistry.Entry("设计模式", "结构型", new com.algoviz.engine.pattern.FlyweightDemo()),
                new AlgorithmRegistry.Entry("设计模式", "结构型", new com.algoviz.engine.pattern.ProxyDemo()),
                new AlgorithmRegistry.Entry("设计模式", "行为型", new com.algoviz.engine.pattern.ChainDemo()),
                new AlgorithmRegistry.Entry("设计模式", "行为型", new com.algoviz.engine.pattern.CommandDemo()),
                new AlgorithmRegistry.Entry("设计模式", "行为型", new com.algoviz.engine.pattern.InterpreterDemo()),
                new AlgorithmRegistry.Entry("设计模式", "行为型", new com.algoviz.engine.pattern.IteratorDemo()),
                new AlgorithmRegistry.Entry("设计模式", "行为型", new com.algoviz.engine.pattern.MediatorDemo()),
                new AlgorithmRegistry.Entry("设计模式", "行为型", new com.algoviz.engine.pattern.MementoDemo()),
                new AlgorithmRegistry.Entry("设计模式", "行为型", new com.algoviz.engine.pattern.ObserverDemo()),
                new AlgorithmRegistry.Entry("设计模式", "行为型", new com.algoviz.engine.pattern.StateDemo()),
                new AlgorithmRegistry.Entry("设计模式", "行为型", new com.algoviz.engine.pattern.StrategyDemo()),
                new AlgorithmRegistry.Entry("设计模式", "行为型", new com.algoviz.engine.pattern.TemplateMethodDemo()),
                new AlgorithmRegistry.Entry("设计模式", "行为型", new com.algoviz.engine.pattern.VisitorDemo())
        );
    }

    @Override
    public Algorithm variantOf(String id, int n, java.util.Random r) {
        return null;   // 固定剧本域：无「换一批数据」
    }
}
