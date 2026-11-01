public class BridgeDemo {
    interface Color { String apply(); }                 // 独立变化的一维
    static class Red implements Color { public String apply() { return "红"; } }
    static class Blue implements Color { public String apply() { return "蓝"; } }
    abstract static class Shape {                       // 另一维：持有桥（引用）
        protected Color color;                          // ← 这根引用就是桥
        Shape(Color c) { color = c; }
        abstract void draw();
    }
    static class Circle extends Shape {
        Circle(Color c) { super(c); }
        void draw() { System.out.println(color.apply() + " 的圆"); }
    }
    static class Square extends Shape {
        Square(Color c) { super(c); }
        void draw() { System.out.println(color.apply() + " 的方"); }
    }
    public static void main(String[] args) {
        Shape s1 = new Circle(new Red());
        Shape s2 = new Square(new Blue());
        s1.draw(); s2.draw();                           // 2+2 个类，4 种组合
    }
}
