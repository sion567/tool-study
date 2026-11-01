public class LevelOrderTraversal {
    static void level(Node root) {
        Queue<Node> q = new ArrayDeque<>();
        q.offer(root);                        // 根入队
        while (!q.isEmpty()) {
            Node cur = q.poll();              // 出队：这一层最先进的
            visit(cur);
            if (cur.left != null) q.offer(cur.left);    // 下一层从左到右
            if (cur.right != null) q.offer(cur.right);
        }
    }
}
