package com.algoviz.engine.olympiad;

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
 * 数论·扩展欧几里得与中国剩余：先算出「模世界的除法」，再合并同余方程组。
 * 第一幕 exGCD(m, n)：迭代表 (old_r,r)(old_s,s)(old_t,t) 逐行演算——ms+nt=1 时 s 就是 m mod n 的逆元。
 * 第二幕 CRT：x≡a (mod m) 与 x≡b (mod n)（m,n 互质）——代入 x=a+mk → k≡(b−a)·m⁻¹ (mod n) → 唯一解。
 * 画面（math.matrix 复用）：3×2 迭代表 + 4×1 CRT 演算栏，双网格同屏。
 */
public final class CrtDemo implements Algorithm {

    private static final String SCHEMA = "1";
    private static final String INVARIANT =
            "Bézout 恒等式 ms+nt=gcd(m,n)；gcd=1 → s 就是 m 在 mod n 下的逆元——「除法」在模世界存在；"
                    + "互质模数下 CRT 恰有一解（mod mn）：两个余数唯一确定一个数";

    private final int a, m, b, n;

    private final List<Step> steps = new ArrayList<>();
    private final List<String> table = new ArrayList<>();   // 迭代表 3×2 展示值
    private final List<String> crtRows = new ArrayList<>(); // CRT 演算 4×1
    private int inverse;                                     // m⁻¹ mod n
    private int kValue, xValue;

    public CrtDemo() {
        this(2, 3, 3, 5);   // x≡2 (mod 3)，x≡3 (mod 5) → x=8
    }

    /** 变体：互质素数模数 + 各自的非零余数（CRT 解恒存在）。 */
    public CrtDemo(int a, int m, int b, int n) {
        this.a = a;
        this.m = m;
        this.b = b;
        this.n = n;
    }

    @Override public String id() { return "olympiad.crt"; }
    @Override public String title() { return "数论·扩展欧几里得与CRT"; }
    @Override public String codeResource() { return "/code/CrtDemo.java"; }
    @Override public String docResource() { return "/markdown/crt.md"; }

    @Override
    public Trace produce() {
        steps.clear();
        table.clear();
        crtRows.clear();
        setRows("", "", "", "");   // 先占位 4 行：合约要求网格全程矩形
        // ---- 第一幕：扩展欧几里得 exGCD(m, n) ----
        int oldR = m, r = n, oldS = 1, s = 0, oldT = 0, t = 1;
        setTable(oldR, r, oldS, s, oldT, t);
        emit(5, List.of(VizEvent.of(VizEvent.EventType.INIT,
                String.format("exGCD(%d, %d)：目标 m·s + n·t = gcd——三列系数随商滚动", m, n))),
                "普通 GCD 只留商数，扩展版把「怎么凑出 gcd」的系数一并记下——Bézout 的逐行流水账");
        while (r != 0) {
            int q = oldR / r;
            int tr = oldR - q * r, ts = oldS - q * s, tt = oldT - q * t;
            oldR = r; r = tr;
            oldS = s; s = ts;
            oldT = t; t = tt;
            setTable(oldR, r, oldS, s, oldT, t);
            emit(8, List.of(VizEvent.of(VizEvent.EventType.SET,
                    String.format("商 q=%d → (%d,%d)·(%d,%d)·(%d,%d)", q, oldR, r, oldS, s, oldT, t))),
                    "同一套「除→减」操作同步作用在 (m,n)、(1,0)、(0,1) 三行——不变式 ms+nt=旧 gcd 全程成立");
        }
        inverse = ((oldS % n) + n) % n;
        emit(12, List.of(VizEvent.of(VizEvent.EventType.MARK_SORTED,
                String.format("r=0 → gcd=%d，s=%d：%d·%d+%d·%d=%d，即 %d⁻¹ ≡ %d (mod %d)",
                        oldR, oldS, m, oldS, n, oldT, m * oldS + n * oldT, m, inverse, n))),
                "gcd=1 的瞬间，s 就是 m 的逆元——「1/m」在模世界里有了身份证");
        // ---- 第二幕：CRT 合并 x≡a (mod m) 与 x≡b (mod n) ----
        setRows("x = " + a + " + " + m + "k", "", "", "");
        emit(13, List.of(VizEvent.of(VizEvent.EventType.MESSAGE,
                String.format("同余组：x≡%d (mod %d) 与 x≡%d (mod %d)——代入 x = %d + %dk", a, m, b, n, a, m))),
                "第一个方程给出通解形式 x = a + mk：所有候选数排成一条等差链");
        int rhs = ((b - a) % n + n) % n;
        kValue = (int) ((long) rhs * inverse % n);
        setRows("x = " + a + " + " + m + "k", String.format("%dk ≡ %d (mod %d)", m, rhs, n),
                String.format("k ≡ %d·%d ≡ %d (mod %d)", rhs, inverse, kValue, n), "");
        emit(14, List.of(VizEvent.of(VizEvent.EventType.SET,
                String.format("代入：%dk ≡ %d (mod %d) → k ≡ (%d)·%d⁻¹ ≡ %d (mod %d)", m, rhs, n, rhs, m, kValue, n))),
                "逆元登场：「除以 m」=「乘 m 的逆元」——第一幕的系数在这里兑成现金");
        xValue = a + m * kValue;
        setRows("x = " + a + " + " + m + "k", String.format("%dk ≡ %d (mod %d)", m, rhs, n),
                String.format("k ≡ %d (mod %d)", kValue, n), String.format("x = %d + %d×%d = %d", a, m, kValue, xValue));
        emit(15, List.of(VizEvent.of(VizEvent.EventType.MARK_SORTED,
                String.format("x = %d：验算 x mod %d = %d ✓，x mod %d = %d ✓——双同余同时成立", xValue, m, xValue % m, n, xValue % n))),
                "mod mn = " + (m * n) + " 内解唯一：两个余数像经纬度一样把 x 钉死");
        emit(20, List.of(VizEvent.of(VizEvent.EventType.RETURN,
                "「物不知数」出自《孙子算经》（约 4 世纪）：今有物不知其数，三三数之剩二……——CRT 比欧洲早了一千多年")),
                " RSA 解密、快速傅里叶卷积、多精度运算全靠 CRT 拆装——古人娱乐题成了现代密码学的承重墙");
        return new Trace(SCHEMA, meta(), steps);
    }

