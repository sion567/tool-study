public class RnnDemo {
    public static void main(String[] args) {
        int[] xs = {1, 0, 1, 1, 0};   //@DATA
        double wx = 1.0, wh = 0.8, h = 0;
        for (int t = 0; t < xs.length; t++) {
            h = Math.tanh(wx * xs[t] + wh * h);   // 同一个细胞滚动 5 步
            System.out.printf("t=%d h=%.2f%n", t + 1, h);
        }
        System.out.println("h₅ = " + h + " ← 整条序列被压缩成一个向量");
        // 梯度 ∝ wh^T 会消失/爆炸 → LSTM/GRU 的门控就是为它而生
    }
}
