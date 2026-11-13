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
 * 哈希·SHA-256（FIPS 180-2）：消息 "abc" 的 64 轮压缩现场——与 SM3 同构的国际标准对照。
 * Ch/Maj 布尔函数 + Σ/σ 双旋转置换；K/H 常数程序生成（前 64 个素数的立方根/平方根小数部分）。
 * 行为测试断言 NIST 向量：SHA-256("abc") = ba7816bf 8f01cfea 414140de 5dae2223 b00361a3 96177a9c b410ff61 f20015ad。
 * 画面（math.matrix 复用）：网格一 = 分组字节；网格二 = H0..H7（每帧 8 轮）。
 */
public final class Sha256Demo implements Algorithm {

    private static final String SCHEMA = "1";
    private static final String INVARIANT =
            "64 轮：Ch/Maj 布尔函数 + Σ/σ 大小双旋转——与 SM3 同构（填充→扩展→压缩），常数取自素数根；"
                    + "压缩不可逆：256 位指纹，改一位雪崩一半";

    private static final String EXPECTED = "ba7816bf 8f01cfea 414140de 5dae2223 "
            + "b00361a3 96177a9c b410ff61 f20015ad";   // NIST 示例：SHA-256("abc")

    private final String message;

    private final List<Step> steps = new ArrayList<>();
    private int[] h = new int[8];
    private int[] w = new int[64];
    private int round;
    private byte[] group;

    public Sha256Demo() {
        this("abc");
    }

    /** 变体：换消息（UTF-8）。 */
    public Sha256Demo(String message) {
        this.message = message;
    }

    @Override public String id() { return "crypto.sha256"; }
    @Override public String title() { return "哈希·SHA-256"; }
    @Override public String codeResource() { return "/code/Sha256Demo.java"; }
    @Override public String docResource() { return "/markdown/sha256.md"; }

    @Override
    public Trace produce() {
        steps.clear();
        byte[] msg = message.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        long bitLen = msg.length * 8L;
        int padded = (msg.length + 9 + 63) / 64 * 64;
        group = new byte[padded];
        System.arraycopy(msg, 0, group, 0, msg.length);
        group[msg.length] = (byte) 0x80;
        for (int i = 0; i < 8; i++) {
            group[padded - 1 - i] = (byte) (bitLen >>> (8 * i));
        }
        h = initialHash();
        emit(6, List.of(VizEvent.of(VizEvent.EventType.INIT,
                String.format("\"%s\"=%d 字节 → 填充 %d 字节；H0..H7 = 前 8 个素数的平方根小数部分",
                        message, msg.length, padded))),
                "常数不靠背：立方根给 K、平方根给 H——'无先验结构'的数学出处");
        expand();
        emit(11, List.of(VizEvent.of(VizEvent.EventType.MESSAGE,
                "消息扩展：σ0/σ1 小旋转造 W16..W63（对照 SM3 的 P1 扩展）")),
                "同构的扩展思路：让 16 个消息字长出 48 个新对手");
        int[] work = h.clone();   // 工作变量独立于 H：64 轮全走完才模加回写（SHA-256 的回写语义）
        for (int frame = 0; frame < 8; frame++) {
            int from = frame * 8;
            for (int j = from; j < from + 8; j++) {
                round = j;
                compressRound(j, work);
            }
            if (from + 8 == 64) {
                for (int i = 0; i < 8; i++) {
                    h[i] += work[i];
                }
            }
            emit(16, List.of(VizEvent.of(VizEvent.EventType.SET,
                    String.format("轮 %d~%d：Ch/Maj + Σ0/Σ1 逐轮搅动工作变量", from, from + 7))),
                    from < 16 ? "T1 的 Ch(X,Y,Z)=(X∧Y)⊕(¬X∧Z)：'Y 说了算，除非 X 否决'——位级三选二"
                            : "T2 的 Maj(X,Y,Z)=多数表决——三个输入按位投票");
        }
        emit(20, List.of(VizEvent.of(VizEvent.EventType.MARK_SORTED,
                "摘要 = H0..H7（初值 + 64 轮工作变量的模加收尾——注意：SHA-256 用加法回写，SM3 用异或）")),
                "同构算法的细节分野：加法回写 vs 异或回写——都是把'搅拌结果'记入状态");
        emit(23, List.of(VizEvent.of(VizEvent.EventType.RETURN,
                "NIST 示例验证：SHA-256(\"abc\") = " + EXPECTED)),
                "与 SM3 并肩：TLS 双证书链、区块链（比特币用双 SHA-256）——国际线与国密线");
        return new Trace(SCHEMA, meta(), steps);
    }

