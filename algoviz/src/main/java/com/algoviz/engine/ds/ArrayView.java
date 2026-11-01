package com.algoviz.engine.ds;

import com.algoviz.engine.Layer;

import java.util.List;

/**
 * 图层 ds.array：数组的算法语义视图（蓝图 §2.2 视图 B）。
 * compareA/B = 本步比较的下标（橙）；swapA/B = 刚交换/移动的下标（红）；
 * sortedFrom = 尾部已就位起点（冒泡：i 及之后绿）；sortedTo = 头部已就位终点（选择/插入：i 及之前绿），-1 表示无。
 */
public record ArrayView(String arrayId, String name, List<Integer> values,
                        Integer compareA, Integer compareB,
                        Integer swapA, Integer swapB,
                        int sortedFrom, int sortedTo) implements Layer {
}
