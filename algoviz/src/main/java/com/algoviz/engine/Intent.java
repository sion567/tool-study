package com.algoviz.engine;

/**
 * 意图层（蓝图 §2.4）：本步为什么对 + 当前不变量。
 * 可视化回答「发生了什么」，意图层回答「为什么这一步是对的」。
 * 每帧可自动断言校验：不变量被破坏 = 代码错了 = 脑内画面错了。
 */
public record Intent(String why, String invariant) {
}
