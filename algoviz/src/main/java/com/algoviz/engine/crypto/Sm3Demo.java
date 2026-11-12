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
 * 哈希·SM3（GB/T 32905-2016）：消息 "abc" 的逐段压缩现场。
 * 填充（补 1/补 0/64 位长度）→ 消息扩展（P1 置换造 W16..W67）→ 64 轮压缩（FF/GG 布尔函数 + P0/P1 置换）→ 摘要。
 * 行为测试直接断言国标向量：SM3("abc") = 66c7f0f4 62eeedd9 d1f2d46b dc10e4e2 4167c487 5cf2f7a2 297da02b 8f4ba8e0。
 * 画面（math.matrix 复用）：网格一 = 512 位分组字节 4×16；网格二 = V0..V7 逐帧更新（每帧 4 轮）。
 */
public final class Sm3Demo implements Algorithm {

    private static final String SCHEMA = "1";
    private static final String INVARIANT =
            "压缩函数把 512 位分组搅进 256 位状态 V：FF/GG 布尔函数按轮段切换 + P0/P1 置换扩散——"
                    + "每一位的改动雪崩到全部 256 位；压缩不可逆 = 哈希的单向门";

    private static final int[] IV = {
            0x7380166f, 0x4914b2b9, 0x172442d7, 0xda8a0600,
            0xa96f30bc, 0x163138aa, 0xe38dee4d, 0xb0fb0e4e
    };
    private static final String EXPECTED = "66c7f0f4 62eeedd9 d1f2d46b dc10e4e2 "
            + "4167c487 5cf2f7a2 297da02b 8f4ba8e0";   // 国标示例：SM3("abc")

    private final String message;

    private final List<Step> steps = new ArrayList<>();
    private int[] v = new int[8];
    private int[] w = new int[68];
    private int round;                                  // 当前轮号（0..63）
    private byte[] group;                               // 填充后的 64 字节

    public Sm3Demo() {
        this("abc");
    }

    /** 变体：换消息（UTF-8 字节参与填充）。 */
    public Sm3Demo(String message) {
        this.message = message;
    }

    @Override public String id() { return "crypto.sm3"; }
    @Override public String title() { return "哈希·SM3"; }
    @Override public String codeResource() { return "/code/Sm3Demo.java"; }
    @Override public String docResource() { return "/markdown/sm3.md"; }

    @Override
    public Trace produce() {
        steps.clear();
        // ---- 填充 ----
        byte[] msg = message.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        long bitLen = msg.length * 8L;
        int padded = (msg.length + 9 + 63) / 64 * 64;
        group = new byte[padded];
        System.arraycopy(msg, 0, group, 0, msg.length);
        group[msg.length] = (byte) 0x80;
        for (int i = 0; i < 8; i++) {
            group[padded - 1 - i] = (byte) (bitLen >>> (8 * i));
        }
        Set<Integer> active = new LinkedHashSet<>();
        for (int i = msg.length; i < padded; i++) {
            active.add(i);   // 填充进来的字节
        }
        v = IV.clone();
        emit(6, List.of(VizEvent.of(VizEvent.EventType.INIT,
                String.format("\"%s\"=%d 字节 → 填充后 %d 字节（补 0x80 + 补 0 + 64 位长度 %d）",
                        message, msg.length, padded, bitLen))),
                "填充保证消息长度唯一编码——两条不同的消息永远填出不同的分组尾巴");
        // ---- 消息扩展 ----
        expand();
        emit(11, List.of(VizEvent.of(VizEvent.EventType.MESSAGE,
                "消息扩展：W16..W67 = P1(W[j−16]⊕W[j−9]⊕rotl15(W[j−3]))⊕rotl7(W[j−13])⊕W[j−6]")),
                "扩展让 16 个消息字'发酵'成 68 个——每一轮的对手都不同，抗差分分析的纵深");
        // ---- 压缩：64 轮，每帧 4 轮 ----
        int[] wPrime = new int[68];
        for (int j = 0; j < 64; j++) {
            wPrime[j] = w[j] ^ w[j + 4];
        }
        for (int frame = 0; frame < 16; frame++) {
            int from = frame * 4;
            for (int j = from; j < from + 4; j++) {
                round = j;
                compressRound(j, w, wPrime, v);
            }
            if (from + 4 == 64) {   // 64 轮收尾：V(i+1) = V(i) ⊕ A..H（v 此刻已是轮转终态，与 IV 异或）
                int[] old = IV.clone();
                for (int i = 0; i < 8; i++) {
                    v[i] ^= old[i];
                }
            }
            emit(17, List.of(VizEvent.of(VizEvent.EventType.SET,
                    String.format("轮 %d~%d 完成：V0..V7 被轮函数 FF/GG + P0 置换反复搅动", from, from + 3))),
                    j0to15(from) ? "前 16 轮布尔函数是纯异或（FF/GG = X⊕Y⊕Z）——线性段先让位扩散"
                            : "后 48 轮切换到与/或/非布尔函数——非线性段是抗代数攻击的主力");
        }
        emit(21, List.of(VizEvent.of(VizEvent.EventType.MARK_SORTED,
                "摘要 = V0..V7（逐字与初始 IV 异或收尾）")),
                "雪崩验证：改消息一个 bit，摘要平均一半的位翻转——256 位的'指纹'");
        emit(24, List.of(VizEvent.of(VizEvent.EventType.RETURN,
                "国标示例验证：SM3(\"abc\") = " + EXPECTED)),
                "数字签名、证书链、完整性校验全踩在 SM3 上——国密体系的地基");
        return new Trace(SCHEMA, meta(), steps);
    }

