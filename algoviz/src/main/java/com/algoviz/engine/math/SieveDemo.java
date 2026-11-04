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
 * 素数筛（埃氏筛）：从 2 开始，每个素数的倍数逐个划掉——剩下的就是素数。
 * 画面：一排格子，灰色 = 未判断，绿色 = 素数，红色划线 = 合数。
 */
public final class SieveDemo implements Algorithm {

    private static final String SCHEMA = "1";
    private static final String INVARIANT =
            "每个素数 p 的倍数都不是素数——从 p² 开始逐个划掉（小于 p² 的已被更小素数划过）";

    private final int LIMIT;

    public SieveDemo() {
        this(20);
    }

    public SieveDemo(int limit) {
        this.LIMIT = limit;
        this.isComposite = new boolean[LIMIT + 1];
    }

    private final List<Step> steps = new ArrayList<>();
    private final boolean[] isComposite;
    private int curPrime = -1;
    private int curMultiple = -1;

    @Override public String id() { return "math.sieve"; }
    @Override public String title() { return "素数筛（埃氏筛）"; }
    @Override public String codeResource() { return "/code/SieveDemo.java"; }
    @Override public String docResource() { return "/markdown/sieve.md"; }

    @Override
    public Trace produce() {
        emit(3, List.of(VizEvent.of(VizEvent.EventType.INIT,
                "初始化：0..20 全部标为「可能是素数」（灰色）")),
                "埃氏筛：从 2 开始，每个素数的倍数都不是素数");
        for (int p = 2; p * p <= LIMIT; p++) {
            if (isComposite[p]) continue;
            curPrime = p;
            emit(4, List.of(VizEvent.of(VizEvent.EventType.COMPARE,
                    "p = " + p + " 是素数（绿色）→ 划掉它的倍数", "n:" + p)),
                    p + " 是素数：从 p² 开始，p 的每个倍数都标记为合数");
            for (int m = p * p; m <= LIMIT; m += p) {
                if (!isComposite[m]) {
                    isComposite[m] = true;
                    curMultiple = m;
                    emit(5, List.of(VizEvent.of(VizEvent.EventType.SET,
                            "划掉 " + m + "（=" + p + " × " + (m / p) + "）", "n:" + m)),
                            m + " = " + p + " × " + (m / p) + " → 不是素数");
                }
            }
        }
        curPrime = -1;
        curMultiple = -1;
        StringBuilder primes = new StringBuilder();
        for (int i = 2; i <= LIMIT; i++) {
            if (!isComposite[i]) {
                if (primes.length() > 0) primes.append(", ");
                primes.append(i);
            }
        }
        emit(7, List.of(VizEvent.of(VizEvent.EventType.RETURN,
                "素数：" + primes)),
                "筛完后绿色格子就是全部素数——O(n log log n)");
        return new Trace(SCHEMA, meta(), steps);
    }

    private void emit(int line, List<VizEvent> events, String why) {
        StackFrame frame = new StackFrame("sieve",
                OrderedMaps.of("limit", Value.prim(LIMIT),
                       "curPrime", Value.prim(curPrime),
                       "curMultiple", Value.prim(curMultiple)), null);
        Map<String, Layer> layers = new LinkedHashMap<>();
        layers.put("mem.stack", new StackLayer(List.of(frame)));
        layers.put("mem.heap", new HeapLayer(snapshotHeap()));
        layers.put("ds.array", snapshotArray());
        steps.add(new Step(steps.size(), Loc.line(line, why),
                Collections.unmodifiableMap(layers), List.copyOf(events), new Intent(why, INVARIANT)));
    }

    private Heap snapshotHeap() {
        Map<String, HeapObject> objs = new LinkedHashMap<>();
        List<Value> elems = new ArrayList<>();
        for (int i = 0; i <= LIMIT; i++) {
            elems.add(Value.prim(isComposite[i] ? 1 : 0));
        }
        objs.put("@1", new HeapObject("@1", "isComposite", "boolean[" + (LIMIT + 1) + "]",
                new LinkedHashMap<>(), List.copyOf(elems)));
        return new Heap(Collections.unmodifiableMap(objs));
    }

    private ArrayView snapshotArray() {
        List<Integer> vals = new ArrayList<>();
        for (int i = 0; i <= LIMIT; i++) vals.add(isComposite[i] ? 1 : 0);
        return new ArrayView("@1", "sieve", List.copyOf(vals),
                curPrime, null, curMultiple, null, LIMIT + 1, -1);
    }

    private Trace.TraceMeta meta() {
        return new Trace.TraceMeta(id(), title(), codeResource(), "limit=" + LIMIT, LIMIT + 1);
    }

    @Override
    public java.util.List<String> dataLines() {
        return java.util.List.of("int limit = " + LIMIT + ";");
    }

    @Override
    public Complexity complexity() {
        return new Complexity("O(n log log n)", "O(n) boolean 数组",
                "每个合数只被最小素因子划一次 → 总操作数 = n(1/2 + 1/3 + 1/5 + …) ≈ n·ln(ln n)");
    }
}
