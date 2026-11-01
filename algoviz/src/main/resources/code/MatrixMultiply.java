public class MatrixMultiply {
    public static void main(String[] args) {
        double[][] A = { {1, 2, 3}, {4, 5, 6} };      // 2×3
        double[][] B = { {7, 8}, {9, 10}, {11, 12} }; // 3×2
        double[][] C = new double[2][2];              // 结果 2×2
        for (int i = 0; i < 2; i++) {
            for (int j = 0; j < 2; j++) {
                for (int k = 0; k < 3; k++) {
                    C[i][j] += A[i][k] * B[k][j];     // ← 三重循环的本体
                }
            }
        }
    }
}
