public class FastPowerDemo {
    public static void main(String[] args) {
        System.out.println("3^13 = " + fastPower(3, 13));
    }
    static long fastPower(int base, int exp) {
        long result = 1;
        while (exp > 0) {
            if ((exp & 1) == 1) result *= base;
            base *= base;
            exp >>= 1;
        }
        return result;
    }
}
