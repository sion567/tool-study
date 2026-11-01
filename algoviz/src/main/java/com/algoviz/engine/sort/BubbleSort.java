package com.algoviz.engine.sort;

import com.algoviz.engine.Algorithm;
import com.algoviz.engine.Value;
import com.algoviz.engine.VizEvent;
import com.algoviz.engine.trace.Trace;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 冒泡排序生产者：逐行吐 Step（蓝图 §2「当 JVM 去执行」）。
 * 行号常量必须与 /code/BubbleSort.java 对齐——改那份代码要同步这里（golden 测试会拦）。
 */
public final class BubbleSort implements Algorithm {

    private static final String SCHEMA = "1";
    private static final String INVARIANT = "每轮结束后，尾部 i 个元素已就位且有序";

    /** 与 /code/BubbleSort.java 的行号对齐 */
    private static final int L_FOR_I = 3;
    private static final int L_FOR_J = 4;
    private static final int L_IF = 5;
    private static final int L_T = 6;
    private static final int L_AJ = 7;
    private static final int L_AJP = 8;
    private static final int L_END = 12;

    private final int[] input;

    public BubbleSort(int[] input) {
        this.input = input.clone();
    }

    @Override
    public String id() {
        return "sort.bubble";
    }

    @Override
    public String title() {
        return "冒泡排序";
    }

    @Override
    public String codeResource() {
        return "/code/BubbleSort.java";
    }

    @Override
    public String docResource() {
        return "/markdown/bubble-sort.md";
    }

    @Override
    public Trace produce() {
        StepEmitter em = new StepEmitter(input.clone(), INVARIANT);
        int[] a = em.a;
        int n = em.n;

        if (n <= 1) {
            em.sortedFrom = 0;
            em.emit(L_END, locals(null, null, null),
                    List.of(VizEvent.of(VizEvent.EventType.RETURN, "n <= 1，天然有序")),
                    em.view(null, null, null, null),
                    "长度 <= 1，天然有序");
            return new Trace(SCHEMA, meta(input), em.steps);
        }

        // L3: int i = 0（首个外层步兼作条件成立说明）
        em.emit(L_FOR_I, locals(0, null, null),
                List.of(VizEvent.of(VizEvent.EventType.INIT, "i = 0，第 1 轮开始", "i")),
                em.view(null, null, null, null),
                "i = 0：第 1 轮开始（i < " + (n - 1) + " 成立）");

        for (int i = 0; i < n - 1; i++) {
            if (i > 0) {
                em.emit(L_FOR_I, locals(i, null, null), List.of(),
                        em.view(null, null, null, null),
                        "i++：尾部已就位 " + i + " 个，进入第 " + (i + 1) + " 轮（i < " + (n - 1) + " 成立）");
            }
            for (int j = 0; j < n - 1 - i; j++) {
                if (j > 0) {
                    em.emit(L_FOR_J, locals(i, j, null), List.of(),
                            em.view(null, null, null, null),
                            "j++：窗口右移，接下来比较 a[" + j + "] 与 a[" + (j + 1) + "]");
                } else {
                    em.emit(L_FOR_J, locals(i, j, null),
                            List.of(VizEvent.of(VizEvent.EventType.INIT, "j = 0", "j")),
                            em.view(null, null, null, null),
                            "j = 0：从队头开始两两比较（上界 n-1-i = " + (n - 1 - i) + "，尾部已就位的不比）");
                }
                // L5: if 比较
                em.emit(L_IF, locals(i, j, null),
                        List.of(VizEvent.of(VizEvent.EventType.COMPARE,
                                "a[" + j + "]=" + a[j] + " vs a[" + (j + 1) + "]=" + a[j + 1]
                                        + (a[j] > a[j + 1] ? " → 交换" : " → 不动"),
                                "a[" + j + "]", "a[" + (j + 1) + "]")),
                        em.view(j, j + 1, null, null),
                        "相邻比较：a[" + j + "] 和 a[" + (j + 1) + "] 谁大？");
                if (a[j] > a[j + 1]) {
                    int t = a[j];
                    // L6: int t = a[j]
                    em.emit(L_T, locals(i, j, t),
                            List.of(VizEvent.of(VizEvent.EventType.SET, "t ← a[" + j + "] = " + t, "t")),
                            em.view(j, j + 1, null, null),
                            "t = a[j]：先记住，否则 a[j] 被覆盖就丢了");
                    // L7: a[j] = a[j+1]
                    a[j] = a[j + 1];
                    em.emit(L_AJ, locals(i, j, t),
                            List.of(VizEvent.of(VizEvent.EventType.SWAP,
                                    "a[" + j + "] ← a[" + (j + 1) + "] = " + a[j],
                                    "a[" + j + "]", "a[" + (j + 1) + "]")),
                            em.view(null, null, j, j + 1),
                            "a[j] ← a[j+1]：小的左移（此刻数组里有两个 " + a[j] + "——这是真实的执行中间态）");
                    // L8: a[j+1] = t
                    a[j + 1] = t;
                    em.emit(L_AJP, locals(i, j, t),
                            List.of(VizEvent.of(VizEvent.EventType.SET,
                                    "a[" + (j + 1) + "] ← t = " + t, "a[" + (j + 1) + "]")),
                            em.view(null, null, j, j + 1),
                            "a[j+1] ← t：大的右移，交换完成");
                }
            }
            // 内层结束：本轮最大值就位（不变量达成时刻）
            em.sortedFrom = n - 1 - i;
            em.emit(L_FOR_J, locals(i, null, null),
                    List.of(VizEvent.of(VizEvent.EventType.MARK_SORTED,
                            "a[" + (n - 1 - i) + "] = " + a[n - 1 - i] + " 已就位",
                            "a[" + (n - 1 - i) + "]")),
                    em.view(null, null, null, null),
                    "本轮结束：j = n-1-i 不成立 → 最大值已冒泡到 a[" + (n - 1 - i) + "]");
        }

        // 外层结束 + 返回
        em.emit(L_FOR_I, locals(n - 1, null, null), List.of(),
                em.view(null, null, null, null),
                "i = " + (n - 1) + "：i < " + (n - 1) + " 不成立，外层结束");
        em.sortedFrom = 0;
        em.emit(L_END, locals(n - 1, null, null),
                List.of(VizEvent.of(VizEvent.EventType.RETURN, "排序完成，全部就位")),
                em.view(null, null, null, null),
                "排序完成：全部 " + n + " 个元素已就位");

        return new Trace(SCHEMA, meta(input), em.steps);
    }

    private static Map<String, Value> locals(Integer i, Integer j, Integer t) {
        Map<String, Value> m = new LinkedHashMap<>();
        m.put("a", Value.ref("@1"));
        if (i != null) m.put("i", Value.prim(i));
        if (j != null) m.put("j", Value.prim(j));
        if (t != null) m.put("t", Value.prim(t));
        return m;
    }
}
