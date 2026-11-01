public class BinarySearch {
    static int search(int[] a, int target) {
        int lo = 0, hi = a.length - 1;
        while (lo <= hi) {
            int mid = (lo + hi) / 2;               // 折半：一次排除一半
            if (a[mid] == target) return mid;       // 命中
            if (a[mid] < target) lo = mid + 1;      // 目标在右半
            else hi = mid - 1;                      // 目标在左半
        }
        return -1;                                  // 没找到
    }
}
