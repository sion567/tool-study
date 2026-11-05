package com.algoviz.ui;

import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

/**
 * 复杂度 / 对比两个 Tab 的静态内容（架构归档 §七债务① 的轻量部分）。
 * MainApp 只负责挂 Tab；文案与布局收敛在这里。
 */
public final class TabContents {

    public static javafx.scene.layout.VBox complexity() {
        Label title = new Label("复杂度可视化");
        title.setStyle("-fx-font-size:15px; -fx-font-weight:bold; -fx-text-fill:#111;");
        Label desc = new Label("中间画布显示 O(1)~O(n²) 增长曲线（切到此 Tab 即绘制）。"
                + "曲线在 n=50 内对比；不同算法可切过去按 🎲 换 n 看步数变化。");
        desc.setWrapText(true);
        desc.setStyle("-fx-text-fill:#374151; -fx-font-size:12px;");
        Label table = new Label(ComplexityPanel.feelTable());
        table.setStyle("-fx-font-family:monospace; -fx-font-size:12px; -fx-text-fill:#111827;"
                + " -fx-background-color:#f6f8fa; -fx-padding:10; -fx-background-radius:4;");
        var box = new javafx.scene.layout.VBox(8, title, desc, table);
        box.setPadding(new Insets(10));
        return box;
    }

    public static javafx.scene.layout.VBox compare() {
        Label title = new Label("实测对比：同一批数据，六种排序各跑一遍");
        title.setStyle("-fx-font-size:15px; -fx-font-weight:bold; -fx-text-fill:#111;");
        Label desc = new Label("纵轴 = 步数（trace 长度），横轴 = n。固定种子随机数据，六个算法吃完全相同的输入。"
                + "看冒泡（红）与快排（绿）的裂口随 n 拉大——公式里的 O(n²) vs O(n log n) 落到实数上就是这样。");
        desc.setWrapText(true);
        desc.setStyle("-fx-text-fill:#374151; -fx-font-size:12px;");
        var box = new javafx.scene.layout.VBox(8, title, desc);
        box.setPadding(new Insets(10));
        return box;
    }
}
