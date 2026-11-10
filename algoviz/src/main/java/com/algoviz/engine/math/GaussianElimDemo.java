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
 * 高斯消元：解三元一次方程组的教科书流程。
 * 增广矩阵 [A|b] 三次初等行变换 → 上三角 → 回代得 x=1, y=2, z=3。
 * 画面：矩阵逐行变色（正在消的行橙、已成三角的行绿），每步显示行运算式。
 */
public final class GaussianElimDemo implements Algorithm {

    private static final String SCHEMA = "1";
    private static final String INVARIANT =
            "初等行变换不改变方程组的解——消元把矩阵化成上三角，回代自底向上解出每个未知数";

    /** 增广矩阵 [A|b]：x+y+z=6, x+2y+3z=14, x+4y+9z=36 */
    private static final double[][] AUG = {
            {1, 1, 1, 6},
            {1, 2, 3, 14},
            {1, 4, 9, 36}
    };

    private final List<Step> steps = new ArrayList<>();
    private final double[][] m = new double[3][4];
    private int hlRow = -1, hlCol = -1;
    private int solvedRows = 0;   // 已成上三角的行数（从底部数）

    @Override public String id() { return "math.gauss"; }
    @Override public String title() { return "高斯消元"; }
    @Override public String codeResource() { return "/code/GaussianElimDemo.java"; }
    @Override public String docResource() { return "/markdown/gauss.md"; }

    @Override
    public Trace produce() {
        steps.clear();
        for (int r = 0; r < 3; r++) {
            System.arraycopy(AUG[r], 0, m[r], 0, 4);
        }
        hlRow = hlCol = -1;
        solvedRows = 0;
        emit(4, List.of(VizEvent.of(VizEvent.EventType.INIT,
                "增广矩阵 [A|b]——三行分别是三个方程")),
                "目标：消成上三角（每行首非零元素逐行右移），再回代");
        // 消 x：R2 -= R1, R3 -= R1
        rowOp(1, 0, 1);
        rowOp(2, 0, 1);
        // 消 y：R3 -= 3·R2
        rowOp(2, 1, 3);
        emit(11, List.of(VizEvent.of(VizEvent.EventType.MARK_SORTED,
                "上三角完成：z 只属于第三行 → z = 6/2 = 3")),
                "回代自底向上：一次解一个未知数");
        back(2, "z", 3);
        back(1, "y", 2);
        back(0, "x", 1);
        hlRow = hlCol = -1;
        emit(16, List.of(VizEvent.of(VizEvent.EventType.RETURN,
                "解得 x=1, y=2, z=3——O(n³) 的消元过程")),
                "为什么解不会变？三种初等变换（换行/倍乘/倍加）都保持方程组同解——这是高斯消元的合法性证明");
        return new Trace(SCHEMA, meta(), steps);
    }

    /** 行运算：m[r] -= k·m[pivot]。 */
    private void rowOp(int r, int pivot, int k) {
        for (int c = 0; c < 4; c++) {
            m[r][c] -= k * m[pivot][c];
        }
        hlRow = r;
        hlCol = 0;
        StringBuilder sb = new StringBuilder("R" + (r + 1) + " ← R" + (r + 1));
        if (k == 1) {
            sb.append(" − R").append(pivot + 1);
        } else {
            sb.append(" − ").append(k).append("·R").append(pivot + 1);
        }
        sb.append(" → [").append(fmt(m[r][0])).append(", ").append(fmt(m[r][1]))
          .append(", ").append(fmt(m[r][2])).append(" | ").append(fmt(m[r][3])).append("]");
        emit(r == 2 && pivot == 1 ? 10 : 7, List.of(VizEvent.of(VizEvent.EventType.SWAP, sb.toString(), "R" + (r + 1))),
                "倍加变换：让第 " + (pivot + 1) + " 列在 R" + (r + 1) + " 中归零——未知数逐个被消掉");
    }

    /** 回代：从下三角直接解出未知数。 */
    private void back(int row, String var, double val) {
        hlRow = row;
        hlCol = row;
        solvedRows = 3 - row;
        emit(13, List.of(VizEvent.of(VizEvent.EventType.SET,
                "第 " + (row + 1) + " 行 → " + var + " = " + fmt(val))),
                "回代：把已知未知数代入上一行——自底向上一次解一个");
    }

    private static String fmt(double v) {
        if (v == Math.round(v)) {
            return String.valueOf((long) v);
        }
        return String.format("%.2f", v);
    }

    private void emit(int line, List<VizEvent> events, String why) {
        StackFrame frame = new StackFrame("gauss",
                OrderedMaps.of("阶段", Value.prim(solvedRows > 0 ? "回代" : "消元"),
                        "已解未知数", Value.prim(String.valueOf(solvedRows))), null);
        Map<String, Layer> layers = new LinkedHashMap<>();
        layers.put("mem.stack", new StackLayer(List.of(frame)));
        layers.put("mem.heap", new HeapLayer(snapshotHeap()));
        layers.put("math.matrix", snapshotMatrix());
        steps.add(new Step(steps.size(), Loc.line(line, why),
                Collections.unmodifiableMap(layers), List.copyOf(events), new Intent(why, INVARIANT)));
    }

    private Heap snapshotHeap() {
        Map<String, Value> fields = new LinkedHashMap<>();
        if (solvedRows >= 1) fields.put("z", Value.prim(fmt(m[2][3] / m[2][2])));
        if (solvedRows >= 2) fields.put("y", Value.prim("2"));
        if (solvedRows >= 3) fields.put("x", Value.prim("1"));
        Map<String, HeapObject> objs = new LinkedHashMap<>();
        objs.put("@1", new HeapObject("@1", "solution", "GaussSolution",
                Collections.unmodifiableMap(fields), List.of()));
        return new Heap(Collections.unmodifiableMap(objs));
    }

    private MathView snapshotMatrix() {
        List<String> vals = new ArrayList<>();
        Set<Integer> done = new LinkedHashSet<>();
        Set<Integer> active = new LinkedHashSet<>();
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 4; c++) {
                vals.add(fmt(m[r][c]));
                int idx = r * 4 + c;
                if (r < solvedRows) {
                    done.add(idx);
                } else if (r == hlRow) {
                    active.add(idx);
                }
            }
        }
        Integer hlRowV = hlRow >= 0 ? hlRow : null;
        MathView.Grid grid = new MathView.Grid("aug", "增广矩阵 [A|b]（末列=b）", 3, 4,
                Collections.unmodifiableList(vals), hlRowV, null,
                Collections.unmodifiableSet(active), Collections.unmodifiableSet(done));
        return new MathView("高斯消元", List.of(grid));
    }

    private Trace.TraceMeta meta() {
        return new Trace.TraceMeta(id(), title(), codeResource(), "3 元 1 组", 3);
    }

    @Override
    public java.util.List<String> dataLines() {
        return java.util.List.of("double[][] m = {{1,1,1,6},{1,2,3,14},{1,4,9,36}};");
    }

    @Override
    public Complexity complexity() {
        return new Complexity("O(n³) 三重循环消元", "O(n²) 矩阵",
                "解 1000 元方程组 ≈ 10 亿次乘加——线性代数计算的最底层引擎（BLAS/LAPACK 优化到极值）");
    }
}
