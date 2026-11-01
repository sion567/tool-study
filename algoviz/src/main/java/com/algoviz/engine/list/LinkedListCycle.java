package com.algoviz.engine.list;

import com.algoviz.engine.Algorithm;
import com.algoviz.engine.Intent;
import com.algoviz.engine.Layer;
import com.algoviz.engine.Loc;
import com.algoviz.engine.Step;
import com.algoviz.engine.Value;
import com.algoviz.engine.VizEvent;
import com.algoviz.engine.ds.ListView;
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
 * 快慢指针判环演示（Floyd 判圈）：慢走 1 快走 2——有环必相遇。
 * 不变量：若有环，快慢指针的差距每轮 +1（模环长），迟早追上——速度差决定必然相遇。
 */
public final class LinkedListCycle implements Algorithm {

    private static final String SCHEMA = "1";
    private static final String INVARIANT =
            "有环时：每轮快慢差距 +1（模环长）→ 迟早追上；无环时：快指针先到 null";

    /** 与 /code/LinkedListCycle.java 的行号对齐 */
    private static final int L_INIT = 3;
    private static final int L_WHILE = 4;
    private static final int L_SLOW = 5;
    private static final int L_FAST = 6;
    private static final int L_MEET = 7;
    private static final int L_NO_CYCLE = 9;

    private final int[] vals;
    private final int[] next;
    private final int n;

    public LinkedListCycle() {
        this(new int[]{5, 2, 9, 1, 5}, 1);   // @5.next → @2：有环！
    }

    /** 随机数据：环从尾部接回 anchorIdx。 */
    public LinkedListCycle(int[] vals, int anchorIdx) {
        this.vals = vals.clone();
        this.n = vals.length;
        this.next = new int[n];
        for (int i = 0; i < n; i++) {
            next[i] = i + 1 < n ? i + 1 : anchorIdx;   // 尾节点接回 anchor → 环
        }
    }

    private final List<Step> steps = new ArrayList<>();
    private int slow;
    private int fast;

    @Override public String id() { return "list.cycle"; }
    @Override public String title() { return "快慢指针判环"; }
    @Override public String codeResource() { return "/code/LinkedListCycle.java"; }
    @Override public String docResource() { return "/markdown/linked-list-cycle.md"; }

    @Override
    public Trace produce() {
        slow = 0;
        fast = 0;
        emit(L_INIT, List.of(VizEvent.of(VizEvent.EventType.INIT,
                "slow = fast = @1（同一起点起跑）", "slow", "fast")),
                "快慢指针从同一起点出发——注意 @5 的箭头绕回了 @2（环在堆里可见）");
        while (fast != -1 && next[fast] != -1) {
            emit(L_WHILE, List.of(VizEvent.of(VizEvent.EventType.COMPARE,
                    "fast = @" + (fast + 1) + " ≠ null 且 fast.next ≠ null → 继续", "fast")),
                    "只要快指针没到头，就再跑一轮");
            slow = next[slow];
            emit(L_SLOW, List.of(VizEvent.of(VizEvent.EventType.SET,
                    "slow ← @" + (slow + 1) + "（走 1 步）", "slow")),
                    "慢指针走 1 步");
            fast = next[next[fast]];
            emit(L_FAST, List.of(VizEvent.of(VizEvent.EventType.SET,
                    "fast ← @" + (fast + 1) + "（走 2 步）", "fast")),
                    "快指针走 2 步：每轮把差距拉大 1——有环就必追上");
            if (slow == fast) {
                emit(L_MEET, List.of(VizEvent.of(VizEvent.EventType.MARK_SORTED,
                        "相遇于 @" + (slow + 1) + " → 有环！", "slow", "fast")),
                        "两个标签叠在同一个盒子上——这就是判环的全部证据");
                return new Trace(SCHEMA, meta(), steps);
            }
            emit(L_MEET, List.of(VizEvent.of(VizEvent.EventType.COMPARE,
                    "slow=@" + (slow + 1) + " fast=@" + (fast + 1) + " → 未相遇，继续", "slow", "fast")),
                    "还没追上：差距在缩小，数学保证迟早相遇");
        }
        emit(L_NO_CYCLE, List.of(VizEvent.of(VizEvent.EventType.RETURN,
                "fast 到头 → return false（无环）")), "快指针先撞墙：链有终点，不可能有环");
        return new Trace(SCHEMA, meta(), steps);
    }

    private void emit(int line, List<VizEvent> events, String why) {
        Map<String, Value> locals = new LinkedHashMap<>();
        locals.put("head", Value.ref("@1"));
        locals.put("slow", Value.ref("@" + (slow + 1)));
        locals.put("fast", Value.ref("@" + (fast + 1)));
        StackFrame frame = new StackFrame("hasCycle", Collections.unmodifiableMap(locals), null);
        Map<String, Layer> layers = new LinkedHashMap<>();
        layers.put("mem.stack", new StackLayer(List.of(frame)));
        layers.put("mem.heap", new HeapLayer(snapshotHeap()));
        layers.put("ds.list", snapshotList());
        steps.add(new Step(steps.size(), Loc.line(line, why),
                Collections.unmodifiableMap(layers), List.copyOf(events), new Intent(why, INVARIANT)));
    }

    private Heap snapshotHeap() {
        Map<String, HeapObject> objs = new LinkedHashMap<>();
        for (int i = 0; i < n; i++) {
            Map<String, Value> fields = new LinkedHashMap<>();
            fields.put("val", Value.prim(vals[i]));
            fields.put("next", next[i] >= 0 ? Value.ref("@" + (next[i] + 1)) : Value.NULL);
            objs.put("@" + (i + 1), new HeapObject("@" + (i + 1), "node", "Node",
                    Collections.unmodifiableMap(fields), List.of()));
        }
        return new Heap(Collections.unmodifiableMap(objs));
    }

    private ListView snapshotList() {
        List<ListView.NodeBox> nodes = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            nodes.add(new ListView.NodeBox("@" + (i + 1), vals[i],
                    next[i] >= 0 ? "@" + (next[i] + 1) : null));
        }
        return new ListView("带环链表", "@1", Collections.unmodifiableList(nodes));
    }

    private Trace.TraceMeta meta() {
        StringBuilder sb = new StringBuilder();
        for (int k = 0; k < n; k++) {
            if (k > 0) sb.append(',');
            sb.append(vals[k]);
        }
        return new Trace.TraceMeta(id(), title(), codeResource(), sb.toString(), n);
    }
}
