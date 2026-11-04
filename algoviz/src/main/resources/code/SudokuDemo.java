public class SudokuDemo {
    // 0 = 空；行、列、3×3 宫都不能重复
    static int[][] board = {          //@DATA
            {5, 3, 4, 6, 7, 0, 9, 1, 2},
            {6, 0, 0, 1, 9, 5, 3, 0, 0},
            {0, 9, 8, 0, 4, 0, 0, 6, 0},
            {8, 0, 0, 0, 6, 1, 0, 0, 3},
            {4, 0, 0, 8, 5, 3, 0, 0, 1},
            {7, 0, 0, 0, 2, 4, 0, 0, 6},
            {0, 6, 0, 0, 3, 0, 2, 8, 0},
            {0, 0, 0, 4, 1, 9, 6, 0, 5},
            {0, 0, 0, 0, 8, 0, 0, 7, 9}
    };

    public static void main(String[] args) {
        solve();
        for (int[] row : board) System.out.println(java.util.Arrays.toString(row));
    }

    static boolean solve() {
        int[] cell = nextEmpty();            // 找第一个空格
        if (cell == null) return true;       // 没空格 = 解出
        int r = cell[0], c = cell[1];
        for (int v = 1; v <= 9; v++) {
            if (conflict(r, c, v)) continue; // 约束剪枝
            board[r][c] = v;                 // 选择
            if (solve()) return true;        // 递归下探
            board[r][c] = 0;                 // 撤销（回溯）
        }
        return false;                        // 1~9 全不行 → 上层换选择
    }

    static int[] nextEmpty() {
        for (int r = 0; r < 9; r++)
            for (int c = 0; c < 9; c++)
                if (board[r][c] == 0) return new int[]{r, c};
        return null;
    }

    static boolean conflict(int r, int c, int v) {
        for (int i = 0; i < 9; i++)
            if (board[r][i] == v || board[i][c] == v) return true;  // 行/列
        int br = r / 3 * 3, bc = c / 3 * 3;
        for (int i = br; i < br + 3; i++)
            for (int j = bc; j < bc + 3; j++)
                if (board[i][j] == v) return true;                   // 3×3 宫
        return false;
    }
}
