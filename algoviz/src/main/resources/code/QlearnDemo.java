public class QlearnDemo {
    public static void main(String[] args) {
        double lr = 0.5, gamma = 0.9;   //@DATA 学习率与折扣因子
        int w = 5, h = 4, goal = 19;
        java.util.Set<Integer> walls = java.util.Set.of(7, 11);
        double[][] q = new double[20][4];
        int pos = 0;
        int[] round = {0, 0, 0, 0, 1, 1, 1};   // 一轮：右×4 下×3 到 G
        for (int roundNo = 1; roundNo <= 12; roundNo++) {   // 奖励沿经验反向传播：每轮约 1 格
            for (int act : round) {
                int next = pos;
                switch (act) { case 0 -> next = Math.min(4, pos%5+1) + pos/5*5; case 1 -> next = Math.min(3, pos/5+1)*5 + pos%5; case 2 -> next = Math.max(0, pos%5-1) + pos/5*5; default -> next = Math.max(0, pos/5-1)*5 + pos%5; }
                boolean wall = walls.contains(next) || next == pos;
                if (wall) next = pos;
                double r = next == goal ? 1.0 : wall ? -1.0 : -0.04;
                double maxN = -Double.MAX_VALUE;
                for (int i = 0; i < 4; i++) maxN = Math.max(maxN, q[next][i]);
                if (next == goal) maxN = 0;
                q[pos][act] += lr * (r + gamma * maxN - q[pos][act]);   // TD 更新
                pos = next;
                if (next == goal) pos = 0;
            }
            System.out.printf("轮%d done%n", roundNo);
        }
        for (int r = 0; r < h; r++) {
            for (int c = 0; c < w; c++) {
                int idx = c + r * w;
                double m = -Double.MAX_VALUE;
                for (int i = 0; i < 4; i++) m = Math.max(m, q[idx][i]);
                System.out.print((walls.contains(idx) ? "#" : String.format("%.2f", m == -Double.MAX_VALUE ? 0 : m)) + " ");
            }
            System.out.println();
        }
        // 贪婪策略沿 max Q 走出最短路径——奖励已从 G 反向传播到 S
    }
}
