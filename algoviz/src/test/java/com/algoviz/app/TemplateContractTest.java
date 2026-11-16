package com.algoviz.app;

import com.algoviz.engine.Algorithm;
import com.algoviz.engine.AlgorithmRegistry;
import org.junit.jupiter.api.Test;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 课件代码合约（纯 JVM）：CodePanel 显示的是「//@DATA 标记行被 dataLines() 替换」后的代码，
 * 且「复制全文」按钮承诺可直接粘贴运行——所以显示版必须能编译。
 * 背景：PathSumDemo 模板把 //@DATA 标在输出行上，dataLines 注入两行声明后
 * main 变成"调了 build() 却什么都不做"——磁盘原件是好的，用户看到的却是坏的。
 * 本测试锁三件事：模板存在、标记唯一（CodePanel 只替换第一个）、
 * 原件与显示版（模拟替换）都能编译。
 */
class TemplateContractTest {

    @Test
    void everyTemplateCompilesRawAndAsDisplayed() throws Exception {
        List<String> problems = new ArrayList<>();
        Path tmp = Files.createTempDirectory("algoviz-tpl");
        for (AlgorithmRegistry.Entry entry : AlgorithmRegistry.all()) {
            Algorithm alg = entry.algorithm();
            String id = alg.id();
            try (var in = getClass().getResourceAsStream(alg.codeResource())) {
                if (in == null) {
                    problems.add(id + " 模板缺失: " + alg.codeResource());
                    continue;
                }
                List<String> source = new ArrayList<>(
                        new String(in.readAllBytes(), StandardCharsets.UTF_8).lines().toList());
                String className = fileNameOf(alg.codeResource());
                // 1) 磁盘原件必须能编译（默写参考答案本身要可运行）
                String raw = String.join("\n", source);
                compile(className, raw, tmp, problems, id + " 原件");
                // 2) 标记唯一：CodePanel 只替换第一个 //@DATA 行，多标记即显示错位隐患
                int markers = 0;
                for (String line : source) {
                    if (line.contains("//@DATA")) {
                        markers++;
                    }
                }
                if (markers > 1) {
                    problems.add(id + " 有 " + markers + " 个 //@DATA 标记（CodePanel 只替换第一个）");
                }
                // 3) 显示版（模拟 CodePanel 替换）必须能编译：this is what 用户默写/复制的是什么
                List<String> data = alg.dataLines();
                List<String> displayed = new ArrayList<>(source);
                if (markers >= 1 && !data.isEmpty()) {
                    int marker = -1;
                    for (int i = 0; i < source.size(); i++) {
                        if (source.get(i).contains("//@DATA")) {
                            marker = i;
                            break;
                        }
                    }
                    displayed.remove(marker);
                    displayed.addAll(marker, data);
                }
                compile(className, String.join("\n", displayed), tmp, problems, id + " 显示版");
            }
        }
        assertTrue(problems.isEmpty(), "课件代码合约违规 " + problems.size() + " 处：\n  "
                + String.join("\n  ", problems));
    }

    private static String fileNameOf(String resource) {
        int at = resource.lastIndexOf('/');
        return at >= 0 ? resource.substring(at + 1) : resource;
    }

    private static void compile(String className, String src, Path outDir,
                                List<String> problems, String tag) throws Exception {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        DiagnosticCollector<JavaFileObject> diags = new DiagnosticCollector<>();
        try (StandardJavaFileManager fm = compiler.getStandardFileManager(diags, null, null)) {
            JavaFileObject file = new SimpleJavaFileObject(
                    URI.create("string:///" + className), JavaFileObject.Kind.SOURCE) {
                @Override
                public CharSequence getCharContent(boolean ignoreEncoding) {
                    return src;
                }
            };
            var task = compiler.getTask(null, fm, diags,
                    List.of("-encoding", "UTF-8", "-d", outDir.toString()), null, List.of(file));
            if (!Boolean.TRUE.equals(task.call())) {
                StringBuilder sb = new StringBuilder(tag + " 编译失败：");
                for (Diagnostic<? extends JavaFileObject> d : diags.getDiagnostics()) {
                    if (d.getKind() == Diagnostic.Kind.ERROR) {
                        sb.append("行 ").append(d.getLineNumber()).append(" ")
                                .append(d.getMessage(null)).append(" | ");
                    }
                }
                problems.add(sb.toString());
            }
        }
    }
}
