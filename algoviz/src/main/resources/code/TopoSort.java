public class TopoSort {
    public static void main(String[] args) {
        int[][] edges = {{0,1},{0,2},{1,3},{2,3},{1,4},{3,4},{3,5},{2,5}};
        int n = 6;
        int[] inDeg = new int[n];
        for (int[] e : edges) inDeg[e[1]]++;
        var q = new java.util.ArrayDeque<Integer>();
        for (int i = 0; i < n; i++) if (inDeg[i] == 0) q.offer(i);
        var result = new java.util.ArrayList<Integer>();
        while (!q.isEmpty()) {
            int u = q.poll();
            result.add(u);
            for (int[] e : edges) {
                if (e[0] == u && --inDeg[e[1]] == 0) q.offer(e[1]);
            }
        }
        System.out.println("拓扑序: " + result);   // [0, 1, 2, 3, 5, 4] 等合法序
    }
}
