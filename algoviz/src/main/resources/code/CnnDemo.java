public class CnnDemo {
    public static void main(String[] args) {
        int in = 6, k = 3;   //@DATA
        int[][] input = new int[in][in];
        for (int r = 0; r < in; r++)
            for (int c = 0; c < in; c++)
                input[r][c] = c < 3 ? 1 : 5;       // 垂直边缘：左半 1 右半 5
        int[][] kernel = {{-1, 0, 1}, {-1, 0, 1}, {-1, 0, 1}};   // 垂直边缘检测核
        int out = in - k + 1;
        int[][] fmap = new int[out][out];
        for (int r = 0; r < out; r++)
            for (int c = 0; c < out; c++) {
                int sum = 0;
                for (int kr = 0; kr < k; kr++)
                    for (int kc = 0; kc < k; kc++)
                        sum += input[r + kr][c + kc] * kernel[kr][kc];
                fmap[r][c] = sum;
            }
        // 输出只有边界列 = +12，其余 0；再 2×2 最大池化 → 2×2
        for (int r = 0; r < out; r++)
            System.out.println(java.util.Arrays.toString(fmap[r]));
    }
}
