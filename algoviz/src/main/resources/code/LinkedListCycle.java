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
        int[] vals = {5, 2, 9, 1, 5};                  //@DATA
        int anchor = 1;                                // 环接回的下标（0-based）
        Node head = buildCycle(vals, anchor);
        System.out.println("有环吗？" + hasCycle(head));
    }

    static Node buildCycle(int[] vals, int anchor) {
        Node[] ns = new Node[vals.length];
        for (int i = 0; i < ns.length; i++) ns[i] = new Node(vals[i]);
        for (int i = 0; i < ns.length - 1; i++) ns[i].next = ns[i + 1];
        ns[ns.length - 1].next = ns[anchor];
        return ns[0];
    }

    @Override
    public java.util.List<String> dataLines() {
        return java.util.List.of(Algorithm.joinArray(vals), "int anchor = " + anchorIdx + ";");
    }
}
