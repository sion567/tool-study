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
 * 对称·DES（FIPS 46，1977）：16 轮 Feistel 历史线--对照 SM4 的 32 轮不均衡 Feistel。
 * 经典例（Grabbe 教程）：key=133457799BBCDFF1、plain=0123456789ABCDEF → 密文 85E813540F0AB405。
 * IP/FP/E/P/PC1/PC2/S1-S8 全表实现（位操作用 int[64] 0/1 数组--置换直白不易错）。
 * 画面（math.matrix 复用）：L/R 1×2 hex 状态格（对照 SM4 同款）+ 当前轮密钥。
 */
public final class DesDemo implements Algorithm {

    private static final String SCHEMA = "1";
    private static final String INVARIANT =
            "16 轮 Feistel：L(i)=R(i-1)、R(i)=L(i-1)⊕f(R,K)--f=S盒(P(E(R)⊕K))；"
                    + "解密 = 子密钥倒序重放（Feistel 不要求 f 可逆）--56 位密钥 1998 年被 EFF 22 小时破解";

    private static final int[] IP = {58,50,42,34,26,18,10,2,60,52,44,36,28,20,12,4,62,54,46,38,30,22,14,6,64,56,48,40,32,24,16,8,
            57,49,41,33,25,17,9,1,59,51,43,35,27,19,11,3,61,53,45,37,29,21,13,5,63,55,47,39,31,23,15,7};
    private static final int[] FP = {40,8,48,16,56,24,64,32,39,7,47,15,55,23,63,31,38,6,46,14,54,22,62,30,37,5,45,13,53,21,61,29,
            36,4,44,12,52,20,60,28,35,3,43,11,51,19,59,27,34,2,42,10,50,18,58,26,33,1,41,9,49,17,57,25};
    private static final int[] ETAB = {32,1,2,3,4,5,4,5,6,7,8,9,8,9,10,11,12,13,12,13,14,15,16,17,16,17,18,19,20,21,
            20,21,22,23,24,25,24,25,26,27,28,29,28,29,30,31,32,1};
    private static final int[] PTAB = {16,7,20,21,29,12,28,17,1,15,23,26,5,18,31,10,2,8,24,14,32,27,3,9,19,13,30,6,22,11,4,25};
    private static final int[] PC1 = {57,49,41,33,25,17,9,1,58,50,42,34,26,18,10,2,59,51,43,35,27,19,11,3,60,52,44,36,
            63,55,47,39,31,23,15,7,62,54,46,38,30,22,14,6,61,53,45,37,29,21,13,5,28,20,12,4};
    private static final int[] PC2 = {14,17,11,24,1,5,3,28,15,6,21,10,23,19,12,4,26,8,16,7,27,20,13,2,
            41,52,31,37,47,55,30,40,51,45,33,48,44,49,39,56,34,53,46,42,50,36,29,32};
    private static final int[] SHIFTS = {1,1,2,2,2,2,2,2,1,2,2,2,2,2,2,1};
    private static final int[][] SBOX = {
            {14,4,13,1,2,15,11,8,3,10,6,12,5,9,0,7},
            {0,15,7,4,14,2,13,1,10,6,12,11,9,5,3,8},
            {4,1,14,8,13,6,2,11,15,12,9,7,3,10,5,0},
            {15,12,8,2,4,9,1,7,5,11,3,14,10,0,6,13},
            {15,1,8,14,6,11,3,4,9,7,2,13,12,0,5,10},
            {3,13,4,7,15,2,8,14,12,0,1,10,6,9,11,5},
            {0,14,7,11,10,4,13,1,5,8,12,6,9,3,2,15},
            {13,8,10,1,3,15,4,2,11,6,7,12,0,5,14,9},
            {10,0,9,14,6,3,15,5,1,13,12,7,11,4,2,8},
            {13,7,0,9,3,4,6,10,2,8,5,14,12,11,15,1},
            {13,6,4,9,8,15,3,0,11,1,2,12,5,10,14,7},
            {1,10,13,0,6,9,8,7,4,15,14,3,11,5,2,12},
            {7,13,14,3,0,6,9,10,1,2,8,5,11,12,4,15},
            {13,8,11,5,6,15,0,3,4,7,2,12,1,10,14,9},
            {10,6,9,0,12,11,7,13,15,1,3,14,5,2,8,4},
            {3,15,0,6,10,1,13,8,9,4,5,11,12,7,2,14},
            {2,12,4,1,7,10,11,6,8,5,3,15,13,0,14,9},
            {14,11,2,12,4,7,13,1,5,0,15,10,3,9,8,6},
            {4,2,1,11,10,13,7,8,15,9,12,5,6,3,0,14},
            {11,8,12,7,1,14,2,13,6,15,0,9,10,4,5,3},
            {12,1,10,15,9,2,6,8,0,13,3,4,14,7,5,11},
            {10,15,4,2,7,12,9,5,6,1,13,14,0,11,3,8},
            {9,14,15,5,2,8,12,3,7,0,4,10,1,13,11,6},
            {4,3,2,12,9,5,15,10,11,14,1,7,6,0,8,13},
            {4,11,2,14,15,0,8,13,3,12,9,7,5,10,6,1},
            {13,0,11,7,4,9,1,10,14,3,5,12,2,15,8,6},
            {1,4,11,13,12,3,7,14,10,15,6,8,0,5,9,2},
            {6,11,13,8,1,4,10,7,9,5,0,15,14,2,3,12},
            {13,2,8,4,6,15,11,1,10,9,3,14,5,0,12,7},
            {1,15,13,8,10,3,7,4,12,5,6,11,0,14,9,2},
            {7,11,4,1,9,12,14,2,0,6,10,13,15,3,5,8},
            {2,1,14,7,4,10,8,13,15,12,9,0,3,5,6,11},
    };
    private static final String EXPECTED = "85e813540f0ab405";   // 经典例密文（小写与 %x 输出一致）

