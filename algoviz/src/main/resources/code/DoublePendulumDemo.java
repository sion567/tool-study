public class DoublePendulumDemo {
    public static void main(String[] args) {
        double th1 = 2.0, th2 = 0.0, w1 = 0, w2 = 0;   //@DATA
        double l = 1, dt = 0.002, ghost = 0.001;       // 幽灵：θ₁ 初值差 0.001 rad
        double[] a = {th1, w1, th2, w2}, b = {th1 + ghost, w1, th2, w2};
        for (int i = 0; i < 6000; i++) {               // RK4 走 12 秒
            a = rk4(a, dt); b = rk4(b, dt);            // 同一方程，两份命运
            if (i % 200 == 0) {
                double x1 = l * Math.sin(a[0]), y1 = -l * Math.cos(a[0]);
                double x2 = x1 + l * Math.sin(a[2]), y2 = y1 - l * Math.cos(a[2]);
                double gx1 = l * Math.sin(b[0]), gy1 = -l * Math.cos(b[0]);
                double gx2 = gx1 + l * Math.sin(b[2]), gy2 = gy1 - l * Math.cos(b[2]);
                double sep = Math.hypot(x2 - gx2, y2 - gy2);
                System.out.printf("t=%.1f  摆锤2(%+.3f,%+.3f)  幽灵分离=%.4f%n", i * dt, x2, y2, sep);
            }
        }   // 初值差 0.001，十几秒后完全分道扬镳——混沌 = 确定性 + 不可长期预测
    }

    static double[] rk4(double[] s, double h) {
        double[] k1 = accel(s);
        double[] k2 = accel(mix(s, k1, h / 2));
        double[] k3 = accel(mix(s, k2, h / 2));
        double[] k4 = accel(mix(s, k3, h));
        double[] o = new double[4];
        for (int i = 0; i < 4; i++) o[i] = s[i] + h / 6 * (k1[i] + 2 * k2[i] + 2 * k3[i] + k4[i]);
        return o;
    }

    static double[] mix(double[] s, double[] k, double f) {
        double[] r = new double[4];
        for (int i = 0; i < 4; i++) r[i] = s[i] + k[i] * f;
        return r;
    }

    static double[] accel(double[] s) {                // m1=m2=l1=l2=1 的标准双摆方程
        double th1 = s[0], w1 = s[1], th2 = s[2], w2 = s[3];
        double d = th1 - th2, den = 3 - Math.cos(2 * d);
        double a1 = (-9.8 * 3 * Math.sin(th1) - 9.8 * Math.sin(th1 - 2 * th2)
                - 2 * Math.sin(d) * (w2 * w2 + w1 * w1 * Math.cos(d))) / den;
        double a2 = 2 * Math.sin(d) * (2 * w1 * w1 + 2 * 9.8 * Math.cos(th1)
                + w2 * w2 * Math.cos(d)) / den;
        return new double[]{w1, a1, w2, a2};
    }
}
