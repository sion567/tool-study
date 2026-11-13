public class NaiveBayesDemo {
    public static void main(String[] args) {
        String mail = "免费 中奖";   //@DATA 待分类邮件
        String[] words = {"免费", "中奖", "会议", "报告"};
        int[] spamFreq = {3, 2, 0, 1};    // 6 封 spam 中的词频
        int[] hamFreq = {0, 0, 3, 2};     // 4 封 ham 中的词频
        double scoreSpam = 0.6, scoreHam = 0.4;   // 先验
        for (int w = 0; w < words.length; w++) {
            if (!mail.contains(words[w])) continue;
            double pSpam = (spamFreq[w] + 1) / (6.0 + 4);   // 拉普拉斯平滑：+1 防一票否决
            double pHam = (hamFreq[w] + 1) / (4.0 + 4);
            scoreSpam *= pSpam;
            scoreHam *= pHam;
            System.out.printf("「%s」P|spam=%.3f P|ham=%.3f%n", words[w], pSpam, pHam);
        }
        System.out.printf("score_spam=%.4f score_ham=%.4f → 判定 %s%n",
                scoreSpam, scoreHam, scoreSpam > scoreHam ? "spam" : "ham");
        // "朴素" = 假设词与词条件独立——乘法链把每封信压成一个分数
    }
}
