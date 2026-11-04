public class RefactorExtractDemo {
    public static void main(String[] args) {
        System.out.println(process(new int[]{5, -2, 9, -1, 7}));
    }
    // ===== 反例：20 行混三件事（示意压缩）=====
    // void process(int[] a) {
    //     for (int x : a) if (x < 0) throw ...;        // 校验
    //     int sum = 0; for (int x : a) sum += x * 2;   // 计算
    //     System.out.println("sum=" + sum);            // 打印
    // }  → 改谁都要通读 20 行
    //
    // ===== 正例：提取方法，名字就是注释 =====
    static int process(int[] a) {
        validate(a);          // 读名字就知道干什么
        return compute(a);    // 调用处 = 摘要
    }
    static void validate(int[] a) {
        for (int x : a) {
            if (x < 0) throw new IllegalArgumentException("负数: " + x);
        }
    }
    static int compute(int[] a) {
        int sum = 0;
        for (int x : a) sum += x * 2;
        return sum;
    }
}
