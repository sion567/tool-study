public class AcautoDemo {
    public static void main(String[] args) {
        String text = "ushers";   //@DATA 文本
        String[] patterns = {"he", "she", "his", "hers"};   //@DATA 模式
        // Trie：共享前缀只建一次
        java.util.Map<String, Integer> nodes = new java.util.LinkedHashMap<>();
        nodes.put("", 0);
        for (String p : patterns) {
            for (int i = 1; i <= p.length(); i++)
                nodes.putIfAbsent(p.substring(0, i), nodes.size());
            System.out.println("插入 " + p);
        }
        // fail：最长真后缀（she 的 fail = he）
        java.util.Map<String, String> fail = new java.util.LinkedHashMap<>();
        for (String node : nodes.keySet())
            if (!node.isEmpty())
                for (int i = 1; i < node.length(); i++)
                    if (nodes.containsKey(node.substring(i))) { fail.put(node, node.substring(i)); break; }
        // 扫描：文本指针只前进，mismatch 沿 fail 跳
        for (int i = 0; i < text.length(); i++)
            for (int j = 0; j <= i; j++)
                if (patterns.contains(text.substring(j, i + 1)))
                    System.out.println("命中 " + text.substring(j, i + 1) + " @ " + (i + 1));
    }
}
