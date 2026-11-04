import java.util.concurrent.*;

public class ProdConsDemo {
    public static void main(String[] args) throws Exception {
        BlockingQueue<String> queue = new ArrayBlockingQueue<>(2);   //@DATA   容量 2

        // 生产者：连放 4 个——队列满时 put() 自动阻塞（背压）
        Thread producer = new Thread(() -> {
            try {
                for (String v : new String[]{"A", "B", "C", "D"}) {
                    queue.put(v);                     // 满 → 挂起等 notFull
                    System.out.println("put " + v + "  队列=" + queue);
                }
                queue.put("END");
            } catch (InterruptedException ignored) {}
        });

        // 消费者：循环 take——队列空时 take() 自动阻塞
        Thread consumer = new Thread(() -> {
            try {
                while (true) {
                    String v = queue.take();          // 空 → 挂起等 notEmpty
                    if (v.equals("END")) break;
                    System.out.println("take " + v);
                    Thread.sleep(150);                // 消费慢 → 制造背压
                }
            } catch (InterruptedException ignored) {}
        });

        producer.start(); consumer.start();
        producer.join(); consumer.join();
        System.out.println("done");
    }
}
