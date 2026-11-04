public class BloomFilterDemo {
    public static void main(String[] args) {
        boolean[] bits = new boolean[16];
        // 添加
        for (String s : new String[]{"apple", "banana"}) {
            bits[Math.abs(s.hashCode()) % 16] = true;
        }
        // 查询
        for (String s : new String[]{"apple", "cherry", "banana"}) {
            int h = Math.abs(s.hashCode()) % 16;
            System.out.println(s + " → " + (bits[h] ? "可能存在" : "一定不存在"));
        }
    }
}
