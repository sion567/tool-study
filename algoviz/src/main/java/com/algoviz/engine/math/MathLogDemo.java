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
 * 对数·乘法变加法：猜数字二分直觉（⌈log₂N⌉ 次必中）+ 计算尺原理（查表乘变加）+ 换底公式 + 复杂度阶梯。
 * lg 表 1..9 硬编码（计算尺的刻度本来就是查表——顺带规避浮点跨平台漂移，golden 逐字节稳定）。
 * 画面（math.matrix）：砍半序列格 / lg 演算格 / 复杂度阶梯格随幕切换。
 */
public final class MathLogDemo implements Algorithm {

    private static final String SCHEMA = "1";
    private static final String INVARIANT =
            "对数 = 指数的逆运算：log_b(xy)=log_b x+log_b y 把乘法压成加法，"
                    + "二分砍半 n 次覆盖 2ⁿ 个候选——⌈log₂N⌉ 次猜中任何数、复杂度阶梯 O(log n) 每步翻倍覆盖";

    /** 计算尺刻度表（1-based）：LGT[k] = lg(k)，k=1..9；5 位小数硬编码保证 golden 确定性。 */
    private static final double[] LGT = {0, 0, 0.30103, 0.47712, 0.60206, 0.69897, 0.77815, 0.84510, 0.90309, 0.95424};

    private final List<Step> steps = new ArrayList<>();
    private final int limit;                      // 猜数字上限 N（变体可换）
    private List<Integer> halves;                 // 砍半序列：N→…→1
    private int phase;                            // 0=猜数 1=乘变加 2=换底 3=阶梯
    private int cursor;                           // 砍半序列当前可见到的位置

    public MathLogDemo() {
        this(1000);                               // 经典例：1~1000 猜数
    }

    public MathLogDemo(int limit) {
        this.limit = limit;
    }

    @Override public String id() { return "math.log"; }
    @Override public String title() { return "对数·乘法变加法"; }
    @Override public String codeResource() { return "/code/MathLogDemo.java"; }
    @Override public String docResource() { return "/markdown/log.md"; }

    /** 猜数字二分：最坏情况次数 = 砍半到 1 的步数 = ⌈log₂N⌉。 */
    static int guessCount(int n) {
        int h = n, cnt = 0;
        while (h > 1) {
            h = (h + 1) / 2;
            cnt++;
        }
        return cnt;
    }

    @Override
    public Trace produce() {
        steps.clear();
        halves = new ArrayList<>();
        for (int h = limit; h > 1; h = (h + 1) / 2) {
            halves.add(h);
        }
        halves.add(1);
        int cnt = halves.size() - 1;
        phase = 0;
        cursor = 0;
        emit(31, List.of(VizEvent.of(VizEvent.EventType.INIT,
                String.format("猜数字 [1,%d]：逐个猜最坏 %d 次——二分只要 %d 次（⌈log₂%d⌉）", limit, limit, cnt, limit))),
                "对数的体感：砍半 10 次就能从 10 亿里锁定一个数——指数爆炸的逆过程");
        int third = Math.max(1, cnt / 3);
        for (int seg = 0; seg < 3; seg++) {
            int from = Math.min(cnt - 1, seg * third);
            int to = seg == 2 ? cnt : Math.min(cnt, (seg + 1) * third);
            cursor = to;
            emit(35, List.of(VizEvent.of(VizEvent.EventType.SET,
                    String.format("第 %d~%d 次猜：区间 %s（剩 %d）", from + 1, to,
                            slice(from, to), halves.get(to)))),
                    "每次猜中点，区间砍半——3 次砍半就剩 1/8，10 次砍半覆盖 1024");
        }
        emit(40, List.of(VizEvent.of(VizEvent.EventType.MARK_SORTED,
                String.format("%d 次必中：2^%d=%d ≥ %d——这就是 O(log n) 的体感", cnt, cnt, 1 << cnt, limit))),
                "猜数游戏 = 手动二分搜索：数组二分、BST 树高、指数折半全是同一条砍半曲线");
        phase = 1;
        long prod = 2L * 3;
        String lgSum = String.format("%.5f", LGT[2] + LGT[3]);
        emit(45, List.of(VizEvent.of(VizEvent.EventType.SET,
                String.format("乘变加：lg2=%.5f + lg3=%.5f = %s = lg6 ✓（2×3=%d）",
                        LGT[2], LGT[3], lgSum, prod))),
                "计算尺的原理：刻度按 log 摆，乘法变加法——纳皮尔 1614 年的对数表替人类省了 300 年心算");
        phase = 2;
        String swap = String.format("%.2f", Math.log10(limit) / LGT[2]);
        emit(49, List.of(VizEvent.of(VizEvent.EventType.SET,
                String.format("换底：log₂%d = lg%d/lg2 = %s/%.5f ≈ %s——底换成谁都行，只差常数倍",
                        limit, limit, String.format("%.0f", Math.round(Math.log10(limit) * 1e9) / 1e9), LGT[2], swap))),
                "换底公式 log_b a = lg a/lg b：信息论用 bit（底 2）、数学用 e（自然对数）——同一把尺");
        phase = 3;
        long n = limit;
        int logn = guessCount((int) Math.min(Integer.MAX_VALUE, n));
        long nlogn = n * logn;
        emit(54, List.of(VizEvent.of(VizEvent.EventType.SET,
                String.format("复杂度阶梯 n=%d：O(1)=%d，O(log n)=%d，O(n)=%d，O(n·log n)=%d，O(n²)=%d",
                        n, 1, logn, n, nlogn, n * n))),
                "n 翻倍时 O(log n) 只加 1 步——这就是二分/BST/快速幂敢称「极快」的数学底座");
        double paperKm = 0.1 * Math.pow(2, 42) / 1e6;      // 0.1mm × 2^42 折成 km（≈43.98 万 km）
        emit(59, List.of(VizEvent.of(VizEvent.EventType.RETURN,
                String.format("历史：纳皮尔 1614 对数表 → 计算尺 400 年 → 折纸 42 张≈%.1f 万 km 到月球（38.4 万）→ 香农 bit=log₂、里氏 +1 能量×32",
                        paperKm / 1e4))),
                "对数无处不在：地震震级、pH、分贝、信息熵——跨数量级的世界用 log 才看得清");
        return new Trace(SCHEMA, meta(), steps);
    }

