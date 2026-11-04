package com.algoviz.engine.string;

import com.algoviz.engine.OrderedMaps;
import com.algoviz.engine.Algorithm;
import com.algoviz.engine.Intent;
import com.algoviz.engine.Layer;
import com.algoviz.engine.Complexity;
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

/** KMP 字符串匹配：前缀表 + 双串移动——不回头就是 KMP 的全部智慧。 */
public final class KmpDemo implements Algorithm {

    private static final String SCHEMA = "1";
    private static final String INVARIANT =
            "失配时 j = next[j]——利用已匹配的前缀信息跳过不必要的比较，主串指针永不回退";

    private static final String TEXT = "ABABDABACDABABCABAB";
    private static final String PATTERN = "ABABCABAB";

    private final List<Step> steps = new ArrayList<>();
    private final int[] next = new int[PATTERN.length()];
    private int ti, pi;

    @Override public String id() { return "string.kmp"; }
    @Override public String title() { return "KMP"; }
    @Override public String codeResource() { return "/code/KmpDemo.java"; }
    @Override public String docResource() { return "/markdown/kmp.md"; }

    @Override
    public Trace produce() {
        buildNext();
        search();
        return new Trace(SCHEMA, meta(), steps);
    }

    private void buildNext() {
        next[0] = 0;
        int len = 0, i = 1;
        while (i < PATTERN.length()) {
            if (PATTERN.charAt(i) == PATTERN.charAt(len)) {
                len++;
                next[i] = len;
                emit(5, List.of(VizEvent.of(VizEvent.EventType.SET,
                        "next[" + i + "] = " + len + "（前缀匹配）", "next:" + i)),
                        "前缀表构建：P[" + i + "] 与 P[" + len + "] 相等 → 继续延伸");
                i++;
            } else if (len > 0) {
                emit(6, List.of(VizEvent.of(VizEvent.EventType.COMPARE,
                        "失配 → len = next[" + (len - 1) + "] = " + next[len - 1], "next:" + i)),
                        "失配 → 回退到更短的前缀");
                len = next[len - 1];
            } else {
                next[i] = 0;
                emit(5, List.of(VizEvent.of(VizEvent.EventType.SET,
                        "next[" + i + "] = 0", "next:" + i)),
                        "无公共前缀 → 长度归零");
                i++;
            }
        }
    }

    private void search() {
        ti = 0; pi = 0;
        while (ti < TEXT.length()) {
            if (PATTERN.charAt(pi) == TEXT.charAt(ti)) {
                ti++; pi++;
                if (pi == PATTERN.length()) {
                    emit(5, List.of(VizEvent.of(VizEvent.EventType.MARK_SORTED,
                            "匹配！起始位置 " + (ti - pi), "match")),
                            "完整匹配 → 返回起始下标");
                    emit(5, List.of(VizEvent.of(VizEvent.EventType.RETURN,
                            "匹配位置 " + (ti - PATTERN.length()))), "匹配完成");
                    if (ti < TEXT.length()) { pi = 0; }
                    else break;
                }
            } else if (pi > 0) {
                emit(6, List.of(VizEvent.of(VizEvent.EventType.COMPARE,
                        "失配 → j = next[" + (pi - 1) + "] = " + next[pi - 1] + "（主串不回退！）")),
                        "失配 → 利用前缀表跳过已知的匹配部分——主串指针 ti 永不回退");
                pi = next[pi - 1];
            } else {
                emit(6, List.of(VizEvent.of(VizEvent.EventType.COMPARE,
                        "pi = 0 → 主串前进")),
                        "模式串首字符就不匹配 → 主串前进");
                ti++;
            }
        }
        emit(10, List.of(VizEvent.of(VizEvent.EventType.RETURN,
                "搜索完成 → O(n+m)")),
                "KMP 完成：主串 n 步 + 模式串 m 步 → 线性时间");
    }

    private void emit(int line, List<VizEvent> events, String why) {
        StackFrame frame = new StackFrame("kmp",
                OrderedMaps.of("text", Value.prim(TEXT),
                       "pattern", Value.prim(PATTERN),
                       "ti", Value.prim(ti),
                       "pi", Value.prim(pi)), null);
        Map<String, Layer> layers = new LinkedHashMap<>();
        layers.put("mem.stack", new StackLayer(List.of(frame)));
        layers.put("mem.heap", new HeapLayer(snapshotHeap()));
        layers.put("ds.array", snapshotArray());
        steps.add(new Step(steps.size(), Loc.line(line, why),
                Collections.unmodifiableMap(layers), List.copyOf(events), new Intent(why, INVARIANT)));
    }

    private Heap snapshotHeap() {
        Map<String, HeapObject> objs = new LinkedHashMap<>();
        List<Value> nextElems = new ArrayList<>();
        for (int v : next) nextElems.add(Value.prim(v));
        Map<String, Value> nextFields = new LinkedHashMap<>();
        for (int i = 0; i < next.length; i++) {
            nextFields.put("next[" + i + "]", Value.prim(next[i]));
        }
        objs.put("@1", new HeapObject("@1", "next", "int[" + next.length + "]",
                new LinkedHashMap<>(), List.copyOf(nextElems)));
        return new Heap(Collections.unmodifiableMap(objs));
    }

    private ArrayView snapshotArray() {
        List<Integer> vals = new ArrayList<>();
        for (int v : next) vals.add(v);
        return new ArrayView("@1", "next", List.copyOf(vals),
                pi < next.length ? pi : null, null, null, null, next.length, -1);
    }

    private Trace.TraceMeta meta() {
        return new Trace.TraceMeta(id(), title(), codeResource(),
                TEXT + "/" + PATTERN, PATTERN.length());
    }

    @Override
    public java.util.List<String> dataLines() {
        return java.util.List.of(
                "String text = \"" + TEXT + "\";",
                "String pattern = \"" + PATTERN + "\";");
    }

    @Override
    public Complexity complexity() {
        return new Complexity("O(n+m)——主串不回退", "O(m) 前缀表",
                "next 表保证失配时主串指针 ti 永不回退 → 线性时间。对比暴力 O(nm)");
    }
}
