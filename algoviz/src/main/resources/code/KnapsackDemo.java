public class KnapsackDemo {
    public static void main(String[] args) {
        int[] weights = {1, 3, 4};
        int[] values = {15, 20, 30};
        int capacity = 4;
        int[][] dp = new int[N + 1][CAPACITY + 1];
        for (int i = 1; i <= N; i++) {
            for (int w = 0; w <= CAPACITY; w++) {
                if (weights[i-1] > w) dp[i][w] = dp[i-1][w];
                else dp[i][w] = Math.max(dp[i-1][w], dp[i-1][w-weights[i-1]] + values[i-1]);
            }
        }
        System.out.println("最大价值 = " + dp[N][CAPACITY]);
    }
}
