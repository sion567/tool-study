public class MergeLists {
    static class Node { int val; Node next; Node(int v) { val = v; } }
    static Node merge(Node a, Node b) {
        Node dummy = new Node(0), tail = dummy;
        while (a != null && b != null) {            // 双指针比小
            if (a.val <= b.val) { tail.next = a; a = a.next; }
            else { tail.next = b; b = b.next; }
            tail = tail.next;
        }
        tail.next = (a != null) ? a : b;            // 一条耗尽，另一条整体接上
        return dummy.next;
    }
    public static void main(String[] args) {
        Node a = new Node(1); a.next = new Node(3); a.next.next = new Node(5);
        Node b = new Node(2); b.next = new Node(4); b.next.next = new Node(6);
        Node r = merge(a, b);
        for (Node c = r; c != null; c = c.next) System.out.print(c.val + " ");
        System.out.println();
    }
}
