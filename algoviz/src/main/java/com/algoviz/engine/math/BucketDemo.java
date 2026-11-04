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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 分桶去重（小内存版位图）：数据范围 [0,1024) 切成 4 个桶各 256 位。
 * 一次全量位图要 1024 bit = 128 字节；分桶每次只装一个 256 位的桶 = 32 字节，
 * 代价是两遍扫描（先分桶、再逐桶去重）——空间换时间的镜像：用时间换空间。
 * 画面：数据条 + 4 个 16×16 位图同屏，当前桶位点亮，重复位橙显。
 * 不变量：桶 b 只装 [b·256, (b+1)·256) 的数——桶内位图不越界、互不干扰。
 */
public final class BucketDemo implements Algorithm {

    private static final String SCHEMA = "1";
    private static final String INVARIANT =
            "n → 桶 b = n >> 8（n/256），桶内位 = n & 255——每个桶只管自己那 256 个数";

    private static final int BUCKETS = 4;
    private static final int BUCKET_BITS = 256;          // 每桶 256 位（16×16）
    private static final int[] DATA = {700, 300, 50, 999, 700, 100, 512, 260};

    private final List<Step> steps = new ArrayList<>();
    private final boolean[][] bits = new boolean[BUCKETS][BUCKET_BITS];
    private final List<Integer> dupNums = new ArrayList<>();
    private int readIdx = -1;
    private int curBucket = -1, curBit = -1;
    private int phase = 0;   // 0=分桶 1=去重

    @Override public String id() { return "math.bucket"; }
    @Override public String title() { return "分桶去重（小内存位图）"; }
    @Override public String codeResource() { return "/code/BucketDemo.java"; }
    @Override public String docResource() { return "/markdown/bucket.md"; }

    @Override
    public Trace produce() {
        reset();
        emit(3, List.of(VizEvent.of(VizEvent.EventType.INIT,
                "范围 [0,1024) 切 4 桶 × 256 位——单桶只占 32 字节")),
                "全量位图 128 字节 vs 分桶 32 字节：内存减 ¾，代价是多一遍分桶扫描");
        // ===== 第一遍：分桶 =====
        phase = 0;
        for (int i = 0; i < DATA.length; i++) {
            readIdx = i;
            int n = DATA[i];
            curBucket = n >> 8;
            curBit = n & 255;
            emit(6, List.of(VizEvent.of(VizEvent.EventType.SET,
                    n + " → 桶" + curBucket + "（位 " + curBit + "）")),
                    "桶号 = n >> 8（整除 256）——一次移位搞定路由");
        }
        // ===== 第二遍：逐桶位图去重 =====
        phase = 1;
        curBucket = curBit = -1;
        for (int i = 0; i < DATA.length; i++) {
            readIdx = i;
            int n = DATA[i];
            curBucket = n >> 8;
            curBit = n & 255;
            if (bits[curBucket][curBit]) {
                dupNums.add(n);
                emit(10, List.of(VizEvent.of(VizEvent.EventType.COMPARE,
                        n + "：桶" + curBucket + " 位" + curBit + " 已是 1 → 重复！")),
                        "位已置 1 = 第二次出现——O(1) 判重，这就是位图的力量");
            } else {
                bits[curBucket][curBit] = true;
                emit(9, List.of(VizEvent.of(VizEvent.EventType.SET,
                        n + "：桶" + curBucket + " 位" + curBit + " ← 1")),
                        "第一次出现——置位记账");
            }
        }
        curBucket = curBit = readIdx = -1;
        emit(13, List.of(VizEvent.of(VizEvent.EventType.RETURN,
                "发现重复：" + dupNums)),
                "两遍 O(n)：分桶一趟 + 判重一趟——外部排序/海量数据去重的通用套路");
        return new Trace(SCHEMA, meta(), steps);
    }

    private void reset() {
        steps.clear();
        dupNums.clear();
        readIdx = -1;
        curBucket = curBit = -1;
        phase = 0;
        for (int b = 0; b < BUCKETS; b++) {
            java.util.Arrays.fill(bits[b], false);
        }
    }

