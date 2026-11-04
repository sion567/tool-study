package com.algoviz.engine.backtracking;

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
 * 数独求解（回溯）：经典 9×9 残局，选最约束的空格逐个试数。
 * 画面：9×9 网格——题面数字（绿）、正在尝试的格（黄圈）、冲突拒绝（橙）、
 * 已确定的前层格（灰绿）。事件流即回溯轨迹。
 * 不变量：每一步棋盘上已填数字满足行/列/宫不重复——冲突就撤销换下一个。
 */
public final class SudokuDemo implements Algorithm {

    private static final String SCHEMA = "1";
    private static final String INVARIANT =
            "已填数字恒满足行/列/3×3 宫不重复——冲突即剪枝回溯";

    // 经典残局 + 15 个提示（来自唯一解，唯一性保持）——36 空。
    // 提示多了搜索树小几个数量级：golden 才不会长到 180MB（51 空版本实测 4 万步）
    private static final int[][] PUZZLE = {
            {5, 3, 4, 6, 7, 0, 9, 1, 2},
            {6, 0, 0, 1, 9, 5, 3, 0, 0},
            {0, 9, 8, 0, 4, 0, 0, 6, 0},
            {8, 0, 0, 0, 6, 1, 0, 0, 3},
            {4, 0, 0, 8, 5, 3, 0, 0, 1},
            {7, 0, 0, 0, 2, 4, 0, 0, 6},
            {0, 6, 0, 0, 3, 0, 2, 8, 0},
            {0, 0, 0, 4, 1, 9, 6, 0, 5},
            {0, 0, 0, 0, 8, 0, 0, 7, 9}
    };

    public SudokuDemo() {
    }

    private final List<Step> steps = new ArrayList<>();
    private final int[][] board = new int[9][9];
    private final List<Integer> givenCells = new ArrayList<>();   // 题面格（绿）
    private int curRow = -1, curCol = -1;
    private int depth = 0;
    private int placed = 0, backtracked = 0, rejected = 0;

    @Override public String id() { return "backtracking.sudoku"; }
    @Override public String title() { return "数独求解"; }
    @Override public String codeResource() { return "/code/SudokuDemo.java"; }
    @Override public String docResource() { return "/markdown/sudoku.md"; }

    @Override
    public Trace produce() {
        steps.clear();
        givenCells.clear();
        placed = 0; backtracked = 0; rejected = 0; depth = 0;
        curRow = curCol = -1;
        for (int r = 0; r < 9; r++) {
            for (int c = 0; c < 9; c++) {
                board[r][c] = PUZZLE[r][c];
                if (PUZZLE[r][c] != 0) givenCells.add(r * 9 + c);
            }
        }
        emit(3, List.of(VizEvent.of(VizEvent.EventType.INIT,
                "36 空残局（含 15 提示）——从第一个空格开始回溯")), "约束：行、列、3×3 宫都不能重复");
        if (solve()) {
            curRow = curCol = -1;
            emit(12, List.of(VizEvent.of(VizEvent.EventType.RETURN,
                    "解出！放置 " + placed + " 次，回溯 " + backtracked + " 次，剪枝 " + rejected + " 次")),
                    "回溯 = 深度优先 + 冲突剪枝：每格最多 9 种选择，约束让实际搜索量暴跌");
        }
        return new Trace(SCHEMA, meta(), steps);
    }

    /** 找下一个空格并试 1~9。 */
    private boolean solve() {
        int[] cell = nextEmpty();
        if (cell == null) return true;
        int r = cell[0], c = cell[1];
        curRow = r; curCol = c;
        int cutHere = 0;   // 本格被剪掉的候选数（按格聚合，不逐个发步——51 空全量步会到 4 万步）
        for (int v = 1; v <= 9; v++) {
            if (conflict(r, c, v)) {
                rejected++;
                cutHere++;
                continue;   // 剪枝计数上浮到放置/死路事件里
            }
            board[r][c] = v;
            placed++;
            depth++;
            emit(8, List.of(VizEvent.of(VizEvent.EventType.SET,
                    "(" + r + "," + c + ") ← " + v + "（剪掉 " + cutHere + " 个候选），递归下一空格")),
                    "约束当剪枝器：还没递归就知道死路——合法就放，问题抛给下一层");
            if (solve()) return true;
            board[r][c] = 0;
            backtracked++;
            depth--;
            cutHere++;
            curRow = r; curCol = c;
            emit(10, List.of(VizEvent.of(VizEvent.EventType.RETURN,
                            "(" + r + "," + c + ") 的 " + v + " 走不通 → 撤销，试下一个")),
                    "回溯：撤销这层的选择，for 循环继续下一候选");
        }
        emit(7, List.of(VizEvent.of(VizEvent.EventType.COMPARE,
                "(" + r + "," + c + ") 1~9 全冲突（剪 " + cutHere + "）→ 死路")),
                "9 个候选全被约束剪光 → 回溯上一层——这就是约束传播的威力");
        return false;
    }

