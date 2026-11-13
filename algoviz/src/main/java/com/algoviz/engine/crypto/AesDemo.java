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
 * 对称·AES-128（FIPS-197）：10 轮 SPN 现场——SubBytes/ShiftRows/MixColumns/AddRoundKey。
 * 状态 4×4 字节（列主序 in[r+4c]），每轮全状态替换（对照 SM4 的不均衡 Feistel：每轮只动 1/4）。
 * 行为测试断言 FIPS-197 附录 C.1 向量：key=000102…0f、plain=001122…ff → 69c4e0d8 6a7b0430 d8cdb780 70b4c55a。
 * 画面（math.matrix 复用）：4×4 状态字节网格（hex），黄圈 = 本轮变换涉及的格子——🎯 点选预测友好。
 */
public final class AesDemo implements Algorithm {

    private static final String SCHEMA = "1";
    private static final String INVARIANT =
            "SPN：每轮全状态替换（对照 SM4 每轮只动 1/4）；SubBytes 是唯一非线性，MixColumns 在 GF(2⁸) 上"
                    + "把 1 字节改动散满一列——10 轮后密文与明文逐位无关；解密 = 逆变换倒序";

    private static final int[] SBOX = {
            0x63, 0x7c, 0x77, 0x7b, 0xf2, 0x6b, 0x6f, 0xc5, 0x30, 0x01, 0x67, 0x2b, 0xfe, 0xd7, 0xab, 0x76,
            0xca, 0x82, 0xc9, 0x7d, 0xfa, 0x59, 0x47, 0xf0, 0xad, 0xd4, 0xa2, 0xaf, 0x9c, 0xa4, 0x72, 0xc0,
            0xb7, 0xfd, 0x93, 0x26, 0x36, 0x3f, 0xf7, 0xcc, 0x34, 0xa5, 0xe5, 0xf1, 0x71, 0xd8, 0x31, 0x15,
            0x04, 0xc7, 0x23, 0xc3, 0x18, 0x96, 0x05, 0x9a, 0x07, 0x12, 0x80, 0xe2, 0xeb, 0x27, 0xb2, 0x75,
            0x09, 0x83, 0x2c, 0x1a, 0x1b, 0x6e, 0x5a, 0xa0, 0x52, 0x3b, 0xd6, 0xb3, 0x29, 0xe3, 0x2f, 0x84,
            0x53, 0xd1, 0x00, 0xed, 0x20, 0xfc, 0xb1, 0x5b, 0x6a, 0xcb, 0xbe, 0x39, 0x4a, 0x4c, 0x58, 0xcf,
            0xd0, 0xef, 0xaa, 0xfb, 0x43, 0x4d, 0x33, 0x85, 0x45, 0xf9, 0x02, 0x7f, 0x50, 0x3c, 0x9f, 0xa8,
            0x51, 0xa3, 0x40, 0x8f, 0x92, 0x9d, 0x38, 0xf5, 0xbc, 0xb6, 0xda, 0x21, 0x10, 0xff, 0xf3, 0xd2,
            0xcd, 0x0c, 0x13, 0xec, 0x5f, 0x97, 0x44, 0x17, 0xc4, 0xa7, 0x7e, 0x3d, 0x64, 0x5d, 0x19, 0x73,
            0x60, 0x81, 0x4f, 0xdc, 0x22, 0x2a, 0x90, 0x88, 0x46, 0xee, 0xb8, 0x14, 0xde, 0x5e, 0x0b, 0xdb,
            0xe0, 0x32, 0x3a, 0x0a, 0x49, 0x06, 0x24, 0x5c, 0xc2, 0xd3, 0xac, 0x62, 0x91, 0x95, 0xe4, 0x79,
            0xe7, 0xc8, 0x37, 0x6d, 0x8d, 0xd5, 0x4e, 0xa9, 0x6c, 0x56, 0xf4, 0xea, 0x65, 0x7a, 0xae, 0x08,
            0xba, 0x78, 0x25, 0x2e, 0x1c, 0xa6, 0xb4, 0xc6, 0xe8, 0xdd, 0x74, 0x1f, 0x4b, 0xbd, 0x8b, 0x8a,
            0x70, 0x3e, 0xb5, 0x66, 0x48, 0x03, 0xf6, 0x0e, 0x61, 0x35, 0x57, 0xb9, 0x86, 0xc1, 0x1d, 0x9e,
            0xe1, 0xf8, 0x98, 0x11, 0x69, 0xd9, 0x8e, 0x94, 0x9b, 0x1e, 0x87, 0xe9, 0xce, 0x55, 0x28, 0xdf,
            0x8c, 0xa1, 0x89, 0x0d, 0xbf, 0xe6, 0x42, 0x68, 0x41, 0x99, 0x2d, 0x0f, 0xb0, 0x54, 0xbb, 0x16
    };
    private static final int[] RCON = {0x01, 0x02, 0x04, 0x08, 0x10, 0x20, 0x40, 0x80, 0x1b, 0x36};

