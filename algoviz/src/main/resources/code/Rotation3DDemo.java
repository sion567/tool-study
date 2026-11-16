public class Rotation3DDemo {
    // 立方体 8 顶点（±1,±1,±1），姿态 = 3×3 旋转矩阵 R；正交变换保范数（‖v‖≡√3）
    public static void main(String[] args) {
        double stepDeg = 30;   //@DATA 步进角（变体 15/30/45）
        double[][] rot = {{1, 0, 0}, {0, 1, 0}, {0, 0, 1}};   // 初始姿态 = 单位矩阵
        // 幕一：绕 Z 轴（平面旋转的推广——z 行列不动）
        rot = spinZ(rot, stepDeg);
        printVerts(rot, "绕 Z " + (int) stepDeg + "° 后");
        // 幕二：绕 X 轴（翻滚 pitch）
        rot = mul(rot, rotX(stepDeg));
        printVerts(rot, "再绕 X " + (int) stepDeg + "° 后");
        // 幕三：万向锁——外旋 R = Rz(φ)·Ry(90°)·Rx(ψ) 只依赖 (φ−ψ)
        double[][] a = mul(mul(rotZ(30), rotY(90)), rotX(0));     // (Z30, Y90, X0)
        double[][] b = mul(mul(rotZ(50), rotY(90)), rotX(20));    // (Z50, Y90, X20)
        System.out.println("万向锁：两组欧拉角姿态相同？ " + same(a, b) + "——两个输入一个输出，丢一个自由度");
        System.out.println("解法 = 换表示：四元数 4 个数、无奇异点、插值平滑（slerp）");
    }

    static double[][] spinZ(double[][] r, double deg) {           // 累计绕 Z 三步
        double[][] out = r;
        for (int k = 0; k < 3; k++) out = mul(out, rotZ(deg));
        return out;
    }

    static double[] mul(double[][] m, double[] v) {               // 3×3 矩阵乘向量
        return new double[]{
                m[0][0] * v[0] + m[0][1] * v[1] + m[0][2] * v[2],
                m[1][0] * v[0] + m[1][1] * v[1] + m[1][2] * v[2],
                m[2][0] * v[0] + m[2][1] * v[1] + m[2][2] * v[2]};
    }

    static double[][] mul(double[][] a, double[][] b) {           // 姿态复合 R = A·B
        double[][] r = new double[3][3];
        for (int i = 0; i < 3; i++)
            for (int j = 0; j < 3; j++)
                r[i][j] = a[i][0] * b[0][j] + a[i][1] * b[1][j] + a[i][2] * b[2][j];
        return r;
    }

    static double[][] rotX(double deg) {
        double c = Math.cos(Math.toRadians(deg)), s = Math.sin(Math.toRadians(deg));
        return new double[][]{{1, 0, 0}, {0, c, -s}, {0, s, c}};
    }

    static double[][] rotY(double deg) {
        double c = Math.cos(Math.toRadians(deg)), s = Math.sin(Math.toRadians(deg));
        return new double[][]{{c, 0, s}, {0, 1, 0}, {-s, 0, c}};
    }

    static double[][] rotZ(double deg) {
        double c = Math.cos(Math.toRadians(deg)), s = Math.sin(Math.toRadians(deg));
        return new double[][]{{c, -s, 0}, {s, c, 0}, {0, 0, 1}};
    }

    static boolean same(double[][] a, double[][] b) {             // 逐元素比较
        for (int i = 0; i < 3; i++)
            for (int j = 0; j < 3; j++)
                if (Math.abs(a[i][j] - b[i][j]) > 1e-9) return false;
        return true;
    }

    static void printVerts(double[][] r, String tag) {            // 8 顶点变换 + 范数自检
        System.out.println(tag);
        double[][] base = {{-1,-1,-1},{1,-1,-1},{1,1,-1},{-1,1,-1},{-1,-1,1},{1,-1,1},{1,1,1},{-1,1,1}};
        for (double[] v : base) {
            double[] w = mul(r, v);
            double n = Math.sqrt(w[0] * w[0] + w[1] * w[1] + w[2] * w[2]);
            System.out.printf("(%.2f, %.2f, %.2f)  ‖v‖=%.4f%n", w[0], w[1], w[2], n);
        }
    }
}
