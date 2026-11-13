public class BackpropDemo {
    public static void main(String[] args) {
        double x = 1.0, t = 1.0, lr = 0.5;   //@DATA 样本与学习率
        double[] w1 = {0.5, -0.3}, b1 = {0.1, 0.2}, w2 = {0.8, -0.6};
        // 前向：h = σ(w1·x+b1)，y = w2·h
        double[] h = new double[2];
        for (int i = 0; i < 2; i++) h[i] = sigmoid(w1[i] * x + b1[i]);
        double y = w2[0] * h[0] + w2[1] * h[1];
        double loss = (y - t) * (y - t);
        System.out.printf("前向 y=%.3f L=%.4f%n", y, loss);
        // 反向：链式法则逐层算——每层的梯度由后一层乘局部导数
        double dLdy = 2 * (y - t);
        double[] dLdw2 = {dLdy * h[0], dLdy * h[1]};
        double[] dLdh = {dLdy * w2[0], dLdy * w2[1]};
        double[] dLdw1 = {dLdh[0] * h[0] * (1 - h[0]) * x, dLdh[1] * h[1] * (1 - h[1]) * x};
        System.out.printf("dLdw2=[%.3f,%.3f] dLdw1=[%.4f,%.4f]%n", dLdw2[0], dLdw2[1], dLdw1[0], dLdw1[1]);
        // 更新：w ← w − lr·∂L/∂w
        for (int i = 0; i < 2; i++) { w2[i] -= lr * dLdw2[i]; w1[i] -= lr * dLdw1[i]; }
        double[] h2 = new double[2];
        for (int i = 0; i < 2; i++) h2[i] = sigmoid(w1[i] * x + b1[i]);
        double y2 = w2[0] * h2[0] + w2[1] * h2[1];
        System.out.printf("二次前向 y=%.3f L=%.4f（损失下降）%n", y2, (y2 - t) * (y2 - t));
        // sigmoid 局部导数 h(1−h) ≤ 0.25——乘得越多越小，深层的"梯度消失"
    }

    static double sigmoid(double x) { return 1 / (1 + Math.exp(-x)); }
}
