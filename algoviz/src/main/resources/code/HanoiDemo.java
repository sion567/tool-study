public class HanoiDemo {
    static int moves = 0;

    public static void main(String[] args) {
        hanoi(5, 'A', 'C', 'B');   //@DATA
        System.out.println("共 " + moves + " 步 = 2⁵−1");
    }

    // 把 n 个盘从 from 挪到 to，借助 via
    static void hanoi(int n, char from, char to, char via) {
        if (n == 0) return;
        hanoi(n - 1, from, via, to);              // ① 让开：n−1 个盘先到中转柱
        moves++;
        System.out.println("盘 " + n + ": " + from + " → " + to);   // ② 挪最大的盘
        hanoi(n - 1, via, to, from);              // ③ 盖上：n−1 个盘到目标柱
    }
}
