package com.algoviz.ui;

import com.algoviz.engine.Step;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;

/** 意图层常驻显示（蓝图 §2.4）：当前不变量 + 本步为什么。 */
public final class IntentBar extends HBox {

    private final Label label = new Label();

    public IntentBar() {
        label.setWrapText(true);
        setStyle(Theme.intentBarStyle());
        HBox.setHgrow(label, Priority.ALWAYS);
        getChildren().add(label);
    }

    /** 主题切换后重刷配色。 */
    public void applyTheme() {
        setStyle(Theme.intentBarStyle());
        label.setStyle(Theme.intentTextFill());
    }

    public void render(Step step) {
        setStyle(Theme.intentBarStyle());
        label.setStyle(Theme.intentTextFill());
        if (step == null || step.intent() == null) {
            label.setText("");
            return;
        }
        label.setText("🧭 不变量：" + step.intent().invariant() + "　｜　本步：" + step.intent().why());
    }
}
