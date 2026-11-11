public class ProjectileDemo {
    public static void main(String[] args) {
        double v0 = 25, theta = Math.toRadians(45);   //@DATA
        double g = 9.8;
        double vx = v0 * Math.cos(theta), vy = v0 * Math.sin(theta);   // 分解：水平匀速 + 竖直上抛
        double T = 2 * vy / g;                        // 飞行时间只由竖直分量决定
        double range = vx * T;                        // 射程 = v²·sin(2θ)/g
        System.out.printf("R=%.2fm  H=%.2fm  T=%.2fs%n", range, vy * vy / (2 * g), T);
        for (int i = 0; i <= 24; i++) {
            double t = T * i / 24;
            double x = vx * t, y = vy * t - 0.5 * g * t * t;   // y 是抛物线
            System.out.printf("t=%.2fs (%6.2f,%6.2f) vy=%+6.2f%n", t, x, y, vy - g * t);
        }
        // 30° 与 60° 射程相同（sin2θ 关于 45° 对称）——45° 独占最大射程
    }
}
