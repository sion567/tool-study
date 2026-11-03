public class UnitCircleDemo {
    // 单位圆→正弦波：sin(θ) = 圆上动点的 y 坐标
    public static void main(String[] args) {
        for (int i = 0; i <= 48; i++) {
            double theta = i * Math.PI / 24;              // 0 → 2π
            double sin = Math.sin(theta);
            double cos = Math.cos(theta);
            System.out.printf("θ=%.2f  sin=%.3f  cos=%.3f%n", theta, sin, cos);
        }
    }
}
