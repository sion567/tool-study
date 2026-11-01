package com.algoviz.engine.math;

import com.algoviz.engine.Algorithm;
import com.algoviz.engine.Intent;
import com.algoviz.engine.Layer;
import com.algoviz.engine.Loc;
import com.algoviz.engine.Step;
import com.algoviz.engine.Value;
import com.algoviz.engine.VizEvent;
import com.algoviz.engine.math.MathView.Grid;
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
 * 矩阵乘法生产者（数学域第一个演示）：
 * C[i][j] = Σ A[i][k]·B[k][j]——部分和逐帧累加，让「求和符号 Σ」长出画面。
 * 不变量：C 的 (i,j) 只由 A 的第 i 行和 B 的第 j 列决定，与其他行列无关（所以能并行/分块）。
 */
public final class MatrixMultiply implements Algorithm {

    private static final String SCHEMA = "1";
    private static final String INVARIANT =
            "C(i,j) 只由 A 的第 i 行和 B 的第 j 列决定——行列独立，与别的格子无关（可并行/可分块）";

    private static final int L_A = 3;
    private static final int L_B = 4;
    private static final int L_C = 5;
    private static final int L_FOR_J = 7;
    private static final int L_BODY = 9;
    private static final int L_FOR_K = 8;
    private static final int L_END = 13;

    private static final int M = 2;   // A 的行
    private static final int K = 3;   // A 的列 = B 的行
    private static final int N = 2;   // B 的列

    private static final int[][] A = {{1, 2, 3}, {4, 5, 6}};
    private static final int[][] B = {{7, 8}, {9, 10}, {11, 12}};

    private final int[][] c = new int[M][N];
    private final Set<Integer> doneCells = new LinkedHashSet<>();
    private final List<Step> steps = new ArrayList<>();

    // 当前焦点（emit 时进入快照，-1 = 无）
    private int curI = -1;
    private int curJ = -1;
    private int curK = -1;

    @Override
    public String id() {
        return "math.matrix.multiply";
    }

    @Override
    public String title() {
        return "矩阵乘法";
    }

    @Override
    public String codeResource() {
        return "/code/MatrixMultiply.java";
    }

    @Override
    public String docResource() {
        return "/markdown/matrix-multiply.md";
    }

    @Override
    public Trace produce() {
        // 重复 produce：清零全部状态（Registry 缓存实例）
        doneCells.clear();
        steps.clear();
        for (int[] row : c) {
            java.util.Arrays.fill(row, 0);
        }
        curI = -1;
        curJ = -1;
        curK = -1;
        emit(L_A, List.of(VizEvent.of(VizEvent.EventType.INIT, "A(2×3)、B(3×2) 就位，C 全 0")),
                "A 和 B 摆上桌：内维相等（3=3）才能乘");
        for (int i = 0; i < M; i++) {
            for (int j = 0; j < N; j++) {
                c[i][j] = 0;
                curI = i;
                curJ = j;
                curK = -1;
                emit(L_FOR_J, List.of(VizEvent.of(VizEvent.EventType.INIT,
                        "目标格 C[" + i + "][" + j + "] 清零——准备累加", "C:" + (i * N + j))),
                        "C(" + i + "," + j + ") = A 的第 " + i + " 行 · B 的第 " + j + " 列");
                int sum = 0;
                for (int k = 0; k < K; k++) {
                    curK = k;
                    int term = A[i][k] * B[k][j];
                    sum += term;
                    c[i][j] = sum;
                    emit(L_BODY, List.of(VizEvent.of(VizEvent.EventType.SET,
                            "A[" + i + "][" + k + "]×B[" + k + "][" + j + "] = " + A[i][k] + "×" + B[k][j]
                                    + " → C[" + i + "][" + j + "] 累加为 " + sum,
                            "A:" + (i * K + k), "B:" + (k * N + j), "C:" + (i * N + j))),
                            "Σ 的第 " + (k + 1) + " 项：对应相乘再相加——第 " + (k + 1) + " 滴涟漪");
                }
                curK = -1;
                doneCells.add(i * N + j);
                emit(L_FOR_K, List.of(VizEvent.of(VizEvent.EventType.MARK_SORTED,
                        "C[" + i + "][" + j + "] 定格 = " + sum + "（三项加完）", "C:" + (i * N + j))),
                        "K 项加完：这个格子从此不再变（行列独立的直接推论）");
            }
        }
        curI = -1;
        curJ = -1;
        emit(L_END, List.of(VizEvent.of(VizEvent.EventType.RETURN,
                "完成：C = [[" + c[0][0] + ", " + c[0][1] + "], [" + c[1][0] + ", " + c[1][1] + "]]")),
                "2×3 · 3×2 = 2×2：内维消化掉，剩下外维");
        return new Trace(SCHEMA, meta(new int[0]), steps);
    }

