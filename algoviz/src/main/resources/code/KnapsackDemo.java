public class KnapsackDemo {
    public static void main(String[] args) {
        int[] weights = {1, 3, 4}; int[] values = {15, 20, 30}; int capacity = 4;   //@DATA
        int n = weights.length;
        int[][] dp = new int[n + 1][capacity + 1];
        for (int i = 1; i <= n; i++) {
            for (int w = 0; w <= capacity; w++) {
                if (weights[i-1] > w) dp[i][w] = dp[i-1][w];
                else dp[i][w] = Math.max(dp[i-1][w], dp[i-1][w-weights[i-1]] + values[i-1]);
            }
        }
        System.out.println("最大价值 = " + dp[n][capacity]);
    }
}
