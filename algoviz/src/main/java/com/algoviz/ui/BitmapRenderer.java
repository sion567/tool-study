package com.algoviz.ui;

import com.algoviz.engine.Step;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;

import java.util.Set;

/**
 * 渲染层之一：位图判重的「空间对比」现场（math.bitmap 图层）。
 * 左：原始数据列表（橙 = 当前正在读，灰 = 已读）；右：BitSet 格子一排
 * （白 = 0 没见过，黑 = 1 见过，红圈红底 = 本步发现的重复）；紫虚线 = 预测选中。
 */
public final class BitmapRenderer {

    private static final Color CELL_WHITE = Color.web("#ffffff");
    private static final Color CELL_BLACK = Color.web("#111827");
    private static final Color DUP = Color.web("#ef4444");
    private static final Color LINE = Color.web("#94a3b8");
    private static final Color PICKED = Color.web("#8b5cf6");
    private static final Color READ = Color.web("#f59e0b");
    private static final Color READ_BG = Color.web("#fde68a");

    private BitmapRenderer() {
    }

    public static void draw(Canvas canvas, Step step, Set<Integer> picked, Set<Integer> changedBits) {
        GraphicsContext g = canvas.getGraphicsContext2D();
        g.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
        if (step == null || step.bitmap() == null) {
            return;
        }
        var bm = step.bitmap();
        double w = canvas.getWidth();
        double h = canvas.getHeight();
        double cell = Math.min(44, (w - 320) / Math.max(1, bm.bits()));
        double bitY = h * 0.40;
        double bitX = 300;

        g.setTextAlign(TextAlignment.CENTER);

        // 1. 左：原始数据（纵向列表）
        g.setFont(Font.font(13));
        for (int i = 0; i < bm.data().size(); i++) {
            double y = h * 0.14 + i * 30;
            boolean current = i == bm.readIndex();
            boolean past = i < bm.readIndex();
            if (current) {
                g.setFill(READ_BG);
                g.fillRoundRect(60, y - 18, 150, 26, 6, 6);
            }
            g.setFill(current ? Color.web("#b45309") : past ? Color.web("#9ca3af") : Color.web("#334155"));
            g.fillText("data[" + i + "] = " + bm.data().get(i) + (current ? "   ← 正在读" : ""), 135, y);
        }

        // 2. 右：BitSet 格子
        g.setFill(Color.web("#6b7280"));
        g.setFont(Font.font(12));
        g.fillText("BitSet（白=0 没见过 · 黑=1 见过 · 红圈=重复）", bitX + bm.bits() * cell / 2, bitY - 30);
        for (int b = 0; b < bm.bits(); b++) {
            double x = bitX + b * cell;
            boolean one = bm.ones().contains(b);
            boolean dup = bm.dupBit() != null && bm.dupBit() == b;
            boolean changed = changedBits != null && changedBits.contains(b);
            g.setFill(one ? CELL_BLACK : CELL_WHITE);
            g.fillRect(x, bitY, cell, cell);
            if (dup) {
                g.setStroke(DUP);
                g.setLineWidth(3.5);
                g.strokeRect(x - 2, bitY - 2, cell + 4, cell + 4);
                g.setFill(DUP);
            } else {
                g.setStroke(changed ? Color.web("#eab308") : LINE);
                g.setLineWidth(changed ? 2.5 : 1.4);
                g.strokeRect(x, bitY, cell, cell);
                g.setFill(Color.web("#6b7280"));
            }
            g.setFont(Font.font(14));
            g.fillText(String.valueOf(one ? 1 : 0), x + cell / 2, bitY + cell / 2 + 5);
            g.setFill(Color.web("#6b7280"));
            g.setFont(Font.font(10));
            g.fillText("[" + b + "]", x + cell / 2, bitY + cell + 14);
        }
    }

    /** 供预测模式点击命中：返回 bit 下标，未命中 -1。 */
    public static int hitIndex(Canvas canvas, Step step, double x, double y) {
        if (step == null || step.bitmap() == null) {
            return -1;
        }
        var bm = step.bitmap();
        double w = canvas.getWidth();
        double h = canvas.getHeight();
        double cell = Math.min(44, (w - 320) / Math.max(1, bm.bits()));
        double bitY = h * 0.40;
        double bitX = 300;
        int col = (int) Math.floor((x - bitX) / cell);
        if (y >= bitY && y <= bitY + cell && col >= 0 && col < bm.bits()) {
            return col;
        }
        return -1;
    }
}
