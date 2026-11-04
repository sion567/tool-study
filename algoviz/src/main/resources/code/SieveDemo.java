public class SieveDemo {
    public static void main(String[] args) {
        int limit = 20;
        boolean[] isComposite = new boolean[limit + 1];
        for (int p = 2; p * p <= limit; p++) {
            if (isComposite[p]) continue;
            for (int m = p * p; m <= limit; m += p) {
                isComposite[m] = true;                // 划掉 p 的倍数
            }
        }
        for (int i = 2; i <= limit; i++) {
            if (!isComposite[i]) System.out.print(i + " ");
        }
        System.out.println();
    }
}
