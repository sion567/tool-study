public class GraphBFS {
    static void bfs(Graph g, int src) {
        Queue<Integer> q = new ArrayDeque<>();
        visited[src] = true; q.offer(src);       // 起点入队
        while (!q.isEmpty()) {
            int u = q.poll();                     // 出队 → 访问
            System.out.println("访问 " + u);
            for (int v : g.neighbors(u)) {
                if (!visited[v]) {                // 没访问过才入队
                    visited[v] = true;
                    q.offer(v);
                }
            }
        }
    }
}
