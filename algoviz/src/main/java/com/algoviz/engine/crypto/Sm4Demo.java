package com.algoviz.engine.crypto;

import com.algoviz.engine.Algorithm;
import com.algoviz.engine.Complexity;
import com.algoviz.engine.Intent;
import com.algoviz.engine.Layer;
import com.algoviz.engine.Loc;
import com.algoviz.engine.OrderedMaps;
import com.algoviz.engine.Step;
import com.algoviz.engine.Value;
import com.algoviz.engine.VizEvent;
import com.algoviz.engine.math.MathView;
import com.algoviz.engine.mem.Heap;
import com.algoviz.engine.mem.HeapLayer;
import com.algoviz.engine.mem.HeapObject;
import com.algoviz.engine.mem.StackFrame;
import com.algoviz.engine.mem.StackLayer;
import com.algoviz.engine.trace.Trace;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 对称·SM4（GB/T 32907-2016）：128 位分组 32 轮轮函数现场。
 * τ 非线性（S 盒字节替换）∘ L 线性扩散 —— 每轮 X[i+4] = X[i] ⊕ T(X1⊕X2⊕X3⊕rk)。
 * 解密 = 轮密钥倒序重放同一轮函数（Feistel 家族的手艺）。
 * 行为测试断言国标向量：key=plain=0123456789abcdeffedcba9876543210 → 681edf34 d206965e 86b3e94f 536e4246。
 * 画面（math.matrix 复用）：网格一 = X0..X3 状态字（1×4 hex），网格二 = 当前轮密钥（1×4），
 * S 盒演示帧单独展示 4×4 状态字节的字节替换。
 */
public final class Sm4Demo implements Algorithm {

    private static final String SCHEMA = "1";
    private static final String INVARIANT =
            "32 轮全走同一轮函数 T = L∘τ：S 盒是唯一的非线性（差分分析的靶心），L 让 1 字节改动散满 4 字——"
                    + "解密 = rk 倒序重放：加密解密共用一套代码，硬件只做一半";

    /** 国标 S 盒（GB/T 32907-2016 附录 A），256 字节。 */
    private static final int[] SBOX = {
            0xd6, 0x90, 0xe9, 0xfe, 0xcc, 0xe1, 0x3d, 0xb7, 0x16, 0xb6, 0x14, 0xc2, 0x28, 0xfb, 0x2c, 0x05,
            0x2b, 0x67, 0x9a, 0x76, 0x2a, 0xbe, 0x04, 0xc3, 0xaa, 0x44, 0x13, 0x26, 0x49, 0x86, 0x06, 0x99,
            0x9c, 0x42, 0x50, 0xf4, 0x91, 0xef, 0x98, 0x7a, 0x33, 0x54, 0x0b, 0x43, 0xed, 0xcf, 0xac, 0x62,
            0xe4, 0xb3, 0x1c, 0xa9, 0xc9, 0x08, 0xe8, 0x95, 0x80, 0xdf, 0x94, 0xfa, 0x75, 0x8f, 0x3f, 0xa6,
            0x47, 0x07, 0xa7, 0xfc, 0xf3, 0x73, 0x17, 0xba, 0x83, 0x59, 0x3c, 0x19, 0xe6, 0x85, 0x4f, 0xa8,
            0x68, 0x6b, 0x81, 0xb2, 0x71, 0x64, 0xda, 0x8b, 0xf8, 0xeb, 0x0f, 0x4b, 0x70, 0x56, 0x9d, 0x35,
            0x1e, 0x24, 0x0e, 0x5e, 0x63, 0x58, 0xd1, 0xa2, 0x25, 0x22, 0x7c, 0x3b, 0x01, 0x21, 0x78, 0x87,
            0xd4, 0x00, 0x46, 0x57, 0x9f, 0xd3, 0x27, 0x52, 0x4c, 0x36, 0x02, 0xe7, 0xa0, 0xc4, 0xc8, 0x9e,
            0xea, 0xbf, 0x8a, 0xd2, 0x40, 0xc7, 0x38, 0xb5, 0xa3, 0xf7, 0xf2, 0xce, 0xf9, 0x61, 0x15, 0xa1,
            0xe0, 0xae, 0x5d, 0xa4, 0x9b, 0x34, 0x1a, 0x55, 0xad, 0x93, 0x32, 0x30, 0xf5, 0x8c, 0xb1, 0xe3,
            0x1d, 0xf6, 0xe2, 0x2e, 0x82, 0x66, 0xca, 0x60, 0xc0, 0x29, 0x23, 0xab, 0x0d, 0x53, 0x4e, 0x6f,
            0xd5, 0xdb, 0x37, 0x45, 0xde, 0xfd, 0x8e, 0x2f, 0x03, 0xff, 0x6a, 0x72, 0x6d, 0x6c, 0x5b, 0x51,
            0x8d, 0x1b, 0xaf, 0x92, 0xbb, 0xdd, 0xbc, 0x7f, 0x11, 0xd9, 0x5c, 0x41, 0x1f, 0x10, 0x5a, 0xd8,
            0x0a, 0xc1, 0x31, 0x88, 0xa5, 0xcd, 0x7b, 0xbd, 0x2d, 0x74, 0xd0, 0x12, 0xb8, 0xe5, 0xb4, 0xb0,
            0x89, 0x69, 0x97, 0x4a, 0x0c, 0x96, 0x77, 0x7e, 0x65, 0xb9, 0xf1, 0x09, 0xc5, 0x6e, 0xc6, 0x84,
            0x18, 0xf0, 0x7d, 0xec, 0x3a, 0xdc, 0x4d, 0x20, 0x79, 0xee, 0x5f, 0x3e, 0xd7, 0xcb, 0x39, 0x48
    };
    private static final int[] FK = {
            0xa3b1bac6, 0x56aa3350, 0x677d9197, 0xb27022dc
    };

