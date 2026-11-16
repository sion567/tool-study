public class GraphDFS {
    // 邻接表：第 i 行 = 节点 i 的邻居列表
    static java.util.List<java.util.List<Integer>> graph = java.util.List.of(
            java.util.List.of(1, 2),      // 0 → 1,2
            java.util.List.of(3),         // 1 → 3
            java.util.List.of(3),         // 2 → 3
            java.util.List.of(4),         // 3 → 4
            java.util.List.of());         // 4 → （终点）
    static boolean[] visited = new boolean[graph.size()];

    static void dfs(int src) {
        java.util.ArrayDeque<Integer> stack = new java.util.ArrayDeque<>();
        stack.push(src);                          // 起点入栈
        while (!stack.isEmpty()) {
            int u = stack.pop();                  // 弹栈 → 访问
            if (visited[u]) continue;             // 栈里可能有重复，跳过
            visited[u] = true;
            System.out.println("访问 " + u);
            for (int v : reverse(graph.get(u))) {  // 逆序压栈 → 先访问编号小的
                if (!visited[v]) stack.push(v);
            }
        }
    }

    static java.util.List<Integer> reverse(java.util.List<Integer> l) {
        java.util.List<Integer> r = new java.util.ArrayList<>(l);
        java.util.Collections.reverse(r);
        return r;
    }

    public static void main(String[] args) {
        dfs(0);   // 输出 0 1 3 4 2——栈的一条道走到黑
    }
}
