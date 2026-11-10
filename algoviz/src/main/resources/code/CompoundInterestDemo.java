public class CompoundInterestDemo {
    public static void main(String[] args) {
        int[] ns = {1,2,4,8,16,32,64,128};   //@DATA
        for (int n : ns) {
            // 本金 1 元、年利率 100%，一年结 n 次
            double v = Math.pow(1 + 1.0 / n, n);
            System.out.printf("n=%3d  终值=%.4f%n", n, v);
        }
        System.out.println("连续复利 → e = " + Math.E);
        // (1+1/n)ⁿ 单调递增有上界 → 极限 = e（伯努利 1683）
    }
}
