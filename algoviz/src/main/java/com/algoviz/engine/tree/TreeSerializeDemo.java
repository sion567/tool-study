package com.algoviz.engine.tree;

import com.algoviz.engine.Algorithm;
import com.algoviz.engine.Complexity;
import com.algoviz.engine.Intent;
import com.algoviz.engine.Loc;
import com.algoviz.engine.OrderedMaps;
import com.algoviz.engine.Step;
import com.algoviz.engine.Value;
import com.algoviz.engine.VizEvent;
import com.algoviz.engine.trace.Trace;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 序列化 / 反序列化：LeetCode 297 的经典结构题——树怎么存进一个字符串，又怎么原样长回来。
 * 答案：前序遍历 + null 占位符 "#"。有 "#" 占住空位，反序列化时递归结构才能对齐。
 * 画面：阶段一沿树走，流字符串在堆里逐 token 生长；阶段二推倒重建——
 * 读 token → new 节点 → 递归挂左右，树一节一节长回来。
 * 不变量：读写用同一种遍历序（前序）——写的时候 "#" 怎么占位，读的时候就怎么还位。
 */
public final class TreeSerializeDemo implements Algorithm {

    private static final String SCHEMA = "1";
    private static final String INVARIANT =
            "前序 + '#' 占空位：写出什么序就读回什么序——null 占位让递归结构在字符串里无歧义";

    private static final int[] INPUT = {5, 3, 8, 1, 4, 7, 9};

    /** 与 /code/TreeSerializeDemo.java 的行号对齐 */
    private static final int L_SER_NULL = 6;
    private static final int L_SER_NODE = 7;
    private static final int L_DESER_NEW = 14;
    private static final int L_DESER_LINK = 15;

    private TreeEmitter em;
    private final StringBuilder stream = new StringBuilder();   // 序列化流（堆里可见）
    private int readPos = 0;
    private String[] tokens;
    private String phase = "serialize";

    @Override public String id() { return "tree.serialize"; }
    @Override public String title() { return "序列化 / 反序列化"; }
    @Override public String codeResource() { return "/code/TreeSerializeDemo.java"; }
    @Override public String docResource() { return "/markdown/tree-serialize.md"; }

    @Override
    public Trace produce() {
        phase = "serialize";   // 重复 produce：阶段标记归位
        em = TreeEmitter.bst(INPUT.clone(), "serialize", INVARIANT, Map.of());
        stream.setLength(0);
        readPos = 0;
        em.tag("root", 0);
        syncStreamVar();
        em.emit(4, List.of(VizEvent.of(VizEvent.EventType.INIT, "流 = \"\"，从根开始前序写出")),
                "前序 = 根 → 左 → 右：先写自己，再递归孩子——'#' 给空位留座");
        ser(0);
        String saved = stream.toString();
        em.injectLocals.clear();
        em.injectLocals.put("stream", Value.prim("\"" + saved + "\""));
        em.emit(9, List.of(VizEvent.of(VizEvent.EventType.RETURN, "序列化完成：\"" + saved + "\"")),
                "11 个 token = 7 个值 + 4 个 '#'——null 指针数和 '#' 数的关系，数一数就懂");

        // ===== 反序列化：推倒重建 =====
        phase = "deserialize";
        em.injectLocals.clear();
        em.resetForRebuild();
        tokens = saved.split(",");
        readPos = 0;
        em.emit(11, List.of(VizEvent.of(VizEvent.EventType.INIT, "推倒重建：读指针归零，树 = 空")),
                "同一个字符串，同样的读法——递归结构自己会对齐");
        int root = deser();
        em.tag("root", root);
        em.emit(17, List.of(VizEvent.of(VizEvent.EventType.RETURN,
                "重建完成：新树与原树逐节点一致（7 节点）")),
                "反序列化的正确性 = 再序列化一遍得到同一个字符串（往返一致性）");
        em.untag("root");
        em.injectLocals.clear();
        return new Trace(SCHEMA, meta(INPUT), em.steps);
    }

