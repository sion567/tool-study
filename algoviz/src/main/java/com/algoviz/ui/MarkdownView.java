package com.algoviz.ui;

import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** 极简 Markdown 块级渲染（M2 v0）：标题 / 列表 / 代码块 / **加粗** / `行内码`——够课件用。 */
public final class MarkdownView extends ScrollPane {

    private static final Pattern INLINE = Pattern.compile("(\\*\\*[^*]+\\*\\*|`[^`]+`)");
    private static final String MONO = "-fx-font-family:'Consolas','Courier New',monospace;";

    private final VBox content = new VBox(6);
    private String lastResource;

    public MarkdownView() {
        setContent(content);
        setFitToWidth(true);
        content.setPadding(new javafx.geometry.Insets(12));
    }

    public void loadResource(String resource) {
        lastResource = resource;
        renderResource(resource);
    }

    /** 主题切换后按当前文档重渲染。 */
    public void applyTheme() {
        if (lastResource != null) {
            renderResource(lastResource);
        }
    }

    private void renderResource(String resource) {
        content.getChildren().clear();
        boolean inCode = false;
        VBox codeBox = null;
        for (String raw : readLines(resource)) {
            if (raw.startsWith("```")) {
                if (inCode) {
                    content.getChildren().add(codeBox);
                    codeBox = null;
                } else {
                    codeBox = new VBox(2);
                    codeBox.setStyle("-fx-background-color:" + (Theme.isDark() ? "#313244" : "#f6f8fa")
                            + "; -fx-padding:8; -fx-background-radius:4;");
                }
                inCode = !inCode;
                continue;
            }
            if (inCode) {
                Label l = new Label(raw);
                l.setStyle("-fx-text-fill:#cdd6f4; -fx-font-size:12px;" + MONO);
                codeBox.getChildren().add(l);
                continue;
            }
            if (raw.startsWith("### ")) {
                content.getChildren().add(heading(raw.substring(4), 14));
            } else if (raw.startsWith("## ")) {
                content.getChildren().add(heading(raw.substring(3), 15));
            } else if (raw.startsWith("# ")) {
                content.getChildren().add(heading(raw.substring(2), 18));
            } else if (raw.startsWith("- ")) {
                HBox bullet = new HBox(6, new Label("•"), inline(raw.substring(2)));
                content.getChildren().add(bullet);
            } else if (raw.isBlank()) {
                Region spacer = new Region();
                spacer.setMinHeight(4);
                content.getChildren().add(spacer);
            } else {
                content.getChildren().add(inline(raw));
            }
        }
    }

    private static Label heading(String text, int size) {
        Label l = new Label(text);
        l.setStyle("-fx-font-size:" + size + "px; -fx-font-weight:bold; -fx-text-fill:"
                + (Theme.isDark() ? "#cdd6f4" : "#111") + ";");
        return l;
    }

    private static TextFlow inline(String s) {
        TextFlow flow = new TextFlow();
        Matcher m = INLINE.matcher(s);
        int last = 0;
        while (m.find()) {
            if (m.start() > last) {
                flow.getChildren().add(text(s.substring(last, m.start()), false, false));
            }
            String tok = m.group();
            if (tok.startsWith("**")) {
                flow.getChildren().add(text(tok.substring(2, tok.length() - 2), true, false));
            } else {
                flow.getChildren().add(text(tok.substring(1, tok.length() - 1), false, true));
            }
            last = m.end();
        }
        if (last < s.length()) {
            flow.getChildren().add(text(s.substring(last), false, false));
        }
        return flow;
    }

    private static Text text(String s, boolean bold, boolean code) {
        Text t = new Text(s);
        if (code) {
            t.setFont(Font.font("Consolas", FontWeight.BOLD, 12));
            t.setFill(javafx.scene.paint.Color.web(Theme.isDark() ? "#f9e2af" : "#be185d"));
        } else if (bold) {
            t.setFont(Font.font("System", FontWeight.BOLD, 13));
            t.setFill(javafx.scene.paint.Color.web(Theme.isDark() ? "#cdd6f4" : "#111827"));
        } else {
            t.setFont(Font.font("System", FontWeight.NORMAL, 13));
            t.setFill(javafx.scene.paint.Color.web(Theme.isDark() ? "#a6adc8" : "#374151"));
        }
        return t;
    }

    private static List<String> readLines(String resource) {
        try (InputStream in = MarkdownView.class.getResourceAsStream(resource)) {
            if (in == null) {
                return List.of("(找不到课件 " + resource + ")");
            }
            return new ArrayList<>(new String(in.readAllBytes(), StandardCharsets.UTF_8).lines().toList());
        } catch (IOException e) {
            return List.of("(读取失败: " + e.getMessage() + ")");
        }
    }
}
