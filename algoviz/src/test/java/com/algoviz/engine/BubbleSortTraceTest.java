package com.algoviz.engine;

import com.algoviz.engine.sort.BubbleSort;
import com.algoviz.engine.trace.Trace;
import com.algoviz.engine.trace.TraceCodec;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static com.algoviz.engine.TraceAsserts.assertContract;
import static com.algoviz.engine.TraceAsserts.assertFinalSorted;
import static org.junit.jupiter.api.Assertions.assertEquals;

class BubbleSortTraceTest {

    private static final int[] INPUT = {5, 2, 9, 1, 5};
    private static final String GOLDEN = "src/test/resources/traces/bubble_sort.golden.json";

    private static Trace trace() {
        return new BubbleSort(INPUT).produce();
    }

    @Test
    void finalStateIsSorted() {
        assertFinalSorted(trace(), List.of(1, 2, 5, 5, 9));
    }

    @Test
    void contractAndInvariantHold() {
        assertContract(trace());
    }

    /** 深拷贝铁律：产出之后再改输入，不得影响已产出的 Trace。 */
    @Test
    void snapshotsAreIndependentOfInput() {
        int[] mutable = {3, 1, 2};
        BubbleSort algo = new BubbleSort(mutable);
        Trace t = algo.produce();
        mutable[0] = 999;
        var last = t.steps().get(t.steps().size() - 1).array();
        assertEquals(List.of(1, 2, 3), last.values(), "快照被外部修改污染");
    }

    /** golden：Trace JSON 与基准逐字节比对（蓝图 §7 M1）。 */
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
