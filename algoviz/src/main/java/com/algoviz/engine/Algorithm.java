package com.algoviz.engine;

import com.algoviz.engine.trace.Trace;

/**
 * 生产者接口（蓝图 §6.1 引擎层，策略模式）：
 * 算法只吐 Trace，永远不碰 UI（蓝图 §8 原则 1）。
 * 未来生产者——JDI 自动插桩 / AI 生成 / 数学推导器——都实现这个口。
 */
public interface Algorithm {

    String id();

    String title();

    /** 展示用源码资源路径：代码面板 + Loc 行号都对齐它。 */
    String codeResource();

    /** 课件 Markdown 资源路径（M2：右侧「说明」页）。 */
    String docResource();

    Trace produce();

    /** 统一构造 TraceMeta（input 如 "5,2,9,1,5"）。 */
    default Trace.TraceMeta meta(int[] input) {
        StringBuilder sb = new StringBuilder();
        for (int k = 0; k < input.length; k++) {
            if (k > 0) {
                sb.append(',');
            }
            sb.append(input[k]);
        }
        return new Trace.TraceMeta(id(), title(), codeResource(), sb.toString(), input.length);
    }
}
