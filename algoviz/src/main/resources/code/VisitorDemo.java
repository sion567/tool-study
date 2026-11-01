public class VisitorDemo {
    interface Shape { void accept(Visitor v); }          // 元素结构稳定
    static class Circle implements Shape {
        public void accept(Visitor v) { v.visitCircle(this); }   // 双重分发第一跳
    }
    static class Square implements Shape {
        public void accept(Visitor v) { v.visitSquare(this); }
    }
    interface Visitor {                                  // 操作可无限加
        void visitCircle(Circle c);
        void visitSquare(Square s);
    }
    static class DrawVisitor implements Visitor {
        public void visitCircle(Circle c) { System.out.println("画圆"); }
        public void visitSquare(Square s) { System.out.println("画方"); }
    }
    public static void main(String[] args) {
        var shapes = java.util.List.of(new Circle(), new Square());
        DrawVisitor v = new DrawVisitor();
        for (Shape s : shapes) s.accept(v);               // 结构在循环外，操作在 Visitor 里
    }
}
