package com.algoviz.engine.chem;

import com.algoviz.engine.Algorithm;
import com.algoviz.engine.Complexity;
import com.algoviz.engine.Intent;
import com.algoviz.engine.Layer;
import com.algoviz.engine.Loc;
import com.algoviz.engine.OrderedMaps;
import com.algoviz.engine.Step;
import com.algoviz.engine.Value;
import com.algoviz.engine.VizEvent;
import com.algoviz.engine.ds.GraphView;
import com.algoviz.engine.mem.Heap;
import com.algoviz.engine.mem.HeapLayer;
import com.algoviz.engine.mem.HeapObject;
import com.algoviz.engine.mem.StackFrame;
import com.algoviz.engine.mem.StackLayer;
import com.algoviz.engine.trace.Trace;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 分子结构·同分异构：同样的分子式 C₄H₁₀，两种碳骨架——图同构的化学判决。
 * 两幕：正丁烷（链状，度序列 1,2,2,1，沸点 −0.5°C）vs 异丁烷（星形，度序列 3,1,1,1，沸点 −11.7°C）。
 * 度序列（排序后）是同构不变量：不同 → 必非同构 → 结构不同 → 性质不同。连接度即物理。
 * 画面（ds.graph 复用）：顶点 = 碳原子，边 = C-C 键（边权 = 键级），逐键生长。
 * 🎲 变体：C₅H₁₂ 三种异构体（正戊烷/异戊烷/新戊烷）换展品。
 */
public final class IsomerDemo implements Algorithm {

    private static final String SCHEMA = "1";
    private static final String INVARIANT =
            "度序列（顶点度排序后）是图同构的不变量：序列不同必非同构——"
                    + "分子式相同而碳骨架不同 = 同分异构体，连接数不同 → 范德华力不同 → 沸点不同";

    /** 单个展品：碳骨架 + 沸点。 */
    private record Skeleton(String name, int[][] edges, String boiling) {
    }

    private static final Skeleton C4_LINEAR =
            new Skeleton("正丁烷 C₄H₁₀", new int[][]{{0, 1}, {1, 2}, {2, 3}}, "−0.5°C");
    private static final Skeleton C4_BRANCHED =
            new Skeleton("异丁烷 C₄H₁₀", new int[][]{{0, 1}, {0, 2}, {0, 3}}, "−11.7°C");
    private static final Skeleton C5_LINEAR =
            new Skeleton("正戊烷 C₅H₁₂", new int[][]{{0, 1}, {1, 2}, {2, 3}, {3, 4}}, "36.1°C");
    private static final Skeleton C5_ISO =
            new Skeleton("异戊烷 C₅H₁₂", new int[][]{{0, 1}, {0, 2}, {0, 3}, {3, 4}}, "27.9°C");
    private static final Skeleton C5_NEO =
            new Skeleton("新戊烷 C₅H₁₂", new int[][]{{0, 1}, {0, 2}, {0, 3}, {0, 4}}, "9.5°C");

    private final int variant;   // 0 = C4 双幕对比（默认），1/2/3 = C5 单幕展品

    private final List<Step> steps = new ArrayList<>();
    private final List<int[]> drawn = new ArrayList<>();
    private Skeleton current;
    private int[] degrees;
    private String verdict;

    public IsomerDemo() {
        this(0);
    }

    /** 变体：1 = 正戊烷，2 = 异戊烷，3 = 新戊烷（单幕展示 + 度序列判决）。 */
    public IsomerDemo(int variant) {
        this.variant = variant;
    }

    @Override public String id() { return "chem.isomer"; }
    @Override public String title() { return "分子结构·同分异构"; }
    @Override public String codeResource() { return "/code/IsomerDemo.java"; }
    @Override public String docResource() { return "/markdown/isomer.md"; }

    @Override
    public Trace produce() {
        steps.clear();
        drawn.clear();
        verdict = "";
        degrees = new int[0];
        if (variant == 0) {
            twoActs(C4_LINEAR, C4_BRANCHED);
        } else {
            Skeleton s = variant == 1 ? C5_LINEAR : variant == 2 ? C5_ISO : C5_NEO;
            singleAct(s, "C₅H₁₂ 三兄弟之一——换 🎲 见另外两位");
        }
        return new Trace(SCHEMA, meta(), steps);
    }

