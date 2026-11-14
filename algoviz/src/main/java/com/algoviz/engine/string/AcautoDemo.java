package com.algoviz.engine.string;

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
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 字符串·AC 自动机：KMP 的多模式推广——Trie + fail 指针 + 一次扫描命中全部模式。
 * 教科书例：模式 {he, she, his, hers}、文本 "ushers" → 一次扫描命中 she + he(沿 fail 链) + hers。
 * fail 指针 = 最长真后缀转移：she 的 fail 指向 he——扫描中沿 fail 链跳就能"顺手"捡到嵌套命中。
 * 画面（math.matrix 复用）：网格一 = 文本扫描行（字符/状态），网格二 = Trie 节点表（节点/代表串/词尾/fail）。
 */
public final class AcautoDemo implements Algorithm {

    private static final String SCHEMA = "1";
    private static final String INVARIANT =
            "fail(节点) = Trie 中该串的最长真后缀——扫描时 mismatch 沿 fail 跳（不回退文本指针），"
                    + "命中一个模式后沿 fail 链还能捡到嵌套的短模式——KMP 的单模式 fail 推广到 Trie 上";

    private static final String[] PATTERNS = {"he", "she", "his", "hers"};
    private static final String TEXT = "ushers";

    private final List<Step> steps = new ArrayList<>();
    private final List<String> nodeStr = new ArrayList<>(List.of(""));
    private final List<Integer> failOf = new ArrayList<>(List.of(0));
    private final List<String> tailOf = new ArrayList<>(List.of(""));
    private int[][] go;
    private int state;
    private int scanPos = -1;
    private final List<String> hits = new ArrayList<>();

    @Override public String id() { return "string.acauto"; }
    @Override public String title() { return "AC 自动机"; }
    @Override public String codeResource() { return "/code/AcautoDemo.java"; }
    @Override public String docResource() { return "/markdown/acauto.md"; }

    @Override
    public Trace produce() {
        steps.clear();
        nodeStr.clear();
        failOf.clear();
        tailOf.clear();
        nodeStr.add("");
        failOf.add(0);
        tailOf.add("");
        hits.clear();
        state = 0;
        scanPos = -1;
        go = new int[16][26];
        for (int[] row : go) {
            Arrays.fill(row, -1);
        }
        for (String p : PATTERNS) {
            int cur = 0;
            for (int i = 0; i < p.length(); i++) {
                int ch = p.charAt(i) - 'a';
                if (go[cur][ch] == -1) {
                    nodeStr.add(p.substring(0, i + 1));
                    failOf.add(0);
                    tailOf.add("");
                    go[cur][ch] = nodeStr.size() - 1;
                }
                cur = go[cur][ch];
            }
            tailOf.set(cur, p);
            emit(9, List.of(VizEvent.of(VizEvent.EventType.SET,
                    String.format("插入「%s」——Trie 共 %d 节点，共享前缀不重复建（she 复用 he 的两条边）", p, nodeStr.size()))),
                    "Trie 的省字诀：公共前缀只建一次——新模式直接挂在已有前缀上");
        }
        java.util.ArrayDeque<Integer> bq = new java.util.ArrayDeque<>();
        for (int ch = 0; ch < 26; ch++) {
            if (go[0][ch] != -1) {
                failOf.set(go[0][ch], 0);
                bq.add(go[0][ch]);
            }
        }
        while (!bq.isEmpty()) {
            int u = bq.poll();
            for (int ch = 0; ch < 26; ch++) {
                int v = go[u][ch];
                if (v == -1) {
                    continue;
                }
                int f = failOf.get(u);
                while (f != 0 && go[f][ch] == -1) {
                    f = failOf.get(f);
                }
                int nf = go[f][ch] == -1 || go[f][ch] == v ? 0 : go[f][ch];
                failOf.set(v, nf);
                bq.add(v);
            }
        }
        emit(15, List.of(VizEvent.of(VizEvent.EventType.MESSAGE,
                "fail 指针构建完成（BFS）：fail(节点she)=节点he——最长真后缀转移")),
                "fail 链 = '这个串的最长真后缀还在 Trie 里吗'——KMP 的 next 数组推广到树上");
        state = 0;
        scanPos = -1;
        emit(18, List.of(VizEvent.of(VizEvent.EventType.MESSAGE,
                "扫描开始：文本指针只前进不回退——mismatch 时状态沿 fail 跳（KMP 的多模式版）")),
                "一次扫描命中全部模式：n 个字符跑完，不用为每个模式重扫");
        for (int i = 0; i < TEXT.length(); i++) {
            int ch = TEXT.charAt(i) - 'a';
            while (state != 0 && go[state][ch] == -1) {
                state = failOf.get(state);
            }
            state = Math.max(go[state][ch], 0);
            scanPos = i;
            int t = state;
            while (t != 0) {
                if (!tailOf.get(t).isEmpty()) {
                    hits.add(tailOf.get(t) + "@" + (i + 1));
                }
                t = failOf.get(t);
            }
            boolean hit = !tailOf.get(state).isEmpty();
            emit(21, List.of(VizEvent.of(VizEvent.EventType.SET,
                    String.format("文本[%d]='%c' → 状态 %d%s", i, TEXT.charAt(i), state,
                            hit ? "（词尾：" + tailOf.get(state) + " 命中！）" : ""))),
                    hit ? "词尾命中：本状态代表的模式在文本中结束——继续沿 fail 链看嵌套"
                            : "状态沿 Trie 前进或 fail 跳——文本指针永不回退");
            int fl = failOf.get(state);
            if (fl > 0 && !tailOf.get(fl).isEmpty()) {
                emit(23, List.of(VizEvent.of(VizEvent.EventType.MARK_SORTED,
                        String.format("沿 fail 链：节点 %d 也是词尾（%s）——嵌套命中！", fl, tailOf.get(fl)))),
                        "she 命中的同时沿 fail 到 he——'一个文本位置可命中多个模式'");
            }
        }
        state = 0;
        scanPos = -1;
        emit(26, List.of(VizEvent.of(VizEvent.EventType.MARK_SORTED,
                "扫描完成：命中 " + hits + "——3 次命中 1 次扫描")),
                "AC 自动机(1975)：grep 多模式搜索、入侵检测、DNA 序列比对——多模式匹配的工业标准");
        emit(28, List.of(VizEvent.of(VizEvent.EventType.RETURN,
                "对照 KMP：单模式 next → 多模式 fail；单模式扫描 → Trie 上扫描——推广而不重写")),
                "复杂度 O(文本 + 模式总长 + 命中数)——模式再多也只扫一遍文本");
        return new Trace(SCHEMA, meta(), steps);
    }

