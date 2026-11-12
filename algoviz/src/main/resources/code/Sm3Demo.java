public class Sm3Demo {
    public static void main(String[] args) {
        String message = "abc";   //@DATA 消息
        byte[] msg = message.getBytes();
        long bitLen = msg.length * 8L;
        int padded = (msg.length + 9 + 63) / 64 * 64;   // 补 0x80 + 补 0 + 64 位长度
        byte[] gr = new byte[padded];
        System.arraycopy(msg, 0, gr, 0, msg.length);
        gr[msg.length] = (byte) 0x80;
        for (int i = 0; i < 8; i++) gr[padded - 1 - i] = (byte) (bitLen >>> (8 * i));
        int[] v = {0x7380166f, 0x4914b2b9, 0x172442d7, 0xda8a0600,
                0xa96f30bc, 0x163138aa, 0xe38dee4d, 0xb0fb0e4e};   // 初始值 IV
        int[] w = new int[68];
        for (int j = 0; j < 16; j++)
            w[j] = ((gr[4*j]&0xff)<<24)|((gr[4*j+1]&0xff)<<16)|((gr[4*j+2]&0xff)<<8)|(gr[4*j+3]&0xff);
        for (int j = 16; j < 68; j++) {   // 消息扩展：P1 置换"发酵"出 52 个新字
            int x = w[j-16] ^ w[j-9] ^ Integer.rotateLeft(w[j-3], 15);
            w[j] = x ^ Integer.rotateLeft(x, 15) ^ Integer.rotateLeft(x, 23)
                    ^ Integer.rotateLeft(w[j-13], 7) ^ w[j-6];
        }
        for (int j = 0; j < 64; j++) {    // 64 轮压缩：FF/GG 布尔函数 + P0 置换逐轮搅动状态
            int a = v[0], b = v[1], c = v[2], d = v[3], e = v[4], f = v[5], g = v[6], h = v[7];
            int tj = j < 16 ? 0x79cc4519 : 0x7a879d8a;
            int ss1 = Integer.rotateLeft(Integer.rotateLeft(a, 12) + e + Integer.rotateLeft(tj, j), 7);
            int ss2 = ss1 ^ Integer.rotateLeft(a, 12);
            int tt1 = (j < 16 ? a^b^c : (a&b)|(a&c)|(b&c)) + d + ss2 + (w[j] ^ w[j+4]);
            int tt2 = (j < 16 ? e^f^g : (e&f)|(~e&g)) + h + ss1 + w[j];
            int p0 = tt2 ^ Integer.rotateLeft(tt2, 9) ^ Integer.rotateLeft(tt2, 17);
            v = new int[]{tt1, a, Integer.rotateLeft(b, 9), c, p0, e, Integer.rotateLeft(f, 19), g};
        }
        StringBuilder sb = new StringBuilder();   // 收尾：V = V ⊕ 轮转终态（异或回初始值）
        for (int i = 0; i < 8; i++) { if (i > 0) sb.append(' '); sb.append(String.format("%08x", v[i] ^ iv0(i))); }
        System.out.println("SM3(\"" + message + "\") = " + sb);
    }
    static int[] IV = {0x7380166f, 0x4914b2b9, 0x172442d7, 0xda8a0600,
            0xa96f30bc, 0x163138aa, 0xe38dee4d, 0xb0fb0e4e};
    static int iv0(int i) { return IV[i]; }
}
