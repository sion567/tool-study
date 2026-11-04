public class TreeSerializeDemo {
    // LeetCode 297：前序 + "#" 占空位——写读同序，字符串里递归结构无歧义
    static class TreeNode {
        int val; TreeNode left, right;
        TreeNode(int v) { val = v; }
    }

    public static void main(String[] args) {
        TreeNode root = build();
        String data = serialize(root);
        System.out.println(data);                        // 5,3,1,#,#,4,#,#,8,7,#,#,9,#,#
        TreeNode rebuilt = deserialize(data);
        System.out.println(serialize(rebuilt));          // 再序列化 = 原串（往返一致）
    }

    static String serialize(TreeNode node) {
        if (node == null) return "#";                    // null 占位：没有它结构有歧义
        return node.val + "," + serialize(node.left) + "," + serialize(node.right);
    }

    static int pos;   // 读指针（真实代码建议用队列/迭代器）

    static TreeNode deserialize(String data) {
        String[] tokens = data.split(",");
        pos = 0;
        return read(tokens);
    }

    static TreeNode read(String[] tokens) {
        String t = tokens[pos++];
        if (t.equals("#")) return null;                  // 占位符还位
        TreeNode node = new TreeNode(Integer.parseInt(t));
        node.left = read(tokens);                        // 先读左——和写的顺序咬合
        node.right = read(tokens);
        return node;
    }

    static TreeNode build() {   // 5,3,8,1,4,7,9
        TreeNode n5 = new TreeNode(5), n3 = new TreeNode(3), n8 = new TreeNode(8);
        TreeNode n1 = new TreeNode(1), n4 = new TreeNode(4);
        TreeNode n7 = new TreeNode(7), n9 = new TreeNode(9);
        n5.left = n3; n5.right = n8;
        n3.left = n1; n3.right = n4;
        n8.left = n7; n8.right = n9;
        return n5;
    }
}
