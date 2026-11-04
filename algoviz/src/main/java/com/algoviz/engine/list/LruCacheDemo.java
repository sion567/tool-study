package com.algoviz.engine.list;

import com.algoviz.engine.OrderedMaps;
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
 * LRU 缓存演示：get 触发提升到头部，put 超容量触发淘汰尾部。
 * 画面：双向链表节点从新到旧排列，每次 get/put 把节点移到头部，满了踢掉尾部。
 * 不变量：头部 = 最近使用，尾部 = 最久未用——淘汰永远发生在尾部。
 */
public final class LruCacheDemo implements Algorithm {

    private static final String SCHEMA = "1";
    private static final String INVARIANT =
            "头部 = 最近使用，尾部 = 最久未用——get/put 都把节点移到头部，满了淘汰尾部";

    private static final int CAPACITY = 3;
    private static final String[] OPS = {"put:1=A", "put:2=B", "get:1", "put:3=C", "put:4=D"};

    private final List<Step> steps = new ArrayList<>();
    private final List<String[]> entries = new ArrayList<>();   // {key, value}
    private final List<int[]> order = new ArrayList<>();        // {id} 头到尾
    private int opIdx;
    private int nextId;
    private String curOp, curAction, curDetail;

    @Override public String id() { return "list.lru"; }
    @Override public String title() { return "LRU 缓存"; }
    @Override public String codeResource() { return "/code/LruCacheDemo.java"; }
    @Override public String docResource() { return "/markdown/lru-cache.md"; }

    @Override
    public Trace produce() {
        entries.clear(); order.clear(); nextId = 0; opIdx = 0;
        emit(2, List.of(VizEvent.of(VizEvent.EventType.INIT,
                "LRU 缓存初始化：容量 " + CAPACITY + "，链表空")),
                "容量 " + CAPACITY + "：超过就淘汰最久未用的（尾部）");
        for (String op : OPS) {
            curOp = op; opIdx++;
            executeOp(op);
        }
        emit(10, List.of(VizEvent.of(VizEvent.EventType.RETURN,
                "最终缓存状态：" + currentListStr())),
                "LRU 演示完成：看头部（最近用）和尾部（最久未用→被淘汰）");
        return new Trace(SCHEMA, meta(), steps);
    }

    private void executeOp(String op) {
        String[] parts = op.split(":");
        String action = parts[0];
        String arg = parts.length > 1 ? parts[1] : "";
        if (action.equals("get")) {
            // find in entries
            int found = -1;
            for (int i = 0; i < entries.size(); i++) {
                if (entries.get(i)[0].equals(arg)) { found = i; break; }
            }
            if (found >= 0) {
                moveToHead(found);
                emit(4, List.of(VizEvent.of(VizEvent.EventType.SET,
                        "get(" + arg + ") → 命中！移到头部", "@" + (found + 1))),
                        "get 命中：节点移到头部（变成最近使用）");
            } else {
                emit(4, List.of(VizEvent.of(VizEvent.EventType.COMPARE,
                        "get(" + arg + ") → 未命中")),
                        "get 未命中：返回 null，缓存不变");
            }
        } else if (action.equals("put")) {
            // check if key exists
            int existing = -1;
            for (int i = 0; i < entries.size(); i++) {
                if (entries.get(i)[0].equals(arg)) { existing = i; break; }
            }
            if (existing >= 0) {
                moveToHead(existing);
                emit(5, List.of(VizEvent.of(VizEvent.EventType.SET,
                        "put(" + arg + ") → 已存在，移到头部", "@" + (existing + 1))),
                        "已存在的 key：更新值 + 移到头部");
            } else {
                if (entries.size() >= CAPACITY) {
                    int tail = entries.size() - 1;
                    String evicted = entries.get(tail)[0];
                    entries.remove(tail);
                    emit(5, List.of(VizEvent.of(VizEvent.EventType.SET,
                            "淘汰尾部：" + evicted + "（最久未用）", "@" + (tail + 1))),
                            "容量已满 → 淘汰尾部（最久未用的节点）");
                }
                String id = "k" + nextId;
                entries.add(new String[]{id, arg});
                nextId++;
                emit(5, List.of(VizEvent.of(VizEvent.EventType.SET,
                        "put(" + arg + ") → 新节点加入头部", "@" + nextId)),
                        "新节点加入头部——变成最近使用的");
            }
        }
    }

    private void moveToHead(int idx) {
        String[] entry = entries.remove(idx);
        entries.add(0, entry);
    }

    private String currentListStr() {
        StringBuilder sb = new StringBuilder("[头] ");
        for (String[] e : entries) sb.append(e[0]).append("=").append(e[1]).append(" → ");
        return sb.append("[尾]").toString();
    }

    private void emit(int line, List<VizEvent> events, String why) {
        StackFrame frame = new StackFrame("lru", OrderedMaps.of(
                "op", Value.prim(curOp),
                "capacity", Value.prim(CAPACITY)), null);
        Map<String, Layer> layers = new LinkedHashMap<>();
        layers.put("mem.stack", new StackLayer(List.of(frame)));
        layers.put("mem.heap", new HeapLayer(snapshotHeap()));
        layers.put("ds.array", snapshotArray());
        steps.add(new Step(steps.size(), Loc.line(line, why),
                Collections.unmodifiableMap(layers), List.copyOf(events), new Intent(why, INVARIANT)));
    }

    private Heap snapshotHeap() {
        Map<String, HeapObject> objs = new LinkedHashMap<>();
        Map<String, Value> fields = new LinkedHashMap<>();
        for (int i = 0; i < entries.size(); i++) {
            String[] e = entries.get(i);
            fields.put(e[0], Value.prim(e[1]));
        }
        objs.put("@1", new HeapObject("@1", "cache", "LRUCache(" + CAPACITY + ")",
                Collections.unmodifiableMap(fields), List.of()));
        return new Heap(Collections.unmodifiableMap(objs));
    }

    private ArrayView snapshotArray() {
        List<Integer> vals = new ArrayList<>();
        for (String[] e : entries) {
            vals.add(Integer.parseInt(e[0].replaceAll("[^0-9]", "")));
        }
        while (vals.size() < CAPACITY) vals.add(0);
        return new ArrayView("@1", "cache", List.copyOf(vals),
                lo(), null, null, null, CAPACITY, -1);
    }

    private int lo() {
        return entries.size() > 0 ? Integer.parseInt(entries.get(0)[0].replaceAll("[^0-9]", "")) : 0;
    }

    private Trace.TraceMeta meta() {
        return new Trace.TraceMeta(id(), title(), codeResource(),
                "capacity=" + CAPACITY + " ops=" + OPS.length, OPS.length);
    }

    @Override
    public java.util.List<String> dataLines() {
        return java.util.List.of("int capacity = " + CAPACITY + ";");
    }

    @Override
    public Complexity complexity() {
        return new Complexity("O(1) get/put", "O(n) 哈希+双向链表",
                "哈希表 O(1) 定位 + 双向链表 O(1) 移动/删除——两者结合实现全 O(1)");
    }
}
