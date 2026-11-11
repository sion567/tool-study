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
 * 组合构造·约瑟夫环：n 人围圈报数，报到 k 出列，下一位从头数——谁活到最后？
 * 两幕同屏：①逐轮模拟（报数指针 + 出列置 0）②递推公式 f(j) = (f(j−1)+k) mod j 自底向上填表。
 * 两种世界观必须给出同一个幸存者——模拟给直觉，公式给 O(n)，互相校验。
 * 画面（ds.array 复用）：蓝柱 = 圈中人（编号即柱高），红柱 = 出列者；第二幕换成 f 表。
 */
public final class JosephusDemo implements Algorithm {

    private static final String SCHEMA = "1";
    private static final String INVARIANT =
            "报数取模 = 圆圈没有起点；递推 f(j) = (f(j−1)+k) mod j——"
                    + "j 人圈的幸存者位置 = (j−1 人圈答案 + k) 对 j 取模：小圈答案滚成大圈答案；模拟与公式必须同归";

    private final int n;
    private final int k;

    private final List<Step> steps = new ArrayList<>();
    private final List<Integer> view = new ArrayList<>();   // ArrayView 当前展示值
    private boolean[] alive;
    private int cursor, round, fValue;

    public JosephusDemo() {
        this(12, 3);
    }

    /** 变体：换人数与报数步长。 */
    public JosephusDemo(int n, int k) {
        this.n = n;
        this.k = k;
    }

    @Override public String id() { return "olympiad.josephus"; }
    @Override public String title() { return "组合构造·约瑟夫环"; }
    @Override public String codeResource() { return "/code/JosephusDemo.java"; }
    @Override public String docResource() { return "/markdown/josephus.md"; }

    @Override
    public Trace produce() {
        steps.clear();
        view.clear();
        alive = new boolean[n];
        for (int i = 0; i < n; i++) {
            alive[i] = true;
            view.add(i + 1);
        }
        cursor = n - 1;   // 上一个报数者（下一位从 1 号开始数）
        round = 0;
        emit(4, List.of(VizEvent.of(VizEvent.EventType.INIT,
                String.format("%d 人围圈，1 号起每报到 %d 出列——谁活到最后？", n, k))),
                "史学家约瑟夫斯在洞穴里算出了自己的位置——这道题的答案关乎性命");
        int remaining = n;
        while (remaining > 1) {
            int p = cursor;
            List<Integer> countedList = new ArrayList<>();
            while (countedList.size() < k) {
                p = (p + 1) % n;
                if (alive[p]) {
                    countedList.add(p);
                }
            }
            cursor = p;
            alive[cursor] = false;
            round++;
            view.set(cursor, 0);
            remaining--;
            StringBuilder counted = new StringBuilder();
            for (int person : countedList) {
                counted.append(" ").append(person + 1).append("号");
            }
            emit(13, List.of(VizEvent.of(VizEvent.EventType.SET,
                    String.format("第 %d 轮：报数%s → %d 号出列（剩 %d 人）", round, counted, cursor + 1, remaining),
                    "a[" + cursor + "]")),
                    "圈没有起点：数到尾巴就绕回头——模运算天生就是圆圈的代数");
        }
        int survivorSim = -1;
        for (int i = 0; i < n; i++) {
            if (alive[i]) {
                survivorSim = i + 1;   // 幸存者 = 唯一还活着的人（cursor 是最后出列者，不是幸存者）
            }
        }
        emit(22, List.of(VizEvent.of(VizEvent.EventType.MARK_SORTED,
                String.format("模拟落幕：%d 号幸存——但每轮要走 O(n) 步，n 大了模拟太慢", survivorSim))),
                "模拟给了直觉：幸存者位置和 n、k 的关系藏在淘汰顺序里");
        // ---- 第二幕：递推公式自底向上 ----
        for (int i = 0; i < n; i++) {
            view.set(i, 0);
        }
        fValue = 0;   // f(1) = 0（1 人圈幸存者位置 0）
        emit(16, List.of(VizEvent.of(VizEvent.EventType.MESSAGE,
                "换公式：f(1)=0，f(j) = (f(j−1)+" + k + ") mod j——从 1 人圈滚到 " + n + " 人圈")),
                "删掉出列者后圈少一人：j 人圈的答案 = (j−1 人圈的答案 + k) 对 j 取模");
        for (int j = 2; j <= n; j++) {
            fValue = (fValue + k) % j;
            view.set(j - 1, fValue + 1);
            emit(19, List.of(VizEvent.of(VizEvent.EventType.SET,
                    String.format("f(%d) = (f(%d)+%d) mod %d = %d → %d 号位", j, j - 1, k, j, fValue, fValue + 1),
                    "f[" + (j - 1) + "]")),
                    "每滚一步只用上一步的答案——递推是「记忆」的 O(n)，模拟是「重演」的 O(n²)");
        }
        int survivorFormula = fValue + 1;
        emit(23, List.of(VizEvent.of(VizEvent.EventType.MARK_SORTED,
                survivorFormula == survivorSim
                        ? String.format("公式 f(%d)+1 = %d 号 == 模拟幸存者 %d 号——两种解法握手言和", n, survivorFormula, survivorSim)
                        : String.format("公式 %d 号 ≠ 模拟 %d 号——出 bug 了！", survivorFormula, survivorSim),
                "f[" + (n - 1) + "]")),
                "1 亿人、报到 9 出列？公式毫秒出答案：模拟「做一遍」，递推「想明白」");
        emit(26, List.of(VizEvent.of(VizEvent.EventType.RETURN,
                "递推不动点：小圈答案唯一确定大圈答案——数学归纳法的算法化身")),
                "约瑟夫斯当场算出了自己该站的位置——会递推的人多活一次");
        return new Trace(SCHEMA, meta(), steps);
    }

