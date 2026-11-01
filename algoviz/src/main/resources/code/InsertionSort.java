public class InsertionSort {
    public static void sort(int[] a) {
        for (int i = 1; i < a.length; i++) {
            int key = a[i];
            int j = i - 1;
            while (j >= 0 && a[j] > key) {
                a[j + 1] = a[j];
                j--;
            }
            a[j + 1] = key;
        }
    }

    public static void main(String[] args) {          // 复制本类即可直接运行
        int[] a = {5, 2, 9, 1, 5};
        sort(a);
        System.out.println(java.util.Arrays.toString(a));
    }
