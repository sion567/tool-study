public class DoublyListDemo {
    // 双链表节点：prev 与 next 互为镜像——A.next=B ⟺ B.prev=A
    static class DNode {
        int val; DNode prev, next;
        DNode(int v) { val = v; }
    }

    public static void main(String[] args) {
        int[] init = {4, 9, 2};        //@DATA
        int insertVal = 7, deleteVal = 9;
        DNode head = null, tail = null;
        for (int v : init) {           // 尾插建表
            DNode n = new DNode(v);
            n.prev = tail;
            if (tail != null) tail.next = n; else head = n;
            tail = n;
        }
        // 中间插入：插在 9 之后——四根箭，一根都不能漏
        DNode after = head.next;                  // 9
        DNode n = new DNode(insertVal);
        n.prev = after; n.next = after.next;      // ① ② 新节点自己的两根
        if (after.next != null) after.next.prev = n;   // ③ 右邻.prev 回头
        after.next = n;                           // ④ 左邻.next 前指
        // 删除已知节点：O(1)——prev 现成，两根箭同时改道
        DNode del = head.next;                    // 9
        del.prev.next = del.next;                 // 左邻跳过它
        if (del.next != null) del.next.prev = del.prev;   // 右邻回头
        print(head);   // 4 ↔ 7 ↔ 2
    }

    static void print(DNode head) {
        StringBuilder sb = new StringBuilder();
        for (DNode c = head; c != null; c = c.next) sb.append(c.val).append(" ↔ ");
        System.out.println(sb.append("null"));
    }
}
