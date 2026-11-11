public class EquilibriumDemo {
    public static void main(String[] args) {
        double kf = 0.5, kr = 0.25, pulse = 0.6;   //@DATA 速率与扰动
        double dt = 0.06;
        double a = 1, b = 1, c = 0;                // A + B ⇌ C
        boolean perturbed = false;
        for (int i = 0; i < 150; i++) {            // 9 秒
            double vf = kf * a * b, vr = kr * c;   // 正逆速率的拔河
            a += (vr - vf) * dt; b += (vr - vf) * dt; c += (vf - vr) * dt;
            double t = (i + 1) * dt;
            if (!perturbed && t >= 4.0) {          // ⚡ 扰动：瞬间注入产物
                c += pulse; perturbed = true;
                System.out.printf("t=%.2f 注入 %.1f C → Q=%.2f > K=%.1f，逆反应接管%n", t, pulse, c / (a * b), kf / kr);
            } else if (i % 8 == 0) {
                System.out.printf("t=%.2f vf=%.3f vr=%.3f Q=%.3f%n", t, vf, vr, c / (a * b));
            }
        }
        // K = kf/kr 只认温度；平衡 = v_f == v_r（速率握手），不是浓度对半——勒夏特列：向削弱扰动的方向移动
    }
}
