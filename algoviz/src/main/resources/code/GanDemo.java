import java.util.*;

public class GanDemo {
    public static void main(String[] args) {
        int rounds = 4;   //@DATA
        // 真实分布：半径 1 圆环上的 8 个点；生成器起点：半径 3
        double gRadius = 3.0, dAcc = 0.88;
        for (int r = 1; r <= rounds; r++) {
            gRadius = Math.max(1.0, gRadius - (gRadius - 1.0) * 0.45);   // G 向真实分布挪 45%
            dAcc = Math.max(0.5, dAcc - 0.12);                            // D 准确率跌向 50%
            System.out.printf("轮 %d：G 半径 %.2f  D 准确率 %.2f%n", r, gRadius, dAcc);
        }
        // 纳什均衡：D 准确率 = 0.5（瞎猜）→ 生成分布 ≈ 真实分布
    }
}
