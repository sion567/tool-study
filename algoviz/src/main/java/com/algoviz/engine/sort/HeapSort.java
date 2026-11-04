package com.algoviz.engine.sort;

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
 * 堆排序生产者：先建大顶堆，再逐个弹出堆顶放尾部。
 * 画面：数组视图 + 堆面板显示父子关系（i 的孩子 = 2i+1 / 2i+2）。
 * 不变量：堆化后 a[i] ≥ 它的两个孩子——堆顶永远是最大值。
 */
public final class HeapSort implements Algorithm {

    private static final String SCHEMA = "1";
    private static final String INVARIANT =
            "堆化后 a[i] ≥ a[2i+1] 且 a[i] ≥ a[2i+2]——堆顶 a[0] 永远是当前最大值";

    /** 与 /code/HeapSort.java 的行号对齐 */
    private static final int L_BUILD = 3;
    private static final int L_SIFTDOWN = 4;
    private static final int L_SORT_LOOP = 6;
    private static final int L_SWAP_ROOT = 7;
    private static final int L_FIX = 8;
    private static final int L_END = 10;

    private final int[] input;
    private int[] a;
    private int heapSize;
    private final List<Step> steps = new ArrayList<>();
    private int curIdx = -1;

    public HeapSort(int[] input) {
        this.input = input.clone();
    }

    @Override public String id() { return "sort.heap"; }
    @Override public String title() { return "堆排序"; }
    @Override public String codeResource() { return "/code/HeapSort.java"; }
    @Override public String docResource() { return "/markdown/heap-sort.md"; }

    @Override
    public Trace produce() {
        a = input.clone();
        heapSize = a.length;
        steps.clear();

        // L3-L4: 建堆（从最后一个非叶子节点开始，自底向上堆化）
        emit(L_BUILD, List.of(VizEvent.of(VizEvent.EventType.INIT,
                "建堆：从最后一个非叶子节点 " + (a.length / 2 - 1) + " 开始，自底向上堆化")),
                "叶子节点天然是合法堆——只需堆化每个非叶子节点");
        for (int i = a.length / 2 - 1; i >= 0; i--) {
            curIdx = i;
            siftDown(i, a.length);
        }
        emit(L_BUILD, List.of(VizEvent.of(VizEvent.EventType.MARK_SORTED,
                "大顶堆建好：a[0] = " + a[0] + " 是最大值", "a:0")),
                "堆顶 = 最大值，这就是堆排序的全部秘密");

        // L6-L8: 排序：堆顶换到尾部，堆缩小，重新堆化
        for (int end = a.length - 1; end > 0; end--) {
            curIdx = 0;
            int tmp = a[0];
            a[0] = a[end];
            a[end] = tmp;
            emit(L_SWAP_ROOT, List.of(VizEvent.of(VizEvent.EventType.SWAP,
                    "swap a[0] ↔ a[" + end + "]：最大值 " + a[end] + " 归位", "a:0", "a:" + end)),
                    "堆顶（最大值）放到末尾——它再也不动");
            heapSize = end;
            emit(L_FIX, List.of(VizEvent.of(VizEvent.EventType.SET,
                    "堆缩小到 " + end + " 个元素，重新堆化 a[0]")),
                    "新堆顶可能不合法——siftDown 把它沉到正确位置");
            siftDown(0, end);
        }
        curIdx = -1;
        emit(L_END, List.of(VizEvent.of(VizEvent.EventType.RETURN,
                "排序完成：全部就位")), "原地排序，无辅助数组");
        return new Trace(SCHEMA, meta(), steps);
    }

    /** 下沉：把 a[i] 沉到正确位置（与较大的孩子交换直到合法）。 */
    private void siftDown(int i, int size) {
        while (true) {
            int largest = i;
            int l = 2 * i + 1, r = 2 * i + 2;
            if (l < size && a[l] > a[largest]) largest = l;
            if (r < size && a[r] > a[largest]) largest = r;
            if (largest == i) break;
            int t = a[i]; a[i] = a[largest]; a[largest] = t;
            emit(L_SIFTDOWN, List.of(VizEvent.of(VizEvent.EventType.SWAP,
                    "swap a[" + i + "] ↔ a[" + largest + "]（下沉）",
                    "a:" + i, "a:" + largest)),
                    "父节点比孩子小 → 与较大孩子交换，继续下沉");
            i = largest;
        }
    }

    private void emit(int line, List<VizEvent> events, String why) {
        Map<String, Value> lm = new LinkedHashMap<>();
        lm.put("a", Value.ref("@1"));
        lm.put("heapSize", Value.prim(heapSize));
        StackFrame frame = new StackFrame("heapSort", Collections.unmodifiableMap(lm), null);
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
        for (int v : a) elems.add(Value.prim(v));
        objs.put("@1", new HeapObject("@1", "a", "int[" + a.length + "]",
                new LinkedHashMap<>(), List.copyOf(elems)));
        return new Heap(Collections.unmodifiableMap(objs));
    }

    private ArrayView snapshotArray() {
        List<Integer> vals = new ArrayList<>();
        for (int v : a) vals.add(v);
        return new ArrayView("@1", "a", List.copyOf(vals),
                curIdx >= 0 ? curIdx : null, null, null, null, heapSize, -1);
    }

    private Trace.TraceMeta meta() {
        StringBuilder sb = new StringBuilder();
        for (int k = 0; k < input.length; k++) {
            if (k > 0) sb.append(',');
            sb.append(input[k]);
        }
        return new Trace.TraceMeta(id(), title(), codeResource(), sb.toString(), input.length);
    }

    @Override
    public java.util.List<String> dataLines() {
        return java.util.List.of(Algorithm.joinArray(input));
    }

    @Override
    public Complexity complexity() {
        return new Complexity("O(n log n) 最坏也稳", "O(1) 原地",
                "建堆 O(n)，之后 n-1 次「取堆顶+下沉」各 O(log n) → n log n。最坏情况也是 n log n（比快排稳），原地 O(1)");
    }
}