    /** 单轮压缩（GB/T 32905 §5.3.2）：工作变量 A..H 轮转 + FF/GG + P0；结果写入 state。 */
    private void compressRound(int j, int[] wWord, int[] wPrime, int[] state) {
        int a = state[0], b = state[1], c = state[2], d = state[3];
        int e = state[4], f = state[5], g = state[6], h = state[7];
        int tj = j < 16 ? 0x79cc4519 : 0x7a879d8a;
        int ss1 = Integer.rotateLeft(Integer.rotateLeft(a, 12) + e + Integer.rotateLeft(tj, j), 7);
        int ss2 = ss1 ^ Integer.rotateLeft(a, 12);
        int tt1 = ff(j, a, b, c) + d + ss2 + wPrime[j];
        int tt2 = gg(j, e, f, g) + h + ss1 + wWord[j];
        state[0] = tt1;
        state[1] = a;
        state[2] = Integer.rotateLeft(b, 9);
        state[3] = c;
        state[4] = p0(tt2);
        state[5] = e;
        state[6] = Integer.rotateLeft(f, 19);
        state[7] = g;
    }

    private static int ff(int j, int x, int y, int z) {
        return j < 16 ? x ^ y ^ z : (x & y) | (x & z) | (y & z);
    }

    private static int gg(int j, int x, int y, int z) {
        return j < 16 ? x ^ y ^ z : (x & y) | (~x & z);
    }

    private static int p0(int x) {
        return x ^ Integer.rotateLeft(x, 9) ^ Integer.rotateLeft(x, 17);
    }

    private static int p1(int x) {
        return x ^ Integer.rotateLeft(x, 15) ^ Integer.rotateLeft(x, 23);
    }

    /** 消息扩展：W0..15 取分组大端字，W16..67 按 P1 公式发酵。 */
    private void expand() {
        for (int j = 0; j < 16; j++) {
            w[j] = ((group[4 * j] & 0xff) << 24) | ((group[4 * j + 1] & 0xff) << 16)
                    | ((group[4 * j + 2] & 0xff) << 8) | (group[4 * j + 3] & 0xff);
        }
        for (int j = 16; j < 68; j++) {
            int x = w[j - 16] ^ w[j - 9] ^ Integer.rotateLeft(w[j - 3], 15);
            w[j] = p1(x) ^ Integer.rotateLeft(w[j - 13], 7) ^ w[j - 6];
        }
    }

