public class BucketDemo {
    public static void main(String[] args) {
        // 范围 [0,1024)，4 桶 × 256 位：单桶 32 字节 vs 全量位图 128 字节
        int[] data = {700, 300, 50, 999, 700, 100, 512, 260};   //@DATA
        boolean[][] bits = new boolean[4][256];                 // 4 桶位图

        // ① 第一遍：分桶路由（桶号 = n >> 8，桶内位 = n & 255）
        int[] counts = new int[4];
        for (int n : data) counts[n >> 8]++;

        // ② 第二遍：逐桶位图判重
        for (int n : data) {
            int b = n >> 8, bit = n & 255;
            if (bits[b][bit]) {
                System.out.println(n + " 重复！（桶" + b + " 位" + bit + " 已置 1）");
            } else {
                bits[b][bit] = true;   // 第一次出现 → 置位记账
            }
        }
    }
}
