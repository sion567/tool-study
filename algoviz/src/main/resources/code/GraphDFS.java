public class GraphDFS {
    static void dfs(Graph g, int src) {
        Stack<Integer> stack = new Stack<>();
        stack.push(src);                          // 起点入栈
        while (!stack.isEmpty()) {
            int u = stack.pop();                  // 弹栈 → 访问
            if (visited[u]) continue;             // 栈里可能有重复，跳过
            visited[u] = true;
            System.out.println("访问 " + u);
            for (int v : reverse(g.neighbors(u))) {  // 逆序压栈 → 先访问编号小的
                if (!visited[v]) stack.push(v);
            }
        }
    }

    // 复制说明：Graph / visited 是伪代码骨架——换成你的邻接表与 boolean 数组后即可运行，
    // 核心循环逻辑无需改动。
    public static void main(String[] args) {
        // bfs(graph, 0);   ← 补全 Graph 后取消注释
    }
