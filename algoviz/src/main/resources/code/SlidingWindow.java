public class SlidingWindow {
    public static void main(String[] args) {
        int[] a = {2, 1, 3, 1, 4, 4, 2, 5};
        var seen = new java.util.HashSet<Integer>();
        int lo = 0, best = 0, bestLo = 0, bestHi = 0;
        for (int hi = 0; hi < a.length; hi++) {
            while (seen.contains(a[hi])) {        // 冲突 → lo 收缩
                seen.remove(a[lo]);
                lo++;
            }
            seen.add(a[hi]);
            if (hi - lo + 1 > best) {             // 新纪录
                best = hi - lo + 1; bestLo = lo; bestHi = hi;
            }
        }
        System.out.println("最长无重复窗口 [" + bestLo + "," + bestHi + "] 长度 " + best);
    }
}
