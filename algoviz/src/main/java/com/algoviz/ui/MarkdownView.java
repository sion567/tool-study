package com.algoviz.ui;

import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

import org.commonmark.ext.gfm.tables.TableBlock;
import org.commonmark.ext.gfm.tables.TableBody;
import org.commonmark.ext.gfm.tables.TableCell;
import org.commonmark.ext.gfm.tables.TableHead;
import org.commonmark.ext.gfm.tables.TableRow;
import org.commonmark.ext.gfm.tables.TablesExtension;
import org.commonmark.node.BlockQuote;
import org.commonmark.node.BulletList;
import org.commonmark.node.FencedCodeBlock;
import org.commonmark.node.HardLineBreak;
import org.commonmark.node.Heading;
import org.commonmark.node.IndentedCodeBlock;
import org.commonmark.node.Node;
import org.commonmark.node.OrderedList;
import org.commonmark.node.Paragraph;
import org.commonmark.node.SoftLineBreak;
import org.commonmark.node.ThematicBreak;
import org.commonmark.parser.Parser;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 课件 Markdown 渲染（M2 v0 行级状态机 → v1 commonmark AST 遍历）。
 * 解析交给 commonmark-java（GFM 表格扩展），渲染仍是自绘 TextFlow/Label——javafx-web 有意不引入。
 * 支持：标题 1~4 级 / 有序无序列表（含嵌套）/ 围栏代码块 / **加粗** / 斜体 / `行内码` /
 * 表格（表头加粗+斑马纹）/ 引用块（左边框）/ 分隔线；emoji 为字面 Unicode 直通。
 */
public final class MarkdownView extends ScrollPane {

    private static final String MONO = "-fx-font-family:'Consolas','Courier New',monospace;";
    private static final Parser PARSER = Parser.builder()
            .extensions(List.of(TablesExtension.create()))
            .build();

    private final VBox content = new VBox(6);
    private String lastResource;

    public MarkdownView() {
        setContent(content);
        setFitToWidth(true);
        content.setPadding(new Insets(12));
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
        String src = readText(resource);
        if (src == null) {
            Label missing = new Label("(找不到课件 " + resource + ")");
            missing.setStyle("-fx-text-fill:" + (Theme.isDark() ? "#a6adc8" : "#374151") + ";");
            content.getChildren().add(missing);
            return;
        }
        renderBlocks(PARSER.parse(src), content);
    }

    // ---- 块级遍历 ----

    private void renderBlocks(Node parent, VBox target) {
        for (Node n = parent.getFirstChild(); n != null; n = n.getNext()) {
            if (n instanceof Heading h) {
                int[] sizes = {18, 15, 14, 13};
                int size = sizes[Math.min(h.getLevel(), sizes.length) - 1];
                target.getChildren().add(heading(plainText(n), size));
            } else if (n instanceof Paragraph p) {
                TextFlow flow = new TextFlow();
                appendInline(flow, p.getFirstChild(), false);
                target.getChildren().add(flow);
            } else if (n instanceof BulletList bl) {
                renderList(bl, false, 0, target);
            } else if (n instanceof OrderedList ol) {
                renderList(ol, true, 0, target);
            } else if (n instanceof FencedCodeBlock f) {
                codeBlock(f.getLiteral(), target);
            } else if (n instanceof IndentedCodeBlock i) {
                codeBlock(i.getLiteral(), target);
            } else if (n instanceof BlockQuote q) {
                VBox inner = new VBox(6);
                inner.setStyle("-fx-padding:2 0 2 12; -fx-border-color: transparent transparent transparent "
                        + (Theme.isDark() ? "#58607a" : "#d0d7de") + "; -fx-border-width:0 0 0 3;");
                renderBlocks(q, inner);
                target.getChildren().add(inner);
            } else if (n instanceof ThematicBreak) {
                target.getChildren().add(new Separator());
            } else if (n instanceof TableBlock t) {
                table(t, target);
            }
        }
    }

    /** 列表：marker = • 或 1.；嵌套列表缩进 16px/层（item 内的非列表块进 flow，列表块缩进续排）。 */
    private void renderList(Node list, boolean ordered, int depth, VBox target) {
        int idx = 1;
        for (Node item = list.getFirstChild(); item != null; item = item.getNext()) {
            TextFlow flow = new TextFlow();
            java.util.List<Node> nested = new java.util.ArrayList<>();
            for (Node b = item.getFirstChild(); b != null; b = b.getNext()) {
                if (b instanceof BulletList || b instanceof OrderedList) {
                    nested.add(b);
                } else if (flow.getChildren().isEmpty()) {
                    appendInline(flow, b.getFirstChild(), false);   // item 首段进行内
                }
            }
            Label marker = new Label(ordered ? idx + "." : "•");
            marker.setStyle("-fx-text-fill:" + (Theme.isDark() ? "#94a3b8" : "#6b7280") + ";");
            HBox row = new HBox(6, marker, flow);
            if (depth > 0) {
                row.setStyle("-fx-padding:0 0 0 " + (depth * 16) + "px;");
            }
            target.getChildren().add(row);
            idx++;
            for (Node nl : nested) {
                renderList(nl, nl instanceof OrderedList, depth + 1, target);
            }
        }
    }

