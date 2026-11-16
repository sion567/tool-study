package com.algoviz.engine;

import com.algoviz.engine.registry.AlgorithmSection;
import com.algoviz.engine.registry.CoreSection;
import com.algoviz.engine.registry.CryptoSection;
import com.algoviz.engine.registry.LearnSection;
import com.algoviz.engine.registry.MathSection;
import com.algoviz.engine.registry.PatternSection;

import java.util.List;

/** 算法注册表：按域分片聚合（see registry 包）。新增演示：往对应 Section 加一行 Entry 即可，TreeView 自动多一个叶子。 */
public final class AlgorithmRegistry {

    /** sub = 子分组（设计模式用 创建型/结构型/行为型），其他域传 null。 */
    public record Entry(String category, String sub, Algorithm algorithm) {
    }

    /** 域分片单例（无状态）。展示顺序 = 分片顺序 = entries() 顺序——category 内相对顺序与拆分前一致。 */
    private static final List<AlgorithmSection> SECTIONS = List.of(
            new CoreSection(), new MathSection(), new PatternSection(), new CryptoSection(), new LearnSection());

    /** 缓存的唯一实例列表——调用方（如循环切换）可以做引用比较。 */
    private static final List<Entry> ALL = SECTIONS.stream()
            .flatMap(s -> s.entries().stream())
            .toList();

    private AlgorithmRegistry() {
    }

    public static List<Entry> all() {
        return ALL;
    }

    /** 🎲 换一批随机数据：按 id 返回换了输入的新实例；不支持的演示返回 null（按钮置灰）。 */
    public static Algorithm randomVariant(Algorithm current) {
        return randomVariant(current, 5 + new java.util.Random().nextInt(4));
    }

    /** 该演示是否支持随机变体（不支持则 🎲/滑杆置灰——固定剧本类没有「换一批数据」）。 */
    public static boolean hasVariant(Algorithm current) {
        return variantOf(current, 8) != null;
    }

    /** 数据量滑杆入口：n = 期望元素个数（算法内部再微调，如二分固定 7、双指针需 7+）。 */
    public static Algorithm randomVariant(Algorithm current, int n) {
        return variantOf(current, n);
    }

    /** 分片转发：第一个认识该 id 的 section 出变体。固定剧本类（dl、ml、sm2 等）无 case，
     *  落到 null，hasVariant=false，🎲/滑杆自动置灰（剧本无法"换一批数据"）。 */
    private static Algorithm variantOf(Algorithm current, int n) {
        java.util.Random r = new java.util.Random();
        for (AlgorithmSection s : SECTIONS) {
            Algorithm a = s.variantOf(current.id(), n, r);
            if (a != null) {
                return a;
            }
        }
        return null;
    }
}
