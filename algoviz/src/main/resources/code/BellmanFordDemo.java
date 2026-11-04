public class BellmanFordDemo {
    public static void main(String[] args) {
        int V = 5;
        int[][] edges = {{0,1,6},{0,2,7},{1,2,8},{1,3,5},{1,3,-4},{2,3,-3},{2,4,9},{3,4,7}};
        int[] dist = new int[V];
        Arrays.fill(dist, Integer.MAX_VALUE);
        dist[0] = 0;
        // V-1 轮逐边松弛
        for (int round = 1; round < V; round++) {
            for (int[] e : edges) {
                if (dist[e[0]] != Integer.MAX_VALUE && dist[e[0]] + e[2] < dist[e[1]]) {
                    dist[e[1]] = dist[e[0]] + e[2];
                }
            }
        }
        // 负环检测
        for (int[] e : edges) {
            if (dist[e[0]] != Integer.MAX_VALUE && dist[e[0]] + e[2] < dist[e[1]]) {
                System.out.println("负环！");
                return;
            }
        }
        System.out.println(java.util.Arrays.toString(dist));
    }
}
