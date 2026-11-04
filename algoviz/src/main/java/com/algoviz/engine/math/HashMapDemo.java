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
import com.algoviz.engine.ds.HashView;
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
 * 哈希表：O(1) 查键 + 冲突链可视化。
 * 桶数 4，h(k) = k % 4；依次 put {12, 7, 20, 3, 15}——15 与 7、3 与 7+4 都撞桶，
 * 链条当场生长。再 get(15)：先算桶 3，沿链逐一比对——"O(1)" 的真相是
 * "桶定位 O(1) + 链比较 O(链长)"，负载因子高了链就长。
 * 不变量：每个键必落在 h(k) 号桶——桶内链保存所有撞车的键。
 */
public final class HashMapDemo implements Algorithm {

    private static final String SCHEMA = "1";
    private static final String INVARIANT =
            "键 k 只能落在 h(k)=k%4 号桶——撞车不慌，桶内链排开；查 = 定位桶 + 走链比对";

    private static final int BUCKETS = 4;
    private final int[] PUT_KEYS;
    private final int QUERY_KEY;

    public HashMapDemo() {
        this(new int[]{12, 7, 20, 3, 15}, 7);
    }

    public HashMapDemo(int[] keys, int query) {
        this.PUT_KEYS = keys.clone();
        this.QUERY_KEY = query;
    }

    /** 与 /code/HashMapDemo.java 的行号对齐 */
    private static final int L_PUT = 5;
    private static final int L_COLLIDE = 6;
    private static final int L_GET = 11;
    private static final int L_HIT = 13;

    private final List<Step> steps = new ArrayList<>();
    /** 链节点：{id(1基), val, bucket}；链序 = 插入序（头插模拟 HashMap）。 */
    private final List<int[]> nodes = new ArrayList<>();
    private final Map<Integer, Integer> nextOf = new LinkedHashMap<>();
    private int nextAlloc = 1;
    private int curBucket = -1;
    private int curNodeId = -1;
    private String phase = "put";

    @Override public String id() { return "math.hashmap"; }
    @Override public String title() { return "哈希表（冲突链）"; }
    @Override public String codeResource() { return "/code/HashMapDemo.java"; }
    @Override public String docResource() { return "/markdown/hashmap.md"; }

    @Override
    public Trace produce() {
        steps.clear();
        nodes.clear();
        nextOf.clear();
        nextAlloc = 1;
        curBucket = curNodeId = -1;
        phase = "put";

        emit(3, List.of(VizEvent.of(VizEvent.EventType.INIT,
                "桶数 = 4，h(k) = k % 4，依次 put " + java.util.Arrays.toString(PUT_KEYS))),
                "桶数组是骨架，链是撞车后的容身之所");
        int collisions = 0;
        for (int k : PUT_KEYS) {
            int b = k % BUCKETS;
            curBucket = b;
            boolean collide = bucketSize(b) > 0;
            if (collide) collisions++;
            int id = nextAlloc++;
            int oldHead = headOf(b);         // 先取旧链头（新节点入列后再取会拿到自己）
            nodes.add(new int[]{id, k, b});
            nextOf.put(id, oldHead);         // 头插：新节点指向旧链头
            emit(collide ? L_COLLIDE : L_PUT,
                    List.of(VizEvent.of(collide ? VizEvent.EventType.COMPARE : VizEvent.EventType.SET,
                            "put(" + k + ")：h=" + k + "%4=" + b + (collide
                                    ? " → 桶" + b + " 已有键 → 撞车！新节点头插进链"
                                    : " → 桶" + b + " 空闲 → 直接落位"),
                            "@" + id)),
                    collide ? "冲突链生长：同桶键串成链——哈希函数不完美，链表来兜底"
                            : "桶空 → 一步到位，这就是 O(1) 的理想情况");
        }

        // ===== get(15)：定位桶 + 走链比对 =====
        phase = "get";
        int q = QUERY_KEY;
        int qb = q % BUCKETS;
        curBucket = qb;
        emit(L_GET, List.of(VizEvent.of(VizEvent.EventType.VISIT,
                "get(" + q + ")：h=" + q + "%4=" + qb + " → 直奔桶" + qb)),
                "定位 O(1)：数组下标直接跳桶——不用扫全表");
        int walk = headOf(qb);
        int cmp = 0;
        while (walk > 0) {
            cmp++;
            int val = valOf(walk);
            curNodeId = walk;
            if (val == q) {
                emit(L_HIT, List.of(VizEvent.of(VizEvent.EventType.MARK_SORTED,
                        "走链第 " + cmp + " 步：@" + walk + " 命中 " + q + "！",
                        "@" + walk)),
                        "链比较 O(链长)——哈希越均匀、负载越低，链越短，越接近真 O(1)");
                break;
            }
            emit(L_HIT, List.of(VizEvent.of(VizEvent.EventType.COMPARE,
                    "走链第 " + cmp + " 步：@" + walk + " val=" + val + " ≠ " + q + " → next",
                    "@" + walk)),
                    "比对失败沿 next 继续——链表遍历，这就是冲突的代价");
            walk = nextOf.getOrDefault(walk, -1);
        }
        curNodeId = -1;
        phase = "完成";
        emit(16, List.of(VizEvent.of(VizEvent.EventType.RETURN,
                "put " + PUT_KEYS.length + " 个键撞车 " + collisions + " 次；get(15) 走链 " + cmp + " 步")),
                "平均链长 = 负载因子 n/桶数——扩容（rehash）就是在压这个值");
        return new Trace(SCHEMA, meta(), steps);
    }

