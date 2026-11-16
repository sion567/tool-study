public class DtreeDemo {
    public static void main(String[] args) {
        String[][] data = {{"晴","弱","否"},{"晴","强","否"},{"阴","弱","是"},{"雨","弱","是"},{"雨","强","否"},{"阴","强","是"},{"阴","弱","是"},{"雨","弱","否"}};   //@DATA 精简 play-tennis
        double hS = entropy(count(data, 2, "是"), count(data, 2, "否"));
        System.out.printf("H(S) = %.3f%n", hS);
        for (int feat = 0; feat < 2; feat++) {       // 每个特征算条件熵 → 增益
            double cond = 0;
            for (String val : new String[]{"晴", "阴", "雨", "弱", "强"}) {
                int yes = 0, no = 0;
                for (String[] row : data)
                    if (row[feat].equals(val)) { if (row[2].equals("是")) yes++; else no++; }
                if (yes + no == 0) continue;
                cond += (yes + no) / (double) data.length * entropy(yes, no);
            }
            System.out.printf("增益(%s) = %.3f%n", feat == 0 ? "天候" : "风力", hS - cond);
        }
        // 增益最大的特征当选根（天候 0.311 > 风力 0.049）——ID3 的贪心
    }

    static int count(String[][] d, int col, String v) {
        int n = 0;
        for (String[] row : d) if (row[col].equals(v)) n++;
        return n;
    }

    static double entropy(int yes, int no) {
        int total = yes + no;
        if (total == 0) return 0;
        double p = yes / (double) total, q = no / (double) total;
        if (p == 0 || q == 0) return 0;
        return -(p * Math.log(p) + q * Math.log(q)) / Math.log(2);
    }
}
