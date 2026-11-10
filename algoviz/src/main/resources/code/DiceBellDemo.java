public class DiceBellDemo {
    public static void main(String[] args) {
        int dice = 2;   //@DATA
        // 两枚骰子之和的组合数枚举：36 种
        int[] counts = new int[13];
        for (int a = 1; a <= 6; a++) {
            for (int b = 1; b <= 6; b++) {
                counts[a + b]++;
            }
        }
        for (int s = 2; s <= 12; s++) {
            System.out.printf("和=%2d  %2d/36 = %5.1f%%%n", s, counts[s], 100.0 * counts[s] / 36);
        }
        // 中间高两头低 = 卷积；三枚更像钟形 → 中心极限定理
    }
}
