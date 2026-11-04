import java.util.concurrent.*;

public class ThreadPoolDemo {
    public static void main(String[] args) throws Exception {
        // corePoolSize=2：2 个常驻 worker 循环「take → 执行 → 再 take」
        ThreadPoolExecutor pool = new ThreadPoolExecutor(
                2, 2, 0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>(10));           // workQueue：削峰
        for (int i = 1; i <= 3; i++) {                    //@DATA   提交 T1/T2/T3
            final int id = i;
            pool.submit(() -> {
                System.out.println("T" + id + " 由 " + Thread.currentThread().getName() + " 执行");
                try { Thread.sleep(100); } catch (InterruptedException ignored) {}
            });
        }
        pool.shutdown();
        pool.awaitTermination(5, TimeUnit.SECONDS);
        System.out.println("3 个任务只用了 2 根线程——复用完成");
        // 拒绝策略：队列满 + 到 maximumPoolSize → AbortPolicy/CallerRunsPolicy...
    }
}
