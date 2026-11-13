public class ResNetDemo {
    public static void main(String[] args) {
        int layers = 10;   //@DATA 每层局部导数 0.3
        double local = 0.3, plain = 1, res = 1;
        for (int i = 1; i <= layers; i++) {
            plain *= local;              // 普通深网：梯度连乘 → 指数衰减
            res *= local + 1;            // 残差网：∂L/∂x = ∂L/∂y·(F'(x)+1)——'+1' 直通
            System.out.printf("层%d 普通=%.2e 残差=%.1f%n", i, plain, res);
        }
        // 退化现象：56 层比 20 层训练误差还高——不是过拟合，是深层根本训不动
        // 残差连接与 LSTM 的 c 传送带同源：给梯度开一条"不乘"的高速路
    }
}
