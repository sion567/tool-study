public class LisDemo {
    public static void main(String[] args) {
        int[] a = {3, 1, 4, 1, 5, 9, 2, 6};   //@DATA
        int[] dp = new int[a.length];          // dp[i] = 以 a[i] 结尾的 LIS 长度
        int best = 0;
        for (int i = 0; i < a.length; i++) {
            dp[i] = 1;                         // 至少自己一个
            for (int j = 0; j < i; j++) {
                if (a[j] < a[i]) dp[i] = Math.max(dp[i], dp[j] + 1);
            }
            best = Math.max(best, dp[i]);
        }
        System.out.println("LIS 长度 = " + best);   // 1,4,5,9 → 4
    }
}
