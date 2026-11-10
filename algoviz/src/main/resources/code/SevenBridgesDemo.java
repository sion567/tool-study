import java.util.*;

public class SevenBridgesDemo {
    public static void main(String[] args) {
        // 4 块陆地 7 座桥：度数 3,3,5,3 → 四个奇点 → 欧拉路径不存在
        int[][] bridges = {{0,2},{0,2},{1,2},{1,2},{0,3},{1,3},{2,3}};   //@DATA
        System.out.println("奇顶点数 = " + oddCount(bridges) + " → " +
                (oddCount(bridges) <= 2 ? "有欧拉路径" : "不可能（欧拉，1736）"));

        // 拆掉一座桥后：度数 2,3,5,2 → 两个奇点 → 走 Hierholzer
        int[][] after = {{0,2},{0,2},{1,2},{1,2},{1,3},{2,3}};
        System.out.println("拆桥后奇顶点数 = " + oddCount(after) + " → 可以一笔画");
    }

    static int oddCount(int[][] edges) {
        Map<Integer, Integer> deg = new TreeMap<>();
        for (int[] e : edges) {
            deg.merge(e[0], 1, Integer::sum);
            deg.merge(e[1], 1, Integer::sum);
        }
        return (int) deg.values().stream().filter(d -> d % 2 == 1).count();
    }
}
