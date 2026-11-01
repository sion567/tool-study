package com.algoviz.engine.trace;

import com.algoviz.engine.Step;

import java.util.List;

/**
 * Trace 合约（蓝图 §2.1 铁律 2）：Trace 是文件，不是内存对象。
 * 版本化 JSON 既是 golden 测试的比对基准，也是将来换生产者
 * （手写引擎 → JDI 自动插桩 → AI 生成 → 数学推导器）的统一接口。
 */
public record Trace(String schemaVersion, TraceMeta meta, List<Step> steps) {

    public record TraceMeta(String algorithmId, String title, String codeResource,
                            String input, int inputSize) {
    }
}
