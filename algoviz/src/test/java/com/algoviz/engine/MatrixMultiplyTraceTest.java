package com.algoviz.engine;

import com.algoviz.engine.math.MatrixMultiply;
import com.algoviz.engine.math.MathView;
import com.algoviz.engine.trace.Trace;
import com.algoviz.engine.trace.TraceCodec;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static com.algoviz.engine.TraceAsserts.assertContract;
import static org.junit.jupiter.api.Assertions.assertEquals;

class MatrixMultiplyTraceTest {

    private static final String GOLDEN = "src/test/resources/traces/matrix_multiply.golden.json";

    private static Trace trace() {
        return new MatrixMultiply().produce();
    }

    @Test
    void contractHolds() {
        assertContract(trace());
    }

    /** 行为：最终 C = [[58, 64], [139, 154]]（手算基准）。 */
    @Test
    void finalMatrixIsCorrect() {
        MathView last = trace().steps().get(trace().steps().size() - 1).math();
        MathView.Grid c = last.grids().stream().filter(g -> g.id().equals("C")).findFirst().orElseThrow();
        assertEquals("58", c.value(0, 0));
        assertEquals("64", c.value(0, 1));
        assertEquals("139", c.value(1, 0));
        assertEquals("154", c.value(1, 1));
        assertEquals(4, c.doneCells().size(), "4 个格子都应定格");
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
