public class Rotation2DDemo {
    public static void main(String[] args) {
        double stepDeg = 30;   //@DATA
        double[][] v = {{2, 0}, {-1, 1}, {-1, -1}};   // 三角形顶点
        double rad = Math.toRadians(stepDeg);
        for (int round = 1; round <= 6; round++) {
            for (int i = 0; i < 3; i++) {
                double x = v[i][0], y = v[i][1];
                v[i][0] = x * Math.cos(rad) - y * Math.sin(rad);   // 旋转矩阵
                v[i][1] = x * Math.sin(rad) + y * Math.cos(rad);
            }
            System.out.printf("R(%.0f°)·A = (%.3f, %.3f)%n", stepDeg * round, v[0][0], v[0][1]);
        }
        // 长度/面积全程不变——正交矩阵只动位置不动几何
    }
}
