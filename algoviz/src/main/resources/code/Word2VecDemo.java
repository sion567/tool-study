public class Word2VecDemo {
    public static void main(String[] args) {
        String corpus = "the cat sat on the mat";   //@DATA 语料
        String[] words = corpus.split(" ");
        int[][] pairs = {{0, 1}, {1, 2}, {2, 3}, {3, 0}, {0, 4}};   // 窗口 ±1 配对
        double lr = 0.3;
        double[][] emb = {{2, 2}, {-2, 2}, {2, -2}, {-2, -2}, {0.5, 0.5}};   // 2 维嵌入四角散开
        for (int epoch = 1; epoch <= 3; epoch++) {
            for (int[] pair : pairs) {          // skip-gram：中心词与上下文词互相靠近
                int c = pair[0], ctx = pair[1];
                double d0 = emb[c][0] - emb[ctx][0], d1 = emb[c][1] - emb[ctx][1];
                emb[c][0] -= lr * d0;  emb[c][1] -= lr * d1;            // 中心词挪向上下文
                emb[ctx][0] += lr * d0 / 2;  emb[ctx][1] += lr * d1 / 2; // 上下文反向挪一半
                System.out.printf("轮%d %s↔%s%n", epoch, words[c], words[ctx]);
            }
        }
        for (int i = 0; i < words.length; i++)
            System.out.printf("%s = (%.2f, %.2f)%n", words[i], emb[i][0], emb[i][1]);
        // 共现对的每一步都让两词靠近——高频虚词聚拢、实词抱团：语义从共现统计里结晶
    }
}
