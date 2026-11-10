public class AvlDemo {
    static class Node {
        int val, h = 1;
        Node l, r;
        Node(int v) { val = v; }
    }

    public static void main(String[] args) {
        int[] input = {10, 20, 30, 40, 50, 25};   //@DATA
        Node root = null;
        for (int v : input) root = insert(root, v);
        System.out.println("根 = " + root.val);          // 30：RR 与 RL 旋转后的平衡根
        System.out.println("中序 = " + inorder(root));   // 永远有序
    }

    static Node insert(Node n, int v) {
        if (n == null) return new Node(v);
        if (v < n.val) n.l = insert(n.l, v);
        else if (v > n.val) n.r = insert(n.r, v);
        else return n;
        n.h = 1 + Math.max(h(n.l), h(n.r));
        int bal = h(n.l) - h(n.r);                       // 平衡因子
        if (bal > 1 && v < n.l.val) return rotR(n);      // LL：右旋
        if (bal < -1 && v > n.r.val) return rotL(n);     // RR：左旋
        if (bal > 1) { n.l = rotL(n.l); return rotR(n); }   // LR：先左后右
        if (bal < -1) { n.r = rotR(n.r); return rotL(n); }  // RL：先右后左
        return n;
    }

    static Node rotR(Node y) {   // 右旋
        Node x = y.l;
        y.l = x.r;
        x.r = y;
        y.h = 1 + Math.max(h(y.l), h(y.r));
        x.h = 1 + Math.max(h(x.l), h(x.r));
        return x;
    }

    static Node rotL(Node x) {   // 左旋
        Node y = x.r;
        x.r = y.l;
        y.l = x;
        x.h = 1 + Math.max(h(x.l), h(x.r));
        y.h = 1 + Math.max(h(y.l), h(y.r));
        return y;
    }

    static int h(Node n) { return n == null ? 0 : n.h; }

    static String inorder(Node n) {
        return n == null ? "" : inorder(n.l) + n.val + " " + inorder(n.r);
    }
}
