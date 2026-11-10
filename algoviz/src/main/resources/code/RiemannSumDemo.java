public class RiemannSumDemo {
    public static void main(String[] args) {
        int[] ns = {1, 2, 4, 8, 16, 32};   //@DATA
        for (int n : ns) {
            System.out.printf("n=%2d  黎曼和=%.4f%n", n, riemann(n));
        }
        System.out.println("真值 1/3 = " + 1.0 / 3);
    }

    // ∫₀¹ x² dx 的左端点黎曼和：切 n 个矩形，加面积
    static double riemann(int n) {
        double sum = 0, dx = 1.0 / n;
        for (int i = 0; i < n; i++) {
            double x = i * dx;          // 左端点
            sum += x * x * dx;          // 高 × 宽
        }
        return sum;                     // n→∞ 时 → 1/3
    }
}
