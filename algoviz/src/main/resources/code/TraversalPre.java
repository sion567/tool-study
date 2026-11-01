public class PreOrderTraversal {
    static void traverse(Node node) {
        if (node == null) {
            return;
        }
        visit(node);              // 前序位：根 → 左 → 右
        traverse(node.left);
        traverse(node.right);
    }
}
