package com.algoviz.ui;

import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

/** 预测模式面板（蓝图 §2.5-A v0）：先猜「下一帧哪些格子会变」，提交后揭晓 + diff。 */
public final class PredictionPanel extends VBox {

    private final Label question = new Label("切到本页即开始：下一帧谁会变？");
    private final Label picked = new Label();
    private final Label result = new Label();
    private final Label score = new Label("0 题 · 0 全对 · 连对 0");
    private final Button submit = new Button("提交答案");
    private final Button reveal = new Button("跳过（揭晓但不计分）");

    public Runnable onSubmit;
    public Runnable onReveal;

    public PredictionPanel() {
        setSpacing(8);
        setPadding(new Insets(12));

        question.setWrapText(true);
        picked.setWrapText(true);
        result.setWrapText(true);

        submit.setStyle("-fx-font-size:13px;");
        reveal.setStyle("-fx-font-size:12px;");
        submit.setMaxWidth(Double.MAX_VALUE);
        reveal.setMaxWidth(Double.MAX_VALUE);
        submit.setOnAction(e -> {
            if (onSubmit != null) {
                onSubmit.run();
            }
        });
        reveal.setOnAction(e -> {
            if (onReveal != null) {
                onReveal.run();
            }
        });

        getChildren().addAll(question, picked, submit, reveal, result, score);
        applyTheme();
    }

    /** 主题切换后重刷本面板配色。 */
    public void applyTheme() {
        question.setStyle(Theme.questionText());
        picked.setStyle(Theme.pickedText());
        result.setStyle(Theme.resultStyle());
        score.setStyle(Theme.scoreText());
    }

    public void setQuestion(String text) {
        question.setText(text);
    }

    public void setPicked(String text) {
        picked.setText(text);
    }

    public void setResult(String text) {
        result.setText(text);
    }

    public void setScore(String text) {
        score.setText(text);
    }

    /** armed=false 时两个按钮都禁用；submitText 可切换「提交答案 / 下一题」。 */
    public void setButtons(boolean revealEnabled, boolean submitEnabled, String submitText) {
        submit.setText(submitText);
        submit.setDisable(!submitEnabled);
        reveal.setDisable(!revealEnabled);
    }
}
