public class InvertTree {
    static Node invert(Node root) {
        if (root == null) {
            return null;
        }
        Node left = invert(root.left);       // 递归信任：孩子交给递归
        Node right = invert(root.right);
        root.left = right;                    // ← 本体：左右指针互换
        root.right = left;
        return root;
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
        Node inverted = invert(root);
        System.out.println("翻转后根的左孩子 = " + inverted.left.val);   // 8（原右）
    }
}
