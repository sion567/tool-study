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

    /**
     * 🎲 随机输入后，代码页 main() 里需要同步替换的数据行。
     * CodePanel 会用这些行替换清单中带 //@DATA 标记的那一行；默认空 = 代码与数据无关。
     */
    default java.util.List<String> dataLines() {
        return java.util.Collections.emptyList();
    }

    /** int 数组 → "int[] a = {1, 2, 3};" 形式的代码行（供 dataLines 使用）。 */
    static String joinArray(int[] a) {
        StringBuilder sb = new StringBuilder("int[] a = {");
        for (int k = 0; k < a.length; k++) {
            if (k > 0) {
                sb.append(", ");
            }
            sb.append(a[k]);
        }
        return sb.append("};").toString();
    }

    /**
     * 复杂度标注（M7）：时间 + 空间 + 推导过程。null = 不适用（模式/数学演示）。
     */
    default Complexity complexity() {
        return null;
    }
}
