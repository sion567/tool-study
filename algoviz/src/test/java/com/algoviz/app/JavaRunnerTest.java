package com.algoviz.app;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 行为级判卷引擎：内存编译 + 运行 + 输出捕获 + 超时兜底。 */
@EnabledIf("com.algoviz.app.JavaRunnerTest#compilerAvailable")
class JavaRunnerTest {

    static boolean compilerAvailable() {
        return JavaRunner.available();
    }

    @Test
    void runsAndCapturesStdout() {
        String src = """
                public class T1 {
                    public static void main(String[] args) {
                        System.out.println("1 2 3");
                        System.out.println("done");
                    }
                }
                """;
        var r = JavaRunner.compileAndRun("T1", src, 2000);
        assertTrue(r.ok(), "应编译运行成功: " + (r.error() != null ? r.error() : r.compileErrors()));
        assertEquals(List.of("1 2 3", "done"), nonBlank(r.stdout()));
    }

    @Test
    void compileErrorReportsLineNumber() {
        String src = """
                public class T2 {
                    public static void main(String[] args) {
                        int x = "oops";
                    }
                }
                """;
        var r = JavaRunner.compileAndRun("T2", src, 2000);
        assertFalse(r.compiled());
        assertTrue(r.compileErrors().contains("第 3 行"), "错误应带行号: " + r.compileErrors());
    }

    @Test
    void timeoutInterruptsSleepLoop() {
        String src = """
                public class T3 {
                    public static void main(String[] args) throws Exception {
                        while (true) { Thread.sleep(10); }
                    }
                }
                """;
        var r = JavaRunner.compileAndRun("T3", src, 600);
        assertTrue(r.compiled());
        assertTrue(r.error() != null && r.error().startsWith("超时"), "应报超时: " + r.error());
    }

    @Test
    void runtimeErrorSurfaces() {
        String src = """
                public class T4 {
                    public static void main(String[] args) {
                        System.out.println("before");
                        int[] a = new int[1];
                        System.out.println(a[5]);
                    }
                }
                """;
        var r = JavaRunner.compileAndRun("T4", src, 2000);
        assertTrue(r.compiled());
        assertTrue(r.error() != null && r.error().contains("ArrayIndexOutOfBoundsException"),
                "应带异常类型: " + r.error());
        assertTrue(r.stdout().contains("before"), "异常前输出应保留");
    }

    @Test
    void normalizeWrapsBareStatements() {
        String bare = "System.out.println(\"hi\");";
        String wrapped = JavaRunner.normalize(bare);
        var r = JavaRunner.compileAndRun(JavaRunner.classNameOf(wrapped), wrapped, 2000);
        assertTrue(r.ok(), "裸语句应被包进 main 后可运行: " + (r.error() != null ? r.error() : r.compileErrors()));
        assertTrue(r.stdout().contains("hi"));
        // 已含 class 的代码原样保留
        assertTrue(JavaRunner.normalize("public class Foo {}").contains("class Foo"));
    }

    private static java.util.List<String> nonBlank(String s) {
        return java.util.Arrays.stream(s.split("\r?\n"))
                .map(String::strip).filter(x -> !x.isEmpty()).toList();
    }
}
