package com.algoviz.engine.math;

import com.algoviz.engine.Algorithm;
import com.algoviz.engine.Intent;
import com.algoviz.engine.Layer;
import com.algoviz.engine.Loc;
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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 位图判重演示（空间优化）：一个 bit 记一个数见没见过——白 = 没见过，黑 = 见过，红闪 = 重复。
 * 不变量：bit[x] = 1 ⟺ 数字 x 出现过——查重从 O(n²) 两两比，变成 O(1) 的一次下标。
 */
public final class BitmapDemo implements Algorithm {

    private static final String SCHEMA = "1";
    private static final String INVARIANT =
            "bit[x] = 1 ⟺ x 出现过——查重从两两比较 O(n²) 变成一次下标访问 O(1)，代价是 bit 的空间";

    /** 与 /code/BitmapDemo.java 的行号对齐 */
    private static final int L_INIT = 3;
    private static final int L_READ = 5;
    private static final int L_CHECK = 6;
    private static final int L_DUP = 7;
    private static final int L_SET = 10;

    private static final int BITS = 8;
    private static final int[] DATA = {3, 7, 2, 7, 5, 1};

    private final List<Step> steps = new ArrayList<>();
    private final Set<Integer> ones = new LinkedHashSet<>();
    private int readIndex = -1;
    private Integer dupBit;

    @Override public String id() { return "math.bitmap"; }
    @Override public String title() { return "位图判重"; }
    @Override public String codeResource() { return "/code/BitmapDemo.java"; }
    @Override public String docResource() { return "/markdown/bitmap-dedup.md"; }

    @Override
    public Trace produce() {
        ones.clear();
        readIndex = -1;
        dupBit = null;

        emit(L_INIT, List.of(VizEvent.of(VizEvent.EventType.INIT,
                "bits = 0（" + BITS + " 个格子全白）——还没读任何数")),
                "一个 long 就是 64 个格子：空间优化的本体是「用 bit 当记录本」");
        for (int i = 0; i < DATA.length; i++) {
            int x = DATA[i];
            readIndex = i;
            emit(L_READ, List.of(VizEvent.of(VizEvent.EventType.INIT,
                    "读取 data[" + i + "] = " + x, "data:" + i)),
                    "逐个读数——每读一个，问一句「见过吗」");
            if (ones.contains(x)) {
                dupBit = x;
                emit(L_CHECK, List.of(VizEvent.of(VizEvent.EventType.COMPARE,
                        "bit[" + x + "] = 1 → 见过！", "bit:" + x)),
                        "第二次遇到同一个数：格子上已经有记录");
                emit(L_DUP, List.of(VizEvent.of(VizEvent.EventType.MARK_SORTED,
                        "重复！" + x + " 在 data[" + i + "] 再次出现（格子红闪）", "bit:" + x)),
                        "O(1) 判重完成——两两比较要 9 次，这里 1 次");
                return new Trace(SCHEMA, meta(), steps);
            }
            emit(L_CHECK, List.of(VizEvent.of(VizEvent.EventType.COMPARE,
                    "bit[" + x + "] = 0 → 没见过", "bit:" + x)),
                    "第一次遇到 " + x + "：格子还是白的");
            ones.add(x);
            emit(L_SET, List.of(VizEvent.of(VizEvent.EventType.SET,
                    "bit[" + x + "] ← 1（标记为「见过」）", "bit:" + x)),
                    "标记成本：一次置位——读完全部也只用 8 个 bit");
        }
        emit(L_SET, List.of(VizEvent.of(VizEvent.EventType.RETURN,
                "全部读完：无重复（本例必在 data[3] 命中，不会走到这里）")),
                "读完无重复");
        return new Trace(SCHEMA, meta(), steps);
    }

    private void emit(int line, List<VizEvent> events, String why) {
        StackFrame frame = new StackFrame("dedup", Map.of("bits", Value.prim(bitsStr())), null);
        Map<String, Layer> layers = new LinkedHashMap<>();
        layers.put("mem.stack", new StackLayer(List.of(frame)));
        layers.put("mem.heap", new HeapLayer(snapshotHeap()));
        layers.put("math.bitmap", snapshotBitmap());
        steps.add(new Step(steps.size(), Loc.line(line, why),
                Collections.unmodifiableMap(layers), List.copyOf(events), new Intent(why, INVARIANT)));
    }

    private String bitsStr() {
        StringBuilder sb = new StringBuilder("0b");
        for (int b = BITS - 1; b >= 0; b--) {
            sb.append(ones.contains(b) ? 1 : 0);
        }
        return sb.toString();
    }

    private Heap snapshotHeap() {
        Map<String, Value> fields = new LinkedHashMap<>();
        fields.put("bits", Value.prim(bitsStr()));
        for (int b = 0; b < BITS; b++) {
            fields.put("b" + b, Value.prim(ones.contains(b) ? 1 : 0));
        }
        HeapObject table = new HeapObject("@1", "bits", "BitSet(" + BITS + "b)",
                Collections.unmodifiableMap(fields), List.of());
        Map<String, HeapObject> objs = new LinkedHashMap<>();
        objs.put("@1", table);
        return new Heap(Collections.unmodifiableMap(objs));
    }

    private MathView snapshotBitmap() {
        return new MathView("位图判重", List.of(
                new MathView.Grid("bits", "BitSet " + BITS + "b", 1, BITS,
                        cells(), null, null, Set.of(), Set.of())));
    }

    private List<String> cells() {
        List<String> out = new ArrayList<>();
        for (int b = 0; b < BITS; b++) {
            out.add(String.valueOf(ones.contains(b) ? 1 : 0));
        }
        return out;
    }

    private Trace.TraceMeta meta() {
        StringBuilder sb = new StringBuilder();
        for (int k = 0; k < DATA.length; k++) {
            if (k > 0) sb.append(',');
            sb.append(DATA[k]);
        }
        return new Trace.TraceMeta(id(), title(), codeResource(), sb.toString(), DATA.length);
    }
}
