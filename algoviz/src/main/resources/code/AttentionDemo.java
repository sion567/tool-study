public class AttentionDemo {
    public static void main(String[] args) {
        String[] tokens = {"猫", "坐", "垫"};   //@DATA
        double[][] e = {{1, 0}, {0, 1}, {1, 1}};   // 嵌入 d=2（W_q=W_k=W_v=I 简化）
        for (int q = 0; q < 3; q++) {
            double[] scores = new double[3];
            for (int k = 0; k < 3; k++) {
                scores[k] = (e[q][0] * e[k][0] + e[q][1] * e[k][1]) / Math.sqrt(2);   // ÷√d 防饱和
            }
            // softmax
            double max = Math.max(scores[0], Math.max(scores[1], scores[2]));
            double[] w = new double[3];
            double sum = 0;
            for (int k = 0; k < 3; k++) { w[k] = Math.exp(scores[k] - max); sum += w[k]; }
            for (int k = 0; k < 3; k++) w[k] /= sum;
            System.out.printf("查询[%s] → 权重 %s%n", tokens[q], java.util.Arrays.toString(w));
            // 加权和 V：输出 = w·E——每个词吸收全句上下文
        }
    }
}
