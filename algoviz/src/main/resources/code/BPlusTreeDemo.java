import java.util.*;

public class BPlusTreeDemo {
    // B+ 树（阶 m=4）：内部节点 ≤3 键只是路标；数据全在叶子且叶链有序
    static final int MAX_KEYS = 3;

    static class Node {
        List<Integer> keys = new ArrayList<>();
        List<Node> children = new ArrayList<>();
        Node next;              // 叶链：范围查询的骨架
        boolean leaf = true;
    }

    static Node root = new Node();

    public static void main(String[] args) {
        int[] keys = {10, 20, 5, 15, 25, 30, 8, 35, 6, 12, 18, 22};   //@DATA
        for (int k : keys) insert(k);
        System.out.println("叶链（= 全部数据，有序）：" + leafChain());
    }

    static void insert(int k) {
        Node leaf = descend(root, k);        // ① 下探：路标 ≤ k 走右，> k 走左
        int i = 0;
        while (i < leaf.keys.size() && leaf.keys.get(i) < k) i++;
        leaf.keys.add(i, k);                 // ② 叶内有序插入
        if (leaf.keys.size() > MAX_KEYS) splitLeaf(leaf);   // ③ 满 3 键才分裂
    }

    static Node descend(Node node, int k) {
        if (node.leaf) return node;
        int i = 0;
        while (i < node.keys.size() && k >= node.keys.get(i)) i++;
        return descend(node.children.get(i), k);
    }

    static void splitLeaf(Node leaf) {
        int mid = leaf.keys.size() / 2;
        Node right = new Node();
        right.keys.addAll(leaf.keys.subList(mid, leaf.keys.size()));
        leaf.keys.subList(mid, leaf.keys.size()).clear();
        right.next = leaf.next;              // 叶链接上
        leaf.next = right;
        copyUp(parentOf(leaf), leaf, right, right.keys.get(0));   // ④ 上浮**副本**
    }

    static void copyUp(Node parent, Node left, Node right, int key) {
        if (parent == null) {                // 没父 → 新根，树长高（唯一方式）
            Node nr = new Node();
            nr.leaf = false;
            nr.keys.add(key);
            nr.children.add(left);
            nr.children.add(right);
            root = nr;
            return;
        }
        int i = 0;
        while (i < parent.keys.size() && parent.keys.get(i) < key) i++;
        parent.keys.add(i, key);
        parent.children.add(i + 1, right);
        if (parent.keys.size() > MAX_KEYS) {   // 内部也满 → 中位**搬走**，继续上浮
            int mid = parent.keys.size() / 2;
            int up = parent.keys.get(mid);
            Node rightN = new Node();
            rightN.leaf = false;
            rightN.keys.addAll(parent.keys.subList(mid + 1, parent.keys.size()));
            rightN.children.addAll(parent.children.subList(mid + 1, parent.children.size()));
            parent.keys.subList(mid, parent.keys.size()).clear();
            parent.children.subList(mid + 1, parent.children.size()).clear();
            copyUp(parentOf(parent), parent, rightN, up);
        }
    }

    static Node parentOf(Node target) {
        for (Node n = root; !n.leaf; ) {
            if (n.children.contains(target)) return n;
            n = descendOne(n, target.keys.get(0));
        }
        return null;
    }

    static Node descendOne(Node node, int k) {
        int i = 0;
        while (i < node.keys.size() && k >= node.keys.get(i)) i++;
        return node.children.get(i);
    }

    static String leafChain() {
        Node n = root;
        while (!n.leaf) n = n.children.get(0);
        StringBuilder sb = new StringBuilder();
        for (; n != null; n = n.next) sb.append(n.keys).append(" → ");
        return sb.append("null").toString();
    }
}
