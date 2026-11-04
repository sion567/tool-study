import java.util.concurrent.*;
import java.util.concurrent.locks.*;

public class RwLockDemo {
    static final ReadWriteLock LOCK = new ReentrantReadWriteLock();

    public static void main(String[] args) throws Exception {
        // 场景：读 99% 写 1%（配置表 / 缓存）——读锁共享，吞吐近似乘核数
        Thread r1 = reader("A"), r2 = reader("B");
        Thread w = new Thread(() -> {
            LOCK.writeLock().lock();                  // 等所有读者退出
            try { System.out.println("写者独占：改数据"); }
            finally { LOCK.writeLock().unlock(); }
        });
        r1.start(); r2.start();
        Thread.sleep(50);                             // 确保读者先进
        w.start();
        r1.join(); r2.join(); w.join();
        System.out.println("done");
    }

    static Thread reader(String name) {
        return new Thread(() -> {
            LOCK.readLock().lock();                   // 读-读共享：多个读者并行
            try {
                System.out.println("读者 " + name + " 在读");
                Thread.sleep(100);
            } catch (InterruptedException ignored) {
            } finally {
                LOCK.readLock().unlock();             // 读计数归零 → 唤醒写者
            }
        });
    }
}
