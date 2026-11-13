public class BigIntDemo {
    public static void main(String[] args) {
        int a = 123456, b = 654321;   //@DATA 操作数
        int[] da = new int[6], db = new int[6];
        for (int i = 0; i < 6; i++) { da[i] = (a / pow(5-i)) % 10; db[i] = (b / pow(5-i)) % 10; }
        int carry = 0, sum = 0;
        for (int pos = 5; pos >= 0; pos--) {          // 加法：进位链从个位涌向高位
            int s = da[pos] + db[pos] + carry;
            carry = s / 10;
        }
        System.out.println("加法和 = " + (a + b) + "（末位进位 " + carry + "）");
        long prod = 0;
        for (int i = 5; i >= 0; i--) {                // 乘法：部分积错位累加（朴素 O(n²)）
            long partial = (long) b * da[i];
            prod += partial * pow(5 - i);
            System.out.printf("部分积：%d × %d = %d%n", b, da[i], partial);
        }
        System.out.println("乘积 = " + prod);
        // 对照 Karatsuba：分治把 O(n²) 降到 O(n^1.585)——同一问题的暴力与聪明
    }

    static int pow(int e) { return (int) Math.pow(10, e); }
}
