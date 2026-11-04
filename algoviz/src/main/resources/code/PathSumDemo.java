public class PathSumDemo {
    // 树：    5
    //        / \
    //       3   8
    //      / \ / \
    //     1  4 7  9
    // 叶子和：9 / 12 / 20 / 22 → 目标 12 命中 5→3→4

    public static void main(String[] args) {
        TreeNode root = build();
        System.out.println(hasPathSum(root, 12));   //@DATA
    }

    static boolean hasPathSum(TreeNode node, int sum) {
        if (node == null) return false;             // 空节点不是叶子
        sum -= node.val;                            // 边走边减（等价于边走边加）
        if (node.left == null && node.right == null)
            return sum == 0;                        // 叶子处一判即知
        return hasPathSum(node.left, sum)           // 左子树找剩余目标
            || hasPathSum(node.right, sum);         // 左边命中就不进右边
    }

    static TreeNode build() {
        TreeNode n5 = new TreeNode(5), n3 = new TreeNode(3), n8 = new TreeNode(8);
        TreeNode n1 = new TreeNode(1), n4 = new TreeNode(4);
        TreeNode n7 = new TreeNode(7), n9 = new TreeNode(9);
        n5.left = n3; n5.right = n8;
        n3.left = n1; n3.right = n4;
        n8.left = n7; n8.right = n9;
        return n5;
    }

    static class TreeNode {
        int val; TreeNode left, right;
        TreeNode(int v) { val = v; }
    }
}