    /** 摘要终值（供测试直接断言国标向量）。 */
    public String digestHex() {
        int[] vv = IV.clone();
        byte[] msg = message.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        long bitLen = msg.length * 8L;
        int padded = (msg.length + 9 + 63) / 64 * 64;
        byte[] gr = new byte[padded];
        System.arraycopy(msg, 0, gr, 0, msg.length);
        gr[msg.length] = (byte) 0x80;
        for (int i = 0; i < 8; i++) {
            gr[padded - 1 - i] = (byte) (bitLen >>> (8 * i));
        }
        expandFor(gr, vv);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 8; i++) {
            if (i > 0) {
                sb.append(' ');
            }
            sb.append(String.format("%08x", vv[i]));
        }
        return sb.toString();
    }

    private void expandFor(byte[] gr, int[] vv) {
        int groups = gr.length / 64;
        int[] wp = new int[64];
        for (int gIdx = 0; gIdx < groups; gIdx++) {
            int[] ww = new int[68];
            for (int j = 0; j < 16; j++) {
                int o = gIdx * 64;
                ww[j] = ((gr[o + 4 * j] & 0xff) << 24) | ((gr[o + 4 * j + 1] & 0xff) << 16)
                        | ((gr[o + 4 * j + 2] & 0xff) << 8) | (gr[o + 4 * j + 3] & 0xff);
            }
            for (int j = 16; j < 68; j++) {
                int x = ww[j - 16] ^ ww[j - 9] ^ Integer.rotateLeft(ww[j - 3], 15);
                ww[j] = p1(x) ^ Integer.rotateLeft(ww[j - 13], 7) ^ ww[j - 6];
            }
            for (int j = 0; j < 64; j++) {
                wp[j] = ww[j] ^ ww[j + 4];
            }
            int[] state = vv.clone();
            int round0 = this.round;
            int[] saved = this.v;
            this.v = state;
            for (int j = 0; j < 64; j++) {
                this.round = j;
                compressRound(j, ww, wp, state);
            }
            this.round = round0;
            this.v = saved;
            for (int i = 0; i < 8; i++) {
                vv[i] ^= state[i];
            }
        }
    }

    private boolean j0to15(int from) {
        return from < 16;
    }

    private void emit(int line, List<VizEvent> events, String why) {
        StackFrame fr = new StackFrame("sm3",
                OrderedMaps.of("消息", Value.prim("\"" + message + "\""),
                        "轮", Value.prim(String.valueOf(round)),
                        "V0", Value.prim(String.format("%08x", v[0])),
                        "V7", Value.prim(String.format("%08x", v[7]))), null);
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
        fields.put("国标向量", Value.prim(EXPECTED));
        Map<String, HeapObject> objs = new LinkedHashMap<>();
        objs.put("@1", new HeapObject("@1", "sm3", "HashState",
                Collections.unmodifiableMap(fields), List.of()));
        return new Heap(Collections.unmodifiableMap(objs));
    }

    private MathView snapshotMath() {
        List<String> groupVals = new ArrayList<>();
        for (byte b : group) {
            groupVals.add(String.format("%02x", b));
        }
        Set<Integer> hot = new LinkedHashSet<>();
        for (int i = 0; i < group.length; i++) {
            hot.add(i);
        }
        MathView.Grid groupGrid = new MathView.Grid("group", "512 位分组字节（黄=消息，绿=填充与长度）",
                group.length / 16, 16, groupVals, null, null,
                Set.of(), Collections.unmodifiableSet(hot));   // 不可变集合须保持插入序：Set.copyOf 跨 JVM 迭代序随机，golden 会抖动
        List<String> vVals = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            vVals.add(String.format("%08x", v[i]));
        }
        Set<Integer> vActive = new LinkedHashSet<>();
        for (int i = 0; i < 8; i++) {
            vActive.add(i);
        }
        MathView.Grid vGrid = new MathView.Grid("V", "压缩状态 V0..V7（每帧 4 轮，全字更新）",
                1, 8, vVals, null, null, vActive, Set.of());
        return new MathView("SM3 压缩现场", List.of(groupGrid, vGrid));
    }

    private Trace.TraceMeta meta() {
        return new Trace.TraceMeta(id(), title(), codeResource(),
                "\"" + message + "\"", 24);
    }

    @Override
    public java.util.List<String> dataLines() {
        return java.util.List.of(String.format("String message = \"%s\";   //@DATA 消息", message));
    }

    @Override
    public Complexity complexity() {
        return new Complexity("O(分组数×64) 轮压缩", "O(1) 固定 256 位状态",
                "数字签名先哈希再签名（省事又安全）、文件完整性、证书链——"
                        + "哈希是密码世界的'指纹采集器'；SM3 与 SHA-256 同位级对抗");
    }
}
