public class FibonacciDemo {
    public static void main(String[] args) {
        int n = 7;
        // 版本一：纯递归 O(2ⁿ)——大量重复子问题
        System.out.println("递归: " + fibRec(n));
        // 版本二：备忘录 O(n)——缓存已算过的结果
        int[] memo = new int[n + 1];
        java.util.Arrays.fill(memo, -1);
        System.out.println("备忘录: " + fibMemo(n, memo));
        // 版本三：DP O(n) 时间 O(1) 空间——从底向上，只保留前两个值
        int a = 0, b = 1;
        for (int i = 2; i <= n; i++) { int t = a + b; a = b; b = t; }
        System.out.println("DP: " + b);
    }
    static int fibRec(int n) {
        if (n <= 1) return n;
        return fibRec(n - 1) + fibRec(n - 2);
    }
    static int fibMemo(int n, int[] memo) {
        if (n <= 1) return n;
        if (memo[n] != 0) return memo[n];
        memo[n] = fibMemo(n - 1, memo) + fibMemo(n - 2, memo);
        return memo[n];
    }
}
