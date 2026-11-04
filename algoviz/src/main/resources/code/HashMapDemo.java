import java.util.*;

public class HashMapDemo {
    // 手写最小哈希表：数组 + 冲突链（头插法，模拟 JDK 7 的做法）
    static class Entry {
        int key; Entry next;
        Entry(int k, Entry n) { key = k; next = n; }
    }

    static final int BUCKETS = 4;
    static Entry[] table = new Entry[BUCKETS];

    public static void main(String[] args) {
        int[] keys = {12, 7, 20, 3, 15};   //@DATA
        int query = 7;
        for (int k : keys) put(k);
        System.out.println("get(" + query + ") = " + get(query));
        // 撞车看这里：12%4=0 和 20%4=0 都进桶 0；7/3/15 都进桶 3 → 链长 3
    }

    static void put(int key) {
        int b = hash(key);                 // 定位 O(1)：数组下标直达
        if (table[b] != null) {
            System.out.println("put(" + key + ") 撞车@桶" + b + " → 头插进链");
        }
        table[b] = new Entry(key, table[b]);   // 头插：新节点指向旧链头
    }

    static boolean get(int key) {
        int b = hash(key);
        int cmp = 0;
        for (Entry e = table[b]; e != null; e = e.next) {
            cmp++;
            if (e.key == key) {
                System.out.println("走链 " + cmp + " 步命中");
                return true;               // 链比较 O(链长)
            }
        }
        return false;
    }

    static int hash(int key) { return key % BUCKETS; }
}
