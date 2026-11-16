public class GraphBFS {
    // 邻接表：第 i 行 = 节点 i 的邻居列表
    static java.util.List<java.util.List<Integer>> graph = java.util.List.of(
            java.util.List.of(1, 2),      // 0 → 1,2
            java.util.List.of(3),         // 1 → 3
            java.util.List.of(3),         // 2 → 3
            java.util.List.of(4),         // 3 → 4
            java.util.List.of());         // 4 → （终点）
    static boolean[] visited = new boolean[graph.size()];

    static void bfs(int src) {
        java.util.ArrayDeque<Integer> q = new java.util.ArrayDeque<>();
        visited[src] = true; q.offer(src);       // 起点入队
        while (!q.isEmpty()) {
            int u = q.poll();                     // 出队 → 访问
            System.out.println("访问 " + u);
            for (int v : graph.get(u)) {
                if (!visited[v]) {                // 没访问过才入队
                    visited[v] = true;
                    q.offer(v);
                }
            }
        }
    }

    public static void main(String[] args) {
        bfs(0);   // 输出 0 1 2 3 4——队列的层序扩散
    }
}
