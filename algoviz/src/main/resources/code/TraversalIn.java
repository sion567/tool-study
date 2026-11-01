public class InOrderTraversal {
    static void traverse(Node node) {
        if (node == null) {
            return;
        }
        traverse(node.left);
        visit(node);              // 中序位：左 → 根 → 右
        traverse(node.right);
    }
}
