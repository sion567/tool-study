public class Sha256Demo {
    public static void main(String[] args) {
        String message = "abc";   //@DATA 消息（NIST 示例）
        byte[] msg = message.getBytes();
        long bitLen = msg.length * 8L;
        int padded = (msg.length + 9 + 63) / 64 * 64;
        byte[] gr = new byte[padded];
        System.arraycopy(msg, 0, gr, 0, msg.length);
        gr[msg.length] = (byte) 0x80;
        for (int i = 0; i < 8; i++) gr[padded - 1 - i] = (byte) (bitLen >>> (8 * i));
        int[] h = initialHash();                       // 前 8 个素数的平方根小数部分——常数程序生成
        int[] k = roundConstants();                    // 前 64 个素数的立方根小数部分
        int[] w = new int[64];
        for (int j = 0; j < 16; j++)
            w[j] = ((gr[4*j]&0xff)<<24)|((gr[4*j+1]&0xff)<<16)|((gr[4*j+2]&0xff)<<8)|(gr[4*j+3]&0xff);
        for (int j = 16; j < 64; j++)                  // 扩展：σ0/σ1 小旋转 + 模加（对照 SM3 的 P1 异或）
            w[j] = (Integer.rotateRight(w[j-2],17) ^ Integer.rotateRight(w[j-2],19) ^ (w[j-2]>>>10))
                    + w[j-7]
                    + (Integer.rotateRight(w[j-15],7) ^ Integer.rotateRight(w[j-15],18) ^ (w[j-15]>>>3))
                    + w[j-16];
        for (int j = 0; j < 64; j++) {                 // 64 轮：Ch/Maj + Σ0/Σ1（对照 SM3 的 FF/GG + P0）
            int a = h[0], b = h[1], c = h[2], d = h[3], e = h[4], f = h[5], g = h[6], hh = h[7];
            int t1 = hh + (Integer.rotateRight(e,6) ^ Integer.rotateRight(e,11) ^ Integer.rotateRight(e,25))
                    + ((e & f) ^ (~e & g)) + k[j] + w[j];
            int t2 = (Integer.rotateRight(a,2) ^ Integer.rotateRight(a,13) ^ Integer.rotateRight(a,22))
                    + ((a & b) ^ (a & c) ^ (b & c));
            h = new int[]{t1 + t2, a, b, c, d + t1, e, f, g};
        }
        StringBuilder sb = new StringBuilder();        // 收尾：H += 轮转终态（模加，对照 SM3 的异或）
        for (int i = 0; i < 8; i++) { if (i > 0) sb.append(' '); sb.append(String.format("%08x", h[i] + iv0(i))); }
        System.out.println("SHA256(\"abc\") = " + sb);
    }

    static int[] IV = {0x6a09e667, 0xbb67ae85, 0x3c6ef372, 0xa54ff53a,
            0x510e527f, 0x9b05688c, 0x1f83d9ab, 0x5be0cd19};
    static int iv0(int i) { return IV[i]; }

    static int[] initialHash() { return rootWords(8, false); }
    static int[] roundConstants() { return rootWords(64, true); }
    // 常数程序生成：前 N 个素数的平方根(H)/立方根(K)小数部分 × 2³²——"无先验结构"的数学出处
    static int[] rootWords(int count, boolean cbrt) {
        int[] out = new int[count];
        int found = 0;
        for (int cand = 2; found < count; cand++) {
            boolean prime = true;
            for (int d = 2; d * d <= cand; d++) if (cand % d == 0) { prime = false; break; }
            if (!prime) continue;
            double root = cbrt ? Math.cbrt(cand) : Math.sqrt(cand);
            double frac = root - Math.floor(root);
            out[found++] = (int) ((long) (frac * (1L << 32)) & 0xffffffffL);   // long 转换防 int 饱和
        }
        return out;
    }
}
