public class BstDelete {
    static Node delete(Node root, int val) {
        if (root == null) {
            return null;
        }
        if (val < root.val) {
            root.left = delete(root.left, val);
        } else if (val > root.val) {
            root.right = delete(root.right, val);
        } else {
            if (root.left == null) {
                return root.right;
            }
            if (root.right == null) {
                return root.left;
            }
            Node min = root.right;
            while (min.left != null) {
                min = min.left;
            }
            root.val = min.val;
            root.right = delete(root.right, min.val);
        }
        return root;
    }
}
