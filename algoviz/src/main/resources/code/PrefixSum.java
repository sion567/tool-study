public class PrefixSum {
    public static void main(String[] args) {
        int[] a = {3, 7, 2, 5, 1};
        int[] prefix = new int[a.length];
        prefix[0] = a[0];
        for (int i = 1; i < a.length; i++) {
            prefix[i] = prefix[i - 1] + a[i];     // ← 只看一眼邻居
        }
        System.out.println(java.util.Arrays.toString(prefix));
        // 区间和 [l,r] = prefix[r] - prefix[l-1]，O(1)
        System.out.println("sum[1,3] = " + (prefix[3] - prefix[0]));
    }
}
