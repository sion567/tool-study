public class KMeansDemo {
    public static void main(String[] args) {
        int k = 3;   //@DATA 聚类数
        double[][] pts = {{1, 1.5}, {1.3, 1.1}, {0.8, 1.8}, {1.1, 1.3},
                {4, 4.2}, {4.3, 3.8}, {3.8, 4.5}, {1.2, 4}, {0.9, 4.3}, {1.5, 4.5}, {4.1, 1}, {4.4, 1.3}};
        double[][] c = {{1, 4}, {4, 1}, {4, 4}};   // 初始质心
        for (int it = 1; it <= 4; it++) {           // 分配（就近归属）→ 更新（质心 = 簇均值）交替
            double maxMove = 0;
            double[][] sums = new double[k][3];
            for (double[] p : pts) {
                int best = 0;
                double bestD = Double.MAX_VALUE;
                for (int i = 0; i < k; i++) {
                    double d = (p[0]-c[i][0])*(p[0]-c[i][0]) + (p[1]-c[i][1])*(p[1]-c[i][1]);
                    if (d < bestD) { bestD = d; best = i; }
                }
                sums[best][0] += p[0]; sums[best][1] += p[1]; sums[best][2]++;
            }
            for (int i = 0; i < k; i++) {
                if (sums[i][2] == 0) continue;
                double nx = sums[i][0]/sums[i][2], ny = sums[i][1]/sums[i][2];
                maxMove = Math.max(maxMove, Math.hypot(nx-c[i][0], ny-c[i][1]));
                c[i][0] = nx; c[i][1] = ny;
            }
            System.out.printf("轮%d 质心最大移动 %.3f%n", it, maxMove);
        }
        // 收敛：质心移动距离递减——目标函数（簇内平方和）单调下降
        // K-Means 只保证局部最优：初始质心决定结局——k-means++ 让初始质心互相远离
    }
}
