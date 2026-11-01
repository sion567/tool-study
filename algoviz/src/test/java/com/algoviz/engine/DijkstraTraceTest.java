package com.algoviz.engine;

import com.algoviz.engine.ds.GraphView;
import com.algoviz.engine.graph.Dijkstra;
import com.algoviz.engine.trace.Trace;
import com.algoviz.engine.trace.TraceCodec;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static com.algoviz.engine.TraceAsserts.assertContract;
import static org.junit.jupiter.api.Assertions.assertEquals;

class DijkstraTraceTest {

    private static final String GOLDEN = "src/test/resources/traces/dijkstra.golden.json";

    private static Trace trace() {
        return new Dijkstra().produce();
    }

    @Test
    void contractAndInvariantHold() {
        assertContract(trace());
    }

    /** 行为：全部出圈后的距离表必须等于手算的最短路。 */
    @Test
    void finalDistancesAreShortestPaths() {
        GraphView last = trace().steps().get(trace().steps().size() - 1).graph();
        Map<String, Integer> expected = Map.of("A", 0, "B", 3, "C", 2, "D", 8, "E", 10, "F", 13);
        for (Map.Entry<String, Integer> e : expected.entrySet()) {
            assertEquals(e.getValue(), last.dist().get(e.getKey()),
                    "dist[" + e.getKey() + "] 错误");
        }
        assertEquals(6, last.settledIds().size(), "6 个节点都应出圈");
    }

    @Test
    void goldenTraceMatches() throws Exception {
        String actual = TraceCodec.toJson(trace()).strip();
        Path golden = Path.of(GOLDEN);
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
