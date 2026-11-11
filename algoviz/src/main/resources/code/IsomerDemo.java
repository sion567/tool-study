public class IsomerDemo {
    public static void main(String[] args) {
        int[][] linear = {{0, 1}, {1, 2}, {2, 3}};   // 正丁烷：C1-C2-C3-C4 链
        int[][] star = {{0, 1}, {0, 2}, {0, 3}};     // 异丁烷：C1 中心连三支
        report("正丁烷", linear);                     // 幕一：链状
        report("异丁烷", star);                       // 幕二：星形
    }
    // 同分异构 = 分子式相同 + 碳骨架不同构：度序列不同必非同构 → 不同物质（沸点差 11.2°C）

    static void report(String name, int[][] edges) {
        int[] deg = new int[4];                      // 度 = 这个碳连着几个碳
        for (int[] e : edges) { deg[e[0]]++; deg[e[1]]++; }
        int[] sorted = deg.clone();
        java.util.Arrays.sort(sorted);
        System.out.println(name + " 度序列=" + java.util.Arrays.toString(sorted)
                + "（分子式都是 C4H10，沸点却差 11.2°C）");
        // 支链多 → 分子圆 → 范德华接触面小 → 沸点低：结构决定物理
    }
}
