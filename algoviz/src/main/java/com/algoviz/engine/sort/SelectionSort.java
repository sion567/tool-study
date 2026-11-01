package com.algoviz.engine.sort;

import com.algoviz.engine.Algorithm;
import com.algoviz.engine.Value;
import com.algoviz.engine.VizEvent;
import com.algoviz.engine.trace.Trace;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 选择排序生产者：逐行吐 Step。
 * 行号常量必须与 /code/SelectionSort.java 对齐（golden 测试会拦不同步）。
 * 画面核心：每一轮从无序区「挑最小的」，跟队头交换——橙 = 挑战者 vs 卫冕最小值，红 = 交换对。
 */
public final class SelectionSort implements Algorithm {

    private static final String SCHEMA = "1";
    private static final String INVARIANT = "每轮结束后，前 i+1 个位置是全数组最小的 i+1 个，且有序";

    /** 与 /code/SelectionSort.java 的行号对齐 */
    private static final int L_FOR_I = 3;
    private static final int L_MIN = 4;
    private static final int L_FOR_J = 5;
    private static final int L_IF = 6;
    private static final int L_MINJ = 7;
    private static final int L_T = 10;
    private static final int L_AI = 11;
    private static final int L_AMIN = 12;
    private static final int L_END = 14;

    private final int[] input;

    public SelectionSort(int[] input) {
        this.input = input.clone();
    }

    @Override
    public String id() {
        return "sort.selection";
    }

    @Override
    public String title() {
        return "选择排序";
    }

    @Override
    public String codeResource() {
        return "/code/SelectionSort.java";
    }

    @Override
    public String docResource() {
        return "/markdown/selection-sort.md";
    }

    @Override
    public Trace produce() {
        StepEmitter em = new StepEmitter(input.clone(), INVARIANT);
        int[] a = em.a;
        int n = em.n;

        if (n <= 1) {
            em.sortedTo = n - 1;
            em.emit(L_END, locals(null, null, null, null),
                    List.of(VizEvent.of(VizEvent.EventType.RETURN, "n <= 1，天然有序")),
                    em.view(null, null, null, null),
                    "长度 <= 1，天然有序");
            return new Trace(SCHEMA, meta(input), em.steps);
        }

        for (int i = 0; i < n - 1; i++) {
            em.sortedTo = i - 1;
            if (i == 0) {
                em.emit(L_FOR_I, locals(0, null, null, null),
                        List.of(VizEvent.of(VizEvent.EventType.INIT, "i = 0，第 1 轮开始", "i")),
                        em.view(null, null, null, null),
                        "i = 0：第 1 轮开始——从一堆里挑最小的，放到队头");
            } else {
                em.emit(L_FOR_I, locals(i, null, null, null), List.of(),
                        em.view(null, null, null, null),
                        "i++：前 " + i + " 个已就位，进入第 " + (i + 1) + " 轮");
            }
            int min = i;
            em.emit(L_MIN, locals(i, min, null, null),
                    List.of(VizEvent.of(VizEvent.EventType.INIT, "min ← i = " + i, "min")),
                    em.view(null, null, null, null),
                    "min = i：先假设 a[i] 就是最小值（卫冕者）");
            for (int j = i + 1; j < n; j++) {
                em.emit(L_FOR_J, locals(i, min, j, null),
                        j == i + 1
                                ? List.of(VizEvent.of(VizEvent.EventType.INIT, "j = i+1，开始扫描", "j"))
                                : List.of(),
                        em.view(null, null, null, null),
                        j == i + 1 ? "j = " + j + "：从 i+1 开始向右扫描" : "j++：继续扫描");
                em.emit(L_IF, locals(i, min, j, null),
                        List.of(VizEvent.of(VizEvent.EventType.COMPARE,
                                "a[" + j + "]=" + a[j] + " vs a[min]=" + a[min]
                                        + (a[j] < a[min] ? " → a[j] 更小" : " → min 不变"),
                                "a[" + j + "]", "a[min]")),
                        em.view(j, min, null, null),
                        "a[j] < a[min]？——挑战者 vs 卫冕最小值");
                if (a[j] < a[min]) {
                    min = j;
                    em.emit(L_MINJ, locals(i, min, j, null),
                            List.of(VizEvent.of(VizEvent.EventType.SET,
                                    "min ← j = " + j + "（新最小值 a[j]=" + a[j] + "）", "min")),
                            em.view(null, null, null, null),
                            "min ← j：卫冕失败，新最小值登场");
                }
            }
            int t = a[i];
            em.emit(L_T, locals(i, min, null, t),
                    List.of(VizEvent.of(VizEvent.EventType.SET, "t ← a[i] = " + t, "t")),
                    em.view(null, null, null, null),
                    "t = a[i]：把要换出去的先记住");
            a[i] = a[min];
            em.emit(L_AI, locals(i, min, null, t),
                    List.of(VizEvent.of(VizEvent.EventType.SWAP,
                            "a[i] ← a[min] = " + a[i], "a[i]", "a[min]")),
                    em.view(null, null, i, min),
                    "a[i] ← a[min]：最小的归位（注意如果 min==i，这是和自己换）");
            a[min] = t;
            em.emit(L_AMIN, locals(i, min, null, t),
                    List.of(VizEvent.of(VizEvent.EventType.SET, "a[min] ← t = " + t, "a[min]")),
                    em.view(null, null, i, min),
                    "a[min] ← t：换出值补位，交换完成");
            em.sortedTo = i;
            em.emit(L_FOR_I, locals(i, min, null, t),
                    List.of(VizEvent.of(VizEvent.EventType.MARK_SORTED,
                            "a[" + i + "] = " + a[i] + " 已就位", "a[" + i + "]")),
                    em.view(null, null, null, null),
                    "第 " + (i + 1) + " 轮结束：a[" + i + "] 归位（不变量达成）");
        }

        em.emit(L_FOR_I, locals(n - 1, null, null, null), List.of(),
                em.view(null, null, null, null),
                "i = " + (n - 1) + "：只剩最后一个，天然就位");
        em.sortedTo = n - 1;
        em.emit(L_END, locals(n - 1, null, null, null),
                List.of(VizEvent.of(VizEvent.EventType.RETURN, "排序完成，全部就位")),
                em.view(null, null, null, null),
                "排序完成：全部 " + n + " 个元素已就位");

        return new Trace(SCHEMA, meta(input), em.steps);
    }

    private static Map<String, Value> locals(Integer i, Integer min, Integer j, Integer t) {
        Map<String, Value> m = new LinkedHashMap<>();
        m.put("a", Value.ref("@1"));
        if (i != null) m.put("i", Value.prim(i));
        if (min != null) m.put("min", Value.prim(min));
        if (j != null) m.put("j", Value.prim(j));
        if (t != null) m.put("t", Value.prim(t));
        return m;
    }
}
