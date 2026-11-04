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
 * 欧几里得 GCD：辗转相除——gcd(a,b) = gcd(b, a%b)，直到 b=0。
 * 不变量：gcd(a,b) = gcd(b, a%b)——余数逐步缩小，最终归零。
 */
public final class GcdDemo implements Algorithm {

    private static final String SCHEMA = "1";
    private static final String INVARIANT =
            "gcd(a,b) = gcd(b, a%b)——余数逐步缩小，最终归零时 a 就是答案";

    private static final int A = 48, B = 18;

    private final List<Step> steps = new ArrayList<>();
    private int a, b;
    private int round;

    @Override public String id() { return "math.gcd"; }
    @Override public String title() { return "最大公约数"; }
    @Override public String codeResource() { return "/code/GcdDemo.java"; }
    @Override public String docResource() { return "/markdown/gcd.md"; }

    @Override
    public Trace produce() {
        a = A; b = B; round = 0;
        emit(3, List.of(VizEvent.of(VizEvent.EventType.INIT,
                "gcd(" + a + ", " + b + ") —— 开始辗转相除")),
                "辗转相除 = 用余数代替除数，直到余数为零");
        while (b != 0) {
            int remainder = a % b;
            round++;
            emit(4, List.of(VizEvent.of(VizEvent.EventType.COMPARE,
                    "round " + round + ": gcd(" + a + ", " + b + ") → a%b = " + remainder,
                    "a", "b")),
                    "用余数代替：gcd(" + a + "," + b + ") = gcd(" + b + "," + remainder + ")");
            a = b; b = remainder;
            emit(5, List.of(VizEvent.of(VizEvent.EventType.SET,
                    "→ gcd(" + a + ", " + b + ")", "a", "b")),
                    "参数替换：新的 a = 旧 b，新的 b = 旧余数");
        }
        emit(7, List.of(VizEvent.of(VizEvent.EventType.RETURN,
                "b = 0 → gcd = " + a)), "余数为零：a 就是最大公约数");
        return new Trace(SCHEMA, meta(), steps);
    }

    private void emit(int line, List<VizEvent> events, String why) {
        StackFrame frame = new StackFrame("gcd",
                OrderedMaps.of("a", Value.prim(a), "b", Value.prim(b),
                       "round", Value.prim(round)), null);
        Map<String, Layer> layers = new LinkedHashMap<>();
        layers.put("mem.stack", new StackLayer(List.of(frame)));
        layers.put("mem.heap", new HeapLayer(snapshotHeap()));
        layers.put("ds.array", snapshotArray());
        steps.add(new Step(steps.size(), Loc.line(line, why),
                Collections.unmodifiableMap(layers), List.copyOf(events), new Intent(why, INVARIANT)));
    }

    private Heap snapshotHeap() {
        Map<String, Value> fields = new LinkedHashMap<>();
        fields.put("a", Value.prim(a));
        fields.put("b", Value.prim(b));
        HeapObject obj = new HeapObject("@1", "state", "GcdState",
                Collections.unmodifiableMap(fields), List.of());
        Map<String, HeapObject> objs = new LinkedHashMap<>();
        objs.put("@1", obj);
        return new Heap(Collections.unmodifiableMap(objs));
    }

    private ArrayView snapshotArray() {
        List<Integer> vals = new ArrayList<>();
        vals.add(a); vals.add(b);
        return new ArrayView("@1", "(a,b)", List.copyOf(vals),
                0, 1, null, null, 2, -1);
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
        return new Complexity("O(log min(a,b))", "O(1) 两个变量",
                "每轮余数至少缩小一半（鸽笼原理）→ log 轮收敛。最坏情况：相邻斐波那契数");
    }
}
