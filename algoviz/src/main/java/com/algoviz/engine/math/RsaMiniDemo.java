package com.algoviz.engine.math;

import com.algoviz.engine.Algorithm;
import com.algoviz.engine.Complexity;
import com.algoviz.engine.Intent;
import com.algoviz.engine.Layer;
import com.algoviz.engine.Loc;
import com.algoviz.engine.OrderedMaps;
import com.algoviz.engine.Step;
import com.algoviz.engine.Value;
import com.algoviz.engine.VizEvent;
import com.algoviz.engine.mem.Heap;
import com.algoviz.engine.mem.HeapLayer;
import com.algoviz.engine.mem.HeapObject;
import com.algoviz.engine.mem.StackFrame;
import com.algoviz.engine.mem.StackLayer;
import com.algoviz.engine.math.MathView;
import com.algoviz.engine.trace.Trace;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.List;
import java.util.Map;

/**
 * RSA 迷你版：p=3, q=11, n=33, φ=20, e=3, d=7——明文 4 加密成 31，再解回 4。
 * 加解密都靠同一个动作：模幂（快速幂的应用，见 math.fastpower）。
 * 安全性=把 33 分解回 3×11 很容易，但 2048 位的 n 至今没人能分解——非对称加密的全部秘密。
 */
public final class RsaMiniDemo implements Algorithm {

    private static final String SCHEMA = "1";
    private static final String INVARIANT =
            "c = m^e mod n，m = c^d mod n——e·d ≡ 1 (mod φ(n)) 让加解密互为逆运算；n=pq 是公开的，p·q 是私藏的";

    private static final int P = 3, Q = 11, N = P * Q, PHI = (P - 1) * (Q - 1), E = 3, D = 7;
    private static final int M = 4;   // 明文

    private final List<Step> steps = new ArrayList<>();
    private long value = M;
    private String phase = "密钥生成";

    @Override public String id() { return "math.rsa"; }
    @Override public String title() { return "RSA 迷你版"; }
    @Override public String codeResource() { return "/code/RsaMiniDemo.java"; }
    @Override public String docResource() { return "/markdown/rsa.md"; }

    @Override
    public Trace produce() {
        steps.clear();
        value = M;
        phase = "密钥生成";
        emit(5, List.of(VizEvent.of(VizEvent.EventType.INIT,
                "选质数 p=3, q=11 → n=pq=33, φ(n)=(p−1)(q−1)=20")),
                "n 公开、φ 保密——而 φ 只能由 p·q 算出：这就是'非对称'的钥匙孔");
        emit(7, List.of(VizEvent.of(VizEvent.EventType.SET,
                "e=3（与 φ=20 互质），d=7（e·d=21 ≡ 1 mod 20）")),
                "公钥=(n,e)=(33,3) 随便发；私钥 d=7 自己藏好——d 由 φ 倒推，外人算不出");
        // 加密
        phase = "加密（Alice）";
        value = M;
        emit(10, List.of(VizEvent.of(VizEvent.EventType.CALL,
                "明文 m=" + M + " → c = m^e mod n = " + M + "³ mod 33")),
                "快速幂上场：3 次乘法 + 每步取模——数字才不会爆炸");
        emit(11, List.of(VizEvent.of(VizEvent.EventType.SET,
                "4³ = 64 → 64 − 33 = 31 → **密文 c = 31**")),
                "窃听者看到 31——没有 d 就读不回 4");
        value = 31;
        // 解密
        phase = "解密（Bob）";
        emit(14, List.of(VizEvent.of(VizEvent.EventType.CALL,
                "m = c^d mod n = 31⁷ mod 33——快速幂平方-相乘")),
                "7 = 二进制 111 → 三次平方三次乘：31²≡4, 31⁴≡16, 31⁷ = 16·4·31");
        emit(15, List.of(VizEvent.of(VizEvent.EventType.SET,
                "31² = 961 → 961 mod 33 = 4；31⁴ ≡ 16；31⁷ = 16×4×31 = 1984 → 1984 mod 33 = 4")),
                "模幂的中间值始终 < n²——这就是快速幂对 RSA 生死攸关的原因");
        value = 4;
        emit(17, List.of(VizEvent.of(VizEvent.EventType.MARK_SORTED,
                "解出 m = 4——和明文一模一样！加解密互为逆运算（欧拉定理保证）")),
                "欧拉定理：m^(e·d) = m^(1+kφ) ≡ m (mod n)——数论在为每一次 HTTPS 握手背书");
        phase = "完成";
        emit(19, List.of(VizEvent.of(VizEvent.EventType.RETURN,
                "玩具版 n=33 秒破；真实 RSA n=2048 位——'乘起来容易拆开来难'撑起整个互联网的信任")),
                "量子计算机的 Shor 算法专杀大数分解——RSA 的末日钟由此开始计时");
        return new Trace(SCHEMA, meta(), steps);
    }

