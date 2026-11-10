package com.algoviz.engine.math;

import com.algoviz.engine.Layer;

import java.util.List;

/**
 * 图层 math.plot：函数图像 / 几何图形的数学可视化（按 kind 分形态，渲染器各自绘制）。
 *
 * kind = unitCircle：单位圆→正弦波。angle = 当前角，pointOnCircle = 圆上动点，
 *   sineTrace/cosineTrace = 正在生长的 sin/cos 波（x=角度, y=值）。
 *
 * kind = newton：牛顿迭代求根。sineTrace = 曲线 y=f(x) 采样点列，
 *   cosineTrace = 当前切线段（两点），trace3 = 历代近似根（画在 x 轴上的点列），
 *   pointX/pointY = 当前切点，labels = 每步注解。
 *
 * kind = cordic：向量旋转逼近目标角。pointX/pointY = 当前向量端点（伪旋转，长度渐长），
 *   trace3 = 历代向量端点轨迹，labels = 迭代注解；angle = 当前已转过的角度。
 *
 * kind = riemann：定积分黎曼和。sineTrace = 曲线采样，rects = 当前 n 的矩形
 *   [x0, x1, height] 列表，labels = n/和/真值注解。
 * kind = cartesian：笛卡尔直角坐标平面。sineTrace = 累积点列 (x,y)，
 *   pointX/pointY = 当前动点（如心形线 r=1−cosθ 逐点生成）。
 *
 * xMin/xMax/yMin/yMax = 世界坐标系（渲染器映射用；unitCircle 固定 0..2π / -1..1）。
 */
public record PlotView(String kind, String name, double angle,
                       double pointX, double pointY,
                       double xMin, double xMax, double yMin, double yMax,
                       List<List<Double>> sineTrace,
                       List<List<Double>> cosineTrace,
                       List<List<Double>> trace3,
                       List<List<Double>> rects,
                       List<String> labels) implements Layer {

    public static final String UNIT_CIRCLE = "unitCircle";
    public static final String NEWTON = "newton";
    public static final String CORDIC = "cordic";
    public static final String RIEMANN = "riemann";
    public static final String CARTESIAN = "cartesian";

    /** unitCircle 旧形态的便捷构造（0..2π / -1..1 域）。 */
    public static PlotView unitCircle(String name, double angle,
                                      double px, double py,
                                      List<List<Double>> sine, List<List<Double>> cosine,
                                      List<String> labels) {
        return new PlotView(UNIT_CIRCLE, name, angle, px, py,
                0, 2 * Math.PI, -1, 1, sine, cosine, List.of(), List.of(), labels);
    }
}
