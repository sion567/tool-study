public class MathLogDemo {
    public static void main(String[] args) {
        int limit = 1000;                               //@DATA 猜数字上限 N（变体换 100/1000000）
        // 幕一：猜数字——二分砍半，最坏 ⌈log₂N⌉ 次必中（假设目标总在中点以上）
        int low = 1, high = limit, guesses = 0;
        while (low < high) {                            // 区间只剩 1 个数时结束
            int mid = (low + high) / 2;                 // 每次猜中点——砍掉一半候选
            low = mid + 1;                              // 最坏情况：目标总在另一半
            guesses++;
            System.out.println("第 " + guesses + " 次猜 " + mid + "，区间剩 " + (high - low + 1));
        }
        System.out.println(guesses + " 次必中——2^" + guesses + " ≥ " + limit + "，这就是 O(log n) 的体感");

        // 幕二：乘变加——计算尺原理（刻度按 log 摆，查表加法替代乘法）
        double lg2 = 0.30103, lg3 = 0.47712;            // lg 表：计算尺的刻度就是这张表
        System.out.printf("lg2 + lg3 = %.5f = lg6（2×3=6）✓%n", lg2 + lg3);

        // 幕三：换底公式——log_b a = lg a / lg b
        System.out.printf("log₂%d = lg%d/lg2 = %.0f/%.5f ≈ %.2f%n",
                limit, limit, Math.log10(limit), lg2, Math.log10(limit) / lg2);

        // 幕四：复杂度阶梯——n 翻倍时 O(log n) 只加 1 步
        int logn = guessCount(limit);
        System.out.println("n=" + limit + "：O(1)=1，O(log n)=" + logn + "，O(n)=" + limit
                + "，O(n·log n)=" + ((long) limit * logn) + "，O(n²)=" + ((long) limit * limit));
    }

    static int guessCount(int n) {                      // 砍半到 1 的步数 = ⌈log₂n⌉
        int h = n, cnt = 0;
        while (h > 1) {
            h = (h + 1) / 2;
            cnt++;
        }
        return cnt;
    }
}
