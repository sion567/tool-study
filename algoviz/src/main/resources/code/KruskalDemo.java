public class KruskalDemo {
    public static void main(String[] args) {
        int[][] edges = {{0,1,4},{0,2,2},{1,2,1},{1,3,5},{2,3,8},{2,4,10},{3,4,2},{3,5,6},{4,5,3}};
        int n = 6;
        int[] parent = new int[n];
        for (int i = 0; i < n; i++) parent[i] = i;
        // 按权重排序
        var sorted = new java.util.ArrayList<int[]>();
        for (int[] e : edges) sorted.add(e);
        sorted.sort((a, b) -> a[2] - b[2]);
        // 逐边尝试：并查集判环
        var mst = new java.util.ArrayList<int[]>();
        for (int[] e : sorted) {
            int ru = find(parent, e[0]), rv = find(parent, e[1]);
            if (ru != rv) { parent[rv] = ru; mst.add(e); }
        }
        for (int[] e : mst) System.out.println(e[0] + "-" + e[1] + " w=" + e[2]);
    }
    static int find(int[] parent, int x) {
        while (parent[x] != x) x = parent[x];
        return x;
    }
}