    /** H/K 常数程序生成：前 8/64 个素数的平方根/立方根小数部分 × 2³²。 */
    public static int[] initialHash() {
        return rootWords(8, false);
    }

    public static int[] roundConstants() {
        return rootWords(64, true);
    }

    private static int[] rootWords(int count, boolean cbrt) {
        int[] out = new int[count];
        int found = 0;
        for (int cand = 2; found < count; cand++) {
            boolean prime = true;
            for (int d = 2; d * d <= cand; d++) {
                if (cand % d == 0) {
                    prime = false;
                    break;
                }
            }
            if (!prime) {
                continue;
            }
            double root = cbrt ? Math.cbrt(cand) : Math.sqrt(cand);
            double frac = root - Math.floor(root);
            // double→int 溢出会饱和为 MAX_VALUE（Java 语言规则）——先转 long 再取低 32 位
            out[found++] = (int) ((long) (frac * (1L << 32)) & 0xffffffffL);
        }
        return out;
    }

    /** 单轮压缩（FIPS 180-2 §6.2.2）：工作变量轮转，结果写入 state。 */
    private void compressRound(int j, int[] state) {
        int a = state[0], b = state[1], c = state[2], d = state[3], e = state[4], f = state[5], g = state[6], hh = state[7];
        int[] k = roundConstants();
        int t1 = hh + sigma1(e) + ch(e, f, g) + k[j] + w[j];
        int t2 = sigma0(a) + maj(a, b, c);
        state[7] = g;
        state[6] = f;
        state[5] = e;
        state[4] = d + t1;
        state[3] = c;
        state[2] = b;
        state[1] = a;
        state[0] = t1 + t2;
    }

    private static int ch(int x, int y, int z) {
        return (x & y) ^ (~x & z);
    }

    private static int maj(int x, int y, int z) {
        return (x & y) ^ (x & z) ^ (y & z);
    }

    private static int sigma0(int x) {
        return Integer.rotateRight(x, 2) ^ Integer.rotateRight(x, 13) ^ Integer.rotateRight(x, 22);
    }

    private static int sigma1(int x) {
        return Integer.rotateRight(x, 6) ^ Integer.rotateRight(x, 11) ^ Integer.rotateRight(x, 25);
    }

    private static int sigmaSmall0(int x) {
        return Integer.rotateRight(x, 7) ^ Integer.rotateRight(x, 18) ^ (x >>> 3);
    }

    private static int sigmaSmall1(int x) {
        return Integer.rotateRight(x, 17) ^ Integer.rotateRight(x, 19) ^ (x >>> 10);
    }

    /** W 扩展：W16..63 = σ1(W[j−2]) + W[j−7] + σ0(W[j−15]) + W[j−16]（模加）。 */
    private void expand() {
        for (int j = 0; j < 16; j++) {
            w[j] = ((group[4 * j] & 0xff) << 24) | ((group[4 * j + 1] & 0xff) << 16)
                    | ((group[4 * j + 2] & 0xff) << 8) | (group[4 * j + 3] & 0xff);
        }
        for (int j = 16; j < 64; j++) {
            w[j] = sigmaSmall1(w[j - 2]) + w[j - 7] + sigmaSmall0(w[j - 15]) + w[j - 16];
        }
    }

