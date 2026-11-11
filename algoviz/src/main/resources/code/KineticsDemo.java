public class KineticsDemo {
    public static void main(String[] args) {
        double k1 = 0.2, k2 = 0.2;   //@DATA 速率常数
        double a0 = 1.0;
        System.out.println("一级 t1/2=" + Math.log(2) / k1 + "s（恒定）；二级 t1/2 初值=" + 1 / (k2 * a0) + "s（涨中）");
        for (int i = 0; i <= 24; i++) {          // 每 0.5s 一帧
            double t = 0.5 * i;
            double a1 = a0 * Math.exp(-k1 * t);  // 一级：指数衰减——每过 ln2/k 雷打不动减半
            double a2 = a0 / (1 + k2 * a0 * t);  // 二级：消耗越多走得越慢
            System.out.printf("t=%.1f  [A]一级=%.3f（过了%d个半衰期）  [A]二级=%.3f%n",
                    t, a1, (int) (t * k1 / Math.log(2)), a2);
        }
        // 实验判级数：ln[A]-t 是直线 → 一级；1/[A]-t 是直线 → 二级——谁线性谁上岗
    }
}
// 碳-14 测年、药物代谢半衰期——一级衰减是自然界的默认钟表