    private final int[] key = new int[16];      // 16 字节

    private final List<Step> steps = new ArrayList<>();
    private int[] state = new int[16];          // 列主序：state[r+4c]
    private int[] w = new int[44];              // 44 个 32 位轮密钥字
    private int round;
    private String phase = "";

    public AesDemo() {
        for (int i = 0; i < 16; i++) {
            key[i] = i;                          // FIPS-197 C.1：key = 000102…0f
        }
    }

    /** 变体：换 16 字节密钥。 */
    public AesDemo(int[] keyBytes) {
        System.arraycopy(keyBytes, 0, key, 0, 16);
    }

    @Override public String id() { return "crypto.aes"; }
    @Override public String title() { return "对称·AES-128"; }
    @Override public String codeResource() { return "/code/AesDemo.java"; }
    @Override public String docResource() { return "/markdown/aes.md"; }

    @Override
    public Trace produce() {
        steps.clear();
        // 明文 = FIPS-197 C.1：00112233…ff
        state = new int[]{
                0x00, 0x11, 0x22, 0x33, 0x44, 0x55, 0x66, 0x77,
                0x88, 0x99, 0xaa, 0xbb, 0xcc, 0xdd, 0xee, 0xff};
        expandKey();
        phase = "密钥扩展";
        round = 0;
        emit(8, List.of(VizEvent.of(VizEvent.EventType.INIT,
                "密钥扩展完成：44 个轮密钥字（Rcon 逐轮常量 + RotWord/SubWord）")),
                "AES-128 = 128 位分组 10 轮 SPN：每轮全状态替换——对照 SM4 每轮只动 1/4");
        addRoundKey(0);
        phase = "AddRoundKey(0)";
        emit(13, List.of(VizEvent.of(VizEvent.EventType.MESSAGE,
                "初始轮：明文 ⊕ 密钥——之后的每一轮都在这个状态上搅动")),
                "白化轮：让密钥从一开始就参与，不给明文任何'裸露'的机会");
        for (int r = 1; r <= 10; r++) {
            round = r;
            phase = "第 " + r + " 轮";
            subBytes();
            shiftRows();
            if (r != 10) {
                mixColumns();
                emit(17, List.of(VizEvent.of(VizEvent.EventType.SET,
                        String.format("第 %d 轮：SubBytes → ShiftRows → MixColumns → AddRoundKey 完成", r))),
                        "扩散三连：字节替换（非线性）+ 行移位 + 列混淆（GF(2⁸) 线性）——1 字节改动散满全状态");
            } else {
                emit(20, List.of(VizEvent.of(VizEvent.EventType.SET,
                        "第 10 轮：没有 MixColumns——最后一轮的扩散已完成，省一次列变换")),
                        "末轮去 MixColumns：既不损失安全性，也节省一次矩阵乘法");
            }
            addRoundKey(r);
        }
        phase = "密文";
        emit(24, List.of(VizEvent.of(VizEvent.EventType.MARK_SORTED,
                String.format("密文 = %02x%02x%02x%02x %02x%02x%02x%02x %02x%02x%02x%02x %02x%02x%02x%02x",
                        state[0], state[1], state[2], state[3], state[4], state[5], state[6], state[7],
                        state[8], state[9], state[10], state[11], state[12], state[13], state[14], state[15]))),
                "FIPS-197 附录 C.1 的标准答案——实现正确性的最终裁判");
        emit(27, List.of(VizEvent.of(VizEvent.EventType.RETURN,
                "AES 与 SM4 同级对照：SPN 对 Feistel、代数式 S 盒对黑盒查表——两套设计哲学，同一安全强度")),
                "无线 Wi-Fi、磁盘加密、HTTPS——AES 是现代密码世界的承重墙");
        return new Trace(SCHEMA, meta(), steps);
    }

