public class RedBlackDemo {
    static final boolean RED = true;
    static final boolean BLACK = false;

    static class Node {
        int val; Node left, right; boolean color;      // 颜色长在节点上
        Node(int v, boolean c) { val = v; color = c; }
    }

    static Node insert(Node h, int val) {
        if (h == null) return new Node(val, RED);      // F1：新节点一律红色
        if (val < h.val) h.left = insert(h.left, val);
        else if (val > h.val) h.right = insert(h.right, val);
        // —— 以下三行就是全部修复逻辑 ——
        if (isRed(h.right) && !isRed(h.left)) h = rotateLeft(h);     // F5/F6：右红 → 左旋
        if (isRed(h.left) && isRed(h.left.left)) h = rotateRight(h); // F4：双红 → 右旋
        if (isRed(h.left) && isRed(h.right)) flipColors(h);          // F3：左右皆红 → 变色
        return h;
    }
    static boolean isRed(Node n) { return n != null && n.color == RED; }
    static Node rotateLeft(Node h) {                   // F6：右孩子顶上来
        Node x = h.right;
        h.right = x.left;
        x.left = h;
        x.color = h.color; h.color = RED;              // 颜色随位互换
        return x;
    }
    static Node rotateRight(Node h) {                  // F6：左孩子顶上来
        Node x = h.left;
        h.left = x.right;
        x.right = h;
        x.color = h.color; h.color = RED;
        return x;
    }
    static void flipColors(Node h) {                   // F3：孩子变黑、自己变红
        h.color = RED;
        h.left.color = BLACK;
        h.right.color = BLACK;
    }
    public static void main(String[] args) {
        Node root = null;
        for (int v : new int[]{10, 5, 1, 8, 7, 9}) {   // 依次插入，观察变色与旋转
            root = insert(root, v);
            root.color = BLACK;                        // F7 收尾：根必须为黑
        }
    }
}
