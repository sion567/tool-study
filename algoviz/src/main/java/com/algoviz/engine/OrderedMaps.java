package com.algoviz.engine;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Map.of 的确定性替代：Map.of 的迭代顺序每次 JVM 随机（ImmutableCollections SALT），
 * golden 测试会抖动；这里用 LinkedHashMap 保证插入序稳定。
 */
public final class OrderedMaps {

    private OrderedMaps() {
    }

    /** 与 Map.of(k1,v1,k2,v2,...) 同形，返回插入序 LinkedHashMap。 */
    public static Map<String, Value> of(Object... kv) {
        if (kv.length % 2 != 0) {
            throw new IllegalArgumentException("键值必须成对: " + kv.length);
        }
        Map<String, Value> m = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) {
            m.put((String) kv[i], (Value) kv[i + 1]);
        }
        return m;
    }

    /** 字符串版（TreeEmitter varTags 等场景）。 */
    public static Map<String, String> ofStrings(Object... kv) {
        if (kv.length % 2 != 0) {
            throw new IllegalArgumentException("键值必须成对: " + kv.length);
        }
        Map<String, String> m = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) {
            m.put((String) kv[i], (String) kv[i + 1]);
        }
        return m;
    }
}
