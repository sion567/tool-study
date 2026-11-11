public class CrtDemo {
    public static void main(String[] args) {
        int a = 2, m = 3, b = 3, n = 5;   //@DATA 余数与互质模数
        // ---- 第一幕：扩展欧几里得 exGCD(m, n) ----
        int oldR = m, r = n, oldS = 1, s = 0, oldT = 0, t = 1;   // 目标 m·s + n·t = gcd
        while (r != 0) {
            int q = oldR / r;   // 商
            int tr = oldR - q * r, ts = oldS - q * s, tt = oldT - q * t;   // 三行同步滚
            oldR = r; r = tr; oldS = s; s = ts; oldT = t; t = tt;
        }
        System.out.println("gcd=" + oldR + "，s=" + oldS + "：" + m + "·" + oldS + "+" + n + "·" + oldT + "=" + oldR);
        int inverse = ((oldS % n) + n) % n;   // gcd=1 → s 就是 m 的逆元：「除法」在模世界存在
        // ---- 第二幕：CRT 合并 x≡a (mod m) 与 x≡b (mod n) ----
        int k = (((b - a) % n) + n) * inverse % n;   // x = a + mk ≡ b (mod n) → k = (b-a)·m⁻¹
        int x = a + m * k;
        System.out.println("k=" + k + " → x=" + x);
        System.out.println("验算：x mod " + m + "=" + (x % m) + "，x mod " + n + "=" + (x % n) + "——双同余成立");
    }
}
// CRT：互质模数下解唯一（mod mn）——《孙子算经》"物不知数"比欧洲早一千多年，RSA 解密全靠它
