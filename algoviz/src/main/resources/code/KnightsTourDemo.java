public class KnightsTourDemo {
    public static void main(String[] args) {
        int row = 0, col = 0;   //@DATA 起点
        int[][] board = new int[8][8];   // 0 = 未访
        int[][] moves = {{1,2},{2,1},{2,-1},{1,-2},{-1,-2},{-2,-1},{-2,1},{-1,2}};
        board[row][col] = 1;   // 第 1 步落子，接下来 63 步每格恰访一次
        for (int no = 2; no <= 64; no++) {
            int[] next = warnsdorff(board, row, col, moves);   // 走向出度最小的未访格
            if (next == null) { System.out.println("第 " + (no - 1) + " 步被困——启发式的失败现场"); return; }
            row = next[0]; col = next[1]; board[row][col] = no;
            System.out.printf("第 %d 步 → (%d,%d)%n", no, row, col);
        }   // 64 格全部踏遍，一步没回头——Warnsdorff 单步贪心的完胜
    }

    // 启发式 = 放弃「保证成功」换「几乎总是成功 + 快」：对照 N 皇后的回溯，这里没有撤销
    static int[] warnsdorff(int[][] board, int row, int col, int[][] moves) {
        int[] best = null;
        int bestDeg = 99;
        for (int[] m : moves) {
            int r = row + m[0], c = col + m[1];
            if (r < 0 || r > 7 || c < 0 || c > 7 || board[r][c] != 0) continue;
            int deg = 0;   // 该格的「后续出度」：还能往多少个未访格走
            for (int[] mm : moves) {
                int nr = r + mm[0], nc = c + mm[1];
                if (nr >= 0 && nr < 8 && nc >= 0 && nc < 8 && board[nr][nc] == 0) deg++;
            }
            if (deg < bestDeg) { bestDeg = deg; best = new int[]{r, c}; }   // 最小出度优先，并列取首个
        }
        return best;
    }
}
