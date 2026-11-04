public class BitOpsDemo {
    public static void main(String[] args) {
        int a = 0b1100, b = 0b1010;
        System.out.println("A&B = " + (a & b));   // 8
        System.out.println("A|B = " + (a | b));   // 14
        System.out.println("A^B = " + (a ^ b));   // 6
        System.out.println("A<<1 = " + (a << 1)); // 24
        System.out.println("n&(n-1) 消最低位1: " + (a & (a - 1)));  // 8
    }
}
