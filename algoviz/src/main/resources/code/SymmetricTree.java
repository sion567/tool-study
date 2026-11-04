public class SymmetricTree {
    static boolean isSymmetric(Node root) {
        return isMirror(root.left, root.right);
    }
    static boolean isMirror(Node a, Node b) {
        if (a == null && b == null) return true;          // 都空 → 对称
        if (a == null || b == null) return false;         // 结构不同
        if (a.val != b.val) return false;                 // 值不同
        return isMirror(a.left, b.right)                  // 外外侧
                && isMirror(a.right, b.left);             // 内内侧
    }
    static class Node {
        int val; Node left, right;
        Node(int v) { val = v; }
    }
    public static void main(String[] args) {
        Node root = new Node(5);
        root.left = new Node(3); root.right = new Node(3);
        root.left.left = new Node(1); root.left.right = new Node(4);
        root.right.left = new Node(4); root.right.right = new Node(1);
        System.out.println("对称吗？" + isSymmetric(root));
    }
}