    private void emit(int line, List<VizEvent> events, String why) {
        StackFrame fr = new StackFrame("josephus",
                OrderedMaps.of("圈人数", Value.prim(String.valueOf(n)),
                        "报数 k", Value.prim(String.valueOf(k)),
                        "轮次", Value.prim(String.valueOf(round)),
                        "f 当前", Value.prim(String.valueOf(fValue))), null);
        Map<String, Layer> layers = new LinkedHashMap<>();
        layers.put("mem.stack", new StackLayer(List.of(fr)));
        layers.put("mem.heap", new HeapLayer(snapshotHeap()));
        layers.put("ds.array", snapshotArray());
        steps.add(new Step(steps.size(), Loc.line(line, why),
                Collections.unmodifiableMap(layers), List.copyOf(events), new Intent(why, INVARIANT)));
    }

    private Heap snapshotHeap() {
        int remaining = 0;
        int survivor = -1;
        for (int i = 0; i < n; i++) {
            if (alive[i]) {
                remaining++;
                survivor = i + 1;
            }
        }
        Map<String, Value> fields = new LinkedHashMap<>();
        fields.put("幸存者（模拟）", Value.prim(remaining == 1 ? survivor + " 号" : "进行中"));
        fields.put("幸存者（公式 f+1）", Value.prim(String.valueOf(fValue + 1) + " 号位"));
        fields.put("复杂度对比", Value.prim("模拟 O(n²) · 递推 O(n)"));
        Map<String, HeapObject> objs = new LinkedHashMap<>();
        objs.put("@1", new HeapObject("@1", "josephus", "CircleState",
                Collections.unmodifiableMap(fields), List.of()));
        return new Heap(Collections.unmodifiableMap(objs));
    }

    private ArrayView snapshotArray() {
        return new ArrayView("@1", round == 0 ? "circle" : "circle/f 表", List.copyOf(view),
                null, null, null, null, view.size(), -1);
    }

    private Trace.TraceMeta meta() {
        return new Trace.TraceMeta(id(), title(), codeResource(),
                String.format("n=%d k=%d", n, k), n + 5);
    }

    @Override
    public java.util.List<String> dataLines() {
        return java.util.List.of(String.format("int n = %d, k = %d;   //@DATA 人数与报数", n, k));
    }

    @Override
    public Complexity complexity() {
        return new Complexity("模拟 O(n²) / 递推 O(n)", "O(n) 存活标记",
                "投票轮询、心跳淘汰、令牌环网——「数到 k 出局」无处不在；"
                        + "同一问题的模拟解和递推解，是暴力与聪明的活教材");
    }
}
