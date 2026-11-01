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
}
