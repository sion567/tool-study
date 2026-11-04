public class CordicDemo {
    public static void main(String[] args) {
        // 旋转模式：只用加法和移位逼近 cos/sin
        double target = Math.toRadians(37);   //@DATA
        double x = 1, y = 0, z = target;      // 向量(1,0)，残差角=目标角
        double k = 1.0;                       // 校正因子累积
        for (int i = 0; i < 9; i++) {
            double d = z >= 0 ? 1 : -1;       // 残差符号定方向
            double pow = Math.pow(2, -i);     // 2^-i —— 硬件里就是移位
            double nx = x - d * y * pow;      // 伪旋转（省掉 cos 因子）
            double ny = y + d * x * pow;
            x = nx; y = ny;
            z -= d * Math.atan(pow);          // 查表：atan(2^-i)
            k *= Math.sqrt(1 + pow * pow);
        }
        x /= k; y /= k;                       // 收尾统一归一
        System.out.printf("cos(37°)=%.6f  sin(37°)=%.6f%n", x, y);
    }
}
