public class HeatDemo {
    public static void main(String[] args) {
        double[][] t = new double[8][8];
        for (int r = 3; r <= 4; r++)
            for (int c = 3; c <= 4; c++) t[r][c] = 100;   // 中心 2×2 热块 100°C
        double C = 0.2;   //@DATA   // 扩散系数（稳定要求 ≤ 0.25）
        for (int frame = 1; frame <= 25; frame++) {
            double[][] nt = new double[8][8];
            for (int r = 0; r < 8; r++)
                for (int c = 0; c < 8; c++) {
                    double up = t[r == 0 ? r : r - 1][c], dn = t[r == 7 ? r : r + 1][c];
                    double lf = t[r][c == 0 ? c : c - 1], rt = t[r][c == 7 ? c : c + 1];
                    nt[r][c] = t[r][c] + C * (up + dn + lf + rt - 4 * t[r][c]);   // 绝热 = 镜像邻居
                }   // 傅里叶差分：热流 ∝ 四邻均值差——每轮把温差往平里抹
            t = nt;
            if (frame % 5 == 0) printGrid(t, frame);
        }
        System.out.println("终态：全场 ≈ 6.25°C（400 单位热量摊平到 64 格，分毫不差）");
    }
    // 同一套方程：咖啡变凉、CPU 散热、污染物弥散——∇²T 是热的 universal 搬运工

    static void printGrid(double[][] t, int frame) {
        System.out.println("== 第 " + frame + " 帧 ==");
        for (double[] row : t) {
            for (double v : row) System.out.printf("%6.1f", v);
            System.out.println();
        }
    }
}