    /** 密钥扩展：44 字，每 4 字一轮 RotWord/SubWord/Rcon。 */
    private void expandKey() {
        for (int i = 0; i < 4; i++) {
            w[i] = ((key[4 * i] & 0xff) << 24) | ((key[4 * i + 1] & 0xff) << 16)
                    | ((key[4 * i + 2] & 0xff) << 8) | (key[4 * i + 3] & 0xff);
        }
        for (int i = 4; i < 44; i++) {
            int t = w[i - 1];
            if (i % 4 == 0) {
                int rot = ((t << 8) & 0xffffff00) | ((t >>> 24) & 0xff);   // RotWord：左旋一字节
                t = ((SBOX[(rot >>> 24) & 0xff] & 0xff) << 24) | ((SBOX[(rot >>> 16) & 0xff] & 0xff) << 16)
                        | ((SBOX[(rot >>> 8) & 0xff] & 0xff) << 8) | (SBOX[rot & 0xff] & 0xff);
                t ^= (RCON[i / 4 - 1] & 0xff) << 24;
            }
            w[i] = w[i - 4] ^ t;
        }
    }

    private void subBytes() {
        for (int i = 0; i < 16; i++) {
            state[i] = SBOX[state[i] & 0xff];
        }
    }

    private void shiftRows() {   // 行 r 循环左移 r：state[r][c] ← state[r][(c+r)%4]（列主序 s[4c+r]）
        int[] t = state.clone();
        for (int c = 0; c < 4; c++) {
            for (int r = 0; r < 4; r++) {
                state[4 * c + r] = t[4 * ((c + r) % 4) + r];
            }
        }
    }

    private void mixColumns() {  // 列混淆：GF(2⁸) 上乘 [2 3 1 1; 1 2 3 1; 1 1 2 3; 3 1 1 2]
        for (int c = 0; c < 4; c++) {
            int a0 = state[4 * c], a1 = state[4 * c + 1], a2 = state[4 * c + 2], a3 = state[4 * c + 3];
            state[4 * c] = mul(a0, 2) ^ mul(a1, 3) ^ a2 ^ a3;
            state[4 * c + 1] = a0 ^ mul(a1, 2) ^ mul(a2, 3) ^ a3;
            state[4 * c + 2] = a0 ^ a1 ^ mul(a2, 2) ^ mul(a3, 3);
            state[4 * c + 3] = mul(a0, 3) ^ a1 ^ a2 ^ mul(a3, 2);
        }
    }

    private void addRoundKey(int r) {
        for (int c = 0; c < 4; c++) {
            int k = w[4 * r + c];
            state[4 * c] ^= (k >>> 24) & 0xff;
            state[4 * c + 1] ^= (k >>> 16) & 0xff;
            state[4 * c + 2] ^= (k >>> 8) & 0xff;
            state[4 * c + 3] ^= k & 0xff;
        }
    }

    /** GF(2⁸) 乘法（模 x⁸+x⁴+x³+x+1）：俄式乘法 + xtime。 */
    private static int xtime(int a) {
        return ((a & 0x80) != 0) ? ((a << 1) ^ 0x1b) & 0xff : (a << 1) & 0xff;
    }

    private static int mul(int a, int b) {
        int r = 0;
        while (b > 0) {
            if ((b & 1) != 0) {
                r ^= a;
            }
            a = xtime(a);
            b >>= 1;
        }
        return r & 0xff;
    }