    /** 摘要（供测试断言 NIST 向量）：完整独立路径。 */
    public String digestHex() {
        byte[] msg = message.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        long bitLen = msg.length * 8L;
        int padded = (msg.length + 9 + 63) / 64 * 64;
        byte[] gr = new byte[padded];
        System.arraycopy(msg, 0, gr, 0, msg.length);
        gr[msg.length] = (byte) 0x80;
        for (int i = 0; i < 8; i++) {
            gr[padded - 1 - i] = (byte) (bitLen >>> (8 * i));
        }
        int[] hh = initialHash();
        int[] ww = new int[64];
        int groups = padded / 64;
        for (int gIdx = 0; gIdx < groups; gIdx++) {
            for (int j = 0; j < 16; j++) {
                int o = gIdx * 64;
                ww[j] = ((gr[o + 4 * j] & 0xff) << 24) | ((gr[o + 4 * j + 1] & 0xff) << 16)
                        | ((gr[o + 4 * j + 2] & 0xff) << 8) | (gr[o + 4 * j + 3] & 0xff);
            }
            for (int j = 16; j < 64; j++) {
                ww[j] = sigmaSmall1(ww[j - 2]) + ww[j - 7] + sigmaSmall0(ww[j - 15]) + ww[j - 16];
            }
            int[] work = hh.clone();
            int[] k = roundConstants();
            for (int j = 0; j < 64; j++) {
                int a = work[0], b = work[1], c = work[2], d = work[3], e = work[4], f = work[5], g = work[6], h7 = work[7];
                int t1 = h7 + sigma1(e) + ch(e, f, g) + k[j] + ww[j];
                int t2 = sigma0(a) + maj(a, b, c);
                work[7] = g;
                work[6] = f;
                work[5] = e;
                work[4] = d + t1;
                work[3] = c;
                work[2] = b;
                work[1] = a;
                work[0] = t1 + t2;
            }
            for (int i = 0; i < 8; i++) {
                hh[i] += work[i];
            }
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 8; i++) {
            if (i > 0) {
                sb.append(' ');
            }
            sb.append(String.format("%08x", hh[i]));
        }
        return sb.toString();
    }

    private void emit(int line, List<VizEvent> events, String why) {
        StackFrame fr = new StackFrame("sha256",
                OrderedMaps.of("消息", Value.prim("\"" + message + "\""),
                        "轮", Value.prim(String.valueOf(round)),
                        "H0", Value.prim(String.format("%08x", h[0]))), null);
        Map<String, Layer> layers = new LinkedHashMap<>();
        layers.put("mem.stack", new StackLayer(List.of(fr)));
        layers.put("mem.heap", new HeapLayer(snapshotHeap()));
        layers.put("math.matrix", snapshotMath());
        steps.add(new Step(steps.size(), Loc.line(line, why),
                Collections.unmodifiableMap(layers), List.copyOf(events), new Intent(why, INVARIANT)));
    }

    private Heap snapshotHeap() {
        Map<String, Value> fields = new LinkedHashMap<>();
        fields.put("消息", Value.prim("\"" + message + "\""));
        fields.put("当前轮", Value.prim(String.valueOf(round)));
        fields.put("NIST 向量", Value.prim(EXPECTED));
        Map<String, HeapObject> objs = new LinkedHashMap<>();
        objs.put("@1", new HeapObject("@1", "sha256", "HashState",
                Collections.unmodifiableMap(fields), List.of()));
        return new Heap(Collections.unmodifiableMap(objs));
    }

    private MathView snapshotMath() {
        List<String> groupVals = new ArrayList<>();
        for (byte b : group) {
            groupVals.add(String.format("%02x", b));
        }
        Set<Integer> hot = new LinkedHashSet<>();
        for (int i = 16; i < group.length; i++) {
            hot.add(i);
        }
        MathView.Grid groupGrid = new MathView.Grid("group", "分组字节（绿=填充与长度区）",
                group.length / 16, 16, groupVals, null, null, Set.of(), Collections.unmodifiableSet(hot));
        List<String> hVals = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            hVals.add(String.format("%08x", h[i]));
        }
        Set<Integer> hActive = new LinkedHashSet<>();
        for (int i = 0; i < 8; i++) {
            hActive.add(i);
        }
        MathView.Grid hGrid = new MathView.Grid("H", "压缩状态 H0..H7（每帧 8 轮；模加回写）",
                1, 8, hVals, null, null, hActive, Set.of());
        return new MathView("SHA-256 压缩现场", List.of(groupGrid, hGrid));
    }

    private Trace.TraceMeta meta() {
        return new Trace.TraceMeta(id(), title(), codeResource(),
                "\"" + message + "\"", 23);
    }

    @Override
    public java.util.List<String> dataLines() {
        return java.util.List.of(String.format("String message = \"%s\";   //@DATA 消息", message));
    }

    @Override
    public Complexity complexity() {
        return new Complexity("O(分组数×64) 轮压缩", "O(1) 固定 256 位状态",
                "区块链的双哈希、证书签名、Git 提交号——SHA-256 是国际线的万能指纹；与 SM3 同级对照");
    }
}
