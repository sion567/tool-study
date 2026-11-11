package com.algoviz.engine.olympiad;

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
 * 组合构造·骑士巡游：马走"日"字踏遍 8×8 棋盘，每格恰访一次——64 格一条 63 步的长链。
 * Warnsdorff 启发式：每步走向"出度最小"的未访格（先钻进死角，把宽敞的路留给后面）。
 * 单步贪心没有回头路，却在实战近乎全胜——启发式的教科书案例。
 * 画面（math.matrix 复用）：8×8 棋盘，格值 = 到访序号，黄圈 = 马当前位置。
 */
public final class KnightsTourDemo implements Algorithm {

    private static final String SCHEMA = "1";
    private static final String INVARIANT =
            "马走日字（8 个候选位），每格恰访一次；Warnsdorff = 出度最小优先——"
                    + "局部最优没有回头路，却把「死角」先消耗掉，实战成功率极高（但不保证，是启发式不是定理）";

    private static final int N = 8;
    private static final int[][] MOVES = {{1, 2}, {2, 1}, {2, -1}, {1, -2}, {-1, -2}, {-2, -1}, {-2, 1}, {-1, 2}};

    private final int startRow;
    private final int startCol;

    private final List<Step> steps = new ArrayList<>();
    private final int[][] board = new int[N][N];      // 0 = 未访，1..64 = 到访序号
    private int row, col, moveNo, lastDegree;

    public KnightsTourDemo() {
        this(0, 0);
    }

    /** 变体：不同起点（候选起点均验证可走完全程）。 */
    public KnightsTourDemo(int startRow, int startCol) {
        this.startRow = startRow;
        this.startCol = startCol;
    }

    @Override public String id() { return "olympiad.knights"; }
    @Override public String title() { return "组合构造·骑士巡游"; }
    @Override public String codeResource() { return "/code/KnightsTourDemo.java"; }
    @Override public String docResource() { return "/markdown/knights.md"; }

    @Override
    public Trace produce() {
        steps.clear();
        for (int[] b : board) {
            java.util.Arrays.fill(b, 0);
        }
        row = startRow;
        col = startCol;
        moveNo = 1;
        board[row][col] = 1;
        emit(5, List.of(VizEvent.of(VizEvent.EventType.INIT,
                String.format("骑士落在 (%d,%d)，第 1 步——接下来 63 步每格都是第一次也是最后一次", row, col))),
                "欧拉式问题：找一条哈密顿路径——「存在」容易证明，「构造」才是奥数的功课");
        while (moveNo < N * N) {
            int[] next = warnsdorff();
            if (next == null) {
                emit(9, List.of(VizEvent.of(VizEvent.EventType.MARK_SORTED,
                        String.format("第 %d 步被困：出度 0——启发式的失败现场（换起点再来）", moveNo))),
                        "Warnsdorff 不保证成功：贪心走死路时没有回头机制，这正是它与回溯的分界线");
                break;
            }
            row = next[0];
            col = next[1];
            moveNo++;
            board[row][col] = moveNo;
            lastDegree = degree(row, col);
            emit(11, List.of(VizEvent.of(VizEvent.EventType.SET,
                    String.format("第 %d 步 → (%d,%d)，此处出度 %d", moveNo, row, col, lastDegree))),
                    "走出度最小的格 = 先消耗「快没路走」的格子，宽敞地带留给后面——局部贪心的全局眼光");
        }
        if (moveNo == N * N) {
            emit(12, List.of(VizEvent.of(VizEvent.EventType.MARK_SORTED,
                    "64 格全部踏遍，一步没回头——Warnsdorff 的完胜")),
                    "8×8 巡游路线总数 ≈ 2.6×10¹³ 条：构造成功一条，比数清所有容易得多");
        }
        emit(15, List.of(VizEvent.of(VizEvent.EventType.RETURN,
                "对照回溯（N 皇后）：回溯靠撤销保正确，Warnsdorff 靠眼光免撤销——代价是失去完备性")),
                "启发式 = 放弃「保证成功」换「几乎总是成功 + 快」：AI 搜索的一切起点");
        return new Trace(SCHEMA, meta(), steps);
    }

