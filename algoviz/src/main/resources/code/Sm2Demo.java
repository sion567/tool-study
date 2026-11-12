public class Sm2Demo {
    public static void main(String[] args) {
        int p = 17, a = 2, b = 2;   //@DATA 教学曲线参数
        // 曲线 y² = x³+ax+b (mod p)；点加 = 弦线（倍点 = 切线）交第三点后取反
        int[] g = {6, 3};           // 生成元：9 = 216+12+2 mod 17 ✓
        int[] acc = g.clone();
        for (int k = 2; k <= 8; k++) {
            acc = add(acc, g, p, a);
            System.out.printf("%dG = (%d,%d)%n", k, acc[0], acc[1]);
        }
        System.out.println("18 个仿射点 + 无穷远点 O = 19 阶循环群——SM2 是同一数学的 256 位工业版");
    }

    static int[] add(int[] p1, int[] p2, int p, int a) {
        if (p1[0] < 0) return p2;
        if (p2[0] < 0) return p1;
        if (p1[0] == p2[0] && p1[1] == p2[1]) {          // 倍点：切线斜率 λ = (3x₁²+a)/(2y₁)
            int den = ((2*p1[1]) % p + p) % p;
            if (den == 0) return new int[]{-1, -1};
            int lam = (3*p1[0]*p1[0] + a) % p * inv(den, p) % p;
            int x3 = ((lam*lam - 2*p1[0]) % p + p) % p;
            return new int[]{x3, ((lam*(p1[0]-x3) - p1[1]) % p + p) % p};
        }
        if (p1[0] == p2[0]) return new int[]{-1, -1};    // 同 x 不同 y → 互逆 → 和为 O
        int lam = ((p2[1]-p1[1]) % p + p) % p * inv(((p2[0]-p1[0]) % p + p) % p, p) % p;
        int x3 = ((lam*lam - p1[0] - p2[0]) % p + p) % p;
        return new int[]{x3, ((lam*(p1[0]-x3) - p1[1]) % p + p) % p};
    }

    static int inv(int a, int p) {                       // 模逆元 = 模世界里的"除法"（扩展欧几里得）
        a = ((a % p) + p) % p;
        int r0 = a, r1 = p, s0 = 1, s1 = 0;
        while (r1 != 0) {
            int q = r0 / r1;
            int tr = r0 - q*r1, ts = s0 - q*s1;
            r0 = r1; r1 = tr; s0 = s1; s1 = ts;
        }
        return ((s0 % p) + p) % p;
    }
}
