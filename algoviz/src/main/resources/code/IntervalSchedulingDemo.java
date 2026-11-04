public class IntervalSchedulingDemo {
    public static void main(String[] args) {
        int[][] intervals = {{1,3},{2,5},{4,7},{1,8},{5,9},{6,10}};
        Integer[] order = new Integer[intervals.length];
        for (int i = 0; i < order.length; i++) order[i] = i;
        java.util.Arrays.sort(order, (a, b) -> intervals[a][1] - intervals[b][1]);
        int lastEnd = -1;
        var selected = new java.util.ArrayList<String>();
        for (int idx : order) {
            if (intervals[idx][0] >= lastEnd) {
                lastEnd = intervals[idx][1];
                selected.add("[" + intervals[idx][0] + "," + intervals[idx][1] + "]");
            }
        }
        System.out.println("最多不重叠区间: " + selected);
    }
}
