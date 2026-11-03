package com.algoviz.ui;

import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** 右侧代码区：滚动容器 + 行号 + 注释弱化 + 高亮行自动滚入视野（长清单也不迷路）。 */
public final class CodePanel extends ScrollPane {

    private final VBox box = new VBox(2);
    private final List<Label> lines = new ArrayList<>();
    private final List<String> raws = new ArrayList<>();
    private int highlighted = -1;
    private final Label complexityLabel = new Label();

    public CodePanel(String resource) {
        this(resource, Collections.<String>emptyList(), null);
    }

    /** dataLines：🎲 随机输入后，替换清单中带 //@DATA 标记行的真实数据代码行。
     *  cx：复杂度标注（M7），null = 不显示。 */
    public CodePanel(String resource, java.util.List<String> dataLines,
                     com.algoviz.engine.Complexity cx) {
        box.setPadding(new Insets(10));
        setContent(box);
        setFitToWidth(false);   // 长行出横向滚动条，保证看得全
        // 复杂度横幅（算法演示才有，模式/数学不显示）
        if (cx != null) {
            complexityLabel.setStyle("-fx-text-fill:#1e3a5f; -fx-font-size:12px; -fx-font-weight:bold;"
                    + "-fx-background-color:#dbeafe; -fx-padding:6 10; -fx-background-radius:4;");
            complexityLabel.setWrapText(true);
            complexityLabel.setText("⏱ 时间: " + cx.time() + "   💾 空间: " + cx.space()
                    + "\n🧠 " + cx.why());
            box.getChildren().add(complexityLabel);
        }
        List<String> source = readLines(resource);
        // 找到 //@DATA 标记行，用 dataLines 替换（🎲 后代码与画面数据同步）
        int marker = -1;
        for (int i = 0; i < source.size(); i++) {
            if (source.get(i).contains("//@DATA")) {
                marker = i;
                break;
            }
        }
        List<String> effective = new ArrayList<>(source);
        if (marker >= 0 && !dataLines.isEmpty()) {
            effective.remove(marker);
            effective.addAll(marker, dataLines);
        }
        int no = 1;
        javafx.scene.control.Button copyBtn = new javafx.scene.control.Button("📋 复制全文（可直接粘贴运行）");
        copyBtn.setStyle("-fx-font-size:11px;");
        copyBtn.setOnAction(e -> {
            javafx.scene.input.Clipboard cb = javafx.scene.input.Clipboard.getSystemClipboard();
            cb.setContent(Collections.singletonMap(javafx.scene.input.DataFormat.PLAIN_TEXT,
                    String.join("\n", raws)));
        });
        javafx.scene.layout.HBox toolRow = new javafx.scene.layout.HBox(copyBtn);
        toolRow.setStyle("-fx-padding:0 0 6 0;");
        box.getChildren().add(toolRow);
        for (String text : effective) {
            Label label = new Label(String.format("%2d │ %s", no, text));
            label.setStyle(styleFor(text));
            attachCommentTooltip(label, text, no);
            lines.add(label);
            raws.add(text);
            box.getChildren().add(label);
            no++;
        }
    }

    /** 注释约定：`// 短注释 —— 详细说明`（或用 | 分隔）。
     *  可见部分只显示短注释（加 … 提示有详情），完整说明放进悬停浮窗。 */
    private void attachCommentTooltip(Label label, String raw, int no) {
        String t = raw.stripLeading();
        if (!t.startsWith("//")) {
            return;
        }
        String body = t.substring(2).strip();
        String sep = body.contains("——") ? "——" : (body.contains("|") ? "|" : null);
        if (sep == null) {
            label.setTooltip(new Tooltip("// " + body));
            return;
        }
        int at = body.indexOf(sep);
        String shortPart = body.substring(0, at).strip();
        String detail = body.substring(at + sep.length()).strip();
        label.setText(String.format("%2d │ // %s …", no, shortPart));
        label.setTooltip(new Tooltip("// " + shortPart + "\n\n详细：" + detail));
    }

    private String styleFor(String raw) {
        return raw.stripLeading().startsWith("//") ? Theme.codeComment() : Theme.codeNormal();
    }

    public void highlight(int line1based) {
        if (highlighted >= 0 && highlighted < lines.size()) {
            lines.get(highlighted).setStyle(styleFor(raws.get(highlighted)));
        }
        highlighted = line1based - 1;
        if (highlighted >= 0 && highlighted < lines.size()) {
            lines.get(highlighted).setStyle(Theme.codeHighlight());
            // 自动滚动：让高亮行大致处于视口上 1/3 处
            double frac = lines.size() <= 1 ? 0
                    : (double) Math.max(0, highlighted - 3) / Math.max(1, lines.size() - 6);
            setVvalue(Math.max(0, Math.min(1, frac)));
        }
    }

    /** 主题切换后重刷所有行的配色。 */
    public void applyTheme() {
        for (int i = 0; i < lines.size(); i++) {
            lines.get(i).setStyle(i == highlighted ? Theme.codeHighlight() : styleFor(raws.get(i)));
        }
    }

    private List<String> readLines(String resource) {
        try (InputStream in = CodePanel.class.getResourceAsStream(resource)) {
            if (in == null) {
                return List.of("(找不到资源 " + resource + ")");
            }
            return new ArrayList<>(new String(in.readAllBytes(), StandardCharsets.UTF_8).lines().toList());
        } catch (IOException e) {
            return List.of("(读取失败: " + e.getMessage() + ")");
        }
    }
}
