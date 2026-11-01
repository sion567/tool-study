package com.algoviz.engine;

import com.algoviz.engine.ds.TreeView;
import com.algoviz.engine.trace.Trace;
import com.algoviz.engine.trace.TraceCodec;
import com.algoviz.engine.tree.TreeTraversal;
import com.algoviz.engine.tree.TreeTraversal.Mode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;

import static com.algoviz.engine.TraceAsserts.assertContract;
import static org.junit.jupiter.api.Assertions.assertEquals;

class TreeTraversalTraceTest {

    private static final int[] INPUT = {5, 3, 8, 1, 4, 7, 9};

    private static Trace trace(Mode mode) {
        return new TreeTraversal(mode, INPUT).produce();
    }

    @ParameterizedTest
    @EnumSource(Mode.class)
    void contractAndInvariantHold(Mode mode) {
        assertContract(trace(mode));
    }

    /** 行为：最终访问序号连出来的值序列 = 该序的标准遍历序（调用树视图的可执行定义）。 */
    @ParameterizedTest
    @EnumSource(Mode.class)
    void visitSequenceMatches(Mode mode) {
        TreeView last = trace(mode).steps().get(trace(mode).steps().size() - 1).tree();
        List<Integer> visited = last.nodes().stream()
                .filter(nb -> nb.visitOrder() != null)
                .sorted(Comparator.comparingInt(TreeView.TreeNodeBox::visitOrder))
                .map(TreeView.TreeNodeBox::val)
                .toList();
        List<Integer> expected = switch (mode) {
            case PRE -> List.of(5, 3, 1, 4, 8, 7, 9);
            case IN -> List.of(1, 3, 4, 5, 7, 8, 9);
            case POST -> List.of(1, 4, 3, 7, 9, 8, 5);
        };
        assertEquals(expected, visited, mode + " 序错误");
        assertEquals(7, visited.size(), "7 个节点都应被访问一次");
    }

    @ParameterizedTest
    @EnumSource(Mode.class)
    void goldenTraceMatches(Mode mode) throws Exception {
        String actual = TraceCodec.toJson(trace(mode)).strip();
        String file = "src/test/resources/traces/traversal_"
                + mode.name().toLowerCase() + ".golden.json";
        Path golden = Path.of(file);
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
