public class SolidLspDemo {
    public static void main(String[] args) {
        // ===== 反例：Square extends Rectangle 撕契约 =====
        // Rectangle r = new Square();   // 以父类的方式用
        // r.setWidth(5); r.setHeight(4);
        // r.area() → 16 ≠ 20！调用方算错账
        //
        // ===== 正例：平行实现，可互换 =====
        Shape r = new Rect2(5, 4);
        Shape s = new Square2(4);
        System.out.println(r.area());   // 20 ✓
        System.out.println(s.area());   // 16 ✓（正方形就该这样）
    }
}
interface Shape { double area(); }
class Rect2 implements Shape {
    double w, h;
    Rect2(double w, double h) { this.w = w; this.h = h; }
    public double area() { return w * h; }
}
class Square2 implements Shape {
    double side;
    Square2(double side) { this.side = side; }
    public double area() { return side * side; }   // 不继承，不撕约
}