    private void codeBlock(String literal, VBox target) {
        VBox box = new VBox(2);
        box.setStyle("-fx-background-color:" + (Theme.isDark() ? "#313244" : "#f6f8fa")
                + "; -fx-padding:8; -fx-background-radius:4;");
        for (String line : literal.split("\n")) {
            Label l = new Label(line);
            l.setStyle("-fx-text-fill:#cdd6f4; -fx-font-size:12px;" + MONO);
            box.getChildren().add(l);
        }
        target.getChildren().add(box);
    }

    /** 表格：1px 缝隙显边框色，表头加粗+底色，数据行斑马纹。 */
    private void table(TableBlock tbl, VBox target) {
        boolean dark = Theme.isDark();
        String border = dark ? "#45475a" : "#d0d7de";
        String headerBg = dark ? "#313244" : "#eef2f7";
        String rowA = dark ? "#24243a" : "#ffffff";
        String rowB = dark ? "#2a2a44" : "#f6f8fa";
        GridPane grid = new GridPane();
        grid.setHgap(1);
        grid.setVgap(1);
        grid.setStyle("-fx-background-color:" + border + "; -fx-padding:1; -fx-background-radius:4;");
        int row = 0;
        for (Node section = tbl.getFirstChild(); section != null; section = section.getNext()) {
            boolean head = section instanceof TableHead;
            int r = 0;
            for (Node tr = section.getFirstChild(); tr != null; tr = tr.getNext()) {
                int col = 0;
                for (Node tc = tr.getFirstChild(); tc != null; tc = tc.getNext()) {
                    TextFlow cell = new TextFlow();
                    appendInline(cell, tc.getFirstChild(), head);
                    cell.setStyle("-fx-padding:4 10 4 10; -fx-background-color:"
                            + (head ? headerBg : (r % 2 == 0 ? rowA : rowB)) + ";");
                    grid.add(cell, col, row);
                    col++;
                }
                r++;
                row++;
            }
        }
        target.getChildren().add(grid);
        Region spacer = new Region();
        spacer.setMinHeight(4);
        target.getChildren().add(spacer);
    }

    // ---- 行内遍历 ----

    private static void appendInline(TextFlow flow, Node n, boolean bold) {
        for (; n != null; n = n.getNext()) {
            if (n instanceof org.commonmark.node.Text t) {
                flow.getChildren().add(text(t.getLiteral(), bold, false, false));
            } else if (n instanceof org.commonmark.node.Code c) {
                flow.getChildren().add(text(c.getLiteral(), bold, true, false));
            } else if (n instanceof org.commonmark.node.StrongEmphasis) {
                appendInline(flow, n.getFirstChild(), true);
            } else if (n instanceof org.commonmark.node.Emphasis) {
                appendItalic(flow, n.getFirstChild(), bold);
            } else if (n instanceof SoftLineBreak) {
                flow.getChildren().add(text(" ", bold, false, false));
            } else if (n instanceof HardLineBreak) {
                flow.getChildren().add(text("\n", bold, false, false));
            } else {
                appendInline(flow, n.getFirstChild(), bold);   // Link/Image：退化为其内容文本
            }
        }
    }

    private static void appendItalic(TextFlow flow, Node n, boolean bold) {
        for (; n != null; n = n.getNext()) {
            if (n instanceof org.commonmark.node.Text t) {
                flow.getChildren().add(text(t.getLiteral(), bold, false, true));
            } else if (n instanceof org.commonmark.node.Code c) {
                flow.getChildren().add(text(c.getLiteral(), bold, true, false));
            } else {
                appendItalic(flow, n.getFirstChild(), bold);
            }
        }
    }

    private static String plainText(Node block) {
        StringBuilder sb = new StringBuilder();
        collect(block.getFirstChild(), sb);
        return sb.toString();
    }

    private static void collect(Node n, StringBuilder sb) {
        for (; n != null; n = n.getNext()) {
            if (n instanceof org.commonmark.node.Text t) {
                sb.append(t.getLiteral());
            } else if (n instanceof org.commonmark.node.Code c) {
                sb.append(c.getLiteral());
            } else {
                collect(n.getFirstChild(), sb);
            }
        }
    }

    private static Label heading(String text, int size) {
        Label l = new Label(text);
        l.setStyle("-fx-font-size:" + size + "px; -fx-font-weight:bold; -fx-text-fill:"
                + (Theme.isDark() ? "#cdd6f4" : "#111") + ";");
        return l;
    }

    private static Text text(String s, boolean bold, boolean code, boolean italic) {
        Text t = new Text(s);
        if (code) {
            t.setFont(Font.font("Consolas", FontWeight.BOLD, 12));
            t.setFill(javafx.scene.paint.Color.web(Theme.isDark() ? "#f9e2af" : "#be185d"));
        } else if (bold) {
            t.setFont(Font.font("System", FontWeight.BOLD, 13));
            t.setFill(javafx.scene.paint.Color.web(Theme.isDark() ? "#cdd6f4" : "#111827"));
        } else if (italic) {
            t.setFont(Font.font("System", FontWeight.NORMAL, FontPosture.ITALIC, 13));
            t.setFill(javafx.scene.paint.Color.web(Theme.isDark() ? "#a6adc8" : "#374151"));
        } else {
            t.setFont(Font.font("System", FontWeight.NORMAL, 13));
            t.setFill(javafx.scene.paint.Color.web(Theme.isDark() ? "#a6adc8" : "#374151"));
        }
        return t;
    }

    private static String readText(String resource) {
        try (InputStream in = MarkdownView.class.getResourceAsStream(resource)) {
            if (in == null) {
                return null;
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            return "(读取失败: " + e.getMessage() + ")";
        }
    }
}
