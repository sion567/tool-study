public class Dijkstra {
    static int[] dijkstra(Graph g, int src) {
        dist[src] = 0;                      // 起点入圈，其余 ∞
        while (还有未定的点) {
            u = 未定点中 dist 最小的;        // 贪心挑圆心
            settled[u] = true;              // 出圈：dist[u] 定死
            for (v : u 的邻居) {
                if (dist[u] + w < dist[v]) {
                    dist[v] = dist[u] + w;  // 松弛：涟漪推边界
                }
            }
        }
        return dist;
    }
}
