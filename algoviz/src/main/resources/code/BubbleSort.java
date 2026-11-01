public class BubbleSort {
    public static void sort(int[] a) {
        for (int i = 0; i < a.length - 1; i++) {
            for (int j = 0; j < a.length - 1 - i; j++) {
                if (a[j] > a[j + 1]) {
                    int t = a[j];
                    a[j] = a[j + 1];
                    a[j + 1] = t;
                }
            }
        }
    }

    public static void main(String[] args) {          // 复制本类即可直接运行
        int[] a = {5, 2, 9, 1, 5};
        sort(a);
        System.out.println(java.util.Arrays.toString(a));
    }
