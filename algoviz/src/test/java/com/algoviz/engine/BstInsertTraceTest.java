package com.algoviz.engine;

import com.algoviz.engine.ds.TreeView;
import com.algoviz.engine.trace.Trace;
import com.algoviz.engine.trace.TraceCodec;
import com.algoviz.engine.tree.BstInsert;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static com.algoviz.engine.TraceAsserts.assertContract;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class BstInsertTraceTest {

    private static final String GOLDEN = "src/test/resources/traces/bst_insert.golden.json";

    private static Trace trace() {
        return new BstInsert(new int[]{5, 3, 8}, 4).produce();
    }

    @Test
    void contractAndInvariantHold() {
        assertContract(trace());
    }

    /** 行为：4 必须挂成 3 的右孩子（5→左 3，4>3 → 3.right）。 */
    @Test
    void newNodeAttachedAsRightChildOfThree() {
        TreeView last = trace().steps().get(trace().steps().size() - 1).tree();
        TreeView.TreeNodeBox five = last.nodes().get(0);
        TreeView.TreeNodeBox three = last.nodes().get(1);
        assertEquals(5, five.val());
        assertEquals(3, three.val());
        TreeView.TreeNodeBox four = last.nodes().stream()
                .filter(nb -> nb.val() == 4)
                .findFirst().orElseThrow(() -> new AssertionError("没找到新节点 4"));
        assertEquals(three.nodeId(), five.leftId(), "3 应是 5 的左孩子");
        assertEquals(four.nodeId(), three.rightId(), "4 应挂成 3 的右孩子");
        assertNull(four.leftId());
        assertNull(four.rightId());
        assertEquals("visited", four.state(), "新节点应保持高亮态");
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
