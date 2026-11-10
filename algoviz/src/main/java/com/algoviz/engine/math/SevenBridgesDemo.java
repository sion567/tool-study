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
import com.algoviz.engine.ds.GraphView;
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
 * 哥尼斯堡七桥问题：欧拉用"奇顶点计数"判定了"不重复走遍七桥"不可能——图论诞生。
 * 剧本：四块陆地 7 座桥（度数 3,3,5,3 → 四个奇点 → 不可能）
 *   → 拆掉一座桥（度 2,3,5,2 → 两个奇点）→ 欧拉路径存在 → 逐桥走完。
 * 不变量：连通图存在欧拉路径 ⟺ 奇顶点数 ∈ {0, 2}；0 个奇点 = 欧拉回路。
 */
public final class SevenBridgesDemo implements Algorithm {

    private static final String SCHEMA = "1";
    private static final String INVARIANT =
            "每座桥进出各一次 = 每个顶点的度数必须是偶数（除起点终点可奇）——奇顶点 >2 则无解";

    private static final String[] LANDS = {"北岸", "南岸", "岛", "东岸"};
    private static final int[][] ORIGINAL = {{0, 2}, {0, 2}, {1, 2}, {1, 2}, {0, 3}, {1, 3}, {2, 3}};
    private static final int[][] AFTER = {{0, 2}, {0, 2}, {1, 2}, {1, 2}, {1, 3}, {2, 3}};

    private final List<Step> steps = new ArrayList<>();
    private int[][] bridges = ORIGINAL;
    private int walkFrom = -1, walkTo = -1;
    private int walkBridgeIdx = -1;
    private String active = null;
    private final Set<Integer> visitedBridges = new LinkedHashSet<>();

    @Override public String id() { return "math.sevenbridges"; }
    @Override public String title() { return "七桥问题·欧拉路径"; }
    @Override public String codeResource() { return "/code/SevenBridgesDemo.java"; }
    @Override public String docResource() { return "/markdown/seven-bridges.md"; }

    @Override
    public Trace produce() {
        steps.clear();
        bridges = ORIGINAL;
        visitedBridges.clear();
        walkFrom = walkTo = -1;
        walkBridgeIdx = -1;
        active = null;
        emit(4, List.of(VizEvent.of(VizEvent.EventType.INIT,
                "哥尼斯堡 1736：7 座桥连 4 块陆地，市民想不重复地走遍每座桥")),
                "全城的尝试都失败了——欧拉说：别走了，先数数");
        emit(6, List.of(VizEvent.of(VizEvent.EventType.COMPARE,
                "度数：北岸 3、南岸 3、岛 5、东岸 3——**四个奇顶点**")),
                "每次过桥'进'与'出'成对消耗 2 度——中间的点必须偶数度，奇点只能当起点/终点，而起点终点最多 2 个");
        emit(8, List.of(VizEvent.of(VizEvent.EventType.COMPARE,
                "奇顶点 4 个 > 2 → 欧拉路径不存在（数学上盖棺，不用再试了）")),
                "图论诞生的瞬间：把'陆地和桥'抽象成'点和边'，几何距离全部无关——只剩连接关系");
        // 拆桥
        bridges = AFTER;
        visitedBridges.clear();
        emit(10, List.of(VizEvent.of(VizEvent.EventType.SET,
                "重建后拆掉一座桥 → 度数 2,3,5,2 → 奇顶点只剩 2 个（南岸、岛）")),
                "奇顶点恰 2 个 → 欧拉路径存在，且必须从其中一个奇点出发、在另一个结束");
        // 走欧拉路径：南岸→东岸→岛→南岸→岛→北岸→岛
        walkBridge(4, 1, 3);   // 南岸-东岸（桥 idx4）
        walkBridge(5, 3, 2);   // 东岸-岛（桥 idx5）
        walkBridge(2, 2, 1);   // 岛-南岸（桥 idx2）
        walkBridge(3, 1, 2);   // 南岸-岛（桥 idx3，第二座）
        walkBridge(0, 2, 0);   // 岛-北岸（桥 idx0，第一座）
        walkBridge(1, 0, 2);   // 北岸-岛（桥 idx1，第二座）
        emit(16, List.of(VizEvent.of(VizEvent.EventType.MARK_SORTED,
                "6 座桥全部走过一次 → 欧拉路径完成：南岸 → 东岸 → 岛 → 南岸 → 岛 → 北岸 → 岛")),
                "起点=南岸（奇），终点=岛（奇）——与判据完全吻合");
        active = null;
        emit(18, List.of(VizEvent.of(VizEvent.EventType.RETURN,
                "欧拉路径的算法遗产：Hierholzer 线性时间找路径——一笔画问题从此有 O(E) 解法")),
                "应用：DNA 测序拼接、任务调度、笔画输入识别——本质都是'边不重复走遍'");
        return new Trace(SCHEMA, meta(), steps);
    }