    private final List<Step> steps = new ArrayList<>();
    private final long key;

    public DesDemo() {
        this(0x133457799BBCDFF1L);   // 经典例密钥（Grabbe 教程）
    }

    /** 变体：任意 64 位密钥——每字节第 8 位是奇偶校验位，PC1 不选中（有效密钥 56 位）。 */
    public DesDemo(long key) {
        this.key = key;
    }

    private int[] l = new int[33], r = new int[33];
    private int[][] rk = new int[17][48];
    private int round;

    @Override public String id() { return "crypto.des"; }
    @Override public String title() { return "对称·DES"; }
    @Override public String codeResource() { return "/code/DesDemo.java"; }
    @Override public String docResource() { return "/markdown/des.md"; }

    @Override
    public Trace produce() {
        steps.clear();
        round = 0;
        long plain = Long.parseUnsignedLong("0123456789ABCDEF", 16);
        expandKey(key);
        int[] in = toBits(plain);
        int[] ip = new int[64];
        for (int i = 0; i < 64; i++) {
            ip[i] = in[IP[i] - 1];
        }
        l = new int[33];
        r = new int[33];
        System.arraycopy(ip, 0, l, 1, 32);
        System.arraycopy(ip, 32, r, 1, 32);
        emit(13, List.of(VizEvent.of(VizEvent.EventType.MESSAGE,
                String.format("IP: L0=%s R0=%s", bitsHex(l), bitsHex(r)))),
                "IP 后拆成两半进入 Feistel");
        for (int fr = 0; fr < 8; fr++) {
            int from = fr * 2;
            for (int i = from; i < from + 2; i++) {
                round = i + 1;
                int[] fl = feistel(r, rk[i + 1]);   // 子密钥 1-based：K1..K16 存在 rk[1..16]
                int[] nl = new int[33];
                for (int k = 1; k <= 32; k++) {
                    nl[k] = r[k];
                    r[k] = l[k] ^ fl[k];
                }
                l = nl;
            }
            emit(17, List.of(VizEvent.of(VizEvent.EventType.SET,
                    String.format("轮 %d~%d：L=%s，R=%s", from + 1, from + 2, bitsHex(l), bitsHex(r)),
                    "L" + from + 1)),
                    "轮函数 f 不要求可逆--Feistel 的解密 = 子密钥倒序重放同一轮函数");
        }
        round = 17;
        int[] pre = new int[64];
        System.arraycopy(r, 1, pre, 0, 32);   // 16 轮后交换：R16||L16
        System.arraycopy(l, 1, pre, 32, 32);
        int[] out = new int[64];
        for (int i = 0; i < 64; i++) {
            out[i] = pre[FP[i] - 1];
        }
        String cipher = bitsHexFrom(out, 0, 64);
        boolean classic = String.format("%016X", key).equals("133457799BBCDFF1");
        String tail = classic ? "（经典例 " + EXPECTED
                + (cipher.equals(EXPECTED) ? " 命中）" : " 不符！）") : "";
        emit(21, List.of(VizEvent.of(VizEvent.EventType.MARK_SORTED,
                "密文 = " + cipher + tail)),
                "最终交换 + FP：Feistel 的收尾式--前后各 32 位对调再逆置换");
        emit(24, List.of(VizEvent.of(VizEvent.EventType.RETURN,
                "历史：1977 标准化 → 56 位密钥 → 1998 EFF 22 小时破解 → 3DES 过渡 → AES(2001) 接棒")),
                "DES 的 S 盒是'S 盒之母'：SM4/AES 的非线性层都从它的设计思想出发");
        return new Trace(SCHEMA, meta(), steps);
    }

