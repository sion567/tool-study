package com.algoviz.engine.registry;

import com.algoviz.engine.Algorithm;
import com.algoviz.engine.AlgorithmRegistry;

import java.util.List;

/** 「数理」域：数学（几何/微积分/离散/概率/信号）+ 物理 + 化学 + 奥数。 */
public final class MathSection implements AlgorithmSection {

    @Override
    public List<AlgorithmRegistry.Entry> entries() {
        return List.of(
                new AlgorithmRegistry.Entry("数学", "几何坐标", new com.algoviz.engine.math.UnitCircleDemo()),
                new AlgorithmRegistry.Entry("数学", "微积分", new com.algoviz.engine.math.TaylorDemo()),
                new AlgorithmRegistry.Entry("数学", "微积分", new com.algoviz.engine.math.TaylorDemo(true)),   // cos 版,
                new AlgorithmRegistry.Entry("数学", "微积分", new com.algoviz.engine.math.DerivativeDemo()),
                new AlgorithmRegistry.Entry("数学", "微积分", new com.algoviz.engine.math.RiemannSumDemo()),
                new AlgorithmRegistry.Entry("数学", "几何坐标", new com.algoviz.engine.math.CardioidDemo()),
                new AlgorithmRegistry.Entry("数学", "图论离散", new com.algoviz.engine.math.YangHuiDemo()),
                new AlgorithmRegistry.Entry("数学", "线性代数", new com.algoviz.engine.math.GaussianElimDemo()),
                new AlgorithmRegistry.Entry("数学", "图论离散", new com.algoviz.engine.math.HanoiDemo()),
                new AlgorithmRegistry.Entry("数学", "概率统计", new com.algoviz.engine.math.MonteCarloPiDemo()),
                new AlgorithmRegistry.Entry("数学", "图论离散", new com.algoviz.engine.math.SevenBridgesDemo()),
                new AlgorithmRegistry.Entry("数学", "数与计算", new com.algoviz.engine.math.RsaMiniDemo()),
                new AlgorithmRegistry.Entry("数学", "几何坐标", new com.algoviz.engine.math.GoldenSpiralDemo()),
                new AlgorithmRegistry.Entry("数学", "几何坐标", new com.algoviz.engine.math.RoseCurveGalleryDemo()),
                new AlgorithmRegistry.Entry("数学", "几何坐标", new com.algoviz.engine.math.Rotation2DDemo()),
            new AlgorithmRegistry.Entry("数学", "线性代数", new com.algoviz.engine.math.Rotation3DDemo()),
                new AlgorithmRegistry.Entry("数学", "微积分", new com.algoviz.engine.math.PendulumDemo()),
                new AlgorithmRegistry.Entry("数学", "数与计算", new com.algoviz.engine.math.BisectionDemo()),
                new AlgorithmRegistry.Entry("数学", "微积分", new com.algoviz.engine.math.CompoundInterestDemo()),
                new AlgorithmRegistry.Entry("数学", "数与计算", new com.algoviz.engine.math.FibMatrixDemo()),
                new AlgorithmRegistry.Entry("数学", "概率统计", new com.algoviz.engine.math.DiceBellDemo()),
                new AlgorithmRegistry.Entry("数学", "概率统计", new com.algoviz.engine.math.GamblersRuinDemo()),
                new AlgorithmRegistry.Entry("数学", "数与计算", new com.algoviz.engine.math.FastPowerDemo()),
                new AlgorithmRegistry.Entry("数学", "数与计算", new com.algoviz.engine.math.GcdDemo()),
                new AlgorithmRegistry.Entry("数学", "图论离散", new com.algoviz.engine.math.BloomFilterDemo()),
                new AlgorithmRegistry.Entry("数学", "数与计算", new com.algoviz.engine.math.BitOpsDemo()),
                new AlgorithmRegistry.Entry("数学", "数与计算", new com.algoviz.engine.math.SieveDemo()),
                new AlgorithmRegistry.Entry("数学", "数与计算", new com.algoviz.engine.math.NewtonDemo()),
                new AlgorithmRegistry.Entry("数学", "数与计算", new com.algoviz.engine.math.CordicDemo()),
                new AlgorithmRegistry.Entry("数学", "图论离散", new com.algoviz.engine.math.BucketDemo()),
                new AlgorithmRegistry.Entry("数学", "图论离散", new com.algoviz.engine.math.HashMapDemo()),
                new AlgorithmRegistry.Entry("数学", "信号处理", new com.algoviz.engine.math.FourierDemo()),
                new AlgorithmRegistry.Entry("数学", "几何坐标", new com.algoviz.engine.math.SpiralDemo()),
                new AlgorithmRegistry.Entry("物理", "力学", new com.algoviz.engine.physics.ProjectileDemo()),
                new AlgorithmRegistry.Entry("物理", "力学", new com.algoviz.engine.physics.SpringDemo()),
                new AlgorithmRegistry.Entry("物理", "力学", new com.algoviz.engine.physics.DoublePendulumDemo()),
                new AlgorithmRegistry.Entry("物理", "热与分子", new com.algoviz.engine.physics.HeatDemo()),
                new AlgorithmRegistry.Entry("物理", "热与分子", new com.algoviz.engine.physics.GasDemo()),
                new AlgorithmRegistry.Entry("物理", "天体", new com.algoviz.engine.physics.NBodyDemo()),
                new AlgorithmRegistry.Entry("奥数", "组合构造", new com.algoviz.engine.olympiad.KnightsTourDemo()),
                new AlgorithmRegistry.Entry("奥数", "组合构造", new com.algoviz.engine.olympiad.JosephusDemo()),
                new AlgorithmRegistry.Entry("奥数", "数论", new com.algoviz.engine.olympiad.CrtDemo()),
                new AlgorithmRegistry.Entry("奥数", "不变量", new com.algoviz.engine.olympiad.FifteenPuzzleDemo()),
                new AlgorithmRegistry.Entry("化学", "反应过程", new com.algoviz.engine.chem.KineticsDemo()),
                new AlgorithmRegistry.Entry("化学", "反应过程", new com.algoviz.engine.chem.EquilibriumDemo()),
                new AlgorithmRegistry.Entry("化学", "反应过程", new com.algoviz.engine.chem.TitrationDemo()),
                new AlgorithmRegistry.Entry("化学", "分子结构", new com.algoviz.engine.chem.IsomerDemo()),
                new AlgorithmRegistry.Entry("数学", "数与计算", new com.algoviz.engine.math.KaratsubaDemo()),
                new AlgorithmRegistry.Entry("数学", "微积分", new com.algoviz.engine.math.TrapezoidDemo()),
                new AlgorithmRegistry.Entry("数学", "数与计算", new com.algoviz.engine.math.BigIntDemo()),
                new AlgorithmRegistry.Entry("数学", "数与计算", new com.algoviz.engine.math.MathLogDemo()),
                new AlgorithmRegistry.Entry("数学", "线性代数", new com.algoviz.engine.math.MatrixMultiply()),
                new AlgorithmRegistry.Entry("数学", "图论离散", new com.algoviz.engine.math.BitmapDemo())
        );
    }