    private void walkBridge(int bridgeIdx, int from, int to) {
        walkFrom = from;
        walkTo = to;
        walkBridgeIdx = bridgeIdx;
        active = LANDS[to];
        visitedBridges.add(bridgeIdx);
        emit(13, List.of(VizEvent.of(VizEvent.EventType.VISIT,
                "过桥：" + LANDS[from] + " → " + LANDS[to] +
                        (visitedBridges.size() == AFTER.length ? "（最后一座！）" : ""),
                LANDS[from], LANDS[to])),
                "每座桥恰走一次——剩下的桥越来越少，路径自己延伸");
        walkFrom = walkTo = -1;
        walkBridgeIdx = -1;
    }

    private void emit(int line, List<VizEvent> events, String why) {
        Map<String, Value> locals = new LinkedHashMap<>();
        locals.put("奇点判定", Value.prim(bridges == ORIGINAL ? "4 个奇点→无解" : "2 个奇点→有解"));
        locals.put("已过桥", Value.prim(visitedBridges.size() + " / " + bridges.length));
        StackFrame frame = new StackFrame("eulerPath", Collections.unmodifiableMap(locals), null);
        Map<String, Layer> layers = new LinkedHashMap<>();
        layers.put("mem.stack", new StackLayer(List.of(frame)));
        layers.put("mem.heap", new HeapLayer(snapshotHeap()));
        layers.put("ds.graph", snapshotGraph());
        steps.add(new Step(steps.size(), Loc.line(line, why),
                Collections.unmodifiableMap(layers), List.copyOf(events), new Intent(why, INVARIANT)));
    }

    private Heap snapshotHeap() {
        Map<String, Value> fields = new LinkedHashMap<>();
        int[] deg = new int[4];
        for (int[] b : bridges) {
            deg[b[0]]++;
            deg[b[1]]++;
        }
        for (int i = 0; i < 4; i++) {
            fields.put(LANDS[i] + " 度数", Value.prim(deg[i]));
        }
        Map<String, HeapObject> objs = new LinkedHashMap<>();
        objs.put("@1", new HeapObject("@1", "graph", "Konigsberg",
                Collections.unmodifiableMap(fields), List.of()));
        return new Heap(Collections.unmodifiableMap(objs));
    }

    private GraphView snapshotGraph() {
        List<GraphView.GraphVertex> verts = new ArrayList<>();
        double[][] pos = {{0.18, 0.20}, {0.18, 0.78}, {0.55, 0.48}, {0.85, 0.48}};
        for (int i = 0; i < 4; i++) {
            verts.add(new GraphView.GraphVertex("@" + (i + 1), LANDS[i], pos[i][0], pos[i][1]));
        }
        List<GraphView.GraphEdge> edges = new ArrayList<>();
        for (int[] b : bridges) {
            edges.add(new GraphView.GraphEdge("@" + (b[0] + 1), "@" + (b[1] + 1), 1));
        }
        List<String> settled = new ArrayList<>();
        Set<String> ids = new LinkedHashSet<>();
        for (int i = 0; i < 4; i++) ids.add("@" + (i + 1));
        if (active != null) {
            for (int i = 0; i < 4; i++) {
                if (LANDS[i].equals(active)) {
                    settled.add("@" + (i + 1));
                }
            }
        }
        String relaxFrom = walkFrom >= 0 ? "@" + (walkFrom + 1) : null;
        String relaxTo = walkTo >= 0 ? "@" + (walkTo + 1) : null;
        return new GraphView("哥尼斯堡七桥", Collections.unmodifiableList(verts),
                Collections.unmodifiableList(edges),
                active == null ? null : ids.contains(active) ? "@" + (index(active) + 1) : null,
                Collections.unmodifiableList(settled), Map.of(),
                relaxFrom, relaxTo, List.of());
    }

    private int index(String land) {
        for (int i = 0; i < LANDS.length; i++) {
            if (LANDS[i].equals(land)) return i;
        }
        return 0;
    }

    private Trace.TraceMeta meta() {
        return new Trace.TraceMeta(id(), title(), codeResource(), "4 陆地 7→6 桥", 4);
    }

    @Override
    public java.util.List<String> dataLines() {
        return java.util.List.of("int[][] bridges = {{0,2},{0,2},{1,2},{1,2},{0,3},{1,3},{2,3}};");
    }

    @Override
    public Complexity complexity() {
        return new Complexity("O(V+E) 判定+Hierholzer 找路", "O(V+E)",
                "奇点计数判定 O(V)：一笔画问题从'全城试验'变成'数四个数'——抽象的威力");
    }
}