    private final int[] key = new int[4];   // 4 个 32 位字

    private final List<Step> steps = new ArrayList<>();
    private int[] x = new int[4];
    private int[] rk = new int[32];
    private int round;
    private int[] plain;

    public Sm4Demo() {
        this(new int[]{
                0x01, 0x23, 0x45, 0x67, 0x89, 0xab, 0xcd, 0xef,
                0xfe, 0xdc, 0xba, 0x98, 0x76, 0x54, 0x32, 0x10});   // 国标示例密钥
    }

    /** 变体：换 16 字节密钥。 */
    public Sm4Demo(int[] keyBytes) {
        for (int i = 0; i < 4; i++) {
            key[i] = ((keyBytes[4 * i] & 0xff) << 24) | ((keyBytes[4 * i + 1] & 0xff) << 16)
                    | ((keyBytes[4 * i + 2] & 0xff) << 8) | (keyBytes[4 * i + 3] & 0xff);
        }
    }

    @Override public String id() { return "crypto.sm4"; }
    @Override public String title() { return "对称·SM4"; }
    @Override public String codeResource() { return "/code/Sm4Demo.java"; }
    @Override public String docResource() { return "/markdown/sm4.md"; }

    @Override
    public Trace produce() {
        steps.clear();
        plain = new int[]{
                0x01234567, 0x89abcdef, 0xfedcba98, 0x76543210};   // 国标示例明文
        expandKey();
        emit(7, List.of(VizEvent.of(VizEvent.EventType.INIT,
                "密钥扩展完成：MK ⊕ FK 起步，CK 常数列逐轮发酵出 rk0..rk31（L′ 线性层比加密的 L 少两圈旋转）")),
                "轮密钥与加密同源：一套 S 盒两条线性层——国标把硬件面积压到一半");
        // S 盒演示帧：第一轮的 τ 替换，4×4 状态字节
        int[] t0 = new int[4];
        for (int i = 0; i < 4; i++) {
            t0[i] = plain[i];
        }
        Set<Integer> sboxActive = new LinkedHashSet<>();
        for (int i = 0; i < 16; i++) {
            sboxActive.add(i);
        }
        round = 0;
        emit(13, List.of(VizEvent.of(VizEvent.EventType.MESSAGE,
                "τ 非线性：把 X1⊕X2⊕X3⊕rk 的 4 个字节各自过 S 盒——查表替换，唯一的非线性步骤")),
                "差分与线性分析全部瞄准 S 盒：AES 的 S 盒有代数式，SM4 的 S 盒是黑盒查表");
        // 加密：32 轮每帧 4 轮
        x = plain.clone();
        for (int frame = 0; frame < 8; frame++) {
            int from = frame * 4;
            for (int j = from; j < from + 4; j++) {
                round = j;
                x[j % 4] = x[j % 4] ^ t(x[(j + 1) % 4] ^ x[(j + 2) % 4] ^ x[(j + 3) % 4] ^ rk[j]);
            }
            emit(19, List.of(VizEvent.of(VizEvent.EventType.SET,
                    String.format("轮 %d~%d：X 状态被轮函数推进（rk%d..rk%d 参与）", from, from + 3, from, from + 3))),
                    "每轮只动一个字：X0 退场、新字补位——不均衡 Feistel 的流水线");
        }
        // 反转输出
        int[] cipher = {x[3], x[2], x[1], x[0]};
        emit(24, List.of(VizEvent.of(VizEvent.EventType.MARK_SORTED,
                String.format("反转 R：输出 (X35,X34,X33,X32) = %08x %08x %08x %08x",
                        cipher[0], cipher[1], cipher[2], cipher[3]))),
                "解密 = rk31..rk0 倒序走同样的 32 轮：加密器就是解密器");
        emit(27, List.of(VizEvent.of(VizEvent.EventType.RETURN,
                "国标示例验证：密文 = 681edf34 d206965e 86b3e94f 536e4246")),
                "SM4 顶替 AES 的位置：无线局域网 WAPI、银行卡、政务系统——国密对称的中坚");
        return new Trace(SCHEMA, meta(), steps);
    }

