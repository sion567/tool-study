public class FourierDemo {
    public static void main(String[] args) {
        int N = 64;   //@DATA
        double[] x = new double[N];
        for (int i = 0; i < N; i++) {
            x[i] = Math.sin(2 * Math.PI * 3 * i / N) + 0.5 * Math.sin(2 * Math.PI * 7 * i / N);
        }
        // DFT：对每个频率 k 算幅度（O(n²)；FFT 可到 O(n log n)）
        for (int k = 0; k < N / 2; k++) {
            double re = 0, im = 0;
            for (int i = 0; i < N; i++) {
                double t = 2 * Math.PI * k * i / N;
                re += x[i] * Math.cos(t);
                im -= x[i] * Math.sin(t);
            }
            double amp = 2 * Math.sqrt(re * re + im * im) / N;
            if (amp > 0.1) System.out.printf("频率 %d：幅度 %.2f%n", k, amp);
        }
        // 输出只有：频率 3 幅度 1.0、频率 7 幅度 0.5——与构造信号逐字吻合
    }
}
