public class ListBuildDemo {
    static class Node {
        int val; Node next;
        Node(int v) { val = v; }
    }

    public static void main(String[] args) {
        int[] data = {1, 2, 3};   //@DATA

        // 头插法：O(1) 接入，但结果逆序
        Node head = null;
        for (int v : data) {
            Node n = new Node(v);
            n.next = head;        // ① 新节点先牵住旧链头
            head = n;             // ② head 挪到新节点（顺序不能反！）
        }
        print("头插: ", head);    // 3 → 2 → 1

        // 尾插法：保序，但要养一个 tail 指针
        head = null; Node tail = null;
        for (int v : data) {
            Node n = new Node(v);
            if (tail == null) { head = tail = n; }
            else {
                tail.next = n;    // ① tail 牵住新节点
                tail = n;         // ② tail 挪到新队尾
            }
        }
        print("尾插: ", head);    // 1 → 2 → 3
    }

    static void print(String tag, Node head) {
        StringBuilder sb = new StringBuilder(tag);
        for (Node c = head; c != null; c = c.next) sb.append(c.val).append(" → ");
        System.out.println(sb.append("null"));
    }
}
