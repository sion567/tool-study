public class FifteenPuzzleDemo {
    public static void main(String[] args) {
        java.util.Random rnd = new java.util.Random(7);   //@DATA 打乱种子
        int[] b = new int[16];   // 0 = 空格
        for (int i = 0; i < 15; i++) b[i] = i + 1;
        int blank = 15;
        for (int s = 0; s < 60; s++) {   // 从还原态合法滑动打乱（保奇偶 → 必可解）
            java.util.List<Integer> opts = new java.util.ArrayList<>();
            int row = blank / 4, col = blank % 4;
            if (row > 0) opts.add(blank - 4);
            if (row < 3) opts.add(blank + 4);
            if (col > 0) opts.add(blank - 1);
            if (col < 3) opts.add(blank + 1);
            int pick = opts.get(rnd.nextInt(opts.size()));
            b[blank] = b[pick]; b[pick] = 0; blank = pick;
        }
        int[] flat = new int[15];
        for (int i = 0, f = 0; i < 16; i++) if (b[i] != 0) flat[f++] = b[i];   // 忽略空格拉平
        int inv = 0;
        for (int i = 0; i < 15; i++)
            for (int j = i + 1; j < 15; j++) if (flat[j] < flat[i]) inv++;   // 数逆序对
        int blankRow = 4 - blank / 4;   // 空格所在行（自底 1 起）
        boolean solvableA = (inv + blankRow) % 2 == 1;   // 判据：和为奇 ⇔ 可解
        // ---- 对换两枚牌（合法移动做不到的操作）----
        int tmp = flat[0]; flat[0] = flat[1]; flat[1] = tmp;   // 相邻对换 → 逆序数 ±1 → 奇偶翻转
        System.out.println("局面A可解=" + solvableA + "；局面B（对换后）可解=" + !solvableA + "——无论怎么滑都回不去");
    }
}
// 不变量：合法移动永保「逆序数+空格行」奇偶——1879 年全美疯找解法的 15 谜题风暴被一行数学终结
