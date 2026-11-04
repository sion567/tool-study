public class ListDeleteDemo {
    static class Node {
        int val; Node next;
        Node(int v) { val = v; }
    }

    public static void main(String[] args) {
        int[] vals = {4, 9, 2, 7, 5};   //@DATA
        int target = 7;
        Node head = build(vals);

        // 单链表删除：prev 必须提前一站——要改的就是 prev.next
        Node dummy = new Node(0); dummy.next = head;   // 哨兵：删头节点不用特判
        Node prev = dummy;
        while (prev.next != null && prev.next.val != target) {
            prev = prev.next;                           // prev / curr 同步走
        }
        if (prev.next != null) {
            prev.next = prev.next.next;                 // ← 改道：一句话跳过目标
        }
        print(dummy.next);   // 4 → 9 → 2 → 5
    }

    static Node build(int[] vals) {
        Node dummy = new Node(0), tail = dummy;
        for (int v : vals) { tail.next = new Node(v); tail = tail.next; }
        return dummy.next;
    }

    static void print(Node head) {
        StringBuilder sb = new StringBuilder();
        for (Node c = head; c != null; c = c.next) sb.append(c.val).append(" → ");
        System.out.println(sb.append("null"));
    }
}
