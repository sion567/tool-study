public class NewtonDemo {
    public static void main(String[] args) {
        // 求 x² − 2 = 0 的正根（即 √2）
        double x = 2.0;                       //@DATA
        for (int i = 0; i < 6; i++) {
            double fx = f(x), dfx = df(x);    // 函数值 + 导数值（切线斜率）
            double next = x - fx / dfx;       // 切线与 x 轴的交点
            System.out.printf("x%d = %.9f  f = %.2e%n", i, x, fx);
            if (Math.abs(next - x) < 1e-12) break;
            x = next;
        }
        System.out.println("√2 ≈ " + x);
    }
    static double f(double x)  { return x * x - 2; }
    static double df(double x) { return 2 * x; }
}