    private void setTable(int oldR, int r, int oldS, int s, int oldT, int t) {
        table.clear();
        table.add(String.valueOf(oldR));
        table.add(String.valueOf(r));
        table.add(String.valueOf(oldS));
        table.add(String.valueOf(s));
        table.add(String.valueOf(oldT));
        table.add(String.valueOf(t));
    }

    private void setRows(String r0, String r1, String r2, String r3) {
        crtRows.clear();
        crtRows.add(r0);
        crtRows.add(r1);
        crtRows.add(r2);
        crtRows.add(r3);
    }

    private void emit(int line, List<VizEvent> events, String why) {
        StackFrame fr = new StackFrame("crt",
                OrderedMaps.of("方程组", Value.prim(String.format("x≡%d(%d), x≡%d(%d)", a, m, b, n)),
                        "逆元", Value.prim(String.format("%d⁻¹≡%d (mod %d)", m, inverse, n)),
                        "k / x", Value.prim(kValue + " / " + xValue)), null);
        Map<String, Layer> layers = new LinkedHashMap<>();
        layers.put("mem.stack", new StackLayer(List.of(fr)));
        layers.put("mem.heap", new HeapLayer(snapshotHeap()));
        layers.put("math.matrix", snapshotMath());
        steps.add(new Step(steps.size(), Loc.line(line, why),
                Collections.unmodifiableMap(layers), List.copyOf(events), new Intent(why, INVARIANT)));
    }

    private Heap snapshotHeap() {
        Map<String, Value> fields = new LinkedHashMap<>();
        fields.put("逆元 m⁻¹", Value.prim(String.format("%d (mod %d)", inverse, n)));
        fields.put("解 x", Value.prim(xValue == 0 ? "求解中" : String.valueOf(xValue)));
        fields.put("验算 mod m / mod n", Value.prim(xValue == 0 ? "—" : (xValue % m) + " / " + (xValue % n)));
        Map<String, HeapObject> objs = new LinkedHashMap<>();
        objs.put("@1", new HeapObject("@1", "crt", "CongruenceState",
                Collections.unmodifiableMap(fields), List.of()));
        return new Heap(Collections.unmodifiableMap(objs));
    }

    private MathView snapshotMath() {
        MathView.Grid tableGrid = new MathView.Grid("table",
                "exGCD 迭代表：old_r/r / old_s/s / old_t/t",
                3, 2, List.copyOf(table), null, null, Set.of(), Set.of());
        Set<Integer> active = new LinkedHashSet<>();
        for (int i = 0; i < crtRows.size(); i++) {
            if (!crtRows.get(i).isEmpty()) {
                active.add(i);
            }
        }
        MathView.Grid crtGrid = new MathView.Grid("crt", "CRT 演算栏",
                4, 1, List.copyOf(crtRows), null, null, active, Set.of());
        return new MathView("扩展欧几里得 + CRT", List.of(tableGrid, crtGrid));
    }

    private Trace.TraceMeta meta() {
        return new Trace.TraceMeta(id(), title(), codeResource(),
                String.format("x≡%d(%d), x≡%d(%d)", a, m, b, n), 10);
    }

    @Override
    public java.util.List<String> dataLines() {
        return java.util.List.of(String.format(
                "int a = %d, m = %d, b = %d, n = %d;   //@DATA 余数与互质模数", a, m, b, n));
    }

    @Override
    public Complexity complexity() {
        return new Complexity("O(log min(m,n)) 迭代次数", "O(1) 三对系数",
                "RSA 解密走 CRT 加速、FFT 卷积、任意精度乘法拆装——"
                        + "「先拆余数、各算各的、再合并」是数论送给计算机的分治思想");
    }
}
