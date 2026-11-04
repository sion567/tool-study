public class LcaDemo {
    static Node lca(Node root, int p, int q) {
        if (root == null) return null;
        if (root.val == p || root.val == q) return root;   // 命中目标
        Node left = lca(root.left, p, q);
        Node right = lca(root.right, p, q);
        if (left != null && right != null) return root;    // 分居两侧 → 本节点是 LCA
        return left != null ? left : right;                 // 单侧 → 原样上传
    }
    static class Node {
        int val; Node left, right;
        Node(int v) { val = v; }
    }
    public static void main(String[] args) {
        Node root = new Node(5);
        root.left = new Node(3); root.right = new Node(8);
        root.left.left = new Node(1); root.left.right = new Node(4);
        root.right.left = new Node(7); root.right.right = new Node(9);
        Node lca = lca(root, 1, 4);
        System.out.println("LCA(1,4) = " + lca.val);        // 3
    }
}
