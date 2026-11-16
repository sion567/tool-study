public class Dijkstra {
    // 邻接矩阵：graph[u][v] = 边权（-1 = 无边）
    static int[][] graph = {
            {-1, 2, 4, -1, -1},
            {2, -1, 1, 5, -1},
            {4, 1, -1, 3, -1},
            {-1, 5, 3, -1, 6},
            {-1, -1, -1, 6, -1}};
    static int N = 5;

    public static void main(String[] args) {
        int src = 0;
        int[] dist = new int[N];
        boolean[] settled = new boolean[N];
        java.util.Arrays.fill(dist, Integer.MAX_VALUE);
        dist[src] = 0;                      // 起点入圈，其余 ∞
        for (int round = 0; round < N; round++) {
            int u = -1;                     // 贪心挑圆心：未定点中 dist 最小
            for (int i = 0; i < N; i++) {
                if (!settled[i] && (u == -1 || dist[i] < dist[u])) u = i;
            }
            if (u == -1 || dist[u] == Integer.MAX_VALUE) break;
            settled[u] = true;              // 出圈：dist[u] 定死
            for (int v = 0; v < N; v++) {
                if (graph[u][v] > 0 && dist[u] != Integer.MAX_VALUE
                        && dist[u] + graph[u][v] < dist[v]) {
                    dist[v] = dist[u] + graph[u][v];  // 松弛：涟漪推边界
                }
            }
        }
        System.out.println(java.util.Arrays.toString(dist));
    }
}
