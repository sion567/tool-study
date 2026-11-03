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

    static class Node {
        int val; Node left, right;
        Node(int v) { val = v; }
    }

    public static void main(String[] args) {          // 复制本类即可直接运行
        int[] vals = {5, 3, 8};                        //@DATA
        int insertVal = 4;
        Node root = bstFrom(vals);
        root = insert(root, insertVal);
        System.out.println("插入 " + insertVal + " 后中序: " + inOrder(root));
    }

    static Node bstFrom(int... vals) {
        Node r = null;
        for (int v : vals) r = insert(r, v);
        return r;
    }

    static String inOrder(Node n) {
        if (n == null) return "";
        return inOrder(n.left) + n.val + " " + inOrder(n.right);
    }

    @Override
    public java.util.List<String> dataLines() {
        return java.util.List.of(Algorithm.joinArray(input), "int insertVal = " + insertVal + ";");
    }
}
