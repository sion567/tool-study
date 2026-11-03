package com.algoviz.engine;

/**
 * 复杂度标注（M7 第一版）：时间 + 空间 + 「为什么是这个」的推导。
 * 步数计数来自实际 Trace（steps.size()），代码旁直接对照。
 */
public record Complexity(String time, String space, String why) {
}
