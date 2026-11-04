import java.util.*;

public class AStarDemo {
    static final int ROWS = 8, COLS = 12;
    static final int SR = 0, SC = 0, GR = 7, GC = 11;   // 起点/终点
    static boolean[][] wall = new boolean[ROWS][COLS];  //@DATA

    public static void main(String[] args) {
        // open 集：f = g + h 最小的先出（小根堆）
        PriorityQueue<int[]> open = new PriorityQueue<>(
                Comparator.comparingInt(a -> a[2]));    // {r, c, f}
        int[][] g = new int[ROWS][COLS];
        int[][] pr = new int[ROWS][COLS], pc = new int[ROWS][COLS];
        for (int[] row : g) Arrays.fill(row, Integer.MAX_VALUE);
        g[SR][SC] = 0;
        open.add(new int[]{SR, SC, h(SR, SC)});
        boolean[][] closed = new boolean[ROWS][COLS];

        while (!open.isEmpty()) {
            int[] cur = open.poll();                    // 弹出 f 最小
            int r = cur[0], c = cur[1];
            if (closed[r][c]) continue;
            closed[r][c] = true;                        // 弹出即最优
            if (r == GR && c == GC) break;
            for (int[] d : new int[][]{{-1,0},{1,0},{0,-1},{0,1}}) {
                int nr = r + d[0], nc = c + d[1];
                if (nr < 0 || nr >= ROWS || nc < 0 || nc >= COLS
                        || wall[nr][nc] || closed[nr][nc]) continue;
                int ng = g[r][c] + 1;                   // 邻格的新代价
                if (ng < g[nr][nc]) {                   // 更短才更新
                    g[nr][nc] = ng;
                    pr[nr][nc] = r; pc[nr][nc] = c;     // 父指针
                    open.add(new int[]{nr, nc, ng + h(nr, nc)});
                }
            }
        }
        // 沿父指针回溯路径
        List<int[]> path = new ArrayList<>();
        for (int r = GR, c = GC; r != SR || c != SC; ) {
            path.add(new int[]{r, c});
            int tr = pr[r][c]; c = pc[r][c]; r = tr;
        }
        path.add(new int[]{SR, SC});
        Collections.reverse(path);
        System.out.println("最短路径 " + (path.size() - 1) + " 步：" + path);
    }

    // h = 曼哈顿距离：不上高估（可采纳）→ 弹出即最优
    static int h(int r, int c) { return Math.abs(r - GR) + Math.abs(c - GC); }
}