    private void emit(int line, List<VizEvent> events, String why) {
        StackFrame frame = new StackFrame(phase == 0 ? "bucketize" : "dedup",
                OrderedMaps.of("读取", Value.prim(readIdx >= 0 ? "data[" + readIdx + "]=" + DATA[readIdx] : "-"),
                        "桶", Value.prim(curBucket >= 0 ? String.valueOf(curBucket) : "-"),
                        "位", Value.prim(curBit >= 0 ? String.valueOf(curBit) : "-"),
                        "重复", Value.prim(dupNums.toString())), null);
        Map<String, Layer> layers = new LinkedHashMap<>();
        layers.put("mem.stack", new StackLayer(List.of(frame)));
        layers.put("mem.heap", new HeapLayer(snapshotHeap()));
        layers.put("math.matrix", snapshotMatrix());
        steps.add(new Step(steps.size(), Loc.line(line, why),
                Collections.unmodifiableMap(layers), List.copyOf(events), new Intent(why, INVARIANT)));
    }

    private Heap snapshotHeap() {
        Map<String, Value> fields = new LinkedHashMap<>();
        fields.put("阶段", Value.prim(phase == 0 ? "① 分桶路由" : "② 桶内位图判重"));
        fields.put("单桶内存", Value.prim("256 bit = 32 字节"));
        fields.put("重复数", Value.prim(dupNums.toString()));
        Map<String, HeapObject> objs = new LinkedHashMap<>();
        objs.put("@1", new HeapObject("@1", "state", "BucketState",
                Collections.unmodifiableMap(fields), List.of()));
        return new Heap(Collections.unmodifiableMap(objs));
    }

    private MathView.Grid dataGrid() {
        List<String> vals = new ArrayList<>();
        for (int n : DATA) vals.add(String.valueOf(n));
        Set<Integer> active = new LinkedHashSet<>();
        if (readIdx >= 0) active.add(readIdx);
        return new MathView.Grid("data", "data", 1, DATA.length,
                Collections.unmodifiableList(vals), null, null,
                Collections.unmodifiableSet(active), Set.of());
    }

    private MathView snapshotMatrix() {
        List<MathView.Grid> grids = new ArrayList<>();
        grids.add(dataGrid());
        for (int b = 0; b < BUCKETS; b++) {
            List<String> vals = new ArrayList<>();
            Set<Integer> done = new LinkedHashSet<>();
            for (int i = 0; i < BUCKET_BITS; i++) {
                vals.add(bits[b][i] ? "1" : "");
                if (bits[b][i]) done.add(i);
            }
            Set<Integer> active = new LinkedHashSet<>();
            if (b == curBucket && curBit >= 0) {
                active.add(curBit);
                done.remove(curBit);   // 当前位黄显优先
            }
            grids.add(new MathView.Grid("bucket" + b,
                    "桶" + b + " [" + (b * 256) + "," + ((b + 1) * 256 - 1) + "]",
                    16, 16, Collections.unmodifiableList(vals),
                    curBit >= 0 && b == curBucket ? curBit / 16 : null,
                    curBit >= 0 && b == curBucket ? curBit % 16 : null,
                    Collections.unmodifiableSet(active),
                    Collections.unmodifiableSet(done)));
        }
        return new MathView("分桶位图去重", Collections.unmodifiableList(grids));
    }

    private Trace.TraceMeta meta() {
        return new Trace.TraceMeta(id(), title(), codeResource(), "8 数 4 桶", DATA.length);
    }

    @Override
    public java.util.List<String> dataLines() {
        return java.util.List.of("int[] data = " + java.util.Arrays.toString(DATA) + ";");
    }

    @Override
    public Complexity complexity() {
        return new Complexity("O(n) 两遍线性扫", "O(范围/8 / 桶数) 单桶 32 字节",
                "全量位图吃不下时按高位分桶、逐桶处理——海量数据去重/排序（桶排/外部排）的标准套路");
    }
}
