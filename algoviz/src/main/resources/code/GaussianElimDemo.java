public class GaussianElimDemo {
    public static void main(String[] args) {
        double[][] m = {                 //@DATA
                {1, 1, 1, 6},
                {1, 2, 3, 14},
                {1, 4, 9, 36}
        };
        int n = 3;
        // 前向消元：O(n³)
        for (int col = 0; col < n; col++) {
            for (int row = col + 1; row < n; row++) {
                double k = m[row][col] / m[col][col];
                for (int c = col; c <= n; c++) {
                    m[row][c] -= k * m[col][c];
                }
            }
        }
        // 回代：自底向上
        double[] x = new double[n];
        for (int row = n - 1; row >= 0; row--) {
            x[row] = m[row][n];
            for (int c = row + 1; c < n; c++) {
                x[row] -= m[row][c] * x[c];
            }
            x[row] /= m[row][row];
        }
        System.out.println("x=" + x[0] + " y=" + x[1] + " z=" + x[2]);   // 1, 2, 3
    }
}
