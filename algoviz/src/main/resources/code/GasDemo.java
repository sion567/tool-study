public class GasDemo {
    public static void main(String[] args) {
        int n = 40, hits = 0;
        java.util.Random rnd = new java.util.Random(42);   //@DATA 种子：可复现的"随机"
        double[] x = new double[n], y = new double[n], vx = new double[n], vy = new double[n];
        for (int i = 0; i < n; i++) {
            x[i] = rnd.nextDouble() * 4; y[i] = rnd.nextDouble() * 3;
            double sp = 0.8 + rnd.nextDouble() * 1.6, th = rnd.nextDouble() * 2 * Math.PI;
            vx[i] = sp * Math.cos(th); vy[i] = sp * Math.sin(th);
        }
        for (int step = 0; step < 60; step++) {
            for (int i = 0; i < n; i++) {
                x[i] += vx[i] * 0.05; y[i] += vy[i] * 0.05;   // 匀速直线——理想气体没有"力"
                if (x[i] < 0 || x[i] > 4) { vx[i] = -vx[i]; x[i] = Math.max(0, Math.min(4, x[i])); hits++; }
                if (y[i] < 0 || y[i] > 3) { vy[i] = -vy[i]; y[i] = Math.max(0, Math.min(3, y[i])); hits++; }
            }   // 弹性碰壁：只翻法向速度，速率不变 → RMS 恒定 = 温度恒定
            if (step % 3 == 0) System.out.printf("step=%d 碰壁 %d 次%n", step, hits);
        }
        System.out.println("压强 ∝ 碰壁频率：" + hits + " 次撞出来的宏观量");
    }
}
