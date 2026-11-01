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

    // 复制说明：Graph / visited 是伪代码骨架——换成你的邻接表与 boolean 数组后即可运行，
    // 核心循环逻辑无需改动。
    public static void main(String[] args) {
        // bfs(graph, 0);   ← 补全 Graph 后取消注释
    }
