package com.algoviz.engine.graph;

import com.algoviz.engine.Algorithm;
import com.algoviz.engine.Complexity;
import com.algoviz.engine.Intent;
import com.algoviz.engine.Layer;
import com.algoviz.engine.Loc;
import com.algoviz.engine.OrderedMaps;
import com.algoviz.engine.Step;
import com.algoviz.engine.Value;
import com.algoviz.engine.VizEvent;
import com.algoviz.engine.math.MathView;
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
 * A* 寻路（网格迷宫）：f = g + h（曼哈顿距离）驱动的最佳优先搜索。
 * 画面：12×8 网格——墙(■)、open 集（黄=待考察）、closed 集（绿=已定价）、
 * 最终路径（◎ 连线）。事件流 = 逐个弹出 f 最小的格子并扩展邻居。
 * 不变量：每次弹出的都是 open 集中 f 最小的——h 可采纳（不高估）时，弹出即最优。
 */
public final class AStarDemo implements Algorithm {

    private static final String SCHEMA = "1";
    private static final String INVARIANT =
            "f = g + h（h=曼哈顿距离，从不高估）→ 每次弹出的格子的 g 已是最短——弹出即定格";

    private static final int ROWS = 8, COLS = 12;
    private static final int SR = 0, SC = 0;      // 起点
    private static final int GR = 7, GC = 11;     // 终点
    // 墙（行,列）
    private static final boolean[][] WALL = new boolean[ROWS][COLS];

    static {
        int[][] walls = {
                {1, 2}, {1, 3}, {1, 4}, {1, 8},
                {2, 4}, {2, 8}, {2, 9},
                {3, 4}, {3, 6}, {3, 7}, {3, 8},
                {4, 2}, {4, 4}, {4, 6},
                {5, 2}, {5, 6}, {5, 7}, {5, 8}, {5, 10},
                {6, 2}, {6, 10}
        };
        for (int[] w : walls) WALL[w[0]][w[1]] = true;
    }

    private final List<Step> steps = new ArrayList<>();
    private final int[][] g = new int[ROWS][COLS];
    private final boolean[][] closed = new boolean[ROWS][COLS];
    private final boolean[][] inOpen = new boolean[ROWS][COLS];
    private final int[][] parentR = new int[ROWS][COLS];
    private final int[][] parentC = new int[ROWS][COLS];
    private final List<int[]> path = new ArrayList<>();
    private int curR = -1, curC = -1;

    @Override public String id() { return "graph.astar"; }
    @Override public String title() { return "A* 寻路"; }
    @Override public String codeResource() { return "/code/AStarDemo.java"; }
    @Override public String docResource() { return "/markdown/astar.md"; }

    @Override
    public Trace produce() {
        reset();
        g[SR][SC] = 0;
        inOpen[SR][SC] = true;
        emit(4, List.of(VizEvent.of(VizEvent.EventType.INIT,
                "open = {起点}，g(起)=0，h=曼哈顿距离")),
                "f = g + h：g 是已走代价，h 是乐观估计——f 越小越有希望");
        boolean found = false;
        while (true) {
            int[] best = popMinF();
            if (best == null) break;
            curR = best[0]; curC = best[1];
            closed[curR][curC] = true;
            if (curR == GR && curC == GC) {
                emit(10, List.of(VizEvent.of(VizEvent.EventType.MARK_SORTED,
                        "弹出终点 → f 已最优，停止搜索")), "弹出即最优：终点定格，开始回溯路径");
                found = true;
                break;
            }
            emit(6, List.of(VizEvent.of(VizEvent.EventType.VISIT,
                    "弹出 (" + curR + "," + curC + ") f=" + (g[curR][curC] + h(curR, curC)))),
                    "open 里 f 最小的先出——它的 g 到此最优（h 不高估保证的）");
            for (int[] d : new int[][]{{-1, 0}, {1, 0}, {0, -1}, {0, 1}}) {
                int nr = curR + d[0], nc = curC + d[1];
                if (nr < 0 || nr >= ROWS || nc < 0 || nc >= COLS || WALL[nr][nc] || closed[nr][nc]) continue;
                int ng = g[curR][curC] + 1;
                if (ng < g[nr][nc]) {
                    boolean first = !inOpen[nr][nc];
                    g[nr][nc] = ng;
                    parentR[nr][nc] = curR;
                    parentC[nr][nc] = curC;
                    inOpen[nr][nc] = true;
                    emit(8, List.of(VizEvent.of(first ? VizEvent.EventType.SET : VizEvent.EventType.COMPARE,
                                    "(" + nr + "," + nc + ") g=" + ng + " h=" + h(nr, nc) + " f=" + (ng + h(nr, nc))
                                            + (first ? " 入 open" : " 更优更新"))),
                            first ? "新格子入 open——记下父指针" : "发现更短路径——更新 g 和父指针");
                }
            }
        }
        if (found) {
            int r = GR, c = GC;
            while (!(r == SR && c == SC)) {
                path.add(new int[]{r, c});
                int pr = parentR[r][c], pc = parentC[r][c];
                r = pr; c = pc;
            }
            path.add(new int[]{SR, SC});
            Collections.reverse(path);
            curR = curC = -1;
            emit(14, List.of(VizEvent.of(VizEvent.EventType.RETURN,
                    "路径长 " + (path.size() - 1) + " 步：沿父指针从终点回溯到起点")),
                    "g 最优 → 父指针链就是最短路径——A* 与 Dijkstra 的差别只在 h 这盏探路灯");
        }
        return new Trace(SCHEMA, meta(), steps);
    }

