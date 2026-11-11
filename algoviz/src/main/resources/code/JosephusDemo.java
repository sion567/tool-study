public class JosephusDemo {
    public static void main(String[] args) {
        int n = 12, k = 3;   //@DATA 人数与报数
        boolean[] alive = new boolean[n];           // n 人围圈
        java.util.Arrays.fill(alive, true);
        int cursor = n - 1;                          // 上一个报数者（下一位从 1 号数起）
        for (int remaining = n; remaining > 1; remaining--) {
            int p = cursor;
            for (int c = 0; c < k; ) {               // 每轮报数 k 个活着的人
                p = (p + 1) % n;                     // 圈没有起点：数到尾绕回头
                if (alive[p]) c++;
            }
            alive[p] = false;                        // 第 k 个：出列
            System.out.printf("%d 号出列（剩 %d 人）%n", p + 1, remaining - 1);
        }
        // 第二幕：递推公式 f(1)=0，f(j) = (f(j-1)+k) mod j
        int f = 0;                                   // f(1) = 0：1 人圈幸存者位置 0
        for (int j = 2; j <= n; j++) {
            f = (f + k) % j;                         // 小圈答案滚成大圈答案
        }
        int survivor = 1;
        for (int i = 0; i < n; i++) if (alive[i]) survivor = i + 1;
        System.out.println("模拟幸存者 " + survivor + " 号；公式 f+1 = " + (f + 1) + " 号位——两种解法握手");
    }
}
// 递推 O(n) vs 模拟 O(n²)：约瑟夫斯当场算出了自己该站的位置——会递推的人多活一次
