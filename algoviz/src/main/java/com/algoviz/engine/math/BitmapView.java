package com.algoviz.engine.math;

import com.algoviz.engine.Layer;

import java.util.List;
import java.util.Set;

/**
 * 图层 math.bitmap：位图判重的空间优化现场。
 * bits = 位图大小；data = 原始数据（待查重的数）；readIndex = 当前读到第几个；
 * ones = 已置 1 的位（见过）；dupBit = 本步刚发现的重复位（null = 无，红闪）。
 * 画面不是「一步步执行」，而是空间对比：原始数据 vs 一排 bit 格子。
 */
public record BitmapView(String name, int bits, List<Integer> data, int readIndex,
                         Set<Integer> ones, Integer dupBit) implements Layer {
}
