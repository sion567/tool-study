public class BstInsert {
    static Node insert(Node root, int val) {
        if (root == null) {
            return new Node(val);        // 空位：新盒子造在这里
        }
        if (val < root.val) {
            root.left = insert(root.left, val);
        } else if (val > root.val) {
            root.right = insert(root.right, val);
        }
        return root;                     // 相等：不插（去重）
    }
}
