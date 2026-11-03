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
        int[] vals = {5, 3, 8, 1, 4, 7, 9};            //@DATA
        Node root = bstFrom(vals);
        level(root);
        System.out.println();
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

    @Override
    public java.util.List<String> dataLines() {
        return java.util.List.of(Algorithm.joinArray(input));
    }
}
