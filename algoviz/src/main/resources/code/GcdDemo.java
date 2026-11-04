public class GcdDemo {
    public static void main(String[] args) {
        System.out.println("gcd(48, 18) = " + gcd(48, 18));
    }
    static int gcd(int a, int b) {
        while (b != 0) {
            int t = a % b;
            a = b;
            b = t;
        }
        return a;
    }
}
