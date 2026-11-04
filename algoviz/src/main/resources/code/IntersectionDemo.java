public class IntersectionDemo {
    static class Node { int val; Node next; Node(int v) { val = v; } }
    public static void main(String[] args) {
        // A: 1→2→3→7→8→9  B: 4→5→7→8→9  交点 = 7
        Node c1 = new Node(7), c2 = new Node(8), c3 = new Node(9);
        Node a1 = new Node(1), a2 = new Node(2), a3 = new Node(3);
        a1.next = a2; a2.next = a3; a3.next = c1; c1.next = c2; c2.next = c3;
        Node b1 = new Node(4), b2 = new Node(5);
        b1.next = b2; b2.next = c1;
        // 双指针：走完自己的走对方的，总步数相同 → 同步到交点
        Node pA = a1, pB = b1;
        while (pA != pB) {
            pA = (pA != null) ? pA.next : b1.next;
            pB = (pB != null) ? pB.next : a1.next;
        }
        System.out.println("交点 = " + (pA != null ? pA.val : "null"));
    }
}
