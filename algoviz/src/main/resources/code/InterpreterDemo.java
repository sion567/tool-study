public class InterpreterDemo {
    interface Expr { int interpret(); }                 // 每条语法规则一个类
    record Num(int v) implements Expr {
        public int interpret() { return v; }
    }
    record Plus(Expr left, Expr right) implements Expr {
        public int interpret() { return left.interpret() + right.interpret(); }
    }
    public static void main(String[] args) {
        Expr e = new Plus(new Num(1), new Num(2));       // 语法树：1 + 2
        System.out.println(e.interpret());               // 后序遍历求值 = 3
    }
}
