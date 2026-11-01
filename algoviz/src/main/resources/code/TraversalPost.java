public class PostOrderTraversal {
    static void traverse(Node node) {
        if (node == null) {
            return;
        }
        traverse(node.left);
        traverse(node.right);
        visit(node);              // 后序位：左 → 右 → 根
    }
}
