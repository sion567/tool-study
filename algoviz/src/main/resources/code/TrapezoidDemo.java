public class TrapezoidDemo {
    public static void main(String[] args) {
        int[] ns = {4, 8, 16, 32};   //@DATA f(x)=x² on [0,1]
        double t = 1.0 / 3;
        for (int n : ns) {
            double h = 1.0 / n;
            double lr = 0, tr = 0, sp = 1;      // 左黎曼 / 梯形 / 辛普森
            for (int i = 0; i < n; i++) lr += (i * h) * (i * h) * h;
            for (int i = 0; i < n; i++) tr += ((i*h)*(i*h) + ((i+1)*h)*((i+1)*h)) * h / 2;
            for (int i = 1; i < n; i++) sp += (i * h) * (i * h) * (i % 2 == 0 ? 2 : 4);
            sp = sp * h / 3;
            System.out.printf("n=%d 黎曼误差=%.5f 梯形=%.5f 辛普森=%.2e%n", n, Math.abs(lr-t), Math.abs(tr-t), Math.abs(sp-t));
        }
        // 误差阶 = n 翻倍时误差缩小的比例：黎曼减半 O(1/n)、梯形 1/4 O(1/n²)、辛普森 1/16 O(1/n⁴)
    }
}
