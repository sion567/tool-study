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
 * 大数乘法（Karatsuba）：x·y 的高斯妙招——3 次乘法替代 4 次。
 * x = a·10^m + b，y = c·10^m + d：
 *   朴素  x·y = ac·10^2m + (ad+bc)·10^m + bd   → 4 次乘法
 *   妙招  ad+bc = (a+b)(c+d) − ac − bd          → 3 次乘法
 * 画面：三张网格——拆分（a/b/c/d）、三次乘法（P1=ac, P2=bd, P3=(a+b)(c+d)）、
 * 合并（中间项 = P3−P1−P2）。递归一层展示思想，大数场景下指数 4→3^log n = n^1.585。
 * 不变量：中间项恒等式 ad+bc = (a+b)(c+d) − ac − bd——少乘一次的代数代价是两次加减。
 */
public final class KaratsubaDemo implements Algorithm {

    private static final String SCHEMA = "1";
    private static final String INVARIANT =
            "ad+bc = (a+b)(c+d) − ac − bd——3 次乘法 + 加减法替代 4 次乘法，指数 n² → n^1.585";

    private static final long X = 1234;
    private static final long Y = 5678;
    private static final int M = 2;   // 拆分位：x = 12·10² + 34

    /** 与 /code/KaratsubaDemo.java 的行号对齐 */
    private static final int L_SPLIT = 4;
    private static final int L_P1 = 6;
    private static final int L_P2 = 7;
    private static final int L_P3 = 8;
    private static final int L_MID = 10;
    private static final int L_COMBINE = 12;

    private final List<Step> steps = new ArrayList<>();
    private long a, b, c, d, p1, p2, p3, mid;

    @Override public String id() { return "math.karatsuba"; }
    @Override public String title() { return "大数乘法（Karatsuba）"; }
    @Override public String codeResource() { return "/code/KaratsubaDemo.java"; }
    @Override public String docResource() { return "/markdown/karatsuba.md"; }

    @Override
    public Trace produce() {
        steps.clear();
        a = X / pow10(M);
        b = X % pow10(M);
        c = Y / pow10(M);
        d = Y % pow10(M);
        p1 = p2 = p3 = mid = 0;

        emit(L_SPLIT, List.of(VizEvent.of(VizEvent.EventType.INIT,
                "x=" + X + " → a=" + a + ", b=" + b + "；y=" + Y + " → c=" + c + ", d=" + d)),
                "按一半位数拆开：x = a·10^m + b——大数变小数，递归的入口");
        emit(L_P1, List.of(VizEvent.of(VizEvent.EventType.SET, "P1 = a·c = " + a + "×" + c + " = " + (p1 = a * c))),
                "高位×高位——三次乘法之一");
        emit(L_P2, List.of(VizEvent.of(VizEvent.EventType.SET, "P2 = b·d = " + b + "×" + d + " = " + (p2 = b * d))),
                "低位×低位——三次乘法之二");
        emit(L_P3, List.of(VizEvent.of(VizEvent.EventType.SET,
                "P3 = (a+b)(c+d) = " + (a + b) + "×" + (c + d) + " = " + (p3 = (a + b) * (c + d)))),
                "交叉和整体乘——三次乘法之三：P3 里混着我们要的 ad+bc，还有多余的 P1 和 P2");
        emit(L_MID, List.of(VizEvent.of(VizEvent.EventType.COMPARE,
                "中间项 = P3 − P1 − P2 = " + p3 + " − " + p1 + " − " + p2 + " = " + (mid = p3 - p1 - p2))),
                "恒等式一减，交叉项 ad+bc 原形毕露——少乘一次的代数代价只是两次减法");
        long result = p1 * pow10(2 * M) + mid * pow10(M) + p2;
        emit(L_COMBINE, List.of(VizEvent.of(VizEvent.EventType.RETURN,
                "x·y = P1·10^" + (2 * M) + " + 中间项·10^" + M + " + P2 = " + result
                        + "（验算 " + X + "×" + Y + " = " + (X * Y) + "）")),
                "位权拼回：三次乘法递归到底就是 O(n^1.585)——对比朴素 O(n²)，4096 位大数省一半以上");
        return new Trace(SCHEMA, meta(), steps);
    }

