public class SolidOcpDemo {
    public static void main(String[] args) {
        // ===== 反例：switch 陷进（加三角形要改这里）=====
        // double area(Object shape) {
        //     switch (shape.type) {
        //         case CIRCLE:   return Math.PI * r * r;
        //         case TRIANGLE: return ...;      // ← 改历史代码！
        //     }
        // }
        //
        // ===== 正例：Shape 抽象 + 新类接上，计算器零修改 =====
        Shape circle = new Circle(2);
        Shape triangle = new Triangle(3, 4);
        System.out.println(area(circle));
        System.out.println(area(triangle));   // 新图形只是新类
    }
    static double area(Shape s) { return s.area(); }   // 对修改关闭
}
interface Shape { double area(); }
class Circle implements Shape {
    double r; Circle(double r) { this.r = r; }
    public double area() { return Math.PI * r * r; }
}
class Triangle implements Shape {
    double b, h; Triangle(double b, double h) { this.b = b; this.h = h; }
    public double area() { return b * h / 2; }         // 扩展开放：新类自己来
}
