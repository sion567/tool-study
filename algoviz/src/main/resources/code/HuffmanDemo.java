public class HuffmanDemo {
    public static void main(String[] args) {
        char[] chars = {'A', 'B', 'C', 'D'};
        int[] freqs = {45, 13, 12, 9};
        var pq = new java.util.PriorityQueue<int[]>((a, b) -> a[0] - b[0]);
        for (int i = 0; i < chars.length; i++) pq.offer(new int[]{freqs[i], i});
        while (pq.size() > 1) {
            int[] a = pq.poll(), b = pq.poll();
            pq.offer(new int[]{a[0] + b[0], -1});
            System.out.println("合并 " + a[0] + " + " + b[0] + " → " + (a[0] + b[0]));
        }
        System.out.println("编码树完成，根权重 = " + pq.peek()[0]);
    }
}
