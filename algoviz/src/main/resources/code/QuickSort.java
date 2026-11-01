public class QuickSort {
    static void qs(int[] a, int lo, int hi) {
        if (lo >= hi) return;                     // 基线：0 或 1 个元素天然有序
        int p = partition(a, lo, hi);             // 分区：pivot 归位
        qs(a, lo, p - 1);                          // 左半
        qs(a, p + 1, hi);                          // 右半
    }
    static int partition(int[] a, int lo, int hi) {
        int pivot = a[hi];                         // 以最后一个为基准
        int i = lo;                                // i：小于区的边界
        for (int j = lo; j < hi; j++) {
            if (a[j] < pivot) {                    // 比 pivot 小 → 换到左边
                int t = a[i]; a[i] = a[j]; a[j] = t;
                i++;
            }
        }
        int t = a[i]; a[i] = a[hi]; a[hi] = t;     // pivot 归位
        return i;
    }

    public static void main(String[] args) {          // 复制本类即可直接运行
        int[] a = {5, 2, 9, 1, 5};
        qs(a, 0, a.length - 1);
        System.out.println(java.util.Arrays.toString(a));
    }
