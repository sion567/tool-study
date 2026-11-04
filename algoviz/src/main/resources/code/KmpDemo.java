public class KmpDemo {
    public static void main(String[] args) {
        String text = "ABABDABACDABABCABAB";
        String pattern = "ABABCABAB";
        int[] next = new int[pattern.length()];
        // 构建前缀表
        int len = 0;
        for (int i = 1; i < pattern.length(); i++) {
            while (len > 0 && pattern.charAt(i) != pattern.charAt(len)) len = next[len - 1];
            if (pattern.charAt(i) == pattern.charAt(len)) len++;
            next[i] = len;
        }
        // 搜索
        int j = 0;
        for (int i = 0; i < text.length(); i++) {
            while (j > 0 && text.charAt(i) != pattern.charAt(j)) j = next[j - 1];
            if (text.charAt(i) == pattern.charAt(j)) j++;
            if (j == pattern.length()) {
                System.out.println("匹配位置: " + (i - j + 1));
                j = next[j - 1];
            }
        }
    }
}
