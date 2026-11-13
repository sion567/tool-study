package com.algoviz.engine.math;

import com.algoviz.engine.Algorithm;
import com.algoviz.engine.Complexity;
import com.algoviz.engine.Intent;
import com.algoviz.engine.Layer;
import com.algoviz.engine.Loc;
import com.algoviz.engine.OrderedMaps;
import com.algoviz.engine.Step;
import com.algoviz.engine.Value;
import com.algoviz.engine.VizEvent;
import com.algoviz.engine.math.MathView;
import com.algoviz.engine.mem.Heap;
import com.algoviz.engine.mem.HeapLayer;
import com.algoviz.engine.mem.HeapObject;
import com.algoviz.engine.mem.StackFrame;
import com.algoviz.engine.mem.StackLayer;
import com.algoviz.engine.trace.Trace;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 数与计算·大数竖式运算：123456 + 654321 与 123456 × 654321 的逐位演算现场。
 * 加法：进位传播（从个位到十万位，进位链高亮）；乘法：部分积逐行错位累加——朴素 O(n²)。
 * 对照 math.karatsuba：分治把 O(n²) 降为 O(n^1.585)——同一问题的暴力与聪明。
 * 画面（math.matrix 复用）：网格一 = 加法竖式（进位/a/b/和 4 行），网格二 = 乘法部分积 6 行。
 */
public final class BigIntDemo implements Algorithm {

    private static final String SCHEMA = "1";
    private static final String INVARIANT =
            "竖式 = 位值制的逐位传播：加法进位链从低位涌向高位、乘法部分积错位累加——朴素 O(n²)；"
                    + "Karatsuba 用分治降到 O(n^1.585)——同一问题的暴力与聪明";

    private static final int A = 123456;
    private static final int B = 654321;

    private final List<Step> steps = new ArrayList<>();
    private final int[] digitsA = new int[6];
    private final int[] digitsB = new int[6];
    private final int[] sumD = new int[7];
    private int curPos = -1;
    private int partRow = -1;

    @Override public String id() { return "math.bigint"; }
    @Override public String title() { return "大数竖式运算"; }
    @Override public String codeResource() { return "/code/BigIntDemo.java"; }
    @Override public String docResource() { return "/markdown/bigint.md"; }

    @Override
    public Trace produce() {
        steps.clear();
        for (int i = 0; i < 6; i++) {
            digitsA[i] = (A / pow10(5 - i)) % 10;
            digitsB[i] = (B / pow10(5 - i)) % 10;
        }
        curPos = -1;
        partRow = -1;
        emit(6, List.of(VizEvent.of(VizEvent.EventType.INIT,
                "a=123456、b=654321：先加法（进位传播），再乘法（部分积错位累加）——竖式是位值制的现场")),
                "大数运算的本质：'位'不是数字是容器——每一位的进位都是容器之间的传递");
        // ---- 加法：逐位 + 进位链 ----
        int carry = 0;
        for (int pos = 5; pos >= 0; pos--) {
            curPos = pos;
            int sum = digitsA[pos] + digitsB[pos] + carry;
            sumD[pos + 1] = sum % 10;
            carry = sum / 10;
            emit(11, List.of(VizEvent.of(VizEvent.EventType.SET,
                    String.format("位 %d：%d+%d+%d(进位)=%d → 写 %d，进位 %d",
                            pos, digitsA[pos], digitsB[pos], pos < 5 ? 0 : 0, sum, sum % 10, carry))),
                    carry > 0 ? "进位传播：这一位的'满 10 进 1'会连锁影响更高位——竖式的灵魂"
                            : "无进位：两位独立相加——进位链断开时各位互不干扰");
        }
        sumD[0] = carry;
        curPos = -1;
        emit(15, List.of(VizEvent.of(VizEvent.EventType.MARK_SORTED,
                String.format("加法完成：%d + %d = %d（最高位进位 %d）", A, B, A + B, carry))),
                "加法 O(n)：从个位扫到高位一遍——进位是唯一跨位的依赖");
        // ---- 乘法：部分积逐行 ----
        long prod = (long) A * B;
        for (int i = 5; i >= 0; i--) {
            partRow = 5 - i;
            long partial = (long) B * digitsA[i];
            emit(19, List.of(VizEvent.of(VizEvent.EventType.SET,
                    String.format("部分积 %d：%d × %d（十位权 %d）= %d",
                            partRow + 1, B, digitsA[i], pow10(5 - i), partial))),
                    "每位乘出一个部分积、错位累加——位权决定了'错位'的层数");
        }
        partRow = -1;
        emit(23, List.of(VizEvent.of(VizEvent.EventType.MARK_SORTED,
                String.format("乘法完成：%d × %d = %d（6 行部分积逐行累加）", A, B, prod))),
                "朴素竖式 O(n²)：n 位乘 n 位要 n² 次位乘——Karatsuba 的分治从这里起跑");
        emit(26, List.of(VizEvent.of(VizEvent.EventType.RETURN,
                "对照 math.karatsuba：1234×5678 的分治版——同一个问题的两种'聪明程度'")),
                "计算机里的大数运算（RSA 的 2048 位乘法）正是竖式的位传播思想，基数从 10 换成 2³²");
        return new Trace(SCHEMA, meta(), steps);
    }

