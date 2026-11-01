public class ReverseList {
    static Node reverse(Node head) {
        Node prev = null;
        Node curr = head;
        while (curr != null) {
            Node next = curr.next;
            curr.next = prev;
            prev = curr;
            curr = next;
        }
        return prev;
    }

    static class Node {
        int val; Node next;
        Node(int v) { val = v; }
    }

    public static void main(String[] args) {          // 复制本类即可直接运行
        Node head = new Node(5); head.next = new Node(2); head.next.next = new Node(9);
        head.next.next.next = new Node(1); head.next.next.next.next = new Node(5);
        Node r = reverse(head);
        for (Node c = r; c != null; c = c.next) System.out.print(c.val + " ");
        System.out.println();
    }
