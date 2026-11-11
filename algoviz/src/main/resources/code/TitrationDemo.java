public class TitrationDemo {
    public static void main(String[] args) {
        double ca = 0.10, cb = 0.10;   //@DATA 酸碱浓度
        double va = 25.0, pKa = 4.76;
        double veq = va * ca / cb;     // 等价点体积：Va·Ca/Cb
        for (int d = 0; d <= 60; d++) {
            double vb = 0.5 * d;       // 每 0.5mL 一滴
            double ph;
            if (vb < 0.01) ph = 0.5 * (pKa - Math.log10(ca));            // 纯弱酸
            else if (vb < veq - 0.01) ph = pKa + Math.log10(vb / (veq - vb));   // 缓冲区 HH 式
            else if (vb <= veq + 0.01) ph = 7 + 0.5 * (pKa + Math.log10(ca));   // 等价点：水解 pH>7
            else ph = 14 + Math.log10((vb - veq) * cb / (va + vb));      // 过量 OH⁻
            System.out.printf("Vb=%.1f pH=%.2f%n", vb, ph);
        }
        // 半等价点 pH=pKa（缓冲区酸碱各半）；等价点 pH>7 是弱酸根水解的证据——拐点一锤定音
    }
}
