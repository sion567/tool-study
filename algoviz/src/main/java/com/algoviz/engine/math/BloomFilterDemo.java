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
 * 布隆过滤器：多个 hash 函数将元素映射到 bit 数组的多个位置——查询时全 1 = 可能存在。
 * 画面：BitSet 格子 + 多条 hash 箭头——有 0 位 → 一定不存在，全 1 → 可能存在（有误判）。
 */
public final class BloomFilterDemo implements Algorithm {

    private static final String SCHEMA = "1";
    private static final String INVARIANT =
            "有 0 位 → 一定不存在；全 1 → 可能存在（有误判率）——只查不加，不会有假阴性";

    private static final int BITS = 16;
    private static final String[] TO_ADD = {"apple", "banana"};
    private static final String[] TO_CHECK = {"apple", "cherry", "banana"};

    private final List<Step> steps = new ArrayList<>();
    private final boolean[] bits = new boolean[BITS];
    private String curElement = "";
    private int curHash1, curHash2;
    private boolean isFalsePositive;

    @Override public String id() { return "math.bloomfilter"; }
    @Override public String title() { return "布隆过滤器"; }
    @Override public String codeResource() { return "/code/BloomFilterDemo.java"; }
    @Override public String docResource() { return "/markdown/bloom-filter.md"; }

    @Override
    public Trace produce() {
        emit(3, List.of(VizEvent.of(VizEvent.EventType.INIT,
                "BitSet(" + BITS + "b) 全 0 初始化")), "布隆过滤器：多个 hash 函数 → bit 数组的多个位置");
        for (String s : TO_ADD) {
            curElement = s;
            int h1 = hash1(s), h2 = hash2(s);
            curHash1 = h1; curHash2 = h2;
            bits[h1] = true; bits[h2] = true;
            emit(5, List.of(VizEvent.of(VizEvent.EventType.SET,
                    "add(\"" + s + "\"): hash1=" + h1 + ", hash2=" + h2 + " → 两个位都置 1")),
                    "添加：把两个 hash 位置都设为 1——查询时全 1 才「可能存在」");
        }
        for (String s : TO_CHECK) {
            curElement = s;
            int h1 = hash1(s), h2 = hash2(s);
            curHash1 = h1; curHash2 = h2;
            boolean b1 = bits[h1], b2 = bits[h2];
            boolean exists = b1 && b2;
            isFalsePositive = exists && !wasAdded(s);
            emit(8, List.of(VizEvent.of(VizEvent.EventType.COMPARE,
                    "check(\"" + s + "\"): bit[" + h1 + "]=" + (b1 ? 1 : 0)
                            + ", bit[" + h2 + "]=" + (b2 ? 1 : 0)
                            + " → " + (exists ? "可能存在" : "一定不存在"),
                    "bits:" + h1, "bits:" + h2)),
                    exists ? "全 1 → 可能存在（也可能是 hash 碰撞的误判）"
                           : "有 0 → 一定不存在（布隆过滤器的核心保证：无假阴性）");
        }
        emit(10, List.of(VizEvent.of(VizEvent.EventType.RETURN,
                "布隆过滤器 = 空间高效的存在性判断：无假阴性，有可控误判率")),
                "空间开销：BITS/8 字节，误判率 ≈ (1 - e^(-kn/m))² 其中 k=2 个 hash, m=" + BITS);
        return new Trace(SCHEMA, meta(), steps);
    }

    private boolean wasAdded(String s) {
        for (String a : TO_ADD) if (a.equals(s)) return true;
        return false;
    }

    private int hash1(String s) {
        return Math.abs(s.hashCode()) % BITS;
    }

    private int hash2(String s) {
        return Math.abs((s.hashCode() * 31 + 7) % BITS);
    }

    private void emit(int line, List<VizEvent> events, String why) {
        StackFrame frame = new StackFrame("bloomFilter",
                OrderedMaps.of("cur", Value.prim(curElement),
                       "h1", Value.prim(curHash1),
                       "h2", Value.prim(curHash2),
                       "isFalsePositive", Value.prim(isFalsePositive)), null);
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
        for (int i = 0; i < BITS; i++) elems.add(Value.prim(bits[i] ? 1 : 0));
        Map<String, Value> fields = new LinkedHashMap<>();
        for (int i = 0; i < BITS; i++) fields.put("b" + i, Value.prim(bits[i] ? 1 : 0));
        objs.put("@1", new HeapObject("@1", "filter", "BloomFilter(" + BITS + "b)",
                Collections.unmodifiableMap(fields), List.copyOf(elems)));
        return new Heap(Collections.unmodifiableMap(objs));
    }

    private ArrayView snapshotArray() {
        List<Integer> vals = new ArrayList<>();
        for (int i = 0; i < BITS; i++) vals.add(bits[i] ? 1 : 0);
        return new ArrayView("@1", "filter", List.copyOf(vals),
                curHash1, curHash2, null, null, BITS, -1);
    }

    private Trace.TraceMeta meta() {
        StringBuilder sb = new StringBuilder();
        for (String s : TO_ADD) sb.append(s).append(',');
        for (String s : TO_CHECK) sb.append(s).append(',');
        return new Trace.TraceMeta(id(), title(), codeResource(), sb.toString(), BITS);
    }

    @Override
    public java.util.List<String> dataLines() {
        return java.util.List.of("int bits = " + BITS + ";");
    }

    @Override
    public Complexity complexity() {
        return new Complexity("O(k) k 个 hash 函数", "O(m) bit 数组",
                "空间 O(m) 而不是 O(n·s)——用可控误判率换取巨大空间节省。无假阴性是核心保证");
    }
}
