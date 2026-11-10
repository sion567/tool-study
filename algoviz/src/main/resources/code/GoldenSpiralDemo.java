public class GoldenSpiralDemo {
    public static void main(String[] args) {
        double phi = (1 + Math.sqrt(5)) / 2;   // 1.6180339887   //@DATA
        long prev = 1, curr = 1;
        for (int n = 1; n <= 8; n++) {
            long next = prev + curr;
            System.out.printf("F(%d)/F(%d) = %d/%d ≈ %.4f%n", n + 1, n, next, curr, (double) next / curr);
            prev = curr; curr = next;
        }
        // 对数螺线：r = phi^(2θ/π)——每 1/4 圈半径 × φ
        for (int i = 0; i <= 96; i++) {
            double t = 2 * Math.PI * i / 96;
            double r = Math.pow(phi, 2 * t / Math.PI);
            System.out.printf("(%.3f, %.3f)%n", r * Math.cos(t), r * Math.sin(t));
        }
    }
}