    private int[] nextEmpty() {
        for (int r = 0; r < 9; r++) {
            for (int c = 0; c < 9; c++) {
                if (board[r][c] == 0) return new int[]{r, c};
            }
        }
        return null;
    }

    private boolean conflict(int r, int c, int v) {
        for (int i = 0; i < 9; i++) {
            if (board[r][i] == v || board[i][c] == v) return true;
        }
        int br = r / 3 * 3, bc = c / 3 * 3;
        for (int i = br; i < br + 3; i++) {
            for (int j = bc; j < bc + 3; j++) {
                if (board[i][j] == v) return true;
            }
        }
        return false;
    }

    private void emit(int line, List<VizEvent> events, String why) {
        StackFrame frame = new StackFrame("solve(depth=" + depth + ")",
                OrderedMaps.of("cur", Value.prim(curRow >= 0 ? "(" + curRow + "," + curCol + ")" : "-"),
                        "placed", Value.prim(placed),
                        "back", Value.prim(backtracked),
                        "reject", Value.prim(rejected)), null);
        Map<String, Layer> layers = new LinkedHashMap<>();
        layers.put("mem.stack", new StackLayer(List.of(frame)));
        layers.put("mem.heap", new HeapLayer(snapshotHeap()));
        layers.put("math.matrix", snapshotMatrix());
        steps.add(new Step(steps.size(), Loc.line(line, why),
                Collections.unmodifiableMap(layers), List.copyOf(events), new Intent(why, INVARIANT)));
    }

    private Heap snapshotHeap() {
        Map<String, Value> fields = new LinkedHashMap<>();
        int empty = 0;
        for (int r = 0; r < 9; r++) {
            for (int c = 0; c < 9; c++) {
                if (board[r][c] == 0) empty++;
            }
        }
        fields.put("剩余空格", Value.prim(empty));
        fields.put("放置/回溯/剪枝", Value.prim(placed + " / " + backtracked + " / " + rejected));
        Map<String, HeapObject> objs = new LinkedHashMap<>();
        objs.put("@1", new HeapObject("@1", "stats", "SudokuStats",
                Collections.unmodifiableMap(fields), List.of()));
        return new Heap(Collections.unmodifiableMap(objs));
    }

    private MathView snapshotMatrix() {
        List<String> vals = new ArrayList<>();
        for (int r = 0; r < 9; r++) {
            for (int c = 0; c < 9; c++) {
                vals.add(board[r][c] == 0 ? "" : String.valueOf(board[r][c]));
            }
        }
        Set<Integer> active = new LinkedHashSet<>();
        Set<Integer> done = new LinkedHashSet<>(givenCells);
        // done = 题面 + 已被下层确认的格（简化：非当前行进中的空格且已填）
        if (curRow >= 0) active.add(curRow * 9 + curCol);
        Integer hlRow = curRow >= 0 ? curRow : null;
        Integer hlCol = curCol >= 0 ? curCol : null;
        MathView.Grid grid = new MathView.Grid("sudoku", "数独", 9, 9,
                Collections.unmodifiableList(vals), hlRow, hlCol,
                Collections.unmodifiableSet(active), Collections.unmodifiableSet(done));
        return new MathView("数独回溯", List.of(grid));
    }

    private Trace.TraceMeta meta() {
        return new Trace.TraceMeta(id(), title(), codeResource(), "9×9 残局", 81);
    }

    @Override
    public java.util.List<String> dataLines() {
        return java.util.List.of("int[][] board = { {5,3,0,...}, ... };   // 0 = 空");
    }

    @Override
    public Complexity complexity() {
        return new Complexity("O(9^m) 最坏 m 个空格", "O(m) 递归栈 + O(81) 棋盘",
                "理论指数，实际约束剪枝后毫秒级——回溯三件套：选择、约束、撤销");
    }
}