    /** Warnsdorff：未访邻居中出度最小者；并列取行优先（保证确定性）。 */
    private int[] warnsdorff() {
        int[] best = null;
        int bestDeg = Integer.MAX_VALUE;
        for (int[] m : MOVES) {
            int r = row + m[0], c = col + m[1];
            if (r < 0 || r >= N || c < 0 || c >= N || board[r][c] != 0) {
                continue;
            }
            int deg = degree(r, c);
            if (deg < bestDeg) {
                bestDeg = deg;
                best = new int[]{r, c};
            }
        }
        return best;
    }

    private int degree(int r, int c) {
        int deg = 0;
        for (int[] m : MOVES) {
            int nr = r + m[0], nc = c + m[1];
            if (nr >= 0 && nr < N && nc >= 0 && nc < N && board[nr][nc] == 0) {
                deg++;
            }
        }
        return deg;
    }

    private void emit(int line, List<VizEvent> events, String why) {
        StackFrame fr = new StackFrame("knight",
                OrderedMaps.of("位置", Value.prim(String.format("(%d,%d)", row, col)),
                        "步数", Value.prim(moveNo + "/64"),
                        "出度", Value.prim(String.valueOf(lastDegree))), null);
        Map<String, Layer> layers = new LinkedHashMap<>();
        layers.put("mem.stack", new StackLayer(List.of(fr)));
        layers.put("mem.heap", new HeapLayer(snapshotHeap()));
        layers.put("math.matrix", snapshotMath());
        steps.add(new Step(steps.size(), Loc.line(line, why),
                Collections.unmodifiableMap(layers), List.copyOf(events), new Intent(why, INVARIANT)));
    }

    private Heap snapshotHeap() {
        Map<String, Value> fields = new LinkedHashMap<>();
        fields.put("已踏遍", Value.prim(moveNo + " / 64 格"));
        fields.put("当前出度", Value.prim(String.valueOf(lastDegree)));
        fields.put("规则", Value.prim("马走日 · 每格一次"));
        Map<String, HeapObject> objs = new LinkedHashMap<>();
        objs.put("@1", new HeapObject("@1", "knight", "TourState",
                Collections.unmodifiableMap(fields), List.of()));
        return new Heap(Collections.unmodifiableMap(objs));
    }

    private MathView snapshotMath() {
        List<String> values = new ArrayList<>();
        for (int r = 0; r < N; r++) {
            for (int c = 0; c < N; c++) {
                values.add(board[r][c] == 0 ? "" : String.valueOf(board[r][c]));
            }
        }
        Set<Integer> active = new LinkedHashSet<>();
        active.add(row * N + col);
        Set<Integer> done = new LinkedHashSet<>();
        for (int r = 0; r < N; r++) {
            for (int c = 0; c < N; c++) {
                if (board[r][c] > 0 && !(r == row && c == col)) {
                    done.add(r * N + c);
                }
            }
        }
        MathView.Grid grid = new MathView.Grid("board", "骑士巡游 8×8（格值=到访序号；黄圈=马当前位置）",
                N, N, values, null, null, active, done);
        return new MathView("骑士巡游", List.of(grid));
    }

    private Trace.TraceMeta meta() {
        return new Trace.TraceMeta(id(), title(), codeResource(),
                String.format("起点(%d,%d)", startRow, startCol), N * N + 1);
    }

    @Override
    public java.util.List<String> dataLines() {
        return java.util.List.of(String.format("int row = %d, col = %d;   //@DATA 起点", startRow, startCol));
    }

    @Override
    public Complexity complexity() {
        return new Complexity("O(1) 每步（8 邻居查表）", "O(N²) 棋盘",
                "哈密顿路径是 NPC 的，但 8×8 这种「特例 + 好启发」几乎白送——"
                        + "回溯保完备、启发式保速度，搜索算法的永恒权衡");
    }
}
