package com.algoviz.engine.registry;

import com.algoviz.engine.Algorithm;
import com.algoviz.engine.AlgorithmRegistry;

import java.util.List;

/** 「密码学」域：国密三件套 + AES + DES + SHA-256。 */
public final class CryptoSection implements AlgorithmSection {

    @Override
    public List<AlgorithmRegistry.Entry> entries() {
        return List.of(
                new AlgorithmRegistry.Entry("密码学", "哈希", new com.algoviz.engine.crypto.Sm3Demo()),
                new AlgorithmRegistry.Entry("密码学", "对称", new com.algoviz.engine.crypto.Sm4Demo()),
                new AlgorithmRegistry.Entry("密码学", "非对称", new com.algoviz.engine.crypto.Sm2Demo()),
                new AlgorithmRegistry.Entry("密码学", "对称", new com.algoviz.engine.crypto.AesDemo()),
                new AlgorithmRegistry.Entry("密码学", "对称", new com.algoviz.engine.crypto.DesDemo()),
                new AlgorithmRegistry.Entry("密码学", "哈希", new com.algoviz.engine.crypto.Sha256Demo())
        );
    }

    @Override
    public Algorithm variantOf(String id, int n, java.util.Random r) {
        return switch (id) {
            case "crypto.sm3" -> new com.algoviz.engine.crypto.Sm3Demo(
                    new String[]{"abc", "国密算法", "SM3 test"}[r.nextInt(3)]);
            case "crypto.sm4" -> {
                int[] kb = new int[16];
                for (int i = 0; i < 16; i++) {
                    kb[i] = r.nextInt(256);                        // 随机 16 字节密钥
                }
                yield new com.algoviz.engine.crypto.Sm4Demo(kb);
            }
            case "crypto.aes" -> {
                int[] kb = new int[16];
                for (int i = 0; i < 16; i++) {
                    kb[i] = r.nextInt(256);                        // 随机 16 字节密钥
                }
                yield new com.algoviz.engine.crypto.AesDemo(kb);
            }
            case "crypto.des" -> new com.algoviz.engine.crypto.DesDemo(
                    new long[]{0x0E329232EA6D0D73L, 0x7CA110454A1A6E57L,
                            0x1313131313131313L}[r.nextInt(3)]);                 // 换密钥变体（NIST 测试密钥）
            case "crypto.sha256" -> new com.algoviz.engine.crypto.Sha256Demo(
                    new String[]{"abc", "SHA-256", "哈希对照"}[r.nextInt(3)]);
            default -> null;
        };
    }
}
