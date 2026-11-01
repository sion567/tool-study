package com.algoviz.engine;

/**
 * 图层标记接口（蓝图 §2.1）：一步 = 各图层的不可变快照。
 * 实现者：mem.StackLayer / mem.HeapLayer（算法域内存现场）、ds.ArrayView（算法语义视图）。
 * 未来设计模式域贡献 seq.*、数学域贡献 math.*，
 * 播放器 / diff / 训练层一行不改——加图层即可，这才叫通用底层。
 * （注：工程暂未模块化，sealed 跨包 permit 在无名模块不合法，故用普通接口；模块化后可升级。）
 */
public interface Layer {
}
