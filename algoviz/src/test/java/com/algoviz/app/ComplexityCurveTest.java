package com.algoviz.app;

import com.algoviz.ui.ComplexityCurve;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * ComplexityCurve 语料合约（纯 JVM）：真实演示的 Complexity.time() 自由文本 → 曲线族。
 * 语料取自 registry 里的真实写法（含 ×、ⁿ、下标、斜杠备选、纯 CJK）。
 */
class ComplexityCurveTest {

    private static List<String> ids(String time) {
        return ComplexityCurve.parse(time).ids();
    }

    @Test
    void canonicalForms() {
        assertEquals(List.of("nsq"), ids("O(n²)"));
        assertEquals(List.of("linear"), ids("O(n)"));
        assertEquals(List.of("log"), ids("O(log n)"));
        assertEquals(List.of("nlogn"), ids("O(n log n) 稳定"));
        assertEquals(List.of("linear"), ids("O(V + E)"));
        assertEquals(List.of("fact"), ids("O(n!)"));
        assertEquals(List.of("exp"), ids("O(2ⁿ)"));
        assertEquals(List.of("exp"), ids("O(9^m) 最坏 m 个空格"));
    }

    @Test
    void qualifiedAndAlternativeForms() {
        // 快排：平均 n log n / 最坏 n²——两条曲线都高亮，主选在前
        assertEquals(List.of("nlogn", "nsq"), ids("O(n log n) 平均 / O(n²) 最坏"));
        assertEquals(List.of("nsq", "linear"), ids("O(n²) 最坏 / O(n) 近乎有序"));
        assertEquals(List.of("nsq", "nlogn"), ids("O(V²) 简单版 / O(E log V) 堆版"));
        // DES/SM4：固定轮数 × 常数查表 → 常数族
        assertEquals(List.of("constant"), ids("O(16) 轮 × O(1) 查表轮函数"));
        assertEquals(List.of("constant"), ids("O(32) 轮 × O(1) 查表轮函数"));
        // 树高
        assertEquals(List.of("log", "linear"), ids("O(h) = O(log n) 平衡 / O(n) 最坏"));
        // 特殊函数
        assertEquals(List.of("nlogn"), ids("O(n^1.585) 3 次子乘法"));
        assertEquals(List.of("log"), ids("O(log log 1/ε)"));
        assertEquals(List.of("log"), ids("O(log(1/ε))"));
        assertEquals(List.of("log"), ids("O(log min(a,b))"));
        assertEquals(List.of("linear"), ids("O(k) k 个 hash 函数"));
        assertEquals(List.of("linear"), ids("O(n+m)——主串不回退"));
        assertEquals(List.of("linear"), ids("O(轮数×K×N) 分配主导"));
        assertEquals(List.of("nsq"), ids("O(n²·d) 每层"));
    }

    @Test
    void unrecognizableIsSafe() {
        assertTrue(ids("每轮：D 一次前向 + G 一次前向反向").isEmpty(), "无 O 记号 → 空高亮");
        assertTrue(ComplexityCurve.parse(null).ids().isEmpty(), "null → 空高亮");
        assertTrue(ComplexityCurve.parse("").ids().isEmpty(), "空串 → 空高亮");
        assertEquals(List.of("linear"), ids("O(分组数×64) 轮压缩"), "CJK×常数 → 线性族");
        assertEquals(List.of("linear"), ids("O(神经元) 一次 mask"), "CJK 尺寸词 → 线性族");
        assertEquals(List.of("linear"), ids("O(cells) 每轮扫描全网格"), "拉丁尺寸词 → 线性族");
        assertEquals("O(n²)", ComplexityCurve.parse("O(n²)").raw(), "raw 保留原串供图例展示");
    }
}
