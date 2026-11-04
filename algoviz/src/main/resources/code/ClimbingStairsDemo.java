public class ClimbingStairsDemo {
    public static void main(String[] args) {
        int n = 6;
        int a = 1, b = 1;
        for (int i = 2; i <= n; i++) { int t = a + b; a = b; b = t; }
        System.out.println("走到第 " + n + " 阶共 " + b + " 种方式");
    }
}
