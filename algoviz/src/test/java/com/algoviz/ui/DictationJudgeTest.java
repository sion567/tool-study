package com.algoviz.ui;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 默写判卷五档：行为一致 / 行为不同 / 编译失败 / 运行异常 / 空白纸。 */
@EnabledIf("com.algoviz.ui.JavaRunnerTest#compilerAvailable")
class DictationJudgeTest {

    /** 参考答案：冒泡排序（打印最终数组）。 */
    private static final String REF = """
            public class Ref {
                public static void main(String[] args) {
                    int[] a = {5, 2, 9, 1};
                    for (int i = 0; i < a.length - 1; i++) {
                        for (int j = 0; j < a.length - 1 - i; j++) {
                            if (a[j] > a[j + 1]) {
                                int t = a[j]; a[j] = a[j + 1]; a[j + 1] = t;
                            }
                        }
                    }
                    System.out.println(java.util.Arrays.toString(a));
                }
            }
            """;

    @Test
    void differentCodeSameBehaviorPasses() {
        // 用户用选择排序实现——写法完全不同，行为一致 → 必须判过
        String user = """
                public class Mine {
                    public static void main(String[] args) {
                        int[] a = {5, 2, 9, 1};
                        for (int i = 0; i < a.length; i++) {
                            int min = i;
                            for (int j = i + 1; j < a.length; j++) {
                                if (a[j] < a[min]) min = j;
                            }
                            int t = a[i]; a[i] = a[min]; a[min] = t;
                        }
                        System.out.println(java.util.Arrays.toString(a));
                    }
                }
                """;
        var v = DictationJudge.judge(REF, user);
        assertEquals("pass", v.grade(), "选择排序也应判过（行为一致）: " + v.text());
    }

    @Test
    void wrongBehaviorGetsDiff() {
        String user = """
                public class Wrong {
                    public static void main(String[] args) {
                        int[] a = {5, 2, 9, 1};
                        java.util.Arrays.sort(a, 0, 3);   // 只排前 3 个——行为错
                        System.out.println(java.util.Arrays.toString(a));
                    }
                }
                """;
        var v = DictationJudge.judge(REF, user);
        assertEquals("behavior-diff", v.grade());
        assertTrue(v.text().contains("−") || v.text().contains("+"), "应有 diff 标记");
    }

    @Test
    void compileErrorHasLineNumber() {
        String user = "public class Bad { public static void main(String[] args) { int x = ; } }";
        var v = DictationJudge.judge(REF, user);
        assertEquals("compile-error", v.grade());
        assertTrue(v.text().contains("第") , "应带行号: " + v.text());
    }

    @Test
    void runtimeErrorGrade() {
        String user = "public class Boom { public static void main(String[] args) { throw new RuntimeException(\"炸了\"); } }";
        var v = DictationJudge.judge(REF, user);
        assertEquals("runtime-error", v.grade());
        assertTrue(v.text().contains("RuntimeException"));
    }

    @Test
    void blankPaperGrade() {
        var v = DictationJudge.judge(REF, "   ");
        assertEquals("empty", v.grade());
    }

    @Test
    void bareStatementsAlsoJudged() {
        // 用户只写核心语句——自动包进 main 再判
        String user = """
                int[] a = {5, 2, 9, 1};
                java.util.Arrays.sort(a);
                System.out.println(java.util.Arrays.toString(a));
                """;
        var v = DictationJudge.judge(REF, user);
        assertEquals("pass", v.grade(), "裸语句包装后行为一致应判过: " + v.text());
    }
}
