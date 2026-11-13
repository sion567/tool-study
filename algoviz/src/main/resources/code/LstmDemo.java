public class LstmDemo {
    public static void main(String[] args) {
        int[] xs = {1, 0, 1, 1, 0};   //@DATA 门权重 0.5/0.4/0.6
        double wf = 0.5, wi = 0.4, wo = 0.6;
        double c = 0, h = 0;
        for (int t = 0; t < xs.length; t++) {   // 同一序列滚动 5 步——对照 RNN 的普通 h 滚动
            double x = xs[t];
            double f = sigmoid(wf * x + wf * h);        // 遗忘门：旧记忆留多少
            double i = sigmoid(wi * x + wi * h);        // 输入门：新内容写多少
            double o = sigmoid(wo * x + wo * h);        // 输出门：露出多少给 h
            double ct = Math.tanh(x + h);               // 候选内容
            c = f * c + i * ct;                         // c 传送带：f⊙旧 + i⊙新
            h = o * Math.tanh(c);                       // h 是被输出门过滤的"读出"
            System.out.printf("t=%d f=%.2f i=%.2f o=%.2f c=%.3f h=%.3f%n", t, f, i, o, c, h);
        }
        // 梯度直通：∂c5/∂c4 = f5（门值 0~1）——对照 RNN 的 w_h⁴ 衰减，长序列不再一键失忆
    }

    static double sigmoid(double x) { return 1 / (1 + Math.exp(-x)); }
}
