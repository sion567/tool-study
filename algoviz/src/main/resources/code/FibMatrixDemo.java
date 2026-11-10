public class FibMatrixDemo {
    public static void main(String[] args) {
        int target = 32;   //@DATA
        long[][] m = {{1, 1}, {1, 0}};   // M¹：右上角 = F(1)
        // 平方链：M → M² → M⁴ → M⁸ → M¹⁶ → M³²
        for (int e = 2; e <= target; e *= 2) {
            m = matMul(m, m);
            System.out.printf("M^%d 右上角 = F(%d) = %d%n", e, e, m[0][1]);
        }
        // n=32 只用 5 次矩阵乘法；n=2¹⁰⁰ 用 100 次——O(log n)
    }

    static long[][] matMul(long[][] a, long[][] b) {
        return new long[][]{
                {a[0][0] * b[0][0] + a[0][1] * b[1][0], a[0][0] * b[0][1] + a[0][1] * b[1][1]},
                {a[1][0] * b[0][0] + a[1][1] * b[1][0], a[1][0] * b[0][1] + a[1][1] * b[1][1]}
        };
    }
}
