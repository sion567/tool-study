package com.algoviz.ui;

import com.algoviz.engine.Step;
import com.algoviz.engine.ds.SeqView;
import com.algoviz.engine.math.MathView;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;

import java.util.Set;

/**
 * 渲染层之一：数学域的矩阵现场（math.matrix 图层）。
 * A(左上) × B(右上) = C(下方居中)：橙行/橙列 = 正在读取的行/列，
 * 黄圈 = 本步值变化的格子，绿 = 已定格的结果格，紫虚线 = 预测模式选中（只支持 C 网格）。
 */
public final class MatrixRenderer {

    private static final Color CELL_BG = Color.web("#e8eefc");
    private static final Color CELL_LINE = Color.web("#94a3b8");
    private static final Color HL = Color.web("#f59e0b");
    private static final Color CHANGED = Color.web("#eab308");
    private static final Color DONE = Color.web("#22c55e");
    private static final Color PICKED = Color.web("#8b5cf6");

    private MatrixRenderer() {
    }

    public static void draw(Canvas canvas, Step step, Set<Integer> picked, Set<String> changedKeys) {
        GraphicsContext g = canvas.getGraphicsContext2D();
        g.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
        if (step == null || step.math() == null) {
            return;
        }
        MathView mv = step.math();
        if (mv.grids().isEmpty()) {
            return;
        }
        double w = canvas.getWidth();
        double h = canvas.getHeight();
        MathView.Grid a = mv.grids().get(0);
        MathView.Grid b = mv.grids().size() > 1 ? mv.grids().get(1) : null;
        MathView.Grid c = mv.grids().size() > 2 ? mv.grids().get(2) : null;

        double cell = Math.min(46, Math.min(w / 14, h / 9));
        double topY = h * 0.10;
        double bottomY = h * 0.58;

        double ax = w * 0.10;
        double bx = w * 0.56;
        double cx = w * 0.36;

        g.setTextAlign(TextAlignment.CENTER);
        drawGrid(g, a, ax, topY, cell, picked != null, changedKeys, null);
        if (b != null) {
            g.setFill(Color.web("#6b7280"));
            g.setFont(Font.font(18));
            g.fillText("×", ax + a.cols() * cell + 18, topY + a.rows() * cell / 2 + 6);
            drawGrid(g, b, bx, topY, cell, false, changedKeys, null);
        }
        if (c != null) {
            g.setFill(Color.web("#6b7280"));
            g.setFont(Font.font(18));
            g.fillText("=", w / 2 - 10, topY + Math.max(a.rows(), b == null ? 0 : b.rows()) * cell + 34);
            drawGrid(g, c, cx, bottomY, cell, true, changedKeys, picked);
        }
    }

    private static void drawGrid(GraphicsContext g, MathView.Grid grid, double x, double y,
                                 double cell, boolean clickable, Set<String> changedKeys,
                                 Set<Integer> picked) {
        g.setFill(Color.web("#111827"));
        g.setFont(Font.font(12));
        g.setTextAlign(TextAlignment.CENTER);
        g.fillText(grid.label(), x + grid.cols() * cell / 2, y - 8);

        for (int r = 0; r < grid.rows(); r++) {
            for (int c = 0; c < grid.cols(); c++) {
                int idx = grid.idx(r, c);
                double cx = x + c * cell;
                double cy = y + r * cell;
                boolean done = grid.doneCells().contains(idx);
                boolean hl = (grid.hlRow() != null && grid.hlRow() == r)
                        || (grid.hlCol() != null && grid.hlCol() == c);
                boolean active = grid.activeCells().contains(idx);
                boolean changed = changedKeys != null && changedKeys.contains(grid.id() + ":" + idx);
                g.setFill(done ? Color.web("#dcfce7") : CELL_BG);
                g.fillRect(cx, cy, cell, cell);
                if (done) {
                    g.setStroke(DONE);
                } else if (active || changed) {
                    g.setStroke(CHANGED);
                } else if (hl) {
                    g.setStroke(HL);
                } else {
                    g.setStroke(CELL_LINE);
                }
                g.setLineWidth(active || changed ? 2.6 : 1.4);
                g.strokeRect(cx, cy, cell, cell);
                if (clickable && picked != null && picked.contains(idx)) {
                    g.setStroke(PICKED);
                    g.setLineWidth(2.5);
                    g.setLineDashes(6, 4);
                    g.strokeRect(cx - 3, cy - 3, cell + 6, cell + 6);
                    g.setLineDashes();
                }
                g.setFill(done ? Color.web("#166534") : Color.web("#1e3a8a"));
                g.setFont(Font.font(13));
                g.fillText(grid.values().get(idx), cx + cell / 2, cy + cell / 2 + 5);
            }
        }
    }

    /** 供预测模式点击命中：只认 C 网格（输出），返回其格下标，未命中 -1。 */
    public static int hitIndex(Canvas canvas, Step step, double x, double y) {
        if (step == null || step.math() == null || step.math().grids().size() < 3) {
            return -1;
        }
        MathView mv = step.math();
        MathView.Grid c = mv.grids().get(2);
        double w = canvas.getWidth();
        double h = canvas.getHeight();
        double cell = Math.min(46, Math.min(w / 14, h / 9));
        double cx0 = w * 0.36;
        double cy0 = h * 0.58;
        int col = (int) Math.floor((x - cx0) / cell);
        int row = (int) Math.floor((y - cy0) / cell);
        if (row >= 0 && row < c.rows() && col >= 0 && col < c.cols()) {
            return row * c.cols() + col;
        }
        return -1;
    }
}
