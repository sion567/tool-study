public class CompositeDemo {
    interface Node { void draw(); }                     // 叶子与容器同接口
    static class Bar implements Node {
        public void draw() { System.out.println("画一根柱子"); }
    }
    static class Chart implements Node {                // 容器：装着一组 Node
        private final java.util.List<Node> children = new java.util.ArrayList<>();
        void add(Node n) { children.add(n); }
        public void draw() { for (Node n : children) n.draw(); }
    }
    public static void main(String[] args) {
        Chart root = new Chart();
        root.add(new Bar()); root.add(new Bar());       // 两根柱子（叶子）
        root.draw();                                    // 对整棵树只调一次
    }
}
