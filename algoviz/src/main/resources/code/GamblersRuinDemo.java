import java.util.Random;

public class GamblersRuinDemo {
    public static void main(String[] args) {
        long seed = 42;   //@DATA
        int fortune = 50, goal = 100, steps = 0;
        Random r = new Random(seed);
        while (fortune > 0 && fortune < goal) {
            steps++;
            fortune += r.nextBoolean() ? 1 : -1;
        }
        System.out.println("局数=" + steps + "  本金=" + fortune
                + "  (" + (fortune >= goal ? "暴富" : "破产") + ")");
        // 公平游戏：P(暴富) = 本金/目标 = 50%；庄家优势 2% → 暴富概率暴跌到 ≈2%
        // 期望时长 = 起点 × 终点差 = 50 × 100 = 2500 局
    }
}
