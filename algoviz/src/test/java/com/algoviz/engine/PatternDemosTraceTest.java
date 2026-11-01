package com.algoviz.engine;

import com.algoviz.engine.pattern.StrategyDemo;
import com.algoviz.engine.trace.Trace;
import com.algoviz.engine.trace.TraceCodec;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static com.algoviz.engine.TraceAsserts.assertContract;
import static org.junit.jupiter.api.Assertions.assertEquals;

class PatternDemosTraceTest {

    @Test
    void strategyContractHolds() {
        assertContract(new StrategyDemo().produce());
    }

    /** 行为：演示结束时，@1.strategy 必须已经从 @2 换到 @3（策略模式的本体）。 */
    @Test
    void strategyArrowFlipsAtRuntime() throws Exception {
        Trace t = new StrategyDemo().produce();
        var first = t.steps().get(0).heap().objects().get("@1").fields().get("strategy").display();
        var last = t.steps().get(t.steps().size() - 1).heap().objects().get("@1").fields().get("strategy").display();
        assertEquals("@2", first, "初始装配应是冒泡");
        assertEquals("@3", last, "演示结束应已换到插入排序");
    }

    @Test
    void strategyGoldenMatches() throws Exception {
        golden("pattern_strategy", new StrategyDemo().produce());
    }

    /** golden：Trace JSON 与基准逐字节比对。 */
    static void golden(String name, Trace t) throws Exception {
        String actual = TraceCodec.toJson(t).strip();
        Path golden = Path.of("src/test/resources/traces/" + name + ".golden.json");
        if (Files.notExists(golden)) {
            Files.createDirectories(golden.getParent());
            Files.writeString(golden, actual + "\n", StandardCharsets.UTF_8);
            System.out.println(">>> golden 基准不存在，已自动生成；从下次起逐字节比对");
            return;
        }
        String expected = Files.readString(golden, StandardCharsets.UTF_8).strip();
        assertEquals(expected, actual,
                "Trace 输出发生变化：若是有意的行为变更，删除 golden 文件重新生成");
    }
}
