public class CardioidDemo {
    public static void main(String[] args) {
        double a = 1.0;   //@DATA
        // 极坐标 → 直角坐标：r = a(1−cosθ) 一行，画出来一个心
        for (int i = 0; i <= 48; i++) {
            double t = 2 * Math.PI * i / 48;
            double r = a * (1 - Math.cos(t));
            double x = r * Math.cos(t);   // 极 → 直角
            double y = r * Math.sin(t);
            System.out.printf("t=%5.1f° r=%.2f  →  (%6.3f, %6.3f)%n",
                    Math.toDegrees(t), r, x, y);
        }
        // 连点成线 = 心形线；尖点在原点（t=0 时 r=0）
    }
}
