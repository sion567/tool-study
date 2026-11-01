package com.algoviz.engine;

import com.algoviz.engine.ds.ListView;
import com.algoviz.engine.list.ReverseList;
import com.algoviz.engine.trace.Trace;
import com.algoviz.engine.trace.TraceCodec;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.algoviz.engine.TraceAsserts.assertContract;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ReverseListTraceTest {

    private static final int[] INPUT = {5, 2, 9, 1, 5};
    private static final String GOLDEN = "src/test/resources/traces/reverse_list.golden.json";

    private static Trace trace() {
        return new ReverseList(INPUT).produce();
    }

    /** 行为：沿最终箭头走出来的链，值必须是反序的。 */
    @Test
    void finalChainIsReversed() {
        ListView last = trace().steps().get(trace().steps().size() - 1).list();
        // 新头 = 没有被任何 next 指向的节点
        Set<String> referenced = last.nodes().stream()
                .map(ListView.NodeBox::nextId)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());
        List<ListView.NodeBox> byId = last.nodes();
        Map<String, ListView.NodeBox> nodeMap = byId.stream()
                .collect(Collectors.toMap(ListView.NodeBox::nodeId, Function.identity()));
        String head = byId.stream().map(ListView.NodeBox::nodeId)
                .filter(id -> !referenced.contains(id))
                .findFirst().orElseThrow();

        List<Integer> walked = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        String cur = head;
        while (cur != null && seen.add(cur)) {
            ListView.NodeBox nb = nodeMap.get(cur);
            walked.add(nb.val());
            cur = nb.nextId();
        }
        assertEquals(List.of(5, 1, 9, 2, 5), walked);
    }

    @Test
    void contractAndInvariantHold() {
        assertContract(trace());
    }

    /** 深拷贝铁律：产出之后再改输入，不得影响已产出的 Trace。 */
    @Test
    void snapshotsAreIndependentOfInput() {
        int[] mutable = {3, 1, 2};
        ReverseList algo = new ReverseList(mutable);
        Trace t = algo.produce();
        mutable[0] = 999;
        ListView last = t.steps().get(t.steps().size() - 1).list();
        List<Integer> vals = last.nodes().stream().map(ListView.NodeBox::val).toList();
        assertEquals(List.of(3, 1, 2), vals, "快照被外部修改污染");
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