    private void expandKey(long key) {
        int[] kb = new int[64];
        for (int i = 0; i < 64; i++) {
            kb[i] = (int) ((key >>> (63 - i)) & 1L);
        }
        int[] c = new int[29], d = new int[29];
        for (int i = 0; i < 28; i++) {
            c[i + 1] = kb[PC1[i] - 1];
            d[i + 1] = kb[PC1[i + 28] - 1];
        }
        for (int r = 1; r <= 16; r++) {
            for (int s = 0; s < SHIFTS[r - 1]; s++) {
                int tc = c[1], td = d[1];
                System.arraycopy(c, 2, c, 1, 27);
                c[28] = tc;
                System.arraycopy(d, 2, d, 1, 27);
                d[28] = td;
            }
            for (int i = 0; i < 48; i++) {
                int pc = PC2[i] - 1;
                rk[r][i] = pc < 28 ? c[pc + 1] : d[pc - 27];
            }
        }
    }

    private int[] feistel(int[] r, int[] k) {
        int[] exp = new int[49];
        for (int i = 0; i < 48; i++) {
            exp[i + 1] = r[ETAB[i]] ^ k[i];
        }
        int[] out = new int[33];
        for (int b = 0; b < 8; b++) {
            int row = (exp[b * 6 + 1] << 1) | exp[b * 6 + 6];
            int col = (exp[b * 6 + 2] << 3) | (exp[b * 6 + 3] << 2) | (exp[b * 6 + 4] << 1) | exp[b * 6 + 5];
            int v = SBOX[b * 4 + row][col];   // 表布局：32 行 = 8 盒 × 4 行（每盒 4×16）
            for (int i = 0; i < 4; i++) {
                out[b * 4 + i + 1] = (v >>> (3 - i)) & 1;
            }
        }
        int[] pp = new int[33];
        for (int i = 0; i < 32; i++) {
            pp[i + 1] = out[PTAB[i]];
        }
        return pp;
    }

    private int[] toBits(long v) {
        int[] b = new int[64];
        for (int i = 0; i < 64; i++) {
            b[i] = (int) ((v >>> (63 - i)) & 1L);
        }
        return b;
    }

    private String bitsHex(int[] arr) {
        long v = 0;
        for (int i = 1; i <= 32; i++) {
            v = (v << 1) | arr[i];
        }
        return String.format("%08x", v);
    }

    private String bitsHexFrom(int[] arr, int from, int len) {
        long v = 0;
        for (int i = from; i < from + len; i++) {
            v = (v << 1) | arr[i];
        }
        return String.format("%0" + (len / 4) + "x", v);
    }

    private void emit(int line, List<VizEvent> events, String why) {
        StackFrame frame = new StackFrame("des",
                OrderedMaps.of("轮", Value.prim(String.valueOf(round)),
                        "L", Value.prim(bitsHex(l)),
                        "R", Value.prim(bitsHex(r))), null);
        Map<String, Layer> layers = new LinkedHashMap<>();
        layers.put("mem.stack", new StackLayer(List.of(frame)));
        layers.put("mem.heap", new HeapLayer(snapshotHeap()));
        layers.put("math.matrix", snapshotMath());
        steps.add(new Step(steps.size(), Loc.line(line, why),
                Collections.unmodifiableMap(layers), List.copyOf(events), new Intent(why, INVARIANT)));
    }

    private Heap snapshotHeap() {
        Map<String, Value> fields = new LinkedHashMap<>();
        fields.put("密钥", Value.prim(String.format("%016X", key)));
        fields.put("轮", Value.prim(String.valueOf(round)));
        fields.put("经典例密文", Value.prim("85E813540F0AB405"));
        Map<String, HeapObject> objs = new LinkedHashMap<>();
        objs.put("@1", new HeapObject("@1", "des", "DesState",
                Collections.unmodifiableMap(fields), List.of()));
        return new Heap(Collections.unmodifiableMap(objs));
    }

    private MathView snapshotMath() {
        String lh = bitsHex(l), rh = bitsHex(r);
        List<String> vals = new ArrayList<>(List.of(lh, rh));
        Set<Integer> active = new LinkedHashSet<>(List.of(0, 1));
        MathView.Grid grid = new MathView.Grid("lr", "L/R 状态（hex；每帧 2 轮全字更新）",
                1, 2, vals, null, null, active, Set.of());
        return new MathView("DES", List.of(grid));
    }

    private Trace.TraceMeta meta() {
        return new Trace.TraceMeta(id(), title(), codeResource(),
                String.format("%016X", key).equals("133457799BBCDFF1")
                        ? "经典例 key/plain（64 位块）" : String.format("key=%016X（64 位块）", key), 64);
    }

    @Override
    public java.util.List<String> dataLines() {
        return java.util.List.of("String key = \"133457799BBCDFF1\";   //@DATA 密钥（经典例）");
    }

    @Override
    public Complexity complexity() {
        return new Complexity("O(16) 轮 × O(1) 查表轮函数", "O(1) 64 位状态",
                "DES 是 Feistel 结构的教科书：解密 = 子密钥倒序--3DES 至今仍在银行老系统里服役");
    }
}