    @Override
    public Algorithm variantOf(String id, int n, java.util.Random r) {
        return switch (id) {
            case "math.rotation3d" -> new com.algoviz.engine.math.Rotation3DDemo(
                    new int[]{15, 30, 45}[r.nextInt(3)]);                       // 步进角档位（轨迹粗细）
            case "math.log" -> new com.algoviz.engine.math.MathLogDemo(
                    new int[]{100, 1000, 1000000}[r.nextInt(3)]);              // 换猜数上限（砍半序列/阶梯 n 同步变）
            case "math.gcd" -> new com.algoviz.engine.math.GcdDemo(12 + r.nextInt(988), 12 + r.nextInt(988));
            case "math.fastpower" -> new com.algoviz.engine.math.FastPowerDemo(2 + r.nextInt(8), 10 + r.nextInt(21));
            case "math.sieve" -> new com.algoviz.engine.math.SieveDemo(20 + r.nextInt(41));
            case "math.hashmap" -> {
                int[] keys = new int[5 + r.nextInt(4)];
                for (int i = 0; i < keys.length; i++) keys[i] = 1 + r.nextInt(99);
                yield new com.algoviz.engine.math.HashMapDemo(keys, keys[keys.length - 1]);   // 查询最后一个：走链有戏
            }
            case "math.bucket" -> {
                int[] data = new int[6 + r.nextInt(4)];
                for (int i = 0; i < data.length; i++) data[i] = r.nextInt(1024);
                data[data.length - 1] = data[0];                      // 保证有一个重复
                yield new com.algoviz.engine.math.BucketDemo(data);
            }
            case "physics.projectile" -> new com.algoviz.engine.physics.ProjectileDemo(
                    20 + r.nextInt(11), 30 + r.nextInt(31));   // v₀ 20~30，θ 30°~60°
            case "physics.nbody" -> new com.algoviz.engine.physics.NBodyDemo(1L + r.nextInt(9999));
            case "physics.gas" -> new com.algoviz.engine.physics.GasDemo(1L + r.nextInt(9999));
            // ---- 奥数名题 ----
            case "olympiad.knights" -> {
                int[][] starts = {{0, 0}, {0, 7}, {7, 0}, {7, 7}};   // 四角起点（验证可走完全程）
                int[] s = starts[r.nextInt(starts.length)];
                yield new com.algoviz.engine.olympiad.KnightsTourDemo(s[0], s[1]);
            }
            case "olympiad.josephus" -> new com.algoviz.engine.olympiad.JosephusDemo(
                    8 + r.nextInt(7), 2 + r.nextInt(3));               // n 8~14，k 2~4
            case "olympiad.crt" -> {
                int[] primes = {3, 5, 7, 11};
                int i1 = r.nextInt(primes.length);
                int i2 = r.nextInt(primes.length);
                while (i2 == i1) {
                    i2 = r.nextInt(primes.length);   // 两模数必须不同（互质素数 → CRT 解恒存在）
                }
                int p1 = primes[i1], p2 = primes[i2];
                int aa = 1 + r.nextInt(p1 - 1), bb = 1 + r.nextInt(p2 - 1);
                yield new com.algoviz.engine.olympiad.CrtDemo(aa, p1, bb, p2);
            }
            case "olympiad.fifteen" -> new com.algoviz.engine.olympiad.FifteenPuzzleDemo(1L + r.nextInt(9999));
            // ---- 化学过程类 ----
            case "chem.kinetics" -> new com.algoviz.engine.chem.KineticsDemo(
                    0.05 + 0.05 * r.nextInt(8));                       // k₁ 0.05~0.40
            case "chem.equilibrium" -> new com.algoviz.engine.chem.EquilibriumDemo(
                    0.3 + 0.15 * r.nextInt(4));                        // 扰动注入 0.3~0.75
            case "chem.titration" -> new com.algoviz.engine.chem.TitrationDemo(
                    new double[]{0.05, 0.10, 0.20}[r.nextInt(3)]);     // 酸浓度档位
            case "chem.isomer" -> new com.algoviz.engine.chem.IsomerDemo(
                    1 + r.nextInt(3));                                 // C₅ 三兄弟随机出场
            // ---- 密码学 ----
            default -> null;
        };
    }
}
