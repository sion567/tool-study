public class BitmapDemo {
    public static void main(String[] args) {
        int bits = 8;   //@DATA
        int[] data = {3, 7, 2, 7, 5, 1};
        boolean[] seen = new boolean[bits];   // 一个 byte 搞定 0~7 的判重
        for (int v : data) {
            if (seen[v]) {
                System.out.println(v + " 重复！（bit[" + v + "] 已置 1）");
            } else {
                seen[v] = true;
                System.out.println(v + " → bit[" + v + "] ← 1");
            }
        }
        // 7 在第 4 次读取时命中——位图判重 O(1) 空间 O(1) 时间
    }
}
