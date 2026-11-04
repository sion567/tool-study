package com.algoviz.engine.math;

import com.algoviz.engine.OrderedMaps;
import com.algoviz.engine.Algorithm;
import com.algoviz.engine.Complexity;
import com.algoviz.engine.Intent;
import com.algoviz.engine.Layer;
import com.algoviz.engine.ds.ArrayView;
import com.algoviz.engine.Loc;
import com.algoviz.engine.Step;
import com.algoviz.engine.Value;
import com.algoviz.engine.VizEvent;
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
 * 快速幂：指数二进制分解——O(log n) 而不是 O(n) 的逐个乘。
 * 画面：指数的二进制位逐帧展示，base 逐帧平方。
 */
public final class FastPowerDemo implements Algorithm {

    private static final String SCHEMA = "1";
    private static final String INVARIANT =
            "指数每次折半，base 逐帧平方——log n 次乘法替代 n 次逐乘";

    private final int BASE, EXP;

    public FastPowerDemo() {
        this(3, 13);
    }

    public FastPowerDemo(int base, int exp) {
        this.BASE = base;
        this.EXP = exp;
    }

    private final List<Step> steps = new ArrayList<>();
    private long result;
    private long base;
    private int exp;
    private int stepIdx;

    @Override public String id() { return "math.fastpower"; }
    @Override public String title() { return "快速幂"; }
    @Override public String codeResource() { return "/code/FastPowerDemo.java"; }
    @Override public String docResource() { return "/markdown/fast-power.md"; }

    @Override
    public Trace produce() {
        result = 1;
        base = BASE;
        exp = EXP;
        stepIdx = 0;
        emit(3, List.of(VizEvent.of(VizEvent.EventType.INIT,
                BASE + "^" + EXP + "：result=1, base=" + base + ", exp=" + EXP)),
                "目标：" + BASE + "^" + EXP + "——逐个乘要 " + EXP + " 次，快速幂只要 log₂" + EXP + " ≈ " + (int) (Math.log(EXP) / Math.log(2)) + " 次");
        while (exp > 0) {
            if ((exp & 1) == 1) {
                result *= base;
                emit(5, List.of(VizEvent.of(VizEvent.EventType.SET,
                        "二进制末位 = 1 → result ×= " + base + " → " + result, "result")),
                        "最低位是 1：把当前 base 的贡献累入 result");
            } else {
                emit(5, List.of(VizEvent.of(VizEvent.EventType.COMPARE,
                        "二进制末位 = 0 → 跳过", "result")),
                        "最低位是 0：跳过，只平方 base");
            }
            base *= base;
            exp >>= 1;
            emit(7, List.of(VizEvent.of(VizEvent.EventType.SET,
                    "base² = " + base + ", exp → " + exp + "（右移一位）", "base")),
                    "base 逐帧平方：1→" + BASE + "→" + BASE * BASE + "→…（指数折半）");
            stepIdx++;
        }
        emit(9, List.of(VizEvent.of(VizEvent.EventType.RETURN,
                "完成：" + BASE + "^" + EXP + " = " + result + "（只用了 " + stepIdx + " 次乘法而非 " + EXP + " 次）")),
                "log₂" + EXP + " = " + stepIdx + " 次乘法——对比逐乘 " + EXP + " 次，快了 " + (EXP / stepIdx) + " 倍");
        return new Trace(SCHEMA, meta(), steps);
    }

    private void emit(int line, List<VizEvent> events, String why) {
        StackFrame frame = new StackFrame("fastPower",
                OrderedMaps.of("result", Value.prim(result),
                       "base", Value.prim(base),
                       "exp", Value.prim(exp),
                       "二进制", Value.prim(Integer.toBinaryString(exp))), null);
        Map<String, Layer> layers = new LinkedHashMap<>();
        layers.put("mem.stack", new StackLayer(List.of(frame)));
        layers.put("mem.heap", new HeapLayer(snapshotHeap()));
        layers.put("ds.array", snapshotArray());
        steps.add(new Step(steps.size(), Loc.line(line, why),
                Collections.unmodifiableMap(layers), List.copyOf(events), new Intent(why, INVARIANT)));
    }

    private Heap snapshotHeap() {
        Map<String, Value> fields = new LinkedHashMap<>();
        fields.put("result", Value.prim(result));
        fields.put("base", Value.prim(base));
        fields.put("exp", Value.prim(exp));
        fields.put("exp_binary", Value.prim(Integer.toBinaryString(exp)));
        HeapObject obj = new HeapObject("@1", "state", "FastPowerState",
                Collections.unmodifiableMap(fields), List.of());
        Map<String, HeapObject> objs = new LinkedHashMap<>();
        objs.put("@1", obj);
        return new Heap(Collections.unmodifiableMap(objs));
    }

    private ArrayView snapshotArray() {
        // 用数组视图展示指数二进制位的逐位扫描
        List<Integer> bits = new ArrayList<>();
        String bin = Integer.toBinaryString(EXP);
        for (char c : bin.toCharArray()) bits.add(c - '0');
        return new ArrayView("@1", "exp_bits", List.copyOf(bits),
                null, null, null, null, bits.size(), -1);
    }

    private Trace.TraceMeta meta() {
        return new Trace.TraceMeta(id(), title(), codeResource(),
                BASE + "^" + EXP, EXP);
    }

    @Override
    public java.util.List<String> dataLines() {
        return java.util.List.of("int base = " + BASE + ";", "int exp = " + EXP + ";");
    }

    @Override
    public Complexity complexity() {
        return new Complexity("O(log n)——指数折半", "O(1) 三个变量",
                "每轮 exp 右移一位（折半）+ base 平方 → log₂n 次乘法替代 n 次。对比逐乘：3^13 要 13 次 vs 4 次");
    }
}
