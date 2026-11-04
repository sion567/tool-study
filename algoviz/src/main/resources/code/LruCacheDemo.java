public class LruCacheDemo {
    static final int CAP = 3;
    static java.util.LinkedHashMap<String, String> cache = new java.util.LinkedHashMap<>(3, 0.75f, true);

    public static void main(String[] args) {
        put("1", "A");   // {1=A}
        put("2", "B");   // {1=A, 2=B}
        get("1");        // {2=B, 1=A} ← 1 移到尾部（最近用）
        put("3", "C");   // {2=B, 1=A, 3=C}
        put("4", "D");   // 超容量 → 淘汰 2 → {1=A, 3=C, 4=D}
        System.out.println("缓存: " + cache);
    }
    static void put(String k, String v) {
        cache.put(k, v);
        if (cache.size() > CAP) {
            String oldest = cache.keySet().iterator().next();
            cache.remove(oldest);
        }
    }
    static String get(String k) { return cache.get(k); }
}
