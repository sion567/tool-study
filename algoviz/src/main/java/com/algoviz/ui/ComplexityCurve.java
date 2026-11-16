package com.algoviz.ui;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 复杂度文本 → 曲线族分类器（纯 JVM，零 JavaFX）。
 * Complexity.time() 是自由文本（"O(n log n) 平均 / O(n²) 最坏"、"O(16) 轮 × O(1) 查表"、
 * "O(h) = O(log n) 平衡 / O(n) 最坏"）——按 '/' 切备选、取每段首个 O(...) 记号做启发式归一化，
 * 映射到 ComplexityPanel 的曲线族 id：constant/log/linear/nlogn/nsq/exp/fact。
 * 识别不了的段跳过（图例仍展示原串）——分类是锦上添花，不是门禁。
 */
public final class ComplexityCurve {

    private static final Pattern O_TOKEN = Pattern.compile("O\\(([^)]*)");

    /** ids：按出现顺序的主选+备选曲线族（去重）；raw：原串（图例展示用）。 */
    public record Curve(List<String> ids, String raw) {
    }

    private ComplexityCurve() {
    }

    public static Curve parse(String time) {
        if (time == null || time.isBlank()) {
            return new Curve(List.of(), time == null ? "" : time);
        }
        LinkedHashSet<String> ids = new LinkedHashSet<>();
        for (String seg : time.split("/")) {          // 备选拆分："平均 / 最坏"
            Matcher m = O_TOKEN.matcher(seg);
            while (m.find()) {                        // 段内逐个记号试到命中为止："O(h) = O(log n)"
                String id = classify(m.group(1));
                if (id != null) {
                    ids.add(id);
                    break;
                }
            }
        }
        return new Curve(List.copyOf(ids), time);
    }

    /** 单个 O(...) 记号体 → 曲线族 id；识别不了返回 null。 */
    static String classify(String token) {
        String s = token.toLowerCase()
                .replace(" ", "").replace("·", "")
                .replace("\u00b2", "^2")            // ² → ^2
                .replace("\u00b3", "^3")            // ³ → ^3
                .replace("\u207f", "^n");           // ⁿ → ^n
        if (s.matches("\\d+")) {
            return "constant";                       // O(16)、O(32)、O(1)
        }
        if (s.contains("n!")) {
            return "fact";                           // O(n!)
        }
        if (s.matches(".*\\^\\d+\\.\\d+.*")) {
            return "nlogn";                          // n^1.585（Karatsuba）→ 最近族
        }
        if (s.contains("^2") || s.contains("^3")) {
            return "nsq";                            // n^2、v^2、n^3（立方并入多项式族）
        }
        if (s.contains("^")) {
            return "exp";                            // 2^n、9^m——指数爆
        }
        if (s.contains("log")) {
            if (s.startsWith("nlog") || s.contains("nlogn")
                    || (s.matches(".*[a-z]log.*") && !s.contains("loglog"))) {
                return "nlogn";                      // n log n、E log V——线性×对数族
            }
            return "log";                            // log n、log h、log(1/ε)、log log
        }
        if (s.matches("[velonmdk]+\\+[velonmdk]+") || s.matches("[velonmdk]")
                || s.contains("n+") || s.contains("+n")) {
            return "linear";                         // V+E、n+m、n、V、k
        }
        if (s.equals("h")) {
            return "log";                            // O(h) 树高——平衡树即 log n
        }
        if (s.matches("[a-z]+")) {
            return "linear";                         // batch、cells、words——一个尺寸词
        }
        if (s.contains("+")) {
            return "linear";                         // 文本 + 模式总长 + 命中数（CJK 求和也是线性）
        }
        if (s.contains("×") || s.contains("*")) {
            return "linear";                         // 轮数×K×N、特征×行数——随规模线性铺开
        }
        if (s.matches(".*[\u4e00-\u9fa5].*")) {
            return "linear";                         // 神经元、步数、词表、采样点——单个 CJK 尺寸词
        }
        return null;                                 // 未知 → 跳过
    }
}
