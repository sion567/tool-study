public class PendulumDemo {
    public static void main(String[] args) {
        double L = 1.0, theta0 = Math.toRadians(30);   //@DATA
        double omega = Math.sqrt(9.8 / L);             // 小角度线性化后的角频率
        double T = 2 * Math.PI / omega;
        System.out.printf("周期 T = %.3fs（与质量、幅度几乎无关）%n", T);
        for (int i = 0; i <= 24; i++) {
            double t = 2 * T * i / 24;
            double theta = theta0 * Math.cos(omega * t);   // 简谐运动
            double bx = L * Math.sin(theta);
            double by = -L * Math.cos(theta);
            System.out.printf("t=%.2fs θ=%6.1f°  摆锤(%+.3f, %.3f)%n",
                    t, Math.toDegrees(theta), bx, by);
        }
        // 大角度（>15°）周期变长——sinθ≈θ 近似失效，需椭圆积分
    }
}
