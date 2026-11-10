public class DerivativeDemo {
    public static void main(String[] args) {
        double x0 = 1.5;   //@DATA
        // 割线斜率：Δx 逐次减半，看它收敛到什么
        for (double dx : new double[]{1, 0.5, 0.25, 0.125, 0.0625}) {
            double m = (f(x0 + dx) - f(x0)) / dx;
            System.out.printf("dx=%.4f 割线斜率=%.4f%n", dx, m);
        }
        System.out.println("导数 f'(x0) = " + fPrime(x0));   // 3 = 2x0
    }

    static double f(double x)      { return x * x; }        // 曲线
    static double fPrime(double x) { return 2 * x; }        // 导数（极限的捷径）

    // 数值导数（没有公式时的通用算法）：Δx 很小的一步差分
    static double numericDerivative(double x, double dx) {
        return (f(x + dx) - f(x)) / dx;
    }
}
