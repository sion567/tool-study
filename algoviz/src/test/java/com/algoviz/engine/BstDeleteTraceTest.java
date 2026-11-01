package com.algoviz.engine;

import com.algoviz.engine.ds.TreeView;
import com.algoviz.engine.trace.Trace;
import com.algoviz.engine.trace.TraceCodec;
import com.algoviz.engine.tree.BstDelete;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static com.algoviz.engine.TraceAsserts.assertContract;
import static org.junit.jupiter.api.Assertions.assertEquals;

class BstDeleteTraceTest {

    private static final String GOLDEN = "src/test/resources/traces/bst_delete.golden.json";

    private static Trace trace() {
        return new BstDelete(new int[]{5, 3, 8, 1, 4, 7, 9}, 5).produce();
    }

    @Test
    void contractAndInvariantHold() {
        assertContract(trace());
    }

    /** 行为：删 5（双子）→ 后继 7 的值抄进根，7 的节点从右子树摘除，8 的左孩子变空、右孩子仍是 9。 */
    @Test
    void twoChildrenCaseReplacedBySuccessor() {
        TreeView last = trace().steps().get(trace().steps().size() - 1).tree();
        TreeView.TreeNodeBox root = last.nodes().get(0);   // @1 原 val=5
        TreeView.TreeNodeBox three = last.nodes().get(1);  // @2 val=3
        TreeView.TreeNodeBox eight = last.nodes().get(2);  // @3 val=8
        TreeView.TreeNodeBox nine = last.nodes().get(6);   // @7 val=9
        assertEquals(7, root.val(), "根应装入后继的值 7");
        assertEquals(8, eight.val());
        assertEquals(three.nodeId(), root.leftId(), "根的左孩子仍是 3");
        assertEquals(eight.nodeId(), root.rightId(), "根的右孩子仍是 8 节点");
        org.junit.jupiter.api.Assertions.assertNull(eight.leftId(), "7 的节点被摘除后，8 的左孩子应为空");
        assertEquals(nine.nodeId(), eight.rightId(), "8 的右孩子仍是 9");
        assertEquals(3, three.val());
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
