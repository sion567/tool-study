public class YangHuiDemo {
    public static void main(String[] args) {
        int rows = 9;   //@DATA
        int[][] t = new int[rows][];
        for (int n = 0; n < rows; n++) {
            t[n] = new int[n + 1];
            for (int k = 0; k <= n; k++) {
                // C(n,k) = C(n-1,k-1) + C(n,k-1)——肩上两数之和（DP 的祖先）
                t[n][k] = (k == 0 || k == n) ? 1 : t[n - 1][k - 1] + t[n - 1][k];
            }
            System.out.println(java.util.Arrays.toString(t[n]));
        }
        // 第 8 行 = [1,8,28,56,70,56,28,8,1]；行和 = 2ⁿ
    }
}
