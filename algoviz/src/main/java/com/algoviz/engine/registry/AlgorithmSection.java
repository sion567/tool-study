package com.algoviz.engine.registry;

import com.algoviz.engine.Algorithm;
import com.algoviz.engine.AlgorithmRegistry;

import java.util.List;

/** 注册表的域分片：每个域自带条目清单与变体工厂，AlgorithmRegistry 聚合（顺序 = 分片顺序）。 */
public interface AlgorithmSection {

    /** 本域全部演示条目（category 内相对顺序 = 原 registry 顺序）。 */
    List<AlgorithmRegistry.Entry> entries();

    /** 🎲 按 id 换一批随机数据（n = 期望元素个数）；不认识的 id 返回 null（固定剧本类）。 */
    Algorithm variantOf(String id, int n, java.util.Random r);

    // ---- 随机数据工具（原 AlgorithmRegistry helpers，各域共用） ----

    static int[] randArray(java.util.Random r, int n) {
        int[] a = new int[n];
        for (int i = 0; i < n; i++) {
            a[i] = 1 + r.nextInt(99);
        }
        return a;
    }

    static int[] randDistinct(java.util.Random r, int n, int lo, int hi) {
        java.util.Set<Integer> s = new java.util.LinkedHashSet<>();
        while (s.size() < n) {
            s.add(lo + r.nextInt(hi - lo + 1));
        }
        return s.stream().mapToInt(Integer::intValue).toArray();
    }

    static int[] randomKeys(java.util.Random r, int n) {
        return randDistinct(r, n, 1, 99);
    }

    static int[] randomWeights(java.util.Random r) {
        int[] w = new int[9];   // 示例图固定 9 条边
        for (int i = 0; i < w.length; i++) {
            w[i] = 1 + r.nextInt(20);
        }
        return w;
    }
}
