public class MergeSort {
    static void ms(int[] a, int lo, int hi) {
        if (lo >= hi) return;                     // 基线：单元素天然有序
        int mid = (lo + hi) / 2;
        ms(a, lo, mid);                           // 分：左半
        ms(a, mid + 1, hi);                       // 分：右半
        merge(a, lo, mid, hi);                    // 治：合并
    }
    static void merge(int[] a, int lo, int mid, int hi) {
        int[] tmp = new int[hi - lo + 1];
        int i = lo, j = mid + 1, k = 0;
        while (i <= mid && j <= hi) {
            tmp[k++] = (a[i] <= a[j]) ? a[i++] : a[j++];  // 谁小谁先进
        }
        while (i <= mid) tmp[k++] = a[i++];       // 左半剩余
        while (j <= hi) tmp[k++] = a[j++];        // 右半剩余
        for (int t = 0; t < tmp.length; t++) a[lo + t] = tmp[t];
    }
    public static void main(String[] args) {
        int[] a = {5, 2, 9, 1, 5};
        ms(a, 0, a.length - 1);
        System.out.println(java.util.Arrays.toString(a));
    }
}