    private void emit(int line, List<VizEvent> events, String why) {
        StackFrame frame = new StackFrame("acauto",
                OrderedMaps.of("状态", Value.prim(String.valueOf(state)),
                        "文本位", Value.prim(scanPos >= 0 ? scanPos + "/" + TEXT.length() : "-"),
                        "命中", Value.prim(hits.isEmpty() ? "-" : hits.size() + " 次")), null);
        Map<String, Layer> layers = new LinkedHashMap<>();
        layers.put("mem.stack", new StackLayer(List.of(frame)));
        layers.put("mem.heap", new HeapLayer(snapshotHeap()));
        layers.put("math.matrix", snapshotMath());
        steps.add(new Step(steps.size(), Loc.line(line, why),
                Collections.unmodifiableMap(layers), List.copyOf(events), new Intent(why, INVARIANT)));
    }

    private Heap snapshotHeap() {
        Map<String, Value> fields = new LinkedHashMap<>();
        fields.put("模式", Value.prim(Arrays.toString(PATTERNS)));
        fields.put("文本", Value.prim("\"" + TEXT + "\""));
        fields.put("命中", Value.prim(hits.isEmpty() ? "-" : String.join(", ", hits)));
        Map<String, HeapObject> objs = new LinkedHashMap<>();
        objs.put("@1", new HeapObject("@1", "ac", "AhoState",
                Collections.unmodifiableMap(fields), List.of()));
        return new Heap(Collections.unmodifiableMap(objs));
    }

    private MathView snapshotMath() {
        List<String> scanVals = new ArrayList<>();
        Set<Integer> scanActive = new LinkedHashSet<>();
        for (int i = 0; i < TEXT.length(); i++) {
            scanVals.add(String.valueOf(TEXT.charAt(i)));
        }
        for (int i = 0; i < TEXT.length(); i++) {
            scanVals.add(i == scanPos ? String.valueOf(state) : (i < scanPos ? "OK" : "."));
        }
        if (scanPos >= 0) {
            scanActive.add(scanPos);
            scanActive.add(TEXT.length() + scanPos);
        }
        MathView.Grid scanGrid = new MathView.Grid("scan", "文本扫描（上行=字符，下行=进入的状态；黄=当前位）",
                2, TEXT.length(), scanVals, null, null, scanActive, Set.of());
        List<String> nodeVals = new ArrayList<>();
        Set<Integer> nodeActive = new LinkedHashSet<>();
        for (int n = 0; n < nodeStr.size(); n++) {
            nodeVals.add("N" + n);
            nodeVals.add(nodeStr.get(n).isEmpty() ? "root" : nodeStr.get(n));
            nodeVals.add(tailOf.get(n).isEmpty() ? "-" : tailOf.get(n));
            nodeVals.add(String.valueOf(failOf.get(n)));
            if (state == n && scanPos >= 0) {
                nodeActive.add(n * 4);
                nodeActive.add(n * 4 + 1);
            }
        }
        MathView.Grid nodeGrid = new MathView.Grid("trie", "Trie 节点表（fail = 最长真后缀转移）",
                nodeStr.size(), 4, nodeVals, null, null, nodeActive, Set.of());
        return new MathView("AC 自动机", List.of(scanGrid, nodeGrid));
    }

    private Trace.TraceMeta meta() {
        return new Trace.TraceMeta(id(), title(), codeResource(),
                "4 模式 ushers", PATTERNS.length + TEXT.length() + 6);
    }

    @Override
    public java.util.List<String> dataLines() {
        return java.util.List.of("String text = \"ushers\";   //@DATA 文本");
    }

    @Override
    public Complexity complexity() {
        return new Complexity("O(文本 + 模式总长 + 命中数)", "O(模式总长 × 字符集) Trie",
                "grep 多模式、入侵检测、DNA 序列比对——AC 自动机把'k 个模式'变成'1 次扫描'");
    }
}
