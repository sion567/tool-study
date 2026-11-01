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
        Node root = new Node(5);
        root.left = new Node(3); root.right = new Node(8);
        root = insert(root, 4);                        // 4 挂到 3 的右边
        System.out.println("root.left.right.val = " + root.left.right.val);
    }
