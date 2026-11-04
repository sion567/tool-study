public class TaylorDemo {
    public static void main(String[] args) {
        double x = 1.0;   // 求 sin(1)
        for (int terms : new int[]{1, 3, 5, 7, 9}) {
            System.out.printf("%d 项近似 sin(%.1f) = %.6f（真实 %.6f）%n",
                    terms, x, taylorSin(x, terms), Math.sin(x));
        }
    }
    static double taylorSin(double x, int n) {
        double sum = 0, power = x;
        int sign = 1;
        for (int k = 1; k <= n; k += 2) {     // 奇数次幂
            sum += sign * power / factorial(k);
            power *= x * x;                    // x⁵ = x³ · x²
            sign = -sign;                      // 正负交替
        }
        return sum;
    }
    static double factorial(int n) {
        double f = 1;
        for (int i = 2; i <= n; i++) f *= i;
        return f;
    }
}
