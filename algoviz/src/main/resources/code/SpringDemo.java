public class SpringDemo {
    public static void main(String[] args) {
        double m = 1.0, k = 1.0, dt = 0.1;   //@DATA
        double x = 1.0, v = 0.0;             // 拉到 A=1 静止释放
        double E0 = 0.5 * k * x * x;         // 初始能量：全是势能
        for (int i = 0; i < 189; i++) {      // 3 个周期
            double a = -k / m * x;           // 胡克定律 F = -kx
            x = x + v * dt;                  // 显式欧拉：用旧速度挪位置（偷能量的一步）
            v = v + a * dt;
            if (i % 8 == 0) {
                double E = 0.5 * k * x * x + 0.5 * m * v * v;
                System.out.printf("t=%.2f x_Euler=%+.4f x_exact=%+.4f E/E0=%.4f%n",
                        i * dt, x, Math.cos(i * dt), E / E0);
            }
        }
        // 精确解 cos(t) 能量永守恒；显式欧拉每步偷 ~½(ω·dt)² 能量 → 振幅越摆越大；换辛积分可救
    }
}
