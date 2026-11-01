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
}
