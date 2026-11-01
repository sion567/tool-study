public class LinkedListCycle {
    static boolean hasCycle(Node head) {
        Node slow = head, fast = head;
        while (fast != null && fast.next != null) {
            slow = slow.next;               // 慢指针走 1
            fast = fast.next.next;          // 快指针走 2
            if (slow == fast) return true;  // 相遇 = 有环
        }
        return false;                        // 快指针到头 = 无环
    }

    static class Node {
        int val; Node next;
        Node(int v) { val = v; }
    }

    public static void main(String[] args) {          // 复制本类即可直接运行
        Node head = new Node(5); Node b = new Node(2); Node c = new Node(9);
        Node d = new Node(1); Node e = new Node(5);
        head.next = b; b.next = c; c.next = d; d.next = e;
        e.next = b;                                    // 制造环：尾接回第二个
        System.out.println("有环吗？" + hasCycle(head));
    }
