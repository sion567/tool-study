package com.algoviz.engine.array;

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
import com.algoviz.engine.mem.HeapObject;
import com.algoviz.engine.mem.StackFrame;
import com.algoviz.engine.mem.StackLayer;
import com.algoviz.engine.trace.Trace;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 滑动窗口：无重复字符的最长子串（简化为整数数组无重复的最长子数组）。
 * 右指针扩张，遇重复左指针收缩——窗口内始终无重复。
 */
public final class SlidingWindow implements Algorithm {

    private static final String SCHEMA = "1";
    private static final String INVARIANT =
            "窗口 [lo,hi] 内永远无重复——hi 扩张遇冲突时 lo 收缩到冲突位之后";

    /** 与 /code/SlidingWindow.java 的行号对齐 */
    private static final int L_LO = 3;
    private static final int L_LOOP = 4;
    private static final int L_EXPAND = 5;
    private static final int L_CONFLICT = 6;
    private static final int L_SHRINK = 7;
    private static final int L_RECORD = 8;
    private static final int L_END = 10;

    private final int[] DATA;

    public SlidingWindow() {
        this(new int[]{2, 1, 3, 1, 4, 4, 2, 5});
    }

    public SlidingWindow(int[] data) {
        this.DATA = data.clone();
    }
    private final List<Step> steps = new ArrayList<>();
    private int lo, hi;
    private int bestLen, bestLo, bestHi;

    @Override public String id() { return "array.slidingwindow"; }
    @Override public String title() { return "滑动窗口"; }
    @Override public String codeResource() { return "/code/SlidingWindow.java"; }
    @Override public String docResource() { return "/markdown/sliding-window.md"; }

    @Override
    public Trace produce() {
        lo = 0;
        bestLen = 0;
        bestLo = bestHi = -1;
        emit(L_LO, List.of(VizEvent.of(VizEvent.EventType.INIT, "lo = 0，窗口起点")),
                "lo 是窗口左边界——hi 扩张，lo 负责收缩");
        var seen = new java.util.HashSet<Integer>();
        for (hi = 0; hi < DATA.length; hi++) {
            int v = DATA[hi];
            emit(L_LOOP, List.of(VizEvent.of(VizEvent.EventType.COMPARE,
                    "hi 扩张到 " + hi + "，读入 a[" + hi + "]=" + v, "a:" + hi)),
                    "右指针扩张：把 a[hi] 尝试纳入窗口");
            if (seen.contains(v)) {
                emit(L_CONFLICT, List.of(VizEvent.of(VizEvent.EventType.COMPARE,
                        v + " 已在窗口内 → 冲突！lo 开始收缩", "a:" + hi)),
                        "重复出现：窗口不合法，lo 必须越过上一个 " + v);
                while (DATA[lo] != v) {
                    seen.remove(DATA[lo]);
                    emit(L_SHRINK, List.of(VizEvent.of(VizEvent.EventType.SET,
                            "移出 a[" + lo + "]=" + DATA[lo] + "，lo → " + (lo + 1), "a:" + lo)),
                            "收缩：把左边的元素逐个移出窗口");
                    lo++;
                }
                seen.remove(DATA[lo]);
                lo++;   // 越过冲突位
                emit(L_SHRINK, List.of(VizEvent.of(VizEvent.EventType.SET,
                        "lo 越过冲突位 → " + lo, "a:" + lo)),
                        "lo 停在冲突元素的下一位");
            }
            seen.add(v);
            int len = hi - lo + 1;
            if (len > bestLen) {
                bestLen = len;
                bestLo = lo;
                bestHi = hi;
            }
            emit(L_RECORD, List.of(VizEvent.of(VizEvent.EventType.SET,
                    "窗口 [" + lo + "," + hi + "] 长度 " + len + (len == bestLen ? " ← 新纪录！" : ""))),
                    "窗口合法：记录当前长度，与最佳对比");
        }
        emit(L_END, List.of(VizEvent.of(VizEvent.EventType.RETURN,
                "最长无重复窗口 = [" + bestLo + "," + bestHi + "]，长度 " + bestLen)),
                "O(n)：lo 和 hi 各最多走 n 步——两根指针从不回头");
        return new Trace(SCHEMA, meta(), steps);
    }

    private void emit(int line, List<VizEvent> events, String why) {
        StackFrame frame = new StackFrame("window",
                OrderedMaps.of("a", Value.ref("@1"),
                       "lo", Value.prim(lo), "hi", Value.prim(hi),
                       "best", Value.prim(bestLen)), null);
        Map<String, Layer> layers = new LinkedHashMap<>();
        layers.put("mem.stack", new StackLayer(List.of(frame)));
        layers.put("mem.heap", new HeapLayer(snapshotHeap()));
        layers.put("ds.array", snapshotArray());
        steps.add(new Step(steps.size(), Loc.line(line, why),
                Collections.unmodifiableMap(layers), List.copyOf(events), new Intent(why, INVARIANT)));
    }

    private Heap snapshotHeap() {
        List<Value> elems = new ArrayList<>();
        for (int v : DATA) elems.add(Value.prim(v));
        HeapObject arr = new HeapObject("@1", "a", "int[" + DATA.length + "]",
                new LinkedHashMap<>(), List.copyOf(elems));
        Map<String, HeapObject> objs = new LinkedHashMap<>();
        objs.put("@1", arr);
        return new Heap(Collections.unmodifiableMap(objs));
    }

    private ArrayView snapshotArray() {
        List<Integer> vals = new ArrayList<>();
        for (int v : DATA) vals.add(v);
        return new ArrayView("@1", "a", List.copyOf(vals),
                lo <= hi ? lo : null, hi, null, null, DATA.length, -1);
    }

    private Trace.TraceMeta meta() {
        StringBuilder sb = new StringBuilder();
        for (int k = 0; k < DATA.length; k++) {
            if (k > 0) sb.append(',');
            sb.append(DATA[k]);
        }
        return new Trace.TraceMeta(id(), title(), codeResource(), sb.toString(), DATA.length);
    }

    @Override
    public java.util.List<String> dataLines() {
        return java.util.List.of(Algorithm.joinArray(DATA));
    }

    @Override
    public Complexity complexity() {
        return new Complexity("O(n) 两指针各走一遍", "O(k) 窗口内元素集合",
                "hi 走 n 步，lo 最多走 n 步（从不回头）→ 2n 次操作。对比暴力枚举所有子串 O(n²)");
    }
}
