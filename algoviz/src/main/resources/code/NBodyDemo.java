public class NBodyDemo {
    public static void main(String[] args) {
        double G = 1, M = 60, dt = 0.004;   //@DATA
        double[][] p = {{0.7, 0}, {0, 1.1}, {-1.5, 0}};      // 三行星
        double[][] v = {{0, 9.258}, {-7.385, 0}, {0, -6.325}};   // 圆轨道速度 √(GM/r)
        for (int i = 0; i < 400; i++) {
            for (int a = 0; a < 3; a++) {        // kick：引力改速度
                double dx = -p[a][0], dy = -p[a][1];
                double r2 = dx * dx + dy * dy + 0.01;   // 软化 ε² 防奇点
                double f = G * M / (r2 * Math.sqrt(r2));
                v[a][0] += f * dx * dt; v[a][1] += f * dy * dt;
            }
            for (int a = 0; a < 3; a++) {        // drift：新速度挪位置
                p[a][0] += v[a][0] * dt; p[a][1] += v[a][1] * dt;
            }
            if (i % 14 == 0) System.out.printf("t=%.2f P1(%.2f,%.2f) P2(%.2f,%.2f) P3(%.2f,%.2f)%n",
                    i * dt, p[0][0], p[0][1], p[1][0], p[1][1], p[2][0], p[2][1]);
        }   // 内圈转得快：T²∝r³（开普勒第三定律）；辛积分让能量漂移有界——轨道不会假坠落
    }
}
