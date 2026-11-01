public class LevelOrderTraversal {
    static void level(Node root) {
        java.util.Queue<Node> q = new java.util.ArrayDeque<>();
        q.offer(root);                        // 根入队
        while (!q.isEmpty()) {
            Node cur = q.poll();              // 出队：这一层最先进的
            visit(cur);
            if (cur.left != null) q.offer(cur.left);    // 下一层从左到右
            if (cur.right != null) q.offer(cur.right);
        }
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
        level(root);
        System.out.println();
    }
