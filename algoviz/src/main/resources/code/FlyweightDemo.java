public class FlyweightDemo {
    static class TreeStyle {                            // 共享的内在状态（只有两份）
        final String color; final String shape;
        TreeStyle(String c, String s) { color = c; shape = s; }
    }
    static class StylePool {                            // 池：相同 key 只造一次
        private final java.util.Map<String, TreeStyle> pool = new java.util.HashMap<>();
        TreeStyle get(String key) {
            return pool.computeIfAbsent(key, k -> new TreeStyle(k, "圆锥"));
        }
    }
    public static void main(String[] args) {
        StylePool pool = new StylePool();
        var a = pool.get("绿色");                        // 第一次：真的造
        var b = pool.get("绿色");                        // 第二次：池里拿同一个
        var c = pool.get("蓝色");                        // 新 key：才造新的
        System.out.println(a == b);                      // true —— 共享
    }
}
