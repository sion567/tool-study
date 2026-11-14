public class SpiralDemo {
    public static void main(String[] args) {
        double aArch = 0.13, bLog = 0.22;   //@DATA 螺线参数
        double phi = (1 + Math.sqrt(5)) / 2;
        for (int i = 0; i <= 150; i++) {     // 三条螺线同屏对照
            double th = 6 * Math.PI * i / 150;
            System.out.printf("θ=%.1f 阿基米德=%.2f 对数=%.2f 黄金=%.2f%n",
                    th, aArch * th, Math.exp(bLog * th), Math.pow(phi, 2 * th / Math.PI));
        }
        // 阿基米德等差（每圈 +0.94）vs 对数/黄金等比（每圈 ×3.5 / ×φ）——生长方式决定性格
    }
}
