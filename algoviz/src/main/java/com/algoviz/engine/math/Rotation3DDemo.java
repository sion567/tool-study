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
import com.algoviz.engine.math.PlotView;
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
 * 线性代数·3D 旋转：立方体线框绕三轴旋转（3×3 旋转矩阵）+ 万向锁现场。
 * 帧序列：INIT → 绕 Z 30°×3 → 绕 X 30°×2 → 绕 Y 30°×2 → 万向锁两帧（两组欧拉角同一姿态）→ RETURN。
 * 不变量：正交变换保范数——每帧 8 顶点 ‖v‖ = √3（引擎自检，违反即抛 IllegalState）。
 * 画面（math.plot/wire3d）：弱透视投影立方体（近粗远细）+ 三色坐标轴 + 左上角当前矩阵。
 */
public final class Rotation3DDemo implements Algorithm {

    private static final String SCHEMA = "1";
    private static final String INVARIANT =
            "R = Rx·Ry·Rz 正交矩阵：RᵀR=I，旋转保长度/夹角——每帧 8 顶点 ‖v‖≡√3；"
                    + "欧拉角在中间轴转 90° 时首尾轴重合（万向锁），两个输入一个输出——丢一个自由度";

    private static final double[][] BASE_VERTS = {
            {-1, -1, -1}, {1, -1, -1}, {1, 1, -1}, {-1, 1, -1},
            {-1, -1, 1}, {1, -1, 1}, {1, 1, 1}, {-1, 1, 1}};
    private static final int[][] BASE_EDGES = {
            {0, 1}, {1, 2}, {2, 3}, {3, 0}, {4, 5}, {5, 6}, {6, 7}, {7, 4},
            {0, 4}, {1, 5}, {2, 6}, {3, 7}};
    private static final double SQRT3 = Math.sqrt(3);

    private final List<Step> steps = new ArrayList<>();
    private final int stepDeg;                      // 每帧步进角（变体档位 15/30/45）
    private double[][] verts;                       // 当前 8 顶点（世界坐标）
    private double[][] rot;                         // 当前累积旋转矩阵（3×3）

    public Rotation3DDemo() {
        this(30);
    }

    /** 变体：步进角档位（15/30/45°）——🎲 换一批看同一条轨迹的粗细。 */
    public Rotation3DDemo(int stepDeg) {
        this.stepDeg = stepDeg;
    }

    @Override public String id() { return "math.rotation3d"; }
    @Override public String title() { return "线性代数·3D 旋转"; }
    @Override public String codeResource() { return "/code/Rotation3DDemo.java"; }
    @Override public String docResource() { return "/markdown/rotation3d.md"; }

    /** 3×3 矩阵乘向量（手写内联——项目惯例，无向量工具类）。 */
    static double[] mul(double[][] m, double[] v) {
        return new double[]{
                m[0][0] * v[0] + m[0][1] * v[1] + m[0][2] * v[2],
                m[1][0] * v[0] + m[1][1] * v[1] + m[1][2] * v[2],
                m[2][0] * v[0] + m[2][1] * v[1] + m[2][2] * v[2]};
    }

    /** 绕轴 θ（弧度）的基本旋转矩阵（右手定则）。 */
    static double[][] axisRot(char axis, double t) {
        double c = Math.cos(t), s = Math.sin(t);
        return switch (axis) {
            case 'x' -> new double[][]{{1, 0, 0}, {0, c, -s}, {0, s, c}};
            case 'y' -> new double[][]{{c, 0, s}, {0, 1, 0}, {-s, 0, c}};
            default -> new double[][]{{c, -s, 0}, {s, c, 0}, {0, 0, 1}};
        };
    }

