package com.algoviz.engine.math;

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
 * 位运算技巧演示：& | ^ ~ << >> 逐位展示——同一个数对不同操作的结果。
 * 画面：二进制位逐帧高亮变化的部分，十进制结果显示在旁边。
 */
public final class BitOpsDemo implements Algorithm {

    private static final String SCHEMA = "1";
    private static final String INVARIANT =
            "位运算 = 对二进制每一位独立操作—— & 同1才1，| 有1就1，^ 不同才1";

    private static final int A = 0b1100;   // 12
    private static final int B = 0b1010;   // 10

    private final List<Step> steps = new ArrayList<>();

    @Override public String id() { return "math.bitops"; }
    @Override public String title() { return "位运算技巧"; }
    @Override public String codeResource() { return "/code/BitOpsDemo.java"; }
    @Override public String docResource() { return "/markdown/bit-ops.md"; }

    @Override
    public Trace produce() {
        String binA = Integer.toBinaryString(A);
        String binB = Integer.toBinaryString(B);
        emit(2, List.of(VizEvent.of(VizEvent.EventType.INIT,
                "A = " + binA + " (" + A + "),  B = " + binB + " (" + B + ")")),
                "先把两个数转成二进制——位运算就是逐位操作");
        bitOp("AND &", A & B, binA, binB, "&", "同 1 才 1（用来提取/掩码）");
        bitOp("OR |", A | B, binA, binB, "|", "有 1 就 1（用来合并/设位）");
        bitOp("XOR ^", A ^ B, binA, binB, "^", "不同才 1（用来翻转/找不同）");
        emit(6, List.of(VizEvent.of(VizEvent.EventType.SET,
                "<< 1: " + A + " << 1 = " + (A << 1) + "（×2）\n>> 1: " + A + " >> 1 = " + (A >> 1) + "（÷2）")),
                "左移 = ×2，右移 = ÷2——比乘除法快（CPU 原生指令）");
        emit(8, List.of(VizEvent.of(VizEvent.EventType.RETURN,
                "技巧：n & (n-1) 消最低位 1 → 判断 2 的幂；n ^ n = 0 → 找只出现一次的数")),
                "位运算的本质：把布尔逻辑并行应用到每一位");
        return new Trace(SCHEMA, meta(), steps);
    }

    private void bitOp(String name, int result, String ba, String bb, String op, String explain) {
        String binR = String.format("%4s", Integer.toBinaryString(result)).replace(' ', '0');
        emit(3, List.of(VizEvent.of(VizEvent.EventType.SET,
                name + " → " + binR + " (" + result + ")", binR)),
                explain);
    }

    private void emit(int line, List<VizEvent> events, String why) {
        StackFrame frame = new StackFrame("bitOps",
                OrderedMaps.of("A", Value.prim(A), "B", Value.prim(B)), null);
        Map<String, Layer> layers = new LinkedHashMap<>();
        layers.put("mem.stack", new StackLayer(List.of(frame)));
        layers.put("mem.heap", new HeapLayer(snapshotHeap()));
        layers.put("ds.array", snapshotArray());
        steps.add(new Step(steps.size(), Loc.line(line, why),
                Collections.unmodifiableMap(layers), List.copyOf(events), new Intent(why, INVARIANT)));
    }

    private Heap snapshotHeap() {
        Map<String, HeapObject> objs = new LinkedHashMap<>();
        Map<String, Value> fa = new LinkedHashMap<>();
        fa.put("A", Value.prim(A));
        fa.put("B", Value.prim(B));
        fa.put("A&B", Value.prim(A & B));
        fa.put("A|B", Value.prim(A | B));
        fa.put("A^B", Value.prim(A ^ B));
        objs.put("@1", new HeapObject("@1", "bits", "BitOps",
                Collections.unmodifiableMap(fa), List.of()));
        return new Heap(Collections.unmodifiableMap(objs));
    }

    private ArrayView snapshotArray() {
        List<Integer> vals = new ArrayList<>();
        String binA = String.format("%4s", Integer.toBinaryString(A)).replace(' ', '0');
        for (char c : binA.toCharArray()) vals.add(c - '0');
        return new ArrayView("@1", "A_bits", List.copyOf(vals),
                null, null, null, null, vals.size(), -1);
    }

    private Trace.TraceMeta meta() {
        return new Trace.TraceMeta(id(), title(), codeResource(), A + "," + B, 2);
    }

    @Override
    public java.util.List<String> dataLines() {
        return java.util.List.of("int a = " + A + ";", "int b = " + B + ";");
    }

    @Override
    public Complexity complexity() {
        return new Complexity("O(1) 每个操作", "O(1) 不需要额外空间",
                "位运算直接操作 CPU 寄存器的二进制位——每条指令 O(1)，比算术运算更快");
    }
}
