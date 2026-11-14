public class RedBlackDeleteDemo {
    static Node root;
    static class Node { int v; Node l, r; boolean red; Node(int v, boolean red) { this.v = v; this.red = red; } }

    public static void main(String[] args) {
        for (int i = 1; i <= 10; i++) { root = insert(root, i); root.red = false; }
        for (int del : new int[]{7, 1, 9}) {
            if (root.l != null && !isRed(root.l) && !isRed(root.l.l)) root.red = true;   // 删前喂红
            root = delete(root, del);
            if (root != null) root.red = false;   // 根压黑收尾
        }
        java.util.List<Integer> in = new java.util.ArrayList<>();
        inorder(root, in);
        System.out.println("三次删除后中序 = " + in);
    }

    static Node delete(Node h, int v) {
        if (v < h.v) {
            if (!isRed(h.l) && !isRed(h.l.l)) h = moveRedLeft(h);   // 喂红：借侄
            h.l = delete(h.l, v);
        } else {
            if (isRed(h.l)) h = rotateRight(h);                      // 左红旋到右路
            if (v == h.v && h.r == null) return null;                // 叶命中：断链
            if (!isRed(h.r) && !isRed(h.r.l)) h = moveRedRight(h);   // 喂红：借左兄
            if (v == h.v) {                                          // 双子：后继顶替
                Node min = h.r;
                while (min.l != null) min = min.l;
                h.v = min.v;
                h.r = deleteMin(h.r);
            } else {
                h.r = delete(h.r, v);
            }
        }
        return fixup(h);                                             // 逆序三步恢复
    }

    static Node moveRedLeft(Node h) {
        flip(h);
        if (isRed(h.r.l)) { h.r = rotateRight(h.r); h = rotateLeft(h); flip(h); }
        return h;
    }
    static Node moveRedRight(Node h) {
        flip(h);
        if (isRed(h.l.l)) { h = rotateRight(h); flip(h); }
        return h;
    }
    static Node deleteMin(Node h) {
        if (h.l == null) return null;
        if (!isRed(h.l) && !isRed(h.l.l)) h = moveRedLeft(h);
        h.l = deleteMin(h.l);
        return fixup(h);
    }
    static Node fixup(Node h) {                                      // 逆序三步（对照插入）
        if (isRed(h.r) && !isRed(h.l)) h = rotateLeft(h);
        if (isRed(h.l) && isRed(h.l.l)) h = rotateRight(h);
        if (isRed(h.l) && isRed(h.r)) flip(h);
        return h;
    }
    static Node insert(Node h, int v) {
        if (h == null) return new Node(v, true);
        if (v < h.v) h.l = insert(h.l, v); else if (v > h.v) h.r = insert(h.r, v);
        if (isRed(h.r) && !isRed(h.l)) h = rotateLeft(h);
        if (isRed(h.l) && isRed(h.l.l)) h = rotateRight(h);
        if (isRed(h.l) && isRed(h.r)) flip(h);
        return h;
    }
    static Node rotateLeft(Node h) { Node x = h.r; h.r = x.l; x.l = h; x.red = h.red; h.red = true; return x; }
    static Node rotateRight(Node h) { Node x = h.l; h.l = x.r; x.r = h; x.red = h.red; h.red = true; return x; }
    static void flip(Node h) { h.red = !h.red; if (h.l != null) h.l.red = !h.l.red; if (h.r != null) h.r.red = !h.r.red; }
    static boolean isRed(Node n) { return n != null && n.red; }
    static void inorder(Node n, java.util.List<Integer> out) {
        if (n == null) return;
        inorder(n.l, out); out.add(n.v); inorder(n.r, out);
    }
}