    private long pow10(int k) {
        return (long) Math.pow(10, k);
    }

    private void emit(int line, List<VizEvent> events, String why) {
        StackFrame frame = new StackFrame("karatsuba",
                OrderedMaps.of("x", Value.prim(X), "y", Value.prim(Y),
                        "a,b,c,d", Value.prim(a + "," + b + "," + c + "," + d)), null);
        Map<String, Layer> layers = new LinkedHashMap<>();
        layers.put("mem.stack", new StackLayer(List.of(frame)));
        layers.put("mem.heap", new HeapLayer(snapshotHeap()));
        layers.put("math.matrix", snapshotMatrix());
        steps.add(new Step(steps.size(), Loc.line(line, why),
                Collections.unmodifiableMap(layers), List.copyOf(events), new Intent(why, INVARIANT)));
    }

    private Heap snapshotHeap() {
        Map<String, Value> fields = new LinkedHashMap<>();
        fields.put("P1=ac", Value.prim(p1));
        fields.put("P2=bd", Value.prim(p2));
        fields.put("P3=(a+b)(c+d)", Value.prim(p3));
        fields.put("中间项=P3−P1−P2", Value.prim(mid));
        Map<String, HeapObject> objs = new LinkedHashMap<>();
        objs.put("@1", new HeapObject("@1", "products", "KaratsubaState",
                Collections.unmodifiableMap(fields), List.of()));
        return new Heap(Collections.unmodifiableMap(objs));
    }

    private MathView snapshotMatrix() {
        List<MathView.Grid> grids = new ArrayList<>();
        // 网格一：拆分
        grids.add(new MathView.Grid("split", "拆分 m=" + M, 2, 2,
                List.of("a=" + a, "b=" + b, "c=" + c, "d=" + d),
                null, null, java.util.Set.of(), java.util.Set.of()));
        // 网格二：三次乘法（高亮 = 最后算出的那个）
        List<String> pm = List.of("P1=ac=" + p1, "P2=bd=" + p2, "P3=(a+b)(c+d)=" + p3);
        java.util.Set<Integer> pActive = new java.util.LinkedHashSet<>();
        if (p3 != 0) pActive.add(2);
        else if (p2 != 0) pActive.add(1);
        else if (p1 != 0) pActive.add(0);
        grids.add(new MathView.Grid("products", "三次乘法", 1, 3,
                pm, null, null,
                Collections.unmodifiableSet(pActive), java.util.Set.of()));
        // 网格三：合并（高亮 = 中间项格）
        java.util.Set<Integer> cActive = new java.util.LinkedHashSet<>();
        if (mid != 0) cActive.add(1);
        grids.add(new MathView.Grid("combine", "合并（位权拼回）", 1, 3,
                List.of("ac·10^4 = " + p1, "(ad+bc)·10^2 = " + mid, "bd = " + p2),
                null, null,
                Collections.unmodifiableSet(cActive), java.util.Set.of()));
        return new MathView("Karatsuba 三段乘法", Collections.unmodifiableList(grids));
    }

    private Trace.TraceMeta meta() {
        return new Trace.TraceMeta(id(), title(), codeResource(), X + "×" + Y, 7);
    }

    @Override
    public java.util.List<String> dataLines() {
        return java.util.List.of("long x = " + X + ", y = " + Y + ";   // 拆分位 m = " + M);
    }

    @Override
    public Complexity complexity() {
        return new Complexity("O(n^1.585) 3 次子乘法", "O(n) 拆分暂存",
                "T(n)=3T(n/2)+O(n) → n^log₂3 ≈ n^1.585。朴素 O(n²) 在 4096 位大数上慢 5 倍，"
                        + "Java BigInteger 超过阈值就切 Karatsuba，更大再切 Toom-Cook/FFT");
    }
}
