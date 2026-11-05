package com.algoviz.app;

import java.util.ArrayList;
import java.util.List;

/**
 * 默写对比：用户代码 vs 参考答案，逐行 LCS diff。
 * 输出人类可读的差异报告：缺了哪块、多了哪块、哪些行一致。
 */
public final class DictationDiff {

    public record DiffLine(String type, String text) {}   // type: SAME / MISSING / EXTRA

    private DictationDiff() {
    }

    /** 逐行 LCS diff（简洁实现，够教学场景用）。 */
    public static List<DiffLine> diff(String userCode, String referenceCode) {
        String[] u = normalize(userCode);
        String[] r = normalize(referenceCode);
        int[][] dp = lcsTable(u, r);

        List<DiffLine> out = new ArrayList<>();
        int i = u.length, j = r.length;
        while (i > 0 || j > 0) {
            if (i > 0 && j > 0 && u[i - 1].equals(r[j - 1])) {
                out.add(new DiffLine("SAME", u[i - 1]));
                i--; j--;
            } else if (j > 0 && (i == 0 || dp[i][j - 1] >= dp[i - 1][j])) {
                out.add(new DiffLine("MISSING", r[j - 1]));   // 参考有、用户没有
                j--;
            } else {
                out.add(new DiffLine("EXTRA", u[i - 1]));      // 用户有、参考没有
                i--;
            }
        }
        java.util.Collections.reverse(out);
        return out;
    }

    /** 生成人类可读的差异报告。 */
    public static String report(List<DiffLine> lines) {
        if (lines.isEmpty()) return "（空）";
        int same = 0, missing = 0, extra = 0;
        StringBuilder sb = new StringBuilder();
        for (DiffLine dl : lines) {
            switch (dl.type()) {
                case "SAME" -> same++;
                case "MISSING" -> { missing++; sb.append("  ❌ 缺少: ").append(dl.text()).append('\n'); }
                case "EXTRA" -> { extra++; sb.append("  ⚠️ 多余: ").append(dl.text()).append('\n'); }
            }
        }
        String header = "对比结果: 一致 " + same + " 行 · 缺少 " + missing + " 行 · 多余 " + extra + " 行\n";
        if (missing == 0 && extra == 0) {
            return header + "🎉 完全一致！";
        }
        return header + sb;
    }

    private static String[] normalize(String code) {
        List<String> out = new ArrayList<>();
        for (String line : code.lines().toList()) {
            String t = line.strip();
            if (!t.isEmpty()) out.add(t);
        }
        return out.toArray(new String[0]);
    }

    private static int[][] lcsTable(String[] u, String[] r) {
        int[][] dp = new int[u.length + 1][r.length + 1];
        for (int i = 1; i <= u.length; i++) {
            for (int j = 1; j <= r.length; j++) {
                dp[i][j] = u[i - 1].equals(r[j - 1])
                        ? dp[i - 1][j - 1] + 1
                        : Math.max(dp[i - 1][j], dp[i][j - 1]);
            }
        }
        return dp;
    }
}