    private void emit(int line, List<VizEvent> events, String why) {
        StackFrame frame = new StackFrame("multiply",
                frameLocals(), null);
        Map<String, Layer> layers = new LinkedHashMap<>();
        layers.put("mem.stack", new StackLayer(List.of(frame)));
        layers.put("mem.heap", new HeapLayer(snapshotHeap()));
        layers.put("math.matrix", snapshotMath());
        steps.add(new Step(steps.size(), Loc.line(line, why),
                Collections.unmodifiableMap(layers), List.copyOf(events), new Intent(why, INVARIANT)));
    }

    /** 有序局部变量表：Map.of 的迭代顺序被 JDK SALT 随机化，golden 需要确定序。 */
    private static Map<String, Value> frameLocals() {
        Map<String, Value> m = new LinkedHashMap<>();
        m.put("A", Value.ref("@1"));
        m.put("B", Value.ref("@2"));
        m.put("C", Value.ref("@3"));
        return m;
    }

    private Heap snapshotHeap() {
        Map<String, HeapObject> objs = new LinkedHashMap<>();
        Map<String, Value> fa = new LinkedHashMap<>();
        for (int i = 0; i < M; i++) {
            for (int k = 0; k < K; k++) {
                fa.put("a" + i + k, Value.prim(A[i][k]));
            }
        }
        objs.put("@1", new HeapObject("@1", "A", "Matrix2x3", Collections.unmodifiableMap(fa), List.of()));
        Map<String, Value> fb = new LinkedHashMap<>();
        for (int k = 0; k < K; k++) {
            for (int j = 0; j < N; j++) {
                fb.put("b" + k + j, Value.prim(B[k][j]));
            }
        }
        objs.put("@2", new HeapObject("@2", "B", "Matrix3x2", Collections.unmodifiableMap(fb), List.of()));
        Map<String, Value> fc = new LinkedHashMap<>();
        for (int i = 0; i < M; i++) {
            for (int j = 0; j < N; j++) {
                fc.put("c" + i + j, Value.prim(c[i][j]));
            }
        }
        objs.put("@3", new HeapObject("@3", "C", "Matrix2x2", Collections.unmodifiableMap(fc), List.of()));
        return new Heap(Collections.unmodifiableMap(objs));
    }

    private MathView snapshotMath() {
        List<Grid> grids = new ArrayList<>();
        // A：当前行高亮；本步用到的格子
        Set<Integer> aActive = new LinkedHashSet<>();
        if (curI >= 0 && curK >= 0) {
            aActive.add(curI * K + curK);
        }
        grids.add(new Grid("A", "A 2×3", M, K, textMatrix(A, M, K), curI >= 0 ? curI : null, null,
                aActive, Set.of()));
        // B：当前列高亮
        Set<Integer> bActive = new LinkedHashSet<>();
        if (curK >= 0 && curJ >= 0) {
            bActive.add(curK * N + curJ);
        }
        grids.add(new Grid("B", "B 3×2", K, N, textMatrix(B, K, N), null, curJ >= 0 ? curJ : null,
                bActive, Set.of()));
        // C：目标格 + 已定格
        List<String> cv = new ArrayList<>();
        for (int i = 0; i < M; i++) {
            for (int j = 0; j < N; j++) {
                cv.add(String.valueOf(c[i][j]));
            }
        }
        Set<Integer> cActive = new LinkedHashSet<>();
        if (curI >= 0 && curJ >= 0) {
            cActive.add(curI * N + curJ);
        }
        grids.add(new Grid("C", "C 2×2", M, N, List.copyOf(cv), null, null,
                cActive, new LinkedHashSet<>(doneCells)));
        return new MathView("矩阵乘法", Collections.unmodifiableList(grids));
    }

    private static List<String> textMatrix(int[][] m, int rows, int cols) {
        List<String> out = new ArrayList<>();
        for (int r = 0; r < rows; r++) {
            for (int cc = 0; cc < cols; cc++) {
                out.add(String.valueOf(m[r][cc]));
            }
        }
        return out;
    }
}