    private static int pow10(int e) {
        return (int) Math.pow(10, e);
    }

    private void emit(int line, List<VizEvent> events, String why) {
        StackFrame frame = new StackFrame("bigint",
                OrderedMaps.of("位", Value.prim(curPos >= 0 ? String.valueOf(curPos) : "-"),
                        "加法和", Value.prim(String.valueOf(A + B)),
                        "乘积", Value.prim(String.valueOf((long) A * B))), null);
        Map<String, Layer> layers = new LinkedHashMap<>();
        layers.put("mem.stack", new StackLayer(List.of(frame)));
        layers.put("mem.heap", new HeapLayer(snapshotHeap()));
        layers.put("math.matrix", snapshotMath());
        steps.add(new Step(steps.size(), Loc.line(line, why),
                Collections.unmodifiableMap(layers), List.copyOf(events), new Intent(why, INVARIANT)));
    }

    private Heap snapshotHeap() {
        Map<String, Value> fields = new LinkedHashMap<>();
        fields.put("a / b", Value.prim(A + " / " + B));
        fields.put("加法和", Value.prim(String.valueOf(A + B)));
        fields.put("乘积", Value.prim(String.valueOf((long) A * B)));
        Map<String, HeapObject> objs = new LinkedHashMap<>();
        objs.put("@1", new HeapObject("@1", "big", "BigIntState",
                Collections.unmodifiableMap(fields), List.of()));
        return new Heap(Collections.unmodifiableMap(objs));
    }

    private MathView snapshotMath() {
        // 网格一：加法竖式（进位/a/b/和 4 行 × 6 位）
        List<String> addVals = new ArrayList<>();
        Set<Integer> addActive = new LinkedHashSet<>();
        int carry = 0;
        int[] carries = new int[7];
        for (int pos = 5; pos >= 0; pos--) {
            int sum = digitsA[pos] + digitsB[pos] + carry;
            carries[pos] = carry;
            sumD[pos + 1] = sum % 10;
            carry = sum / 10;
        }
        carries[6] = carry;
        for (int c = 0; c < 6; c++) {
            addVals.add(c == curPos ? "↑" : (carries[c + 1] != 0 ? "1" : "0"));   // 进位行
        }
        for (int c = 0; c < 6; c++) {
            addVals.add(String.valueOf(digitsA[c]));
        }
        for (int c = 0; c < 6; c++) {
            addVals.add(String.valueOf(digitsB[c]));
        }
        for (int c = 0; c < 6; c++) {
            addVals.add(String.valueOf(sumD[c + 1]));
        }
        if (curPos >= 0) {
            addActive.add(curPos);
            addActive.add(6 + curPos);
            addActive.add(12 + curPos);
            addActive.add(18 + curPos);
        }
        MathView.Grid addGrid = new MathView.Grid("add", "加法竖式（进位/a/b/和；黄列 = 当前位）",
                4, 6, addVals, null, null, addActive, Set.of());
        // 网格二：乘法部分积（6 行 × 7 列 右对齐错位）
        List<String> mulVals = new ArrayList<>();
        Set<Integer> mulActive = new LinkedHashSet<>();
        for (int r = 0; r < 6; r++) {
            long partial = (long) B * digitsA[5 - r];
            String ps = String.valueOf(partial);
            for (int c = 0; c < 7; c++) {
                int idx = ps.length() - (7 - c);   // 右对齐错位
                mulVals.add(idx >= 0 && idx < ps.length() ? String.valueOf(ps.charAt(idx)) : "");
            }
        }
        if (partRow >= 0 && partRow < 6) {
            for (int c = 0; c < 7; c++) {
                mulActive.add(partRow * 7 + c);
            }
        }
        MathView.Grid mulGrid = new MathView.Grid("mul", "乘法部分积（行 = a 的位 × b，错位累加）",
                6, 7, mulVals, null, null, mulActive, Set.of());
        return new MathView("大数竖式", List.of(addGrid, mulGrid));
    }

    private Trace.TraceMeta meta() {
        return new Trace.TraceMeta(id(), title(), codeResource(),
                "123456 ↔ 654321", 26);
    }

    @Override
    public java.util.List<String> dataLines() {
        return java.util.List.of("int a = 123456, b = 654321;   //@DATA 操作数");
    }

    @Override
    public Complexity complexity() {
        return new Complexity("加法 O(n) / 乘法 O(n²)", "O(n) 位数存储",
                "竖式 = 位值制的逐位传播；RSA 的 2048 位乘法把基数换为 2³²——"
                        + "计算机的'竖式'每步处理 9 位十进制（32 位字）");
    }
}
