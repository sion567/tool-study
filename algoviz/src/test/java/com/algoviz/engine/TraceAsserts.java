package com.algoviz.engine;

import com.algoviz.engine.ds.ArrayView;
import com.algoviz.engine.ds.GraphView;
import com.algoviz.engine.ds.ListView;
import com.algoviz.engine.ds.SeqView;
import com.algoviz.engine.ds.TreeView;
import com.algoviz.engine.math.MathView;
import com.algoviz.engine.math.PlotView;
import com.algoviz.engine.mem.HeapObject;
import com.algoviz.engine.trace.Trace;
import org.junit.jupiter.api.function.ThrowingConsumer;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 所有算法 Trace 的共享断言：底层合约 + 分区/链表不变量（蓝图 §2.4 自动校验）。 */
final class TraceAsserts {

    private TraceAsserts() {
    }

    /** 底层合约：每步带齐图层 + 意图；已就位区间全程有序；ds.list 与 mem.heap 同源一致。 */
    static void assertContract(Trace t) {
        assertFalse(t.steps().isEmpty(), "steps 为空");
        for (Step s : t.steps()) {
            assertNotNull(s.stack(), "mem.stack 缺失 @step " + s.index());
            assertNotNull(s.heap(), "mem.heap 缺失 @step " + s.index());
            assertNotNull(s.loc(), "loc 缺失 @step " + s.index());
            assertNotNull(s.intent(), "intent 缺失 @step " + s.index());
            assertFalse(s.intent().invariant().isBlank(), "不变量为空 @step " + s.index());
            assertTrue(s.array() != null || s.list() != null || s.tree() != null
                            || s.graph() != null || s.seq() != null || s.math() != null
                            || s.bitmap() != null || s.plot() != null,
                    "缺少 ds/seq/math 图层 @step " + s.index());

            ArrayView av = s.array();
            if (av != null) {
                List<Integer> v = av.values();
                int n = v.size();
                // 尾部已就位区（冒泡）
                for (int i = av.sortedFrom(); i < n - 1; i++) {
                    assertTrue(v.get(i) <= v.get(i + 1),
                            "尾部区破坏 @step " + s.index() + " i=" + i + " values=" + v);
                }
                // 头部已就位区（选择/插入）
                for (int i = 0; i < av.sortedTo(); i++) {
                    assertTrue(v.get(i) <= v.get(i + 1),
                            "头部区破坏 @step " + s.index() + " i=" + i + " values=" + v);
                }
            }

            ListView lv = s.list();
            if (lv != null) {
                Set<String> ids = new HashSet<>();
                for (ListView.NodeBox nb : lv.nodes()) {
                    assertTrue(ids.add(nb.nodeId()), "节点 id 重复 @step " + s.index());
                }
                for (ListView.NodeBox nb : lv.nodes()) {
                    assertTrue(nb.nextId() == null || ids.contains(nb.nextId()),
                            "next 指向不存在的节点 @step " + s.index() + " " + nb.nodeId());
                    HeapObject o = s.heap().heap().objects().get(nb.nodeId());
                    assertNotNull(o, "堆缺节点 " + nb.nodeId() + " @step " + s.index());
                    assertEquals(String.valueOf(nb.val()), o.fields().get("val").display(),
                            "val 与堆不一致 @step " + s.index() + " " + nb.nodeId());
                    String heapNext = o.fields().get("next").display();
                    assertEquals(nb.nextId() == null ? "null" : nb.nextId(), heapNext,
                            "ds.list 与 mem.heap 的 next 不一致 @step " + s.index() + " " + nb.nodeId());
                }
            }

            TreeView tv = s.tree();
            if (tv != null) {
                Set<String> ids = new HashSet<>();
                for (TreeView.TreeNodeBox nb : tv.nodes()) {
                    assertTrue(ids.add(nb.nodeId()), "节点 id 重复 @step " + s.index());
                }
                for (TreeView.TreeNodeBox nb : tv.nodes()) {
                    assertTrue(nb.leftId() == null || ids.contains(nb.leftId()),
                            "left 指向不存在的节点 @step " + s.index() + " " + nb.nodeId());
                    assertTrue(nb.rightId() == null || ids.contains(nb.rightId()),
                            "right 指向不存在的节点 @step " + s.index() + " " + nb.nodeId());
                    assertTrue(Set.of("unvisited", "onstack", "visited").contains(nb.state()),
                            "非法状态 " + nb.state() + " @step " + s.index());
                    assertTrue(nb.visitOrder() == null || nb.visitOrder() >= 1,
                            "非法访问序号 @step " + s.index());
                    HeapObject o = s.heap().heap().objects().get(nb.nodeId());
                    assertNotNull(o, "堆缺节点 " + nb.nodeId() + " @step " + s.index());
                    assertEquals(String.valueOf(nb.val()), o.fields().get("val").display(),
                            "val 与堆不一致 @step " + s.index() + " " + nb.nodeId());
                    assertEquals(nb.leftId() == null ? "null" : nb.leftId(),
                            o.fields().get("left").display(),
                            "ds.tree 与 mem.heap 的 left 不一致 @step " + s.index() + " " + nb.nodeId());
                    assertEquals(nb.rightId() == null ? "null" : nb.rightId(),
                            o.fields().get("right").display(),
                            "ds.tree 与 mem.heap 的 right 不一致 @step " + s.index() + " " + nb.nodeId());
                }
                for (String stackId : tv.callStackIds()) {
                    assertTrue(ids.contains(stackId),
                            "调用栈指向不存在的节点 @step " + s.index());
                }
            }

            GraphView gv = s.graph();
            if (gv != null) {
                Set<String> ids = new HashSet<>();
                for (GraphView.GraphVertex v : gv.vertices()) {
                    assertTrue(ids.add(v.nodeId()), "顶点 id 重复 @step " + s.index());
                }
                for (GraphView.GraphEdge e : gv.edges()) {
                    assertTrue(ids.contains(e.fromId()) && ids.contains(e.toId()),
                            "边指向不存在的顶点 @step " + s.index());
                    assertTrue(e.weight() >= 0, "负权边：Dijkstra 不适用 @step " + s.index());
                }
                for (String settled : gv.settledIds()) {
                    assertTrue(ids.contains(settled), "出圈集含未知顶点 @step " + s.index());
                }
                if (gv.relaxFrom() != null) {
                    assertTrue(ids.contains(gv.relaxFrom()) && ids.contains(gv.relaxTo()),
                            "松弛边指向不存在的顶点 @step " + s.index());
                }
            }

            SeqView sv = s.seq();
            if (sv != null) {
                int p = sv.participants().size();
                assertTrue(p >= 2, "泳道少于 2 个 @step " + s.index());
                Set<String> pids = new HashSet<>();
                for (SeqView.Participant pt : sv.participants()) {
                    assertTrue(pids.add(pt.id()), "泳道 id 重复 @step " + s.index());
                }
                assertTrue(sv.activeIndex() >= -1 && sv.activeIndex() < sv.messages().size(),
                        "activeIndex 越界 @step " + s.index());
                for (SeqView.Message m : sv.messages()) {
                    assertTrue(m.from() >= 0 && m.from() < p && m.to() >= 0 && m.to() < p,
                            "消息指向不存在的泳道 @step " + s.index());
                    assertTrue(Set.of("CALL", "RETURN").contains(m.kind()),
                            "非法消息类型 @step " + s.index());
                }
            }

            MathView mv = s.math();
            if (mv != null) {
                assertFalse(mv.grids().isEmpty(), "math 图层无网格 @step " + s.index());
                for (MathView.Grid g : mv.grids()) {
                    assertEquals(g.rows() * g.cols(), g.values().size(),
                            "网格 " + g.id() + " 不是矩形 @step " + s.index());
                    for (int cell : g.activeCells()) {
                        assertTrue(cell >= 0 && cell < g.values().size(),
                                "active 越界 @step " + s.index() + " " + g.id());
                    }
                    for (int cell : g.doneCells()) {
                        assertTrue(cell >= 0 && cell < g.values().size(),
                                "done 越界 @step " + s.index() + " " + g.id());
                    }
                }
            }

            if (s.bitmap() != null) {
                var bm = s.bitmap();
                assertTrue(bm.bits() > 0, "位图大小非法 @step " + s.index());
                assertTrue(bm.readIndex() >= -1 && bm.readIndex() <= bm.data().size(),
                        "readIndex 越界 @step " + s.index());
                for (int b : bm.ones()) {
                    assertTrue(b >= 0 && b < bm.bits(), "置位越界 @step " + s.index());
                }
            }
        }
    }

    /** 最后一步的数组必须等于期望的有序结果。 */
    static void assertFinalSorted(Trace t, List<Integer> expected) {
        ArrayView last = t.steps().get(t.steps().size() - 1).array();
        assertEquals(expected, last.values());
    }
}
