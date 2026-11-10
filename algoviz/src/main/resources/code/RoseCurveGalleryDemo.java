public class RoseCurveGalleryDemo {
    public static void main(String[] args) {
        int[] ks = {2, 3, 5};   //@DATA
        for (int k : ks) {
            int petals = (k % 2 == 1) ? k : 2 * k;   // 奇 k=k 瓣；偶 k=2k 瓣
            System.out.println("r = cos(" + k + "θ) → " + petals + " 瓣玫瑰");
            for (int i = 0; i <= 120; i++) {
                double t = 2 * Math.PI * i / 120;
                double r = Math.cos(k * t);
                System.out.printf("  (%.3f, %.3f)%n", r * Math.cos(t), r * Math.sin(t));
            }
        }
    }
}
