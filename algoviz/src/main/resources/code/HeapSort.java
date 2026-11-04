public class HeapSort {
    public static void main(String[] args) {
        int[] a = {5, 2, 9, 1, 5};
        int n = a.length;
        // 建大顶堆：从最后一个非叶子节点开始，自底向上堆化
        for (int i = n / 2 - 1; i >= 0; i--) siftDown(a, i, n);
        // 排序：堆顶（最大值）换到末尾，堆缩小，重新堆化
        for (int end = n - 1; end > 0; end--) {
            int t = a[0]; a[0] = a[end]; a[end] = t;
            siftDown(a, 0, end);
        }
        System.out.println(java.util.Arrays.toString(a));
    }
    static void siftDown(int[] a, int i, int size) {
        while (true) {
            int largest = i;
            int l = 2 * i + 1, r = 2 * i + 2;
            if (l < size && a[l] > a[largest]) largest = l;
            if (r < size && a[r] > a[largest]) largest = r;
            if (largest == i) break;
            int t = a[i]; a[i] = a[largest]; a[largest] = t;
            i = largest;
        }
    }
}
