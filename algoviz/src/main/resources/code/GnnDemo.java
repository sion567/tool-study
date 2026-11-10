import java.util.*;

public class GnnDemo {
    public static void main(String[] args) {
        double[] x0 = {1, 0, 2, 0, 1};   //@DATA
        int[][] edges = {{0,1},{1,2},{2,3},{3,4},{4,0},{0,2}};   // 环 + 弦 A-C
        double[] h = x0.clone();
        for (int round = 1; round <= 2; round++) {
            double[] next = new double[5];
            for (int v = 0; v < 5; v++) {
                double sum = h[v];
                int cnt = 1;
                for (int[] e : edges) {
                    int nb = e[0] == v ? e[1] : e[1] == v ? e[0] : -1;
                    if (nb >= 0) { sum += h[nb]; cnt++; }
                }
                next[v] = sum / cnt;   // 消息传递：自己 + 邻居取平均
            }
            h = next;
            System.out.println("轮 " + round + "：" + Arrays.toString(h));
        }
        // 两轮后每节点知道两跳邻域；GCN 公式 H' = Â·H·W
    }
}