    /** 前序写出：node.val 或 '#'。 */
    private void ser(int idx) {
        if (idx == -1) {
            em.push(-1);
            stream.append("#,");
            syncStreamVar();
            em.emit(L_SER_NULL, List.of(VizEvent.of(VizEvent.EventType.SET,
                    "null → 流追加 \"#\"（占住空位）")),
                    "没有 '#'，两个孩子的边界就分不清——1,3 和 3,1 会读成同一棵树");
            em.emit(L_SER_NULL, List.of(VizEvent.of(VizEvent.EventType.RETURN, "return —— 弹栈")),
                    "空位写完，递归返回");
            em.pop();
            return;
        }
        em.push(idx);
        em.keepVisited(idx);
        stream.append(em.nodes.get(idx)[0]).append(",");
        syncStreamVar();
        em.emit(L_SER_NODE, List.of(VizEvent.of(VizEvent.EventType.SET,
                "写出 " + em.refText(idx) + "(val=" + em.nodes.get(idx)[0] + ")",
                em.refText(idx))),
                "前序第一步：先写根——读回来时也先读根，顺序就咬合了");
        ser(em.nodes.get(idx)[1]);
        ser(em.nodes.get(idx)[2]);
        em.pop();
    }

    /** 前序读回：token 是 '#' 返回 -1，否则 new 节点再递归挂左右。 */
    private int deser() {
        String t = tokens[readPos++];
        if (t.equals("#")) {
            em.push(-1);
            syncStreamVar();
            em.emit(L_SER_NULL, List.of(VizEvent.of(VizEvent.EventType.COMPARE,
                    "读 \"" + t + "\" → 这里是 null（第 " + readPos + " 个 token）")),
                    "占位符还位：写时占的座，读时还的座");
            em.pop();
            return -1;
        }
        int idx = em.newNode(Integer.parseInt(t));
        em.push(idx);
        em.keepVisited(idx);
        syncStreamVar();
        em.emit(L_DESER_NEW, List.of(VizEvent.of(VizEvent.EventType.CALL,
                "读 \"" + t + "\" → newNode(" + t + ") = " + em.refText(idx),
                em.refText(idx))),
                "先造节点——它的孩子还没读，位置已经排好");
        int l = deser();
        em.nodes.get(idx)[1] = l;
        int r = deser();
        em.nodes.get(idx)[2] = r;
        em.emit(L_DESER_LINK, List.of(VizEvent.of(VizEvent.EventType.SET,
                em.refText(idx) + " ← 左" + refT(l) + "，右" + refT(r),
                em.refText(idx))),
                "递归回来把左右挂上——子树的事递归全包了");
        em.pop();
        return idx;
    }

    private String refT(int idx) {
        return idx >= 0 ? em.refText(idx) : "null";
    }

    /** 把流文本/读位置注入栈顶 locals（栈面板可见）。 */
    private void syncStreamVar() {
        em.injectLocals.clear();
        if (phase.equals("serialize")) {
            em.injectLocals.put("stream", Value.prim(stream.length() == 0 ? "\"\""
                    : "\"" + stream.substring(0, stream.length() - 1) + "\""));
        } else {
            em.injectLocals.put("read#", Value.prim(String.valueOf(readPos) + "/" + tokens.length));
        }
    }

    @Override
    public Trace.TraceMeta meta(int[] input) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < input.length; i++) {
            if (i > 0) sb.append(',');
            sb.append(input[i]);
        }
        return new Trace.TraceMeta(id(), title(), codeResource(), sb.toString(), input.length);
    }

    @Override
    public java.util.List<String> dataLines() {
        return java.util.List.of(Algorithm.joinArray(INPUT));
    }

    @Override
    public Complexity complexity() {
        return new Complexity("O(n) 每节点/token 恰处理一次", "O(n) 流 + O(h) 递归栈",
                "'#' 占位数 = null 指针数的一半（每个节点最多贡献 2 个 null 位，根除外）——"
                        + "序列化是考察「遍历 + 递归结构」的最短综合题");
    }
}