    /** 加密 128 位（供测试直接断言 FIPS-197 C.1 向量）。 */
    public int[] encrypt(int[] in) {
        int[] s = in.clone();
        expandKey();
        addRoundKeyArr(s, 0);
        for (int r = 1; r <= 10; r++) {
            for (int i = 0; i < 16; i++) {
                s[i] = SBOX[s[i] & 0xff];
            }
            int[] t = s.clone();
            for (int c = 0; c < 4; c++) {
                for (int rr = 0; rr < 4; rr++) {
                    s[4 * c + rr] = t[4 * ((c + rr) % 4) + rr];
                }
            }
            if (r != 10) {
                for (int c = 0; c < 4; c++) {
                    int a0 = s[4 * c], a1 = s[4 * c + 1], a2 = s[4 * c + 2], a3 = s[4 * c + 3];
                    s[4 * c] = mul(a0, 2) ^ mul(a1, 3) ^ a2 ^ a3;
                    s[4 * c + 1] = a0 ^ mul(a1, 2) ^ mul(a2, 3) ^ a3;
                    s[4 * c + 2] = a0 ^ a1 ^ mul(a2, 2) ^ mul(a3, 3);
                    s[4 * c + 3] = mul(a0, 3) ^ a1 ^ a2 ^ mul(a3, 2);
                }
            }
            addRoundKeyArr(s, r);
        }
        return s;
    }

    private void addRoundKeyArr(int[] s, int r) {
        for (int c = 0; c < 4; c++) {
            int k = w[4 * r + c];
            s[4 * c] ^= (k >>> 24) & 0xff;
            s[4 * c + 1] ^= (k >>> 16) & 0xff;
            s[4 * c + 2] ^= (k >>> 8) & 0xff;
            s[4 * c + 3] ^= k & 0xff;
        }
    }

    private void emit(int line, List<VizEvent> events, String why) {
        StackFrame fr = new StackFrame("aes",
                OrderedMaps.of("轮", Value.prim(round + " " + phase),
                        "state[0]", Value.prim(String.format("%02x", state[0])),
                        "state[15]", Value.prim(String.format("%02x", state[15]))), null);
        Map<String, Layer> layers = new LinkedHashMap<>();
        layers.put("mem.stack", new StackLayer(List.of(fr)));
        layers.put("mem.heap", new HeapLayer(snapshotHeap()));
        layers.put("math.matrix", snapshotMath());
        steps.add(new Step(steps.size(), Loc.line(line, why),
                Collections.unmodifiableMap(layers), List.copyOf(events), new Intent(why, INVARIANT)));
    }

    private Heap snapshotHeap() {
        Map<String, Value> fields = new LinkedHashMap<>();
        fields.put("阶段", Value.prim(phase));
        fields.put("轮", Value.prim(String.valueOf(round)));
        fields.put("密钥", Value.prim("000102…0f（FIPS-197 C.1）"));
        Map<String, HeapObject> objs = new LinkedHashMap<>();
        objs.put("@1", new HeapObject("@1", "aes", "CipherState",
                Collections.unmodifiableMap(fields), List.of()));
        return new Heap(Collections.unmodifiableMap(objs));
    }

    private MathView snapshotMath() {
        List<String> vals = new ArrayList<>();
        for (int b : state) {
            vals.add(String.format("%02x", b));
        }
        Set<Integer> active = new LinkedHashSet<>();
        for (int i = 0; i < 16; i++) {
            active.add(i);   // 每轮全状态替换——16 格全是本步变化格（🎯 预测：全变）
        }
        MathView.Grid grid = new MathView.Grid("state", "AES 状态 4×4 字节（列主序；每轮全状态替换）",
                4, 4, vals, null, null, active, Set.of());
        return new MathView("AES-128 轮函数现场", List.of(grid));
    }

    private Trace.TraceMeta meta() {
        return new Trace.TraceMeta(id(), title(), codeResource(),
                "FIPS-197 C.1", 10);
    }

    @Override
    public java.util.List<String> dataLines() {
        return java.util.List.of("int[] key = {0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15};   //@DATA 密钥");
    }

    @Override
    public Complexity complexity() {
        return new Complexity("O(10) 轮 × O(16) 状态替换", "O(1) 128 位状态",
                "磁盘加密（BitLocker）、HTTPS 套件、Wi-Fi WPA2——"
                        + "AES 与 SM4：SPN 全状态替换 vs 不均衡 Feistel 部分替换，殊途同归");
    }
}
