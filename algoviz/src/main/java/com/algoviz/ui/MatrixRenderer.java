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
        double cell = baseCell(w, h);

        // A×B=C 三网格全为 ≤2×2 时保留矩阵乘法专用布局；其余一律流式自适应
        if (isMultiplicationLayout(mv)) {
            MathView.Grid a = mv.grids().get(0);
            MathView.Grid b = mv.grids().get(1);
            MathView.Grid c = mv.grids().get(2);
            double topY = h * 0.10;
            double bottomY = h * 0.58;
            double ax = w * 0.10;
            double bx = w * 0.56;
            double cx = w * 0.36;
            g.setTextAlign(TextAlignment.CENTER);
            drawGrid(g, a, ax, topY, cell, cell, picked != null, changedKeys, null);
            g.setFill(Color.web("#6b7280"));
            g.setFont(Font.font(18));
            g.fillText("×", ax + a.cols() * cell + 18, topY + a.rows() * cell / 2 + 6);
            drawGrid(g, b, bx, topY, cell, cell, false, changedKeys, null);
            g.fillText("=", w / 2 - 10, topY + Math.max(a.rows(), b.rows()) * cell + 34);
            drawGrid(g, c, cx, bottomY, cell, cell, true, changedKeys, picked);
            return;
        }

        g.setTextAlign(TextAlignment.CENTER);
        double[][] pos = layoutGrids(mv, w, h);
        for (int i = 0; i < mv.grids().size(); i++) {
            MathView.Grid grid = mv.grids().get(i);
            drawGrid(g, grid, pos[i][0], pos[i][1], pos[i][3], pos[i][2],
                    picked != null && i == mv.grids().size() - 1, changedKeys,
                    picked != null && i == mv.grids().size() - 1 ? picked : null);
        }
    }

    /** A×B=C 专用布局判定：恰好 3 网格且前两个都 ≤2×2（矩阵乘法的形状签名）。 */
    private static boolean isMultiplicationLayout(MathView mv) {
        if (mv.grids().size() != 3) {
            return false;
        }
        MathView.Grid a = mv.grids().get(0);
        MathView.Grid b = mv.grids().get(1);
        return a.rows() <= 2 && a.cols() <= 2 && b.rows() <= 2 && b.cols() <= 2;
    }

    private static double baseCell(double w, double h) {
        return Math.min(46, Math.min(w / 14, h / 9));
    }

    /** 流式自适应布局：每网格按内容定格宽（钳到可用宽），一行排不下自动换行；
     *  总高超出画布时整体等比缩小——返回每网格 {x, y, cellW, cellH}。 */
    private static double[][] layoutGrids(MathView mv, double w, double h) {
        int n = mv.grids().size();
        double[][] pos = new double[n][4];
        double base = baseCell(w, h);
        double margin = w * 0.04;
        double top = h * 0.08;
        double[] cw = new double[n];
        double[] bh = new double[n];
        for (int i = 0; i < n; i++) {
            MathView.Grid grid = mv.grids().get(i);
            cw[i] = gridCell(grid, base);
            double maxCell = (w - margin * 2 - 12.0 * (grid.cols() - 1)) / grid.cols();
            if (cw[i] > maxCell) {
                cw[i] = Math.max(base, maxCell);
            }
            bh[i] = grid.rows() * base + 26;   // 含 label 行
        }
        double x = margin;
        double y = h * 0.10;
        double rowH = 0;
        for (int i = 0; i < n; i++) {
            MathView.Grid grid = mv.grids().get(i);
            double labelW = grid.label().length() * 12.0 * 0.62;   // label 估宽（中英混排近似）
            double blockW = Math.max(grid.cols() * cw[i], labelW); // label 计入块宽——长 label 不与右侧网格重叠
            if (x + blockW > w - margin && x > margin) {   // 换行
                x = margin;
                y += rowH + 34;
                rowH = 0;
            }
            double cellX = x + (blockW - grid.cols() * cw[i]) / 2;   // 格子在块内居中（label 居中对齐）
            pos[i][0] = cellX;
            pos[i][1] = y;
            pos[i][2] = cw[i];
            pos[i][3] = base;
            x += blockW + 26;
            rowH = Math.max(rowH, bh[i]);
        }
        double contentH = y + rowH - top;
        double availH = h - top - h * 0.04;
        if (contentH > availH) {   // 整体等比缩小，所有网格同屏
            double s = availH / contentH;
            for (int i = 0; i < n; i++) {
                pos[i][0] = margin + (pos[i][0] - margin) * s;
                pos[i][1] = top + (pos[i][1] - top) * s;
                pos[i][2] *= s;
                pos[i][3] *= s;
            }
        }
        return pos;
    }

    private static void drawGrid(GraphicsContext g, MathView.Grid grid, double x, double y,
                                 double cell, double cellW, boolean clickable, Set<String> changedKeys,
                                 Set<Integer> picked) {
        double cw = cellW;   // 格宽由布局层统一决定：长公式横向加宽而非缩字
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

    /** 供预测模式点击命中：只认最后一个网格（输出），返回其格下标，未命中 -1。几何与 draw() 一致。 */
    public static int hitIndex(Canvas canvas, Step step, double x, double y) {
        if (step == null || step.math() == null || step.math().grids().isEmpty()) {
            return -1;
        }
        MathView mv = step.math();
        int last = mv.grids().size() - 1;
        MathView.Grid c = mv.grids().get(last);
        double w = canvas.getWidth();
        double h = canvas.getHeight();
        double base = baseCell(w, h);
        double cx0;
        double cy0;
        double cellW;
        if (isMultiplicationLayout(mv)) {
            cellW = gridCell(c, base);
            cx0 = w * 0.36;
            cy0 = h * 0.58;
        } else {
            double[][] pos = layoutGrids(mv, w, h);
            cellW = pos[last][2];
            cx0 = pos[last][0];
            cy0 = pos[last][1];
            base = pos[last][3];
        }
        int col = (int) Math.floor((x - cx0) / cellW);
        int row = (int) Math.floor((y - cy0) / base);
        if (row >= 0 && row < c.rows() && col >= 0 && col < c.cols()) {
            return row * c.cols() + col;
        }
        return -1;
    }
}
