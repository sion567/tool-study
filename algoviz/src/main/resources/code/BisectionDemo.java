public class BisectionDemo {
    public static void main(String[] args) {
        double a = 1, b = 2;   //@DATA
        // f(x) = x³ − x − 2：f(1)<0, f(2)>0 → 根在 [1,2]
        for (int i = 1; i <= 10; i++) {
            double mid = (a + b) / 2;
            if (f(mid) < 0) a = mid; else b = mid;   // 按符号扔掉一半
            System.out.printf("步%02d 区间=[%.5f, %.5f]%n", i, a, b);
        }
        System.out.println("根 ≈ " + (a + b) / 2);    // 1.5214
        // 真值 1.5213797…：10 步 = 1/1024 ≈ 3 位小数
    }

    static double f(double x) { return x * x * x - x - 2; }
}
