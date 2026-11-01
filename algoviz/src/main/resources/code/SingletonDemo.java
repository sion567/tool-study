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
}