    private String slice(int from, int to) {
        StringBuilder sb = new StringBuilder();
        for (int i = from; i <= to; i++) {
            if (i > from) {
                sb.append("→");
            }
            sb.append(halves.get(i));
        }
        return sb.toString();
    }

    private void emit(int line, List<VizEvent> events, String why) {
        StackFrame frame = new StackFrame("log",
                OrderedMaps.of("阶段", Value.prim(new String[]{"猜数", "乘变加", "换底", "阶梯"}[phase]),
                        "上限", Value.prim(String.valueOf(limit)),
                        "砍半", Value.prim(String.valueOf(cursor) + "/" + (halves.size() - 1))), null);
        Map<String, Layer> layers = new LinkedHashMap<>();
        layers.put("mem.stack", new StackLayer(List.of(frame)));
        layers.put("mem.heap", new HeapLayer(snapshotHeap()));
        layers.put("math.matrix", snapshotMath());
        steps.add(new Step(steps.size(), Loc.line(line, why),
                Collections.unmodifiableMap(layers), List.copyOf(events), new Intent(why, INVARIANT)));
    }

    private Heap snapshotHeap() {
        Map<String, Value> fields = new LinkedHashMap<>();
        fields.put("上限", Value.prim(String.valueOf(limit)));
        fields.put("lg表", Value.prim("lg1..lg9 查表"));
        Map<String, HeapObject> objs = new LinkedHashMap<>();
        objs.put("@1", new HeapObject("@1", "log", "LogState",
                Collections.unmodifiableMap(fields), List.of()));
        return new Heap(Collections.unmodifiableMap(objs));
    }

    private MathView snapshotMath() {
        if (phase == 0) {
            List<String> vals = new ArrayList<>();
            Set<Integer> active = new LinkedHashSet<>();
            for (int i = 0; i < halves.size(); i++) {
                vals.add(String.valueOf(halves.get(i)));
                if (i <= cursor) {
                    active.add(i);
                }
            }
            MathView.Grid grid = new MathView.Grid("guess", "砍半序列（每次猜中点，黄=已砍过）",
                    1, halves.size(), vals, null, null, active, Set.of());
            return new MathView("对数", List.of(grid));
        }
        if (phase == 1) {
            List<String> vals = List.of("2", "3", "6",
                    String.format("%.5f", LGT[2]), String.format("%.5f", LGT[3]), String.format("%.5f", LGT[2] + LGT[3]));
            Set<Integer> active = new LinkedHashSet<>(List.of(3, 4, 5));
            MathView.Grid grid = new MathView.Grid("slide", "计算尺：上行=数，下行=lg（加下行=乘上行）",
                    2, 3, vals, null, null, active, Set.of());
            return new MathView("对数", List.of(grid));
        }
        if (phase == 2) {
            String lgN = String.format("%.5f", Math.round(Math.log10(limit) * 1e5) / 1e5);
            List<String> vals = List.of("lg" + limit + "=" + lgN, "lg2=0.30103",
                    "log₂" + limit + "≈" + String.format("%.2f", Math.log10(limit) / LGT[2]));
            Set<Integer> active = new LinkedHashSet<>(List.of(2));
            MathView.Grid grid = new MathView.Grid("base", "换底公式：log_b a = lg a / lg b",
                    1, 3, vals, null, null, active, Set.of());
            return new MathView("对数", List.of(grid));
        }
        int logn = guessCount(limit);
        List<String> vals = List.of("1", String.valueOf(logn), String.valueOf(limit),
                String.valueOf((long) limit * logn), String.valueOf((long) limit * limit));
        Set<Integer> active = new LinkedHashSet<>(List.of(1));
        MathView.Grid grid = new MathView.Grid("ladder", "复杂度阶梯 n=" + limit + "：O(1)/O(log n)/O(n)/O(n·log n)/O(n²)",
                1, 5, vals, null, null, active, Set.of());
        return new MathView("对数", List.of(grid));
    }

    private Trace.TraceMeta meta() {
        return new Trace.TraceMeta(id(), title(), codeResource(),
                "N=" + limit + "（猜数字）", limit);
    }

    @Override
    public java.util.List<String> dataLines() {
        return java.util.List.of("int limit = " + limit + ";   //@DATA 猜数字上限 N");
    }

    @Override
    public Complexity complexity() {
        return new Complexity("O(log n) 次砍半（n=上限）", "O(1) 状态",
                "对数把乘法压成加法、把指数压成步数——计算尺 400 年与二分搜索是同一个数学的两副面孔");
    }
}
