public class SingletonDemo {
    static class Config {
        private static class Holder {                 // JVM 类加载保证线程安全
            static final Config INSTANCE = new Config();
        }
        static Config getInstance() { return Holder.INSTANCE; }   // 永远返回同一个
    }
    public static void main(String[] args) {
        Config a = Config.getInstance();              // 第一次：类加载 → 造实例
        Config b = Config.getInstance();              // 第二次：直接拿同一个
        System.out.println(a == b);                   // true：同一根引用
    }

    // ===== 变体一：饿汉式（类加载即创建，简单但加载时机不可控） =====
    static class Eager {
        private static final Eager INSTANCE = new Eager();
        static Eager getInstance() { return INSTANCE; }
    }

    // ===== 变体二：懒汉式 + 双重检查锁（延迟加载；注意 volatile 防止半初始化） =====
    static class LazyDcl {
        private static volatile LazyDcl instance;
        static LazyDcl getInstance() {
            if (instance == null) {
                synchronized (LazyDcl.class) {
                    if (instance == null) instance = new LazyDcl();
                }
            }
            return instance;
        }
    }

    // ===== 变体三：枚举（Effective Java 推荐：防反射、防序列化破坏） =====
    enum EnumSingleton {
        INSTANCE;
        void doWork() { System.out.println("枚举单例"); }
    }

    // ===== 变体四：静态内部类（本演示用的 Holder 惯用法） =====
    // 优点：延迟加载 + 线程安全全靠 JVM，零同步成本
}
