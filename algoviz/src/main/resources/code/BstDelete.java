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
        Node root = new Node(5);
        root.left = new Node(3); root.right = new Node(8);
        root.left.left = new Node(1); root.left.right = new Node(4);
        root.right.left = new Node(7); root.right.right = new Node(9);
        root = delete(root, 5);                        // 删根：双子换后继
        System.out.println("新根 = " + root.val);       // 7（后继顶上）
    }
