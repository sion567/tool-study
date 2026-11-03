public class TwoPointers {
    public static void main(String[] args) {
        int[] a = {1, 2, 5, 5, 9, 12, 15};
        int target = 14;
        int lo = 0, hi = a.length - 1;
        while (lo < hi) {
            int sum = a[lo] + a[hi];
            if (sum == target) {
                System.out.println("找到: a[" + lo + "] + a[" + hi + "] = " + target);
                return;
            }
            if (sum < target) lo++; else hi--;
        }
        System.out.println("没找到");
    }
}
