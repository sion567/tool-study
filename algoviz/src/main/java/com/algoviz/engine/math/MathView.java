package com.algoviz.engine.math;

import com.algoviz.engine.Layer;

import java.util.List;
import java.util.Set;

/**
 * 图层 math.matrix：矩阵的执行现场（数学域第一个图层，蓝图 §2.4：数学的命门在意图层）。
 * 多个网格（A/B/C…），每格字符串值（将来可放分数/符号）；高亮语义：
 * hlRow/hlCol = 正在读取的行/列（橙）；activeCells = 本步涉及的数据格（黄圈）；
 * doneCells = 已定格的结果格（绿）；pointerCells = 本步只变化了高亮、值没变的格。
 */
public record MathView(String name, List<Grid> grids) implements Layer {

    public record Grid(String id, String label, int rows, int cols, List<String> values,
                       Integer hlRow, Integer hlCol,
                       Set<Integer> activeCells, Set<Integer> doneCells) {

        /** 行主序下标。 */
        public int idx(int r, int c) {
            return r * cols + c;
        }

        public String value(int r, int c) {
            return values.get(idx(r, c));
        }
    }
}
