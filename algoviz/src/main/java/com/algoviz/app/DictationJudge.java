package com.algoviz.app;

import java.util.ArrayList;
import java.util.List;

/**
 * 默写判卷（蓝图 §2.5-B 升级）：文本 diff 之外的第二把尺子——**行为级**。
 * 用户代码和参考代码各编译运行一遍，比对标准输出：
 * 输出一致 = 行为一致（写法不同不扣分）；输出不同 = 哪行不一样现场 diff。
 * 编译失败给出行号与诊断；死循环给超时标记。
 */
public final class DictationJudge {

    private static final long TIMEOUT_MS = 2000;

    /** 判卷结论。 */
    public record Verdict(String grade, String text) {
    }

    private DictationJudge() {
    }

    public static Verdict judge(String referenceCode, String userCode) {
        if (userCode == null || userCode.strip().isEmpty()) {
            return new Verdict("empty", "白纸还是空的——先凭记忆写点什么，再点行为判卷。");
        }
        String refSrc = JavaRunner.normalize(referenceCode);
        String userSrc = JavaRunner.normalize(userCode.strip());
        String refClass = JavaRunner.classNameOf(refSrc);
        String userClass = JavaRunner.classNameOf(userSrc);

        JavaRunner.RunResult user = JavaRunner.compileAndRun(userClass, userSrc, TIMEOUT_MS);
        if (!user.compiled()) {
            return new Verdict("compile-error",
                    "⚠️ 编译失败（先改语法，判卷不下去了）：\n" + user.compileErrors());
        }
        if (user.error() != null && user.error().startsWith("超时")) {
            return new Verdict("timeout", "⏱ 运行超时（>" + TIMEOUT_MS + "ms）——检查是不是死循环。"
                    + (user.stdout().isEmpty() ? "" : "\n超时前的输出：\n" + user.stdout()));
        }
        if (user.error() != null) {
            return new Verdict("runtime-error", "💥 运行异常：" + shortErr(user.error())
                    + (user.stdout().isEmpty() ? "" : "\n异常前的输出：\n" + user.stdout()));
        }

        // 参考答案跑一遍做基准
        JavaRunner.RunResult ref = JavaRunner.compileAndRun(refClass, refSrc, TIMEOUT_MS);
        if (!ref.compiled() || ref.error() != null) {
            return new Verdict("ref-broken", "参考代码自己没跑起来（请反馈）："
                    + (ref.error() != null ? ref.error() : ref.compileErrors()));
        }

        List<String> a = normalizeLines(user.stdout());
        List<String> b = normalizeLines(ref.stdout());
        if (a.equals(b)) {
            return new Verdict("pass", "✅ 行为一致！输出 " + a.size() + " 行完全吻合——"
                    + "写法可以和参考不一样，跑起来对就是真的对。\n输出：\n" + String.join("\n", a));
        }
        // 行为差异：逐行对照（复用 LCS diff 的标记思路）
        String diff = lineDiff(b, a);
        return new Verdict("behavior-diff",
                "❌ 行为不同——输出 " + a.size() + " 行，参考 " + b.size() + " 行。\n"
                        + "（= 一致 / − 参考有而你没有 / + 你多出来的）\n" + diff);
    }

    private static List<String> normalizeLines(String stdout) {
        List<String> out = new ArrayList<>();
        for (String line : stdout.split("\r?\n")) {
            if (!line.isBlank()) {
                out.add(line.strip());
            }
        }
        return out;
    }

    private static String shortErr(String err) {
        String first = err.split("\n", 2)[0];
        return first.length() > 220 ? first.substring(0, 220) + "…" : first;
    }

    /** 简易 LCS 对齐：= 一致 / − 缺 / + 多。 */
    private static String lineDiff(List<String> ref, List<String> user) {
        int n = ref.size(), m = user.size();
        int[][] dp = new int[n + 1][m + 1];
        for (int i = n - 1; i >= 0; i--) {
            for (int j = m - 1; j >= 0; j--) {
                dp[i][j] = ref.get(i).equals(user.get(j))
                        ? dp[i + 1][j + 1] + 1
                        : Math.max(dp[i + 1][j], dp[i][j + 1]);
            }
        }
        StringBuilder sb = new StringBuilder();
        int i = 0, j = 0, shown = 0;
        while (i < n && j < m && shown < 24) {
            if (ref.get(i).equals(user.get(j))) {
                sb.append("  = ").append(ref.get(i)).append("\n");
                i++;
                j++;
            } else if (dp[i + 1][j] >= dp[i][j + 1]) {
                sb.append("  − ").append(ref.get(i)).append("\n");
                i++;
            } else {
                sb.append("  + ").append(user.get(j)).append("\n");
                j++;
            }
            shown++;
        }
        while (i < n && shown < 24) {
            sb.append("  − ").append(ref.get(i++)).append("\n");
            shown++;
        }
        while (j < m && shown < 24) {
            sb.append("  + ").append(user.get(j++)).append("\n");
            shown++;
        }
        return sb.toString();
    }
}
