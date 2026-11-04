package com.algoviz.ui;

import com.algoviz.engine.Algorithm;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;

/**
 * 默写模式（蓝图 §2.5-B）：白纸编辑器 + 参考答案对照。
 * 用户白纸写完 → 点「对比」→ 与参考答案逐行 diff → 标出缺/多/不一致的 chunk。
 * 这是「说得出、写不出」的正解：产出练习 + 即时纠错。
 */
public final class DictationPanel extends VBox {

    private final TextArea editor = new TextArea();
    private final TextArea referenceView = new TextArea();
    private final Label resultLabel = new Label();
    private final Button compareBtn = new Button("📝 对比");
    private final Button behaviorBtn = new Button("▶ 行为判卷");
    private final Button showRefBtn = new Button("👁 看参考答案");
    private boolean showingRef;

    private String referenceCode = "";
    public Runnable onCompare;
    public Runnable onBehaviorJudge;

    public DictationPanel() {
        setSpacing(6);
        setPadding(new Insets(8));

        Label prompt = new Label("白纸默写：合上参考，凭记忆写出来。写完点「对比」——缺哪块、边界错哪，直接标出来。");
        prompt.setWrapText(true);
        prompt.setStyle("-fx-text-fill:#6b7280; -fx-font-size:12px;");

        editor.setPromptText("在这里凭记忆写代码…");
        editor.setStyle("-fx-font-family:'Consolas','Courier New',monospace; -fx-font-size:13px;");
        editor.setWrapText(false);
        editor.setPrefRowCount(12);

        referenceView.setStyle("-fx-font-family:'Consolas','Courier New',monospace; -fx-font-size:13px;");
        referenceView.setEditable(false);
        referenceView.setVisible(false);
        referenceView.setManaged(false);
        referenceView.setPrefRowCount(12);

        compareBtn.setStyle("-fx-font-size:13px;");
        behaviorBtn.setStyle("-fx-font-size:13px; -fx-font-weight:bold;");
        behaviorBtn.setDisable(!JavaRunner.available());
        showRefBtn.setStyle("-fx-font-size:12px;");
        HBox btns = new HBox(8, compareBtn, behaviorBtn, showRefBtn);
        resultLabel.setWrapText(true);
        resultLabel.setStyle("-fx-text-fill:#374151; -fx-font-size:12px; -fx-background-color:#fef3c7;"
                + " -fx-padding:6; -fx-background-radius:4;");

        compareBtn.setOnAction(e -> { if (onCompare != null) onCompare.run(); });
        behaviorBtn.setOnAction(e -> { if (onBehaviorJudge != null) onBehaviorJudge.run(); });
        showRefBtn.setOnAction(e -> {
            showingRef = !showingRef;
            referenceView.setVisible(showingRef);
            referenceView.setManaged(showingRef);
            showRefBtn.setText(showingRef ? "🙈 收起参考" : "👁 看参考答案");
        });

        getChildren().addAll(prompt, editor, btns, resultLabel, referenceView);
    }

    public void setReferenceCode(String code) {
        this.referenceCode = code;
        this.referenceView.setText(code);
    }

    public void setResultText(String text) { resultLabel.setText(text); }

    public String getUserCode() { return editor.getText(); }

    public String getReferenceCode() { return referenceCode; }

    public void clear() {
        editor.clear();
        resultLabel.setText("");
        showingRef = false;
        referenceView.setVisible(false);
        referenceView.setManaged(false);
        showRefBtn.setText("👁 看参考答案");
    }

    public Button getCompareBtn() { return compareBtn; }
    public Button getBehaviorBtn() { return behaviorBtn; }
    public Button getShowRefBtn() { return showRefBtn; }
    public TextArea getEditor() { return editor; }
    public TextArea getReferenceView() { return referenceView; }
    public Label getResultLabel() { return resultLabel; }
}
