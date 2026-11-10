import java.util.Random;

public class MonteCarloPiDemo {
    public static void main(String[] args) {
        long seed = 42;   //@DATA
        Random r = new Random(seed);   // 固定种子 = 可复现
        int in = 0, n = 6300;
        for (int i = 1; i <= n; i++) {
            double x = r.nextDouble(), y = r.nextDouble();
            if (x * x + y * y <= 1.0) in++;      // 落在四分之一圆内
            if (i % 100 == 0) {
                System.out.printf("n=%5d  π≈%.4f%n", i, 4.0 * in / i);
            }
        }
        // 误差 ~1/√n：点数翻 4 倍，精度才翻 1 倍
    }
}