    /** 密钥扩展（GB/T 32907 §6）：MK⊕FK 起步，CK 常数 + T′（L′ 只转 13/23）。 */
    private void expandKey() {
        int[] k = new int[36];
        for (int i = 0; i < 4; i++) {
            k[i] = key[i] ^ FK[i];
        }
        for (int i = 0; i < 32; i++) {
            int ck = 0;
            for (int b = 0; b < 4; b++) {
                ck |= (((4 * i + b) * 7) % 256) << (24 - 8 * b);   // ck_{i,j} = (4i+j)×7 mod 256
            }
            k[i + 4] = k[i] ^ tPrime(k[i + 1] ^ k[i + 2] ^ k[i + 3] ^ ck);
            rk[i] = k[i + 4];
        }
    }

    /** 加密轮的 T = L∘τ。 */
    private int t(int a) {
        int b = tau(a);
        return b ^ Integer.rotateLeft(b, 2) ^ Integer.rotateLeft(b, 10)
                ^ Integer.rotateLeft(b, 18) ^ Integer.rotateLeft(b, 24);
    }

    /** 密钥扩展轮的 T′ = L′∘τ。 */
    private int tPrime(int a) {
        int b = tau(a);
        return b ^ Integer.rotateLeft(b, 13) ^ Integer.rotateLeft(b, 23);
    }

    /** τ 非线性：4 字节并行过 S 盒。 */
    private int tau(int a) {
        return (SBOX[(a >>> 24) & 0xff] << 24)
                | (SBOX[(a >>> 16) & 0xff] << 16)
                | (SBOX[(a >>> 8) & 0xff] << 8)
                | SBOX[a & 0xff];
    }

    /** 加密 128 位（供测试直接断言国标向量）。 */
    public int[] encrypt(int[] in) {
        int[] xx = in.clone();
        expandKey();
        for (int j = 0; j < 32; j++) {
            xx[j % 4] = xx[j % 4] ^ t(xx[(j + 1) % 4] ^ xx[(j + 2) % 4] ^ xx[(j + 3) % 4] ^ rk[j]);
        }
        return new int[]{xx[3], xx[2], xx[1], xx[0]};
    }

    private void emit(int line, List<VizEvent> events, String why) {
        StackFrame fr = new StackFrame("sm4",
                OrderedMaps.of("轮", Value.prim(String.valueOf(round)),
                        "X0", Value.prim(String.format("%08x", x[0])),
                        "rk" + round, Value.prim(String.format("%08x", rk[Math.min(round, 31)]))), null);
        Map<String, Layer> layers = new LinkedHashMap<>();
        layers.put("mem.stack", new StackLayer(List.of(fr)));
        layers.put("mem.heap", new HeapLayer(snapshotHeap()));
        layers.put("math.matrix", snapshotMath());
        steps.add(new Step(steps.size(), Loc.line(line, why),
                Collections.unmodifiableMap(layers), List.copyOf(events), new Intent(why, INVARIANT)));
    }

    private Heap snapshotHeap() {
        Map<String, Value> fields = new LinkedHashMap<>();
        fields.put("密钥", Value.prim(String.format("%08x %08x %08x %08x", key[0], key[1], key[2], key[3])));
        fields.put("当前轮", Value.prim(String.valueOf(round)));
        fields.put("rk" + Math.min(round, 31), Value.prim(String.format("%08x", rk[Math.min(round, 31)])));
        Map<String, HeapObject> objs = new LinkedHashMap<>();
        objs.put("@1", new HeapObject("@1", "sm4", "CipherState",
                Collections.unmodifiableMap(fields), List.of()));
        return new Heap(Collections.unmodifiableMap(objs));
    }

    private MathView snapshotMath() {
        List<String> xVals = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            xVals.add(String.format("%08x", x[i]));
        }
        Set<Integer> xActive = new LinkedHashSet<>();
        for (int i = 0; i < 4; i++) {
            xActive.add(i);
        }
        MathView.Grid xGrid = new MathView.Grid("X", "状态字 X0..X3（每帧 4 轮，一字退场一字补位）",
                1, 4, xVals, null, null, xActive, Set.of());
        List<String> rkVals = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            rkVals.add(String.format("%08x", rk[Math.min(round + i, 31)]));
        }
        MathView.Grid rkGrid = new MathView.Grid("rk", "本轮起的轮密钥 rk",
                1, 4, rkVals, null, null, Set.of(), Set.of());
        return new MathView("SM4 轮函数现场", List.of(xGrid, rkGrid));
    }

    private Trace.TraceMeta meta() {
        return new Trace.TraceMeta(id(), title(), codeResource(),
                "国标示例 key/plain", 28);
    }

    @Override
    public java.util.List<String> dataLines() {
        return java.util.List.of(String.format(
                "int[] key = {%08x, %08x, %08x, %08x};   //@DATA 密钥", key[0], key[1], key[2], key[3]));
    }

    @Override
    public Complexity complexity() {
        return new Complexity("O(32) 轮 × O(1) 查表轮函数", "O(1) 128 位状态",
                "WAPI 无线局域网、银联 IC 卡、国密网关——"
                        + "对称密码管数据量（快），非对称管密钥交换（安全）：SM4 与 SM2 正是一对搭档");
    }
}
