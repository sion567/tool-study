package com.algoviz.ui;

import com.algoviz.engine.AlgorithmRegistry;
import org.commonmark.ext.gfm.tables.TableBlock;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 课件语料合约测试（纯 JVM，零 JavaFX）：commonmark 0.32.0 解析全量课件。
 * 背景：MarkdownView 渲染升级为 AST 遍历前，28 个课件的 GFM 表格/20 个的有序列表
 * 处于"没人管"状态——本测试锁住三件事：资源必存在、解析必成功、表格必矩形（抓 ragged row）。
 */
class MarkdownDocsTest {

    private static final Parser PARSER = Parser.builder()
            .extensions(List.of(org.commonmark.ext.gfm.tables.TablesExtension.create()))
            .build();

    /** 注册表里每个演示的 docResource 必须真实存在且解析成功（缺课件曾静默显示"找不到课件"）。 */
    @Test
    void everyRegisteredDocResourceParses() throws Exception {
        List<AlgorithmRegistry.Entry> all = AlgorithmRegistry.all();
        assertTrue(all.size() >= 160, "注册表应有 ≥160 个演示，实际 " + all.size());
        for (AlgorithmRegistry.Entry entry : all) {
            String doc = entry.algorithm().docResource();
            assertNotNull(doc, entry.algorithm().id() + " 未配置 docResource");
            try (InputStream in = AlgorithmRegistry.class.getResourceAsStream(doc)) {
                assertNotNull(in, entry.algorithm().id() + " 的课件缺失: " + doc);
                Node ast = PARSER.parse(new String(in.readAllBytes(), StandardCharsets.UTF_8));
                assertNotNull(ast.getFirstChild(),
                        entry.algorithm().id() + " 的课件解析为空文档: " + doc);
            }
        }
    }

    /** markdown 目录全量解析 + 表格矩形校验（列数与表头一致）。 */
    @Test
    void allMarkdownFilesParseAndTablesAreRectangular() throws Exception {
        Path dir = Path.of("src/main/resources/markdown");
        assertTrue(Files.isDirectory(dir), "课件目录不存在: " + dir);
        int files = 0;
        int tables = 0;
        List<String> problems = new ArrayList<>();
        try (Stream<Path> paths = Files.list(dir)) {
            for (Path p : paths.filter(x -> x.getFileName().toString().endsWith(".md")).toList()) {
                files++;
                Node ast = PARSER.parse(Files.readString(p, StandardCharsets.UTF_8));
                tables += checkTables(ast, p.getFileName().toString(), problems);
            }
        }
        assertTrue(files >= 160, "课件应 ≥160 个文件，实际 " + files);
        assertTrue(tables >= 10, "语料应含 ≥10 个表格（现状 28 文件有表），实际 " + tables);
        assertTrue(problems.isEmpty(), "表格列数不齐（ragged row）：" + String.join("; ", problems));
    }

    private static int checkTables(Node ast, String file, List<String> problems) {
        int count = 0;
        for (Node n = ast.getFirstChild(); n != null; n = n.getNext()) {
            if (n instanceof TableBlock tbl) {
                count++;
                int cols = -1;
                for (Node section = tbl.getFirstChild(); section != null; section = section.getNext()) {
                    for (Node tr = section.getFirstChild(); tr != null; tr = tr.getNext()) {
                        int width = 0;
                        for (Node tc = tr.getFirstChild(); tc != null; tc = tc.getNext()) {
                            width++;
                        }
                        if (cols == -1) {
                            cols = width;
                        } else if (width != cols) {
                            problems.add(file + " 表第 " + (count) + " 处：表头 " + cols + " 列，本行 " + width + " 列");
                        }
                    }
                }
            }
            count += checkTables(n, file, problems);   // 引用块内也可能有表（递归）
        }
        return count;
    }
}
