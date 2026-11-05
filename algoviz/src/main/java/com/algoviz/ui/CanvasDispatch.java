package com.algoviz.ui;

import com.algoviz.engine.Step;
import javafx.scene.canvas.Canvas;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 画布分发器（架构归档 §七债务④）：把「图层 → 渲染器」的 if/else 链收敛为有序数组。
 * 顺序 = 优先级（bitmap 在 math 之后判定；bar 永远兜底）。
 * 每个条目自己算 diff 集、自己画——新增图层 = 加一个条目，MainApp 不再动。
 */
public final class CanvasDispatch {

    @FunctionalInterface
    private interface Painter {
        /** 命中该层则绘制并返回 true（MainApp 拿到 true 即停）。 */
        boolean draw(Canvas canvas, Step prev, Step cur, Set<Integer> picked);
    }

    @FunctionalInterface
    private interface Picker {
        int hit(Canvas canvas, Step cur, double x, double y);
    }

    private static final Painter[] PAINTERS = {
            // ---- 链表 ----
            (c, p, cur, pk) -> {
                if (cur.list() == null) return false;
                ListRenderer.draw(c, cur, pk, DiffHighlighter.changedArrows(list(p), cur.list()));
                return true;
            },
            // ---- 树 ----
            (c, p, cur, pk) -> {
                if (cur.tree() == null) return false;
                TreeRenderer.draw(c, cur, pk,
                        DiffHighlighter.changedTreeNodes(tree(p), cur.tree()),
                        DiffHighlighter.changedTreeEdges(tree(p), cur.tree()));
                return true;
            },
            // ---- B+ 树 ----
            (c, p, cur, pk) -> {
                if (cur.btree() == null) return false;
                BTreeRenderer.draw(c, cur, pk,
                        BTreeRenderer.changedNodes(p == null ? null : p.btree(), cur.btree()));
                return true;
            },
            // ---- 图 ----
            (c, p, cur, pk) -> {
                if (cur.graph() == null) return false;
                GraphRenderer.draw(c, cur, pk,
                        DiffHighlighter.changedGraphNodes(graph(p), cur.graph()));
                return true;
            },
            // ---- 哈希表 ----
            (c, p, cur, pk) -> {
                if (cur.hash() == null) return false;
                HashRenderer.draw(c, cur, pk,
                        HashRenderer.changedArrows(p == null ? null : p.hash(), cur.hash()));
                return true;
            },
            // ---- 时序（设计模式/并发/分布式剧本）----
            (c, p, cur, pk) -> {
                if (cur.seq() == null) return false;
                SequenceRenderer.draw(c, cur, pk,
                        DiffHighlighter.changedSeqParticipants(seq(p), cur.seq()));
                return true;
            },
            // ---- 矩阵（math.matrix；bitmap 帧排除）----
            (c, p, cur, pk) -> {
                if (cur.math() == null || cur.bitmap() != null) return false;
                MatrixRenderer.draw(c, cur, pk,
                        DiffHighlighter.changedMatrixCells(math(p), cur.math()));
                return true;
            },
            // ---- 函数图像 ----
            (c, p, cur, pk) -> {
                if (cur.plot() == null) return false;
                PlotRenderer.draw(c, cur);
                return true;
            },
            // ---- 位图（bits: 前缀换算成下标高亮）----
            (c, p, cur, pk) -> {
                if (cur.bitmap() == null) return false;
                Set<Integer> changedBits = new LinkedHashSet<>();
                for (String k : DiffHighlighter.changedMatrixCells(math(p), cur.math())) {
                    if (k.startsWith("bits:")) {
                        changedBits.add(Integer.parseInt(k.substring(5)));
                    }
                }
                BitmapRenderer.draw(c, cur, pk, changedBits);
                return true;
            },
            // ---- 兜底：柱状数组 ----
            (c, p, cur, pk) -> {
                BarRenderer.draw(c, cur, pk, DiffHighlighter.changedIndices(
                        p == null ? null : p.array(), cur.array()));
                return true;
            },
    };

    private static final Picker[] PICKERS = {
            (c, cur, x, y) -> cur.list() == null ? -1
                    : ListRenderer.hitIndex(c.getWidth(), c.getHeight(), cur.list().nodes().size(), x, y),
            (c, cur, x, y) -> cur.tree() == null ? -1 : TreeRenderer.hitIndex(c, cur, x, y),
            (c, cur, x, y) -> cur.graph() == null ? -1 : GraphRenderer.hitIndex(c, cur, x, y),
            (c, cur, x, y) -> cur.hash() == null ? -1 : HashRenderer.hitIndex(c, cur, x, y),
            (c, cur, x, y) -> cur.btree() == null ? -1 : BTreeRenderer.hitIndex(c, cur, x, y),
            (c, cur, x, y) -> cur.seq() == null ? -1 : SequenceRenderer.hitIndex(c, cur, x, y),
            (c, cur, x, y) -> cur.math() == null ? -1 : MatrixRenderer.hitIndex(c, cur, x, y),
            (c, cur, x, y) -> cur.bitmap() == null ? -1 : BitmapRenderer.hitIndex(c, cur, x, y),
            (c, cur, x, y) -> cur.array() == null ? -1
                    : BarRenderer.hitIndex(c.getWidth(), c.getHeight(), cur.array().values().size(), x, y),
    };

    /** 按优先级渲染：第一个命中该层的渲染器胜出。 */
    public static void paint(Canvas canvas, Step prev, Step cur, Set<Integer> picked) {
        for (Painter p : PAINTERS) {
            if (p.draw(canvas, prev, cur, picked)) {
                return;
            }
        }
    }

    /** 预测模式点击命中：返回该层内被点中的元素下标，未命中 -1。 */
    public static int pick(Canvas canvas, Step cur, double x, double y) {
        for (Picker p : PICKERS) {
            int i = p.hit(canvas, cur, x, y);
            if (i >= 0) {
                return i;
            }
        }
        return -1;
    }

    private static com.algoviz.engine.ds.ListView list(Step s) {
        return s == null ? null : s.list();
    }

    private static com.algoviz.engine.ds.TreeView tree(Step s) {
        return s == null ? null : s.tree();
    }

    private static com.algoviz.engine.ds.GraphView graph(Step s) {
        return s == null ? null : s.graph();
    }

    private static com.algoviz.engine.ds.SeqView seq(Step s) {
        return s == null ? null : s.seq();
    }

    private static com.algoviz.engine.math.MathView math(Step s) {
        return s == null ? null : s.math();
    }

    private CanvasDispatch() {
    }
}