    private void emit(int line, List<VizEvent> events, String why) {
        StackFrame frame = new StackFrame(phase,
                OrderedMaps.of("当前值", Value.prim(String.valueOf(value)),
                        "公钥/私钥", Value.prim("(33,3) / d=7")), null);
        Map<String, Layer> layers = new LinkedHashMap<>();
        layers.put("mem.stack", new StackLayer(List.of(frame)));
        layers.put("mem.heap", new HeapLayer(snapshotHeap()));
        layers.put("math.matrix", snapshotPipeline());
        steps.add(new Step(steps.size(), Loc.line(line, why),
                Collections.unmodifiableMap(layers), List.copyOf(events), new Intent(why, INVARIANT)));
    }

    private Heap snapshotHeap() {
        Map<String, Value> pub = new LinkedHashMap<>();
        pub.put("n", Value.prim(N));
        pub.put("e", Value.prim(E));
        Map<String, Value> priv = new LinkedHashMap<>();
        priv.put("p,q", Value.prim(P + " × " + Q));
        priv.put("d", Value.prim(D));
        priv.put("φ", Value.prim(PHI));
        Map<String, Value> msg = new LinkedHashMap<>();
        msg.put("明文 m", Value.prim(M));
        msg.put("密文 c", Value.prim(31));
        msg.put("解出", Value.prim(value));
        Map<String, HeapObject> objs = new LinkedHashMap<>();
        objs.put("@1", new HeapObject("@1", "公钥", "PublicKey",
                Collections.unmodifiableMap(pub), List.of()));
        objs.put("@2", new HeapObject("@2", "私钥", "PrivateKey",
                Collections.unmodifiableMap(priv), List.of()));
        objs.put("@3", new HeapObject("@3", "消息", "Message",
                Collections.unmodifiableMap(msg), List.of()));
        return new Heap(Collections.unmodifiableMap(objs));
    }

    /** 流水线视图：明文 → 公钥加密 → 密文 → 私钥解密。 */
    private MathView snapshotPipeline() {
        boolean encrypted = !"密钥生成".equals(phase) && (value == 31 || "解密（Bob）".equals(phase) || "完成".equals(phase));
        List<String> vals = List.of("m=4", "e=3", "d=7", encrypted ? "c=31" : "c=?");
        Set<Integer> active = new LinkedHashSet<>();
        if ("加密（Alice）".equals(phase)) active.add(0);
        if ("解密（Bob）".equals(phase)) active.add(2);
        if ("完成".equals(phase)) {
            active.add(3);
        }
        MathView.Grid grid = new MathView.Grid("pipe", "RSA 流水线", 1, 4,
                vals, null, null,
                Collections.unmodifiableSet(active), Set.of());
        return new MathView("RSA", List.of(grid));
    }

    private Trace.TraceMeta meta() {
        return new Trace.TraceMeta(id(), title(), codeResource(), "p=3,q=11,m=4", 6);
    }

    @Override
    public java.util.List<String> dataLines() {
        return java.util.List.of("int p = 3, q = 11, e = 3, d = 7, m = " + M + ";");
    }

    @Override
    public Complexity complexity() {
        return new Complexity("加解密各 O(log e) 次乘法（快速幂）", "O(1)",
                "安全性 = 大数分解难度。真实密钥 n=2048 位：把'分解'比作把一杯水重新分成两条河流");
    }
}
