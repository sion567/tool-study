public class RsaMiniDemo {
    public static void main(String[] args) {
        int p = 3, q = 11, e = 3, d = 7, m = 4;   //@DATA
        int n = p * q;                            // 33（公开）
        System.out.println("公钥 (n,e)=(" + n + "," + e + ")  私钥 d=" + d);

        int c = modPow(m, e, n);                  // 加密：4³ mod 33
        System.out.println("密文 c = " + c);       // 31

        int back = modPow(c, d, n);               // 解密：31⁷ mod 33
        System.out.println("解出 m = " + back);    // 4
        // modPow 就是快速幂 + 每步取模——数字永不膨胀
    }

    static int modPow(int base, int exp, int mod) {
        long result = 1, b = base;
        while (exp > 0) {
            if ((exp & 1) == 1) result = result * b % mod;
            b = b * b % mod;
            exp >>= 1;
        }
        return (int) result;
    }
}
