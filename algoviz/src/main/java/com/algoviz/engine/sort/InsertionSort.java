package com.algoviz.engine.sort;

import com.algoviz.engine.Algorithm;
import com.algoviz.engine.Value;
import com.algoviz.engine.VizEvent;
import com.algoviz.engine.trace.Trace;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 插入排序生产者：逐行吐 Step。
 * 行号常量必须与 /code/InsertionSort.java 对齐（golden 测试会拦不同步）。
 * 画面核心：摸扑克牌——key 是抽出来的新牌，手牌区逐个右移腾位，新牌插进正确位置。
 */
public final class InsertionSort implements Algorithm {

    private static final String SCHEMA = "1";
    private static final String INVARIANT = "手牌区 [0..i-1] 始终有序——每张新牌（key）都插进正确位置";

    /** 与 /code/InsertionSort.java 的行号对齐 */
    private static final int L_FOR_I = 3;
    private static final int L_KEY = 4;
    private static final int L_J = 5;
    private static final int L_WHILE = 6;
    private static final int L_SHIFT = 7;
    private static final int L_JDEC = 8;
    private static final int L_PLACE = 10;
    private static final int L_END = 12;

    private final int[] input;

    public InsertionSort(int[] input) {
        this.input = input.clone();
    }

    @Override
    public String id() {
        return "sort.insertion";
    }

    @Override
    public String title() {
        return "插入排序";
    }

    @Override
    public String codeResource() {
        return "/code/InsertionSort.java";
    }

    @Override
    public String docResource() {
        return "/markdown/insertion-sort.md";
    }

    @Override
    public Trace produce() {
        StepEmitter em = new StepEmitter(input.clone(), INVARIANT);
        int[] a = em.a;
        int n = em.n;

        if (n <= 1) {
            em.sortedTo = n - 1;
            em.emit(L_END, locals(null, null, null),
                    List.of(VizEvent.of(VizEvent.EventType.RETURN, "n <= 1，天然有序")),
                    em.view(null, null, null, null),
                    "长度 <= 1，天然有序");
            return new Trace(SCHEMA, meta(input), em.steps);
        }

        em.emit(L_FOR_I, locals(1, null, null),
                List.of(VizEvent.of(VizEvent.EventType.INIT, "i = 1，第 2 张牌开始摸", "i")),
                em.view(null, null, null, null),
                "i = 1：第 1 张天然有序，从第 2 张开始摸牌");

        for (int i = 1; i < n; i++) {
            em.sortedTo = i - 1;
            if (i > 1) {
                em.emit(L_FOR_I, locals(i, null, null), List.of(),
                        em.view(null, null, null, null),
                        "i++：手牌区 [0.." + (i - 1) + "] 有序，摸下一张 a[" + i + "]");
            }
            int key = a[i];
            em.emit(L_KEY, locals(i, key, null),
                    List.of(VizEvent.of(VizEvent.EventType.SET, "key ← a[i] = " + key, "key")),
                    em.view(null, null, null, null),
                    "key = a[i]：把新牌抽出来（原位置待会儿会被腾出来）");
            int j = i - 1;
            em.emit(L_J, locals(i, key, j),
                    List.of(VizEvent.of(VizEvent.EventType.INIT, "j ← i-1 = " + j, "j")),
                    em.view(null, null, null, null),
                    "j = i-1：从手牌最右边开始往左找插入点");
            while (j >= 0 && a[j] > key) {
                em.emit(L_WHILE, locals(i, key, j),
                        List.of(VizEvent.of(VizEvent.EventType.COMPARE,
                                "a[" + j + "]=" + a[j] + " > key=" + key + " → 右移腾位",
                                "a[" + j + "]")),
                        em.view(j, i, null, null),
                        "a[j] > key？——比新牌大的牌要往右挪一格");
                a[j + 1] = a[j];
                em.emit(L_SHIFT, locals(i, key, j),
                        List.of(VizEvent.of(VizEvent.EventType.SWAP,
                                "a[" + (j + 1) + "] ← a[" + j + "] = " + a[j],
                                "a[" + j + "]", "a[" + (j + 1) + "]")),
                        em.view(null, null, j, j + 1),
                        "a[j+1] ← a[j]：大牌右移（此刻有两张 " + a[j] + "——真实的执行中间态）");
                j--;
                em.emit(L_JDEC, locals(i, key, j), List.of(),
                        em.view(null, null, null, null),
                        "j--：继续往左找插入点");
            }
            // while 结束：两种停法
            List<VizEvent> stopEvents = j >= 0
                    ? List.of(VizEvent.of(VizEvent.EventType.COMPARE,
                            "a[" + j + "]=" + a[j] + " ≤ key=" + key + " → 停", "a[" + j + "]"))
                    : List.of(VizEvent.of(VizEvent.EventType.COMPARE,
                            "j = -1 → 到头了", "j"));
            String stopWhy = j >= 0
                    ? "a[j] ≤ key：找到插入点 j+1 = " + (j + 1)
                    : "新牌比手牌都小：插在最前面 j+1 = 0";
            em.emit(L_WHILE, locals(i, key, j), stopEvents,
                    em.view(j >= 0 ? j : null, i, null, null), stopWhy);
            a[j + 1] = key;
            em.sortedTo = i;
            em.emit(L_PLACE, locals(i, key, j),
                    List.of(VizEvent.of(VizEvent.EventType.SET,
                            "a[" + (j + 1) + "] ← key = " + key, "a[" + (j + 1) + "]")),
                    em.view(null, null, j + 1, null),
                    "a[j+1] ← key：新牌插入，手牌区又多一张（不变量达成）");
        }

        em.emit(L_FOR_I, locals(n, null, null), List.of(),
                em.view(null, null, null, null),
                "i = n：全部摸完");
        em.sortedTo = n - 1;
        em.emit(L_END, locals(n, null, null),
                List.of(VizEvent.of(VizEvent.EventType.RETURN, "排序完成，全部就位")),
                em.view(null, null, null, null),
                "排序完成：全部 " + n + " 个元素已就位");

        return new Trace(SCHEMA, meta(input), em.steps);
    }

    private static Map<String, Value> locals(Integer i, Integer key, Integer j) {
        Map<String, Value> m = new LinkedHashMap<>();
        m.put("a", Value.ref("@1"));
        if (i != null) m.put("i", Value.prim(i));
        if (key != null) m.put("key", Value.prim(key));
        if (j != null) m.put("j", Value.prim(j));
        return m;
    }
}
