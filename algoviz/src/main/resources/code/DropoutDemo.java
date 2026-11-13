public class DropoutDemo {
    public static void main(String[] args) {
        double keep = 0.5;   //@DATA 置零概率 = 1-keep
        double[] vals = {1, 2, 3, 4, 5, 6};
        java.util.Random r = new java.util.Random(7);   // 固定种子：mask 可复现
        for (int f = 1; f <= 3; f++) {                  // 训练帧：每帧随机杀一半
            StringBuilder sb = new StringBuilder("[");
            for (int i = 0; i < vals.length; i++) {
                boolean alive = r.nextDouble() < keep;
                sb.append(alive ? 1 : 0);
                if (i < vals.length - 1) sb.append(',');
            }
            sb.append(']');
            System.out.println("训练帧 " + f + " mask=" + sb);
        }
        System.out.println("推理帧：全员上岗、无缩放（inverted 已在训练 ÷keep 预缩放）");
        // 每帧不同组合：网络被迫学"冗余表征"——不能依赖任何特定邻居
    }
}
