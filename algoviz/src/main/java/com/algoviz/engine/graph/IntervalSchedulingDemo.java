package com.algoviz.engine.graph;

import com.algoviz.engine.OrderedMaps;
import com.algoviz.engine.Algorithm;
import com.algoviz.engine.Complexity;
import com.algoviz.engine.Intent;
import com.algoviz.engine.Layer;
import com.algoviz.engine.Loc;
import com.algoviz.engine.Step;
import com.algoviz.engine.Value;
import com.algoviz.engine.VizEvent;
import com.algoviz.engine.ds.ArrayView;
import com.algoviz.engine.mem.Heap;
import com.algoviz.engine.mem.HeapLayer;
import com.algoviz.engine.mem.HeapLayer;
import com.algoviz.engine.mem.HeapObject;
import com.algoviz.engine.mem.StackFrame;
import com.algoviz.engine.mem.StackLayer;
import com.algoviz.engine.trace.Trace;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 区间调度（贪心）：按结束时间排序，每轮选最早结束的不重叠区间。 */
public final class IntervalSchedulingDemo implements Algorithm {

    private static final String SCHEMA = "1";
    private static final String INVARIANT =
            "按结束时间排序后，每轮选最早结束的不重叠区间——贪心选择保证最多不重叠区间";

    private static final int[][] INTERVALS = {{1, 3}, {2, 5}, {4, 7}, {1, 8}, {5, 9}, {6, 10}};
    private static final String[] NAMES = {"A", "B", "C", "D", "E", "F"};

    private final List<Step> steps = new ArrayList<>();
    private final int[] selected = new int[INTERVALS.length];   // 1 = 选中
    private int curIdx = -1;
    private int lastEnd;

    @Override public String id() { return "greedy.interval"; }
    @Override public String title() { return "区间调度"; }
    @Override public String codeResource() { return "/code/IntervalSchedulingDemo.java"; }
    @Override public String docResource() { return "/markdown/interval-scheduling.md"; }

    @Override
    public Trace produce() {
        java.util.Arrays.fill(selected, 0);
        // 按结束时间排序
        Integer[] order = new Integer[INTERVALS.length];
        for (int i = 0; i < order.length; i++) order[i] = i;
        java.util.Arrays.sort(order, (a, b) -> INTERVALS[a][1] - INTERVALS[b][1]);
        emit(3, List.of(VizEvent.of(VizEvent.EventType.INIT,
                "按结束时间排序: " + sortStr(order))),
                "按结束时间排序——最早结束的先考虑，给后面的留更多空间");

        lastEnd = -1;
        for (int idx : order) {
            curIdx = idx;
            int start = INTERVALS[idx][0], end = INTERVALS[idx][1];
            if (start >= lastEnd) {
                selected[idx] = 1;
                lastEnd = end;
                emit(5, List.of(VizEvent.of(VizEvent.EventType.MARK_SORTED,
                        NAMES[idx] + " [" + start + "," + end + "] → 选中（不与上一个重叠）", "a:" + idx)),
                        "不重叠 → 选中（绿），lastEnd 更新为 " + end);
            } else {
                emit(6, List.of(VizEvent.of(VizEvent.EventType.COMPARE,
                        NAMES[idx] + " [" + start + "," + end + "] → 与上一个重叠，跳过", "a:" + idx)),
                        "起点 < 上一个的结束 → 重叠，跳过");
            }
        }
        curIdx = -1;
        int count = 0;
        StringBuilder names = new StringBuilder();
        for (int i = 0; i < INTERVALS.length; i++) {
            if (selected[i] == 1) {
                count++;
                if (names.length() > 0) names.append(", ");
                names.append(NAMES[i]);
            }
        }
        emit(8, List.of(VizEvent.of(VizEvent.EventType.RETURN,
                "最多不重叠区间 = " + count + " 个（" + names + "）")),
                "贪心最优：按结束时间排序保证选出的不重叠区间数最多");
        return new Trace(SCHEMA, meta(), steps);
    }

    private String sortStr(Integer[] order) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < order.length; i++) {
            if (i > 0) sb.append(", ");
            sb.append(NAMES[order[i]]).append("(").append(INTERVALS[order[i]][0])
              .append(",").append(INTERVALS[order[i]][1]).append(")");
        }
        return sb.append("]").toString();
    }

    private void emit(int line, List<VizEvent> events, String why) {
        StackFrame frame = new StackFrame("intervalScheduling",
                OrderedMaps.of("lastEnd", Value.prim(lastEnd)), null);
        Map<String, Layer> layers = new LinkedHashMap<>();
        layers.put("mem.stack", new StackLayer(List.of(frame)));
        layers.put("mem.heap", new com.algoviz.engine.mem.HeapLayer(snapshotHeap()));
        layers.put("ds.array", snapshotArray());
        steps.add(new Step(steps.size(), Loc.line(line, why),
                Collections.unmodifiableMap(layers), List.copyOf(events), new Intent(why, INVARIANT)));
    }

    private Heap snapshotHeap() {
        Map<String, HeapObject> objs = new LinkedHashMap<>();
        Map<String, Value> fields = new LinkedHashMap<>();
        fields.put("lastEnd", Value.prim(lastEnd));
        for (int i = 0; i < INTERVALS.length; i++) {
            fields.put(NAMES[i] + "_sel", Value.prim(selected[i]));
        }
        objs.put("@1", new HeapObject("@1", "state", "GreedyState",
                Collections.unmodifiableMap(fields), List.of()));
        return new Heap(Collections.unmodifiableMap(objs));
    }

    private ArrayView snapshotArray() {
        List<Integer> vals = new ArrayList<>();
        for (int[] iv : INTERVALS) { vals.add(iv[0]); vals.add(iv[1]); }
        return new ArrayView("@1", "intervals", List.copyOf(vals),
                curIdx >= 0 ? curIdx * 2 : null, curIdx >= 0 ? curIdx * 2 + 1 : null,
                null, null, INTERVALS.length * 2, -1);
    }

    private Trace.TraceMeta meta() {
        return new Trace.TraceMeta(id(), title(), codeResource(), "6 个区间", INTERVALS.length);
    }

    @Override
    public java.util.List<String> dataLines() {
        StringBuilder sb = new StringBuilder("int[][] intervals = {");
        for (int i = 0; i < INTERVALS.length; i++) {
            if (i > 0) sb.append(", ");
            sb.append("{").append(INTERVALS[i][0]).append(",").append(INTERVALS[i][1]).append("}");
        }
        return java.util.List.of(sb.append("};").toString());
    }

    @Override
    public Complexity complexity() {
        return new Complexity("O(n log n) 排序 + O(n) 贪心", "O(1) 只记录 lastEnd",
                "按结束时间排序后，每轮只看当前区间是否与上一个重叠——贪心选择保证最多不重叠区间");
    }
}
