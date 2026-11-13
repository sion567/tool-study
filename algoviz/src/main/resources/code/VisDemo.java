public class VisDemo {
    static boolean flag;           // 演示一：无 volatile——写别人看不见（JMM 允许工作副本）
    static volatile boolean vFlag; // 演示二：volatile——写立即刷主存、读绕过副本
    static int count;              // 演示三：count++ = 读-改-写三步，非原子

    public static void main(String[] args) throws Exception {
        Thread writer = new Thread(() -> flag = true);
        writer.start();
        Thread.sleep(100);         // 足够长：写早已完成——但主存可能仍是 false（未刷）
        System.out.println("读 flag = " + flag + "（无 volatile：'看不见'是合法行为）");

        vFlag = true;              // volatile 写：强制刷主存
        System.out.println("读 vFlag = " + vFlag + "（volatile 直读主存）");

        Runnable inc = () -> { for (int i = 0; i < 10000; i++) count++; };   // 非原子：丢失更新
        Thread t1 = new Thread(inc), t2 = new Thread(inc);
        t1.start(); t2.start(); t1.join(); t2.join();
        System.out.println("两次 10000 次 ++ 后 count = " + count + "（20000 才对——丢失更新）");
    }
}
// 解药：volatile 治可见性、AtomicInteger 的 CAS 自旋治原子性——失败不写回，重读再试
