public class NQueensDemo {
    static int[] queens = new int[4];
    public static void main(String[] args) {
        solve(0);
    }
    static void solve(int row) {
        if (row == 4) {
            System.out.println("解: " + java.util.Arrays.toString(queens));
            return;
        }
        for (int col = 0; col < 4; col++) {
            if (isSafe(row, col)) {
                queens[row] = col;
                solve(row + 1);
                queens[row] = -1;   // 回溯
            }
        }
    }
    static boolean isSafe(int row, int col) {
        for (int prev = 0; prev < row; prev++) {
            int pc = queens[prev];
            if (pc == -1) continue;
            if (pc == col) return false;
            if (Math.abs(prev - row) == Math.abs(pc - col)) return false;
        }
        return true;
    }
}
