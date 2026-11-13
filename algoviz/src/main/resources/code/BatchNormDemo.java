public class BatchNormDemo {
    public static void main(String[] args) {
        double[] batch = {1, 2, 3, 9};   //@DATA 离群 9 特意保留
        double gamma = 1.2, beta = 0.5, eps = 1e-5;
        double mean = 0;
        for (double v : batch) mean += v;
        mean /= batch.length;
        double var = 0;
        for (double v : batch) var += (v - mean) * (v - mean);
        var /= batch.length;
        for (int i = 0; i < batch.length; i++) {
            double xhat = (batch[i] - mean) / Math.sqrt(var + eps);   // 标准化：均值 0 方差 1
            double out = gamma * xhat + beta;                          // γβ 可学习：要多标准、偏多少
            System.out.printf("x%d=%.1f xhat=%.3f out=%.3f%n", i + 1, batch[i], xhat, out);
        }
        // 内部协变量偏移：上一层参数一动，下一层输入分布就漂——训练像踩独轮车；BN 把分布拉回正态
        // 推理用训练期滑动平均 μ/σ——单个样本也能归一化
    }
}
