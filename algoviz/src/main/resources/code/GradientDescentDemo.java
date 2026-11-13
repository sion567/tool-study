public class GradientDescentDemo {
    public static void main(String[] args) {
        double lr = 0.08, beta = 0.9;   //@DATA 学习率与动量系数
        double x0 = 0.6;                 // 起点：中央小丘右侧
        double plain = x0, mom = x0, v = 0;
        // 损失 L(x)=0.1x⁴−x²+0.5（双谷：局部谷左、全局谷右）导数 L'=0.4x³−2x
        for (int s = 1; s <= 12; s++) {
            plain = plain - lr * (0.4*plain*plain*plain - 2*plain);   // 普通 GD：每步从零起跑
            v = beta * v + (0.4*mom*mom*mom - 2*mom);                  // 动量：历史速度指数加权
            mom = mom - lr * v;                                        // 惯性冲过中央小丘
            System.out.printf("step%d 普通=%.2f 动量=%.2f%n", s, plain, mom);
        }
        System.out.println("终局：普通 GD 停在局部谷，动量 GD 抵达全局谷");
        // Adam = 动量 + 自适应学习率——两大改进主线（惯性/自适应）本演示各占一条
    }
}
