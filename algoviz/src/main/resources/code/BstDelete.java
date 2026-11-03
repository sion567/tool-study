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

    static class Node {
        int val; Node left, right;
        Node(int v) { val = v; }
    }

    public static void main(String[] args) {          // 复制本类即可直接运行
        int[] vals = {5, 3, 8, 1, 4, 7, 9};            //@DATA
        int deleteVal = 5;
        Node root = bstFrom(vals);
        root = delete(root, deleteVal);
        System.out.println("删除 " + deleteVal + " 后中序: " + inOrder(root) + "｜新根 = " + root.val);
    }

    static Node bstInsert(Node root, int v) {
        if (root == null) return new Node(v);
        if (v < root.val) root.left = bstInsert(root.left, v);
        else if (v > root.val) root.right = bstInsert(root.right, v);
        return root;
    }

    static Node bstFrom(int... vals) {
        Node r = null;
        for (int v : vals) r = bstInsert(r, v);
        return r;
    }

    static String inOrder(Node n) {
        if (n == null) return "";
        return inOrder(n.left) + n.val + " " + inOrder(n.right);
    }

    @Override
    public java.util.List<String> dataLines() {
        return java.util.List.of(Algorithm.joinArray(input), "int deleteVal = " + deleteVal + ";");
    }
}