    private int valOf(int id) {
        for (int[] nd : nodes) {
            if (nd[0] == id) return nd[1];
        }
        return -1;
    }

    private int bucketSize(int b) {
        int n = 0;
        for (int[] nd : nodes) {
            if (nd[2] == b) n++;
        }
        return n;
    }

    /** 桶 b 的链头：桶内没被任何 next 指向的节点（头插法 → 最新节点）。 */
    private int headOf(int b) {
        java.util.Set<Integer> referenced = new java.util.HashSet<>();
        for (int[] nd : nodes) {
            if (nd[2] == b) {
                int nx = nextOf.getOrDefault(nd[0], -1);
                if (nx > 0) referenced.add(nx);
            }
        }
        for (int[] nd : nodes) {
            if (nd[2] == b && !referenced.contains(nd[0])) return nd[0];
        }
        return -1;
    }

    private void emit(int line, List<VizEvent> events, String why) {
        Map<String, Value> locals = new LinkedHashMap<>();
        locals.put("bucket", Value.prim(curBucket >= 0 ? String.valueOf(curBucket) : "-"));
        if (curNodeId > 0) {
            locals.put("cur", Value.ref("@" + curNodeId));
        }
        locals.put("h(k)", Value.prim("k % " + BUCKETS));
        StackFrame frame = new StackFrame(phase,
                Collections.unmodifiableMap(locals), null);
        Map<String, Layer> layers = new LinkedHashMap<>();
        layers.put("mem.stack", new StackLayer(List.of(frame)));
        layers.put("mem.heap", new HeapLayer(snapshotHeap()));
        layers.put("ds.hash", snapshotHash());
        steps.add(new Step(steps.size(), Loc.line(line, why),
                Collections.unmodifiableMap(layers), List.copyOf(events), new Intent(why, INVARIANT)));
    }

    private Heap snapshotHeap() {
        Map<String, HeapObject> objs = new LinkedHashMap<>();
        for (int[] nd : nodes) {
            Map<String, Value> fields = new LinkedHashMap<>();
            fields.put("key", Value.prim(nd[1]));
            int nx = nextOf.getOrDefault(nd[0], -1);
            fields.put("next", nx > 0 ? Value.ref("@" + nx) : Value.NULL);
            objs.put("@" + nd[0], new HeapObject("@" + nd[0], "entry", "Entry",
                    Collections.unmodifiableMap(fields), List.of()));
        }
        return new Heap(Collections.unmodifiableMap(objs));
    }

    private HashView snapshotHash() {
        List<HashView.HashNode> out = new ArrayList<>();
        for (int[] nd : nodes) {
            int nx = nextOf.getOrDefault(nd[0], -1);
            out.add(new HashView.HashNode("@" + nd[0], nd[1], nd[2],
                    nx > 0 ? "@" + nx : null));
        }
        return new HashView("HashMap（" + BUCKETS + " 桶）", BUCKETS,
                Collections.unmodifiableList(out));
    }

    private Trace.TraceMeta meta() {
        StringBuilder sb = new StringBuilder();
        for (int k = 0; k < PUT_KEYS.length; k++) {
            if (k > 0) sb.append(',');
            sb.append(PUT_KEYS[k]);
        }
        return new Trace.TraceMeta(id(), title(), codeResource(), sb.toString(), PUT_KEYS.length);
    }

    @Override
    public java.util.List<String> dataLines() {
        return java.util.List.of(
                "int[] keys = " + java.util.Arrays.toString(PUT_KEYS) + ";",
                "int query = " + QUERY_KEY + ";   // " + BUCKETS + " 桶，h(k)=k%" + BUCKETS);
    }

    @Override
    public Complexity complexity() {
        return new Complexity("O(1) 均（桶定位）+ O(链长)（比对）", "O(n) 表 + O(桶数) 槽",
                "负载因子 = n/桶数 = 平均链长——扩容 rehash 把它压回 ~0.75。"
                        + "Java 8 起链长 ≥8 转红黑树，把最坏 O(n) 压到 O(log n)");
    }
}
