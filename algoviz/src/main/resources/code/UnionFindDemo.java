public class UnionFindDemo {
    static int[] parent = new int[6];
    public static void main(String[] args) {
        for (int i = 0; i < 6; i++) parent[i] = i;   // 初始：各自为根
        union(0, 1);   // A-B
        union(2, 3);   // C-D
        union(1, 3);   // 合并 {A,B} 和 {C,D}
        union(4, 5);   // E-F
        find(1);       // 路径压缩
        union(0, 4);   // 合并最后两个集合
    }
    static int find(int x) {
        if (parent[x] == x) return x;
        parent[x] = find(parent[x]);   // 路径压缩：直接指向根
        return parent[x];
    }
    static void union(int a, int b) {
        int ra = find(a), rb = find(b);
        if (ra != rb) parent[rb] = ra;
    }
}
