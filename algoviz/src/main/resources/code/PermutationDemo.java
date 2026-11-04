public class PermutationDemo {
    static int[] input = {1, 2, 3};
    static boolean[] used = new boolean[input.length];
    static List<Integer> current = new java.util.ArrayList<>();
    static int count = 0;

    public static void main(String[] args) {
        backtrack(0);
        System.out.println("共 " + count + " 个排列");
    }
    static void backtrack(int depth) {
        if (depth == input.length) {
            System.out.println("排列: " + current);
            count++;
            return;
        }
        for (int i = 0; i < input.length; i++) {
            if (used[i]) continue;               // 剪枝：跳过已用元素
            used[i] = true;
            current.add(input[i]);                // 选择
            backtrack(depth + 1);                 // 递归下一层
            used[i] = false;                      // 撤销（回溯）
            current.remove(current.size() - 1);
        }
    }
}
