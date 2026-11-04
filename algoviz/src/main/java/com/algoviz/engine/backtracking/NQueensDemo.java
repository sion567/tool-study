package com.algoviz.engine.backtracking;

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
 * N 皇后回溯演示（n=4）：每行尝试放一个皇后，冲突则回溯。
 * 不变量：已放置的皇后互相不攻击（不同列、不同对角线）。
 */
public final class NQueensDemo implements Algorithm {

    private static final String SCHEMA = "1";
    private static final String INVARIANT =
            "已放置的皇后互相不攻击：不同列、不同对角线——冲突就回溯换列";

    private final int N;

    public NQueensDemo() {
        this(4);
    }

    public NQueensDemo(int n) {
        this.N = n;
        this.queens = new int[N];
    }

    private final List<Step> steps = new ArrayList<>();
    private final List<StackFrame> frames = new ArrayList<>();
    private final int[] queens;   // queens[row] = col（-1 = 未放）
    private int curRow = -1, curCol = -1;

    @Override public String id() { return "backtracking.nqueens"; }
    @Override public String title() { return "N 皇后"; }
    @Override public String codeResource() { return "/code/NQueensDemo.java"; }
    @Override public String docResource() { return "/markdown/nqueens.md"; }

    @Override
    public Trace produce() {
        java.util.Arrays.fill(queens, -1);
        backtrack(0);
        return new Trace(SCHEMA, meta(), steps);
    }

    private void backtrack(int row) {
        push(row);
        emit(2, List.of(VizEvent.of(VizEvent.EventType.CALL,
                "尝试在第 " + row + " 行放皇后")), "进入第 " + row + " 行：从第 0 列开始尝试");
        if (row == N) {
            emit(3, List.of(VizEvent.of(VizEvent.EventType.MARK_SORTED,
                    "4 个皇后全部就位 → 找到解！")),
                    "所有行都放好了 → 一个解");
            emit(2, List.of(VizEvent.of(VizEvent.EventType.RETURN, "回溯（弹栈）")),
                    "找到解后弹栈——继续找其他解");
            pop();
            return;
        }
        for (int col = 0; col < N; col++) {
            curRow = row; curCol = col;
            boolean safe = isSafe(row, col);
            if (safe) {
                queens[row] = col;
                emit(5, List.of(VizEvent.of(VizEvent.EventType.SET,
                        "安全 → 放在 (" + row + "," + col + ")")),
                        "不与任何已放皇后冲突 → 放置，递归下一行");
                backtrack(row + 1);
                queens[row] = -1;
                emit(2, List.of(VizEvent.of(VizEvent.EventType.SET,
                        "撤销 (" + row + "," + col + ") → 尝试下一列")),
                        "回溯：撤销这个皇后，尝试下一列");
            } else {
                emit(5, List.of(VizEvent.of(VizEvent.EventType.COMPARE,
                        "冲突！(" + row + "," + col + ") 与已有皇后攻击")),
                        "与已放皇后在同一列或对角线 → 跳过此列");
            }
        }
        emit(2, List.of(VizEvent.of(VizEvent.EventType.RETURN, "本行全试完 → 回溯")),
                "所有列都试完 → 回溯到上一行");
        pop();
    }

    private boolean isSafe(int row, int col) {
        for (int prev = 0; prev < row; prev++) {
            int pc = queens[prev];
            if (pc == -1) continue;
            if (pc == col) return false;
            if (Math.abs(prev - row) == Math.abs(pc - col)) return false;
        }
        return true;
    }

    private void push(int row) {
        Map<String, Value> locals = new LinkedHashMap<>();
        locals.put("row", Value.prim(row));
        List<Value> q = new ArrayList<>();
        for (int c : queens) q.add(Value.prim(c));
        locals.put("queens", Value.prim(currentQueensStr()));
        frames.add(new StackFrame("backtrack(row=" + row + ")", Collections.unmodifiableMap(locals), null));
    }

    private String currentQueensStr() {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < N; i++) sb.append(queens[i] >= 0 ? queens[i] : "_");
        return sb.append("]").toString();
    }

    private void pop() {
        frames.remove(frames.size() - 1);
    }

    private void emit(int line, List<VizEvent> events, String why) {
        StackFrame frame = frames.isEmpty()
                ? new StackFrame("backtrack", Map.of(), null) : frames.get(frames.size() - 1);
        Map<String, Layer> layers = new LinkedHashMap<>();
        layers.put("mem.stack", new StackLayer(List.copyOf(frames)));
        layers.put("mem.heap", new HeapLayer(snapshotHeap()));
        layers.put("ds.array", snapshotArray());
        steps.add(new Step(steps.size(), Loc.line(line, why),
                Collections.unmodifiableMap(layers), List.copyOf(events), new Intent(why, INVARIANT)));
    }

    private Heap snapshotHeap() {
        Map<String, HeapObject> objs = new LinkedHashMap<>();
        List<Value> q = new ArrayList<>();
        for (int c : queens) q.add(Value.prim(c));
        objs.put("@1", new HeapObject("@1", "queens", "int[" + N + "]",
                new LinkedHashMap<>(), List.copyOf(q)));
        return new Heap(Collections.unmodifiableMap(objs));
    }

    private ArrayView snapshotArray() {
        List<Integer> vals = new ArrayList<>();
        for (int c : queens) vals.add(c);
        return new ArrayView("@1", "queens", List.copyOf(vals),
                curRow >= 0 ? curRow : null, curCol, null, null, N, -1);
    }

    private Trace.TraceMeta meta() {
        return new Trace.TraceMeta(id(), title(), codeResource(), "N=4", N);
    }

    @Override
    public java.util.List<String> dataLines() {
        return java.util.List.of("int n = " + N + ";");
    }

    @Override
    public Complexity complexity() {
        return new Complexity("O(n!) 每行 n 个候选", "O(n) 递归栈 + queens 数组",
                "第一行 n 种、第二行 n-1…但剪枝（冲突检测）大幅减少实际搜索量");
    }
}