    /** 3×3 矩阵乘（姿态复合 R = A·B）。 */
    static double[][] mulM(double[][] a, double[][] b) {
        double[][] r = new double[3][3];
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                r[i][j] = a[i][0] * b[0][j] + a[i][1] * b[1][j] + a[i][2] * b[2][j];
            }
        }
        return r;
    }

    /** 正交性自检：每帧 8 顶点 ‖v‖ 必须仍 = √3（保范数）。 */
    private void checkOrthonormal(double[][] m, double[][] vs) {
        for (double[] v : vs) {
            double n = Math.sqrt(v[0] * v[0] + v[1] * v[1] + v[2] * v[2]);
            if (Math.abs(n - SQRT3) > 1e-9) {
                throw new IllegalStateException("旋转破坏范数：" + n + " ≠ √3");
            }
        }
        for (int i = 0; i < 3; i++) {
            double col = m[0][i] * m[0][i] + m[1][i] * m[1][i] + m[2][i] * m[2][i];
            if (Math.abs(col - 1) > 1e-9) {
                throw new IllegalStateException("矩阵列非单位长：RᵀR ≠ I");
            }
        }
    }

    @Override
    public Trace produce() {
        steps.clear();
        verts = new double[8][3];
        for (int i = 0; i < 8; i++) {
            verts[i] = BASE_VERTS[i].clone();
        }
        rot = new double[][]{{1, 0, 0}, {0, 1, 0}, {0, 0, 1}};
        int step = Math.max(5, stepDeg);
        checkOrthonormal(rot, verts);
        emit(20, List.of(VizEvent.of(VizEvent.EventType.INIT,
                "立方体 8 顶点 12 边：姿态由 3×3 矩阵 R 决定——绕 Z 转 = 2D 旋转的推广")),
                "为什么用立方体：对称又不对称——转到任意角度都能看出「转没转」");

        spinAxis('z', 3, step);        // 绕 Z：平面旋转的推广
        spinAxis('x', 2, step);        // 绕 X：翻滚（pitch）
        spinAxis('y', 2, step);        // 绕 Y：偏航（yaw）
        gimbalLock(step);
        emit(46, List.of(VizEvent.of(VizEvent.EventType.RETURN,
                "历史：旋转矩阵直观但有万向锁 → 1843 Hamilton 四元数（4 个数，无锁）→ 现代 GPU/游戏引擎的标配")),
                "正交性是免费的午餐：RᵀR=I 保证长度/夹角不变——刚体旋转的数学定义");
        return new Trace(SCHEMA, meta(), steps);
    }

    /** 绕 axis 转 times×stepDeg 度，每步一帧。 */
    private void spinAxis(char axis, int times, int stepDeg) {
        for (int k = 1; k <= times; k++) {
            double[][] step = axisRot(axis, Math.toRadians(stepDeg));
            rot = mulM(step, rot);              // 左乘 = 世界系下绕固定轴转（外旋）
            for (int i = 0; i < 8; i++) {
                verts[i] = mul(rot, BASE_VERTS[i]);
            }
            checkOrthonormal(rot, verts);
            String axisName = axis == 'x' ? "X（翻滚 pitch）" : axis == 'y' ? "Y（偏航 yaw）" : "Z（平面旋转的推广）";
            int ax = axis == 'x' ? 0 : axis == 'y' ? 1 : 2;
            emit(31, List.of(VizEvent.of(VizEvent.EventType.SET,
                    String.format("绕 %s 第 %d 步（累计 %d°）：R = R%c(%d°)·R_prev——8 顶点范数仍 = √3 ✓",
                            axisName, k, k * stepDeg, Character.toUpperCase(axis), stepDeg))),
                    "绕 Z 是 2D 旋转的直接推广（z 列不动）；绕 X/Y 才是 3D 新增的自由度", ax);
        }
    }

    /** 万向锁现场：外旋 R = Rz(φ)·Ry(90°)·Rx(ψ) 只依赖 (φ−ψ)——φ、ψ 同加 δ，姿态不变。 */
    private void gimbalLock(int stepDeg) {
        double phi = 30, psi = 0, delta = 20;
        // 姿态 A：Rz(30°)·Ry(90°)·Rx(0°)
        double[][] ra = mulM(mulM(axisRot('z', Math.toRadians(phi)), axisRot('y', Math.PI / 2)),
                axisRot('x', Math.toRadians(psi)));
        // 姿态 B：Rz(50°)·Ry(90°)·Rx(20°)——欧拉角不同，矩阵逐元素相同 = 同一姿态
        double[][] rb = mulM(mulM(axisRot('z', Math.toRadians(phi + delta)), axisRot('y', Math.PI / 2)),
                axisRot('x', Math.toRadians(psi + delta)));
        boolean same = true;
        for (int i = 0; i < 3 && same; i++) {
            for (int j = 0; j < 3; j++) {
                if (Math.abs(ra[i][j] - rb[i][j]) > 1e-9) {
                    same = false;
                    break;
                }
            }
        }
        if (!same) {
            throw new IllegalStateException("万向锁演示失效：两组欧拉角未给出同一姿态");
        }
        double[][] savedRot = rot;
        double[][] savedVerts = verts;
        rot = ra;
        verts = new double[8][3];
        for (int i = 0; i < 8; i++) {
            verts[i] = mul(rot, BASE_VERTS[i]);
        }
        checkOrthonormal(rot, verts);
        emit(41, List.of(VizEvent.of(VizEvent.EventType.MESSAGE,
                String.format("⚠ 万向锁：Y 转 90° 后 Z 轴与 X 轴在世界系重合——(Z%1$.0f°,Y90°,X%2$.0f°) 与 (Z%3$.0f°,Y90°,X%4$.0f°) 两组欧拉角得到同一姿态——两个输入一个输出，丢一个自由度",
                        phi, psi, phi + delta, psi + delta))),
                "两个输入一个输出 = 丢一个自由度——欧拉角的死角，飞船/机械臂控制的真实事故源");
        emit(43, List.of(VizEvent.of(VizEvent.EventType.MARK_SORTED,
                "矩阵逐元素相同（已验证）——第四个参数（四元数 w）或轴角表示才无此锁")),
                "解法不在欧拉角内部——换表示：四元数 4 个数、无奇异点、插值平滑（slerp）");
        rot = savedRot;
        verts = savedVerts;
    }

    private void emit(int line, List<VizEvent> events, String why) {
        emit(line, events, why, -1);
    }

    private void emit(int line, List<VizEvent> events, String why, int axisIndex) {
        StackFrame frame = new StackFrame("rot3d",
                OrderedMaps.of("累计角", Value.prim(String.format("%.0f°", Math.toDegrees(Math.acos(rot[0][0])))),
                        "步进", Value.prim(stepDeg + "°"),
                        "顶点", Value.prim("8")), null);
        Map<String, Layer> layers = new LinkedHashMap<>();
        layers.put("mem.stack", new StackLayer(List.of(frame)));
        layers.put("mem.heap", new HeapLayer(snapshotHeap()));
        layers.put("math.plot", snapshotPlot(axisIndex));
        steps.add(new Step(steps.size(), Loc.line(line, why),
                Collections.unmodifiableMap(layers), List.copyOf(events), new Intent(why, INVARIANT)));
    }

    private Heap snapshotHeap() {
        Map<String, Value> fields = new LinkedHashMap<>();
        fields.put("步进角", Value.prim(stepDeg + "°"));
        fields.put("顶点/边", Value.prim("8 / 12"));
        fields.put("范数校验", Value.prim("‖v‖=√3 ✓"));
        Map<String, HeapObject> objs = new LinkedHashMap<>();
        objs.put("@1", new HeapObject("@1", "rot3d", "CubeState",
                Collections.unmodifiableMap(fields), List.of()));
        return new Heap(Collections.unmodifiableMap(objs));
    }

    private PlotView snapshotPlot(int axisIndex) {
        List<List<Double>> edges = new ArrayList<>();
        for (int[] e : BASE_EDGES) {
            double[] a = verts[e[0]], b = verts[e[1]];
            edges.add(List.of(a[0], a[1], a[2], b[0], b[1], b[2]));
        }
        List<List<Double>> axes = List.of(
                List.of(1.0, 0.0, 0.0, -1.0, 0.0, 0.0),
                List.of(0.0, 1.0, 0.0, 0.0, -1.0, 0.0),
                List.of(0.0, 0.0, 1.0, 0.0, 0.0, -1.0));
        List<List<Double>> vs = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            vs.add(List.of(verts[i][0], verts[i][1], verts[i][2], i == 6 ? 1.0 : 0.0));
        }
        List<String> labels = new ArrayList<>();
        for (int r = 0; r < 3; r++) {
            labels.add(String.format("[ %.2f  %.2f  %.2f ]", rot[r][0], rot[r][1], rot[r][2]));
        }
        return new PlotView(PlotView.WIRE3D, "3D 旋转", axisIndex, 4, 90,
                -3.2, 3.2, -3.2, 3.2, axes, List.of(), edges, vs, labels);
    }

    private Trace.TraceMeta meta() {
        return new Trace.TraceMeta(id(), title(), codeResource(),
                "立方体 8 顶点", 12);
    }

    @Override
    public java.util.List<String> dataLines() {
        return java.util.List.of("double stepDeg = " + stepDeg + ";   //@DATA 步进角（变体 15/30/45）");
    }

    @Override
    public Complexity complexity() {
        return new Complexity("O(V) 每次变换（V 顶点 × 9 乘加）", "O(V) 顶点",
                "GPU 的活就是矩阵乘向量：每帧百万顶点 × 一次 3×3 乘法——旋转矩阵是图形学的原子操作");
    }
}
