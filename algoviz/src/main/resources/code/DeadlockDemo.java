public class DeadlockDemo {
    static Object lockA = new Object(), lockB = new Object();

    public static void main(String[] args) {
        // 危险写法：T1 先 A 后 B、T2 先 B 后 A——交叉持锁可能死锁（四要件全齐）
        // new Thread(() -> { synchronized (lockA) { sleep(); synchronized (lockB) { ... } } }).start();
        // new Thread(() -> { synchronized (lockB) { sleep(); synchronized (lockA) { ... } } }).start();
        // jstack 会看到两个线程互为 BLOCKED——'程序卡死但 CPU 0%'
        // 破解一：锁排序——全程序统一先 A 后 B：环路结构被全局顺序拆掉
        // 破解二：tryLock 带超时——tryLock(1, SECONDS) 拿不到就释放已有锁，退避重试
        Runnable sortedT1 = () -> { synchronized (lockA) { synchronized (lockB) { System.out.println("T1 按序 A→B 完成"); } } };
        Runnable sortedT2 = () -> { synchronized (lockA) { synchronized (lockB) { System.out.println("T2 按序 A→B 完成"); } } };
        new Thread(sortedT1).start();
        new Thread(sortedT2).start();
        // 两条线程都对 A 先下手——必有先后，B 的竞争永远在持有 A 之后：环路不可能形成
    }
}
