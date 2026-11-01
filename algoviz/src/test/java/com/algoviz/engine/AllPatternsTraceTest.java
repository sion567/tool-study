package com.algoviz.engine;

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

/** 全部 20 个模式演示的统一测试：合约 + golden（golden 缺失时自动生成）。 */
class AllPatternsTraceTest {

    static Stream<Object[]> demos() {
        return Stream.of(
                new Object[]{"decorator", new DecoratorDemo()},
                new Object[]{"command", new CommandDemo()},
                new Object[]{"iterator", new IteratorDemo()},
                new Object[]{"state", new StateDemo()},
                new Object[]{"singleton", new SingletonDemo()},
                new Object[]{"factorymethod", new FactoryMethodDemo()},
                new Object[]{"abstractfactory", new AbstractFactoryDemo()},
                new Object[]{"builder", new BuilderDemo()},
                new Object[]{"prototype", new PrototypeDemo()},
                new Object[]{"adapter", new AdapterDemo()},
                new Object[]{"bridge", new BridgeDemo()},
                new Object[]{"composite", new CompositeDemo()},
                new Object[]{"facade", new FacadeDemo()},
                new Object[]{"flyweight", new FlyweightDemo()},
                new Object[]{"proxy", new ProxyDemo()},
                new Object[]{"chain", new ChainDemo()},
                new Object[]{"interpreter", new InterpreterDemo()},
                new Object[]{"mediator", new MediatorDemo()},
                new Object[]{"memento", new MementoDemo()},
                new Object[]{"visitor", new VisitorDemo()}
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("demos")
    void contractHolds(String name, Algorithm alg) {
        assertContract(alg.produce());
    }

    /** 重复 produce 必须幂等（Registry 缓存实例会多次调用）。 */
    @ParameterizedTest(name = "{0}")
    @MethodSource("demos")
    void produceIsIdempotent(String name, Algorithm alg) {
        Trace first = alg.produce();
        Trace second = alg.produce();
        assertEquals(TraceCodec.toJson(first).strip(), TraceCodec.toJson(second).strip(),
                name + " 重复 produce 输出不同——状态没清零");
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("demos")
    void goldenTraceMatches(String name, Algorithm alg) throws Exception {
        String actual = TraceCodec.toJson(alg.produce()).strip();
        Path golden = Path.of("src/test/resources/traces/pattern_" + name + ".golden.json");
        if (Files.notExists(golden)) {
            Files.createDirectories(golden.getParent());
            Files.writeString(golden, actual + "\n", StandardCharsets.UTF_8);
            System.out.println(">>> golden 基准不存在，已自动生成；从下次起逐字节比对");
            return;
        }
        String expected = Files.readString(golden, StandardCharsets.UTF_8).strip();
        assertEquals(expected, actual, name + " 的 Trace 输出变化：有意变更请删 golden 重新生成");
    }
}
