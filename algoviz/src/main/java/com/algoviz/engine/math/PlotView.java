package com.algoviz.engine.math;

import com.algoviz.engine.Layer;

import java.util.List;

/**
 * 图层 math.plot：函数图像 / 几何图形的数学可视化。
 * 单位圆演示：angle = 当前角度（弧度），pointOnCircle = 圆上的动点 (x,y)，
 * sineTrace = 已绘制的正弦波点列 (x=角度, y=sin值)，cosineTrace 同理。
 * 画面 = 左边单位圆（动点）+ 右边正在生长的波。
 */
public record PlotView(String name, double angle,
                       double pointX, double pointY,
                       List<List<Double>> sineTrace,
                       List<List<Double>> cosineTrace,
                       List<String> labels) implements Layer {
}