    /** 双幕对比：画第一幕 → 度序列判决 → 画第二幕 → 非同构终审。 */
    private void twoActs(Skeleton first, Skeleton second) {
        drawSkeleton(first, 6, 9);
        emit(12, List.of(VizEvent.of(VizEvent.EventType.MARK_SORTED,
                String.format("幕一判决：%s 度序列 %s，沸点 %s", first.name(), degSeq(), first.boiling()))),
                "度 = 这个碳连着几个碳：链中段 2 个端点 1 个——骨架的「指纹」就是度序列");
        drawn.clear();
        drawSkeleton(second, 14, 17);
        boolean same = Arrays.equals(sortedDegrees(first), sortedDegrees(second));
        verdict = same ? "度序列相同——需要进一步检验（同构）"
                : String.format("度序列 %s ≠ %s → 非同构 → 两种不同的物质！", degSeq(), seqOf(sortedDegrees(first)));
        emit(20, List.of(VizEvent.of(VizEvent.EventType.MARK_SORTED,
                String.format("幕二判决：%s。沸点差 %.1f°C——结构差 1 根键，物理差一截",
                        verdict, 11.2))),
                "同分异构 = 分子式相同 + 图不同构：原子清单一样，「连接方式」才是身份");
        emit(23, List.of(VizEvent.of(VizEvent.EventType.RETURN,
                "支链越多分子越「圆」→ 范德华接触面越小 → 沸点越低：异丁烷比正丁烷低 11.2°C")),
                "C₅H₁₂ 有 3 种、C₈H₁₈ 有 18 种、C₂₀H₄₂ 有 366319 种——异构体数目是图的计数问题");
    }

    /** 单幕展示（C5 变体）。 */
    private void singleAct(Skeleton s, String closing) {
        drawSkeleton(s, 6, 9);
        emit(12, List.of(VizEvent.of(VizEvent.EventType.MARK_SORTED,
                String.format("%s 度序列 %s，沸点 %s——度序列即指纹", s.name(), degSeq(), s.boiling()))),
                "同一个分子式下的三种骨架，度序列全不同——非同构判决一秒出");
        emit(15, List.of(VizEvent.of(VizEvent.EventType.RETURN, closing)),
                "支链多 → 分子圆 → 接触面小 → 沸点低：正 36.1°C、异 27.9°C、新 9.5°C 排队下降");
    }

    /** 逐键生长一幅碳骨架：每次 emit 添加一条边（度同步更新）。 */
    private void drawSkeleton(Skeleton s, int initLine, int edgeLine) {
        current = s;
        int n = maxVertex(s) + 1;
        degrees = new int[n];
        emit(initLine, List.of(VizEvent.of(VizEvent.EventType.INIT,
                String.format("%s：%d 个碳原子逐键搭骨架（边权 = C-C 单键）", s.name(), n))),
                "碳是建筑大师：4 只手（4 价）搭出万物——骨架图就是它的施工图");
        for (int[] e : s.edges()) {
            drawn.add(e);
            degrees[e[0]]++;
            degrees[e[1]]++;
            emit(edgeLine, List.of(VizEvent.of(VizEvent.EventType.SET,
                    String.format("C%d—C%d 成键（共 %d 键）", e[0] + 1, e[1] + 1, drawn.size()),
                    "e" + drawn.size())),
                    "每加一条键，两个碳的度各 +1——度序列在生长中逐渐定形");
        }
    }

    private static int maxVertex(Skeleton s) {
        int m = 0;
        for (int[] e : s.edges()) {
            m = Math.max(m, Math.max(e[0], e[1]));
        }
        return m;
    }

    private static int[] sortedDegrees(Skeleton s) {
        int[] deg = new int[maxVertex(s) + 1];
        for (int[] e : s.edges()) {
            deg[e[0]]++;
            deg[e[1]]++;
        }
        Arrays.sort(deg);
        return deg;
    }