    private void reset() {
        steps.clear();
        path.clear();
        curR = curC = -1;
        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                g[r][c] = Integer.MAX_VALUE;
                closed[r][c] = false;
                inOpen[r][c] = false;
                parentR[r][c] = parentC[r][c] = -1;
            }
        }
    }

    private int[] popMinF() {
        int best = Integer.MAX_VALUE, br = -1, bc = -1;
        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                if (inOpen[r][c] && !closed[r][c] && g[r][c] + h(r, c) < best) {
                    best = g[r][c] + h(r, c);
                    br = r; bc = c;
                }
            }
        }
        return br < 0 ? null : new int[]{br, bc};
    }

    private int h(int r, int c) {
        return Math.abs(r - GR) + Math.abs(c - GC);
    }

    private void emit(int line, List<VizEvent> events, String why) {
        StackFrame frame = new StackFrame("astar",
                OrderedMaps.of("cur", Value.prim(curR >= 0 ? "(" + curR + "," + curC + ")" : "-"),
                        "f(cur)", Value.prim(curR >= 0 ? String.valueOf(g[curR][curC] + h(curR, curC)) : "-")), null);
        Map<String, Layer> layers = new LinkedHashMap<>();
        layers.put("mem.stack", new StackLayer(List.of(frame)));
        layers.put("mem.heap", new HeapLayer(snapshotHeap()));
        layers.put("math.matrix", snapshotMatrix());
        steps.add(new Step(steps.size(), Loc.line(line, why),
                Collections.unmodifiableMap(layers), List.copyOf(events), new Intent(why, INVARIANT)));
    }

    private Heap snapshotHeap() {
        Map<String, Value> fields = new LinkedHashMap<>();
        int openN = 0, closedN = 0;
        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                if (closed[r][c]) closedN++;
                else if (inOpen[r][c]) openN++;
            }
        }
        fields.put("open 集", Value.prim(openN + " 个待考察"));
        fields.put("closed 集", Value.prim(closedN + " 个已定价"));
        if (curR >= 0) {
            fields.put("当前", Value.prim("(" + curR + "," + curC + ") g=" + g[curR][curC] + " h=" + h(curR, curC)));
        }
        Map<String, HeapObject> objs = new LinkedHashMap<>();
        objs.put("@1", new HeapObject("@1", "frontier", "AStarState",
                Collections.unmodifiableMap(fields), List.of()));
        return new Heap(Collections.unmodifiableMap(objs));
    }

    private MathView snapshotMatrix() {
        List<String> vals = new ArrayList<>();
        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                if (WALL[r][c]) vals.add("■");
                else if (r == SR && c == SC) vals.add("S");
                else if (r == GR && c == GC) vals.add("G");
                else if (onPath(r, c)) vals.add("◎");
                else if (inOpen[r][c] && !closed[r][c]) vals.add(String.valueOf(g[r][c]));
                else if (closed[r][c]) vals.add("·");
                else vals.add("");
            }
        }
        Set<Integer> active = new LinkedHashSet<>();
        Set<Integer> done = new LinkedHashSet<>();
        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                int idx = r * COLS + c;
                if (closed[r][c]) done.add(idx);                       // 绿 = closed
                else if (inOpen[r][c]) active.add(idx);                // 黄 = open
            }
        }
        if (curR >= 0) {
            int cur = curR * COLS + curC;
            done.add(cur);
            active.remove(cur);
        }
        MathView.Grid grid = new MathView.Grid("grid", "迷宫 12×8", ROWS, COLS,
                Collections.unmodifiableList(vals), null, null,
                Collections.unmodifiableSet(active), Collections.unmodifiableSet(done));
        return new MathView("A* 寻路", List.of(grid));
    }

    private boolean onPath(int r, int c) {
        for (int[] p : path) {
            if (p[0] == r && p[1] == c) return true;
        }
        return false;
    }

    private Trace.TraceMeta meta() {
        return new Trace.TraceMeta(id(), title(), codeResource(), ROWS + "×" + COLS + " 迷宫", ROWS * COLS);
    }

    @Override
    public java.util.List<String> dataLines() {
        return java.util.List.of("int[][] walls = {...21 堵墙...};   // " + ROWS + "×" + COLS + " 网格");
    }

    @Override
    public Complexity complexity() {
        return new Complexity("O(E) 最坏全图扫一遍", "O(V) open/closed/父指针",
                "h 可采纳（不高估）→ 首次弹出终点即最短。h=0 退化为 Dijkstra；"
                        + "h 完美则直奔终点。游戏寻路/导航的本质");
    }
}
