public class PrimDemo {
    public static void main(String[] args) {
        int[][] edges = {{0,1,4},{0,2,2},{1,2,1},{1,3,5},{2,3,8},{2,4,10},{3,4,2},{3,5,6},{4,5,3}};
        int n = 6;
        boolean[] inMST = new boolean[n];
        inMST[0] = true;
        var mst = new java.util.ArrayList<int[]>();
        while (mst.size() < n - 1) {
            int bestW = Integer.MAX_VALUE, bf = -1, bt = -1;
            for (int[] e : edges) {
                if (inMST[e[0]] && !inMST[e[1]] && e[2] < bestW) { bf = e[0]; bt = e[1]; bestW = e[2]; }
                if (inMST[e[1]] && !inMST[e[0]] && e[2] < bestW) { bf = e[1]; bt = e[0]; bestW = e[2]; }
            }
            inMST[bt] = true;
            mst.add(new int[]{bf, bt, bestW});
        }
        for (int[] e : mst) System.out.println(e[0] + "-" + e[1] + " w=" + e[2]);
    }
}
