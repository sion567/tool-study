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
        int[] vals = {5, 2, 9, 1, 5};                  //@DATA
        Node head = buildList(vals);
        Node r = reverse(head);
        for (Node c = r; c != null; c = c.next) System.out.print(c.val + " ");
        System.out.println();
    }

    static Node buildList(int[] vals) {
        Node head = new Node(vals[0]); Node cur = head;
        for (int i = 1; i < vals.length; i++) { cur.next = new Node(vals[i]); cur = cur.next; }
        return head;
    }

    @Override
    public java.util.List<String> dataLines() {
        return java.util.List.of(Algorithm.joinArray(input));
    }
}
