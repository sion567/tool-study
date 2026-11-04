public class KaratsubaDemo {
    public static void main(String[] args) {
        long x = 1234, y = 5678;   //@DATA
        System.out.println(karatsuba(x, y));   // 7006652
    }

    static long karatsuba(long x, long y) {
        if (x < 10 || y < 10) return x * y;   // 基线：个位数直接乘
        int m = String.valueOf(Math.max(x, y)).length() / 2;
        long p10m = (long) Math.pow(10, m);
        long a = x / p10m, b = x % p10m;      // x = a·10^m + b
        long c = y / p10m, d = y % p10m;      // y = c·10^m + d
        long p1 = karatsuba(a, c);            // 高位×高位
        long p2 = karatsuba(b, d);            // 低位×低位
        long p3 = karatsuba(a + b, c + d);    // 交叉和整体乘——三次就够
        long mid = p3 - p1 - p2;              // 恒等式：mid = ad + bc
        return p1 * p10m * p10m + mid * p10m + p2;   // 位权拼回
    }
}