    private static String seqOf(int[] deg) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < deg.length; i++) {
            sb.append(deg[i]);
            if (i < deg.length - 1) {
                sb.append(",");
            }
        }
        return sb.append("]").toString();
    }

    private String degSeq() {
        return seqOf(Arrays.stream(degrees).sorted().toArray());
    }

    private void emit(int line, List<VizEvent> events, String why) {
        StackFrame fr = new StackFrame("isomer",
                OrderedMaps.of("展品", Value.prim(current == null ? "—" : current.name()),
                        "度序列", Value.prim(degSeq()),
                        "沸点", Value.prim(current == null ? "—" : current.boiling())), null);
        Map<String, Layer> layers = new LinkedHashMap<>();
        layers.put("mem.stack", new StackLayer(List.of(fr)));
        layers.put("mem.heap", new HeapLayer(snapshotHeap()));
        layers.put("ds.graph", snapshotGraph());
        steps.add(new Step(steps.size(), Loc.line(line, why),
                Collections.unmodifiableMap(layers), List.copyOf(events), new Intent(why, INVARIANT)));
    }

    private Heap snapshotHeap() {
        Map<String, Value> fields = new LinkedHashMap<>();
        fields.put("分子式", Value.prim(variant == 0 ? "C₄H₁₀（两种）" : "C₅H₁₂（三种）"));
        fields.put("当前度序列", Value.prim(degSeq()));
        fields.put("判决", Value.prim(verdict.isEmpty() ? "搭骨架中" : verdict));
        Map<String, HeapObject> objs = new LinkedHashMap<>();
        objs.put("@1", new HeapObject("@1", "isomer", "SkeletonState",
                Collections.unmodifiableMap(fields), List.of()));
        return new Heap(Collections.unmodifiableMap(objs));
    }

    private GraphView snapshotGraph() {
        List<GraphView.GraphVertex> vs = new ArrayList<>();
        int n = degrees.length;
        for (int i = 0; i < n; i++) {
            double x = n == 4 ? (drawnEdgesFormStar() ? starX(i) : 0.08 + 0.84 * i / (n - 1))
                    : (drawnEdgesFormStar() ? starX(i) : 0.08 + 0.84 * i / (n - 1));
            double y = drawnEdgesFormStar() ? (i == 0 ? 0.72 : 0.28) : 0.5;
            vs.add(new GraphView.GraphVertex("@C" + i, "C" + (i + 1), x, y));
        }
        List<GraphView.GraphEdge> es = new ArrayList<>();
        for (int i = 0; i < drawn.size(); i++) {
            es.add(new GraphView.GraphEdge("@C" + drawn.get(i)[0], "@C" + drawn.get(i)[1], 1));
        }
        return new GraphView("碳骨架（顶点=C，边=C-C 键）", vs, es, null,
                List.of(), Map.of(), null, null, List.of());
    }

    /** 是否星形骨架（异丁烷/新戊烷：中心 C 连所有）。 */
    private boolean drawnEdgesFormStar() {
        return current != null && !current.name().contains("正");
    }

    private double starX(int i) {
        return i == 0 ? 0.5 : 0.12 + 0.76 * (i - 1) / Math.max(1, degrees.length - 2);
    }

    private Trace.TraceMeta meta() {
        return new Trace.TraceMeta(id(), title(), codeResource(),
                variant == 0 ? "C₄ 正丁烷 vs 异丁烷" : "C₅ 展品 " + variant, 12);
    }

    @Override
    public java.util.List<String> dataLines() {
        return java.util.List.of(variant == 0
                ? "Skeleton linear = new Skeleton(...)   //@DATA C₄ 双幕对比"
                : String.format("int variant = %d;   //@DATA C₅ 展品编号", variant));
    }

    @Override
    public Complexity complexity() {
        return new Complexity("O(V+E) 度序列统计", "O(V+E) 骨架存储",
                "药物分子的手性异构（沙利度胺事件）、汽油辛烷值（支链抗爆）——"
                        + "同构判定在化学里叫「结构检索」，图数据库的日常业务");
    }
}
