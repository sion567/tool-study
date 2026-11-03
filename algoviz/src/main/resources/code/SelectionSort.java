public class SelectionSort {
    public static void sort(int[] a) {
        for (int i = 0; i < a.length - 1; i++) {
            int min = i;
            for (int j = i + 1; j < a.length; j++) {
                if (a[j] < a[min]) {
                    min = j;
                }
            }
            int t = a[i];
            a[i] = a[min];
            a[min] = t;
        }
    }

    public static void main(String[] args) {          // 复制本类即可直接运行
        int[] a = {5, 2, 9, 1, 5};   //@DATA
        sort(a);
        System.out.println(java.util.Arrays.toString(a));

    @Override
    public java.util.List<String> dataLines() {
        return java.util.List.of(Algorithm.joinArray(input));
    }
}
