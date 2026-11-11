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
        double cw = gridCell(grid, cell);   // 格宽可超出基宽：长公式格子横向加宽而非缩字
        g.setFill(Color.web("#111827"));
        g.setFont(Font.font(12));
        g.setTextAlign(TextAlignment.CENTER);
        g.fillText(grid.label(), x + grid.cols() * cw / 2, y - 8);

        for (int r = 0; r < grid.rows(); r++) {
            for (int c = 0; c < grid.cols(); c++) {
                int idx = grid.idx(r, c);
                double cx = x + c * cw;
                double cy = y + r * cell;
                boolean done = grid.doneCells().contains(idx);
                boolean hl = (grid.hlRow() != null && grid.hlRow() == r)
                        || (grid.hlCol() != null && grid.hlCol() == c);
                boolean active = grid.activeCells().contains(idx);
                boolean changed = changedKeys != null && changedKeys.contains(grid.id() + ":" + idx);
                g.setFill(done ? Color.web("#dcfce7") : CELL_BG);
                g.fillRect(cx, cy, cw, cell);
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
                g.strokeRect(cx, cy, cw, cell);
                if (clickable && picked != null && picked.contains(idx)) {
                    g.setStroke(PICKED);
                    g.setLineWidth(2.5);
                    g.setLineDashes(6, 4);
                    g.strokeRect(cx - 3, cy - 3, cw + 6, cell + 6);
                    g.setLineDashes();
                }
                g.setFill(done ? Color.web("#166534") : Color.web("#1e3a8a"));
                g.setFont(Font.font(fitFontSize(grid.values().get(idx), cw)));
                g.fillText(grid.values().get(idx), cx + cw / 2, cy + cell / 2 + 5);
            }
        }
    }

    /** 网格实际格宽：最长文本按 13px 估宽，允许加宽到基宽 2.6 倍（横向空间足够时保住可读字号）。 */
    private static double gridCell(MathView.Grid grid, double base) {
        double maxEm = 1;
        for (String v : grid.values()) {
            maxEm = Math.max(maxEm, emWidth(v));
        }
        double need = maxEm * 13 * 0.62 + 12;
        return Math.max(base, Math.min(base * 2.6, need));
    }

    /** 文本估宽（em）：CJK ≈ 1.0，其余 ≈ 0.58。 */
    private static double emWidth(String text) {
        double em = 0;
        for (int i = 0; i < text.length(); i++) {
            em += text.charAt(i) > 0x2E80 ? 1.0 : 0.58;
        }
        return em;
    }

    /** 格子文本自适应字号：中文≈1em、ASCII≈0.58em 估宽，超出格子就缩（下限 9），长公式不再溢出。 */
    private static int fitFontSize(String text, double cell) {
        double em = 0;
        for (int i = 0; i < text.length(); i++) {
            em += text.charAt(i) > 0x2E80 ? 1.0 : 0.58;
        }
        double fit = cell * 0.92 / Math.max(em, 1);
        return (int) Math.max(9, Math.min(13, fit));
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
        double base = Math.min(46, Math.min(w / 14, h / 9));
        double cell = gridCell(c, base);
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
