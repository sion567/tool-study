public class PreOrderTraversal {
    static void traverse(Node node) {
        if (node == null) {
            return;
        }
        visit(node);              // 前序位：根 → 左 → 右
        traverse(node.left);
        traverse(node.right);
    }

    static class Node {
        int val; Node left, right;
        Node(int v) { val = v; }
    }

    static void visit(Node n) { System.out.print(n.val + " "); }

    public static void main(String[] args) {          // 复制本类即可直接运行
        Node root = new Node(5);
        root.left = new Node(3); root.right = new Node(8);
        root.left.left = new Node(1); root.left.right = new Node(4);
        root.right.left = new Node(7); root.right.right = new Node(9);
        traverse(root);
        System.out.println();
    }
