package com.algoviz.engine.tree;

import com.algoviz.engine.OrderedMaps;
import com.algoviz.engine.Algorithm;
import com.algoviz.engine.Intent;
import com.algoviz.engine.Layer;
import com.algoviz.engine.Loc;
import com.algoviz.engine.Step;
import com.algoviz.engine.Value;
import com.algoviz.engine.VizEvent;
import com.algoviz.engine.trace.Trace;

import java.util.List;
import java.util.Map;

/**
 * 最近公共祖先（LCA）：两个节点的最深公共祖先。
 * 递归版：左右各自找 p/q，都找到 → 当前节点就是 LCA；只找到一边 → 继续向上传递。
 */
public final class LcaDemo implements Algorithm {

    private static final String SCHEMA = "1";
    private static final String INVARIANT =
            "p 和 q 分别在左右子树 → 当前节点就是 LCA；都在同一边 → LCA 在那边";

    /** 与 /code/LcaDemo.java 的行号对齐 */
    private static final int L_CALL = 2;
    private static final int L_NULL = 3;
    private static final int L_RET_P = 4;
    private static final int L_HIT = 5;
    private static final int L_BOTH = 6;
    private static final int L_LEFT_SIDE = 7;
    private static final int L_RIGHT_SIDE = 8;
    private static final int L_RET_NULL2 = 10;

    private static final int[] INPUT = {5, 3, 8, 1, 4, 7, 9};
    private static final int P_VAL = 1;
    private static final int Q_VAL = 4;

    private TreeEmitter em;
    private String foundMessage = "";

    @Override public String id() { return "tree.lca"; }
    @Override public String title() { return "最近公共祖先"; }
    @Override public String codeResource() { return "/code/LcaDemo.java"; }
    @Override public String docResource() { return "/markdown/lca.md"; }

    @Override
    public Trace produce() {
        em = TreeEmitter.bst(INPUT.clone(), "lca", INVARIANT, OrderedMaps.ofStrings(
                "p", String.valueOf(P_VAL), "q", String.valueOf(Q_VAL)));
        lca(0, P_VAL, Q_VAL);
        return new Trace(SCHEMA, meta(INPUT), em.steps);
    }

    /** 返回：找到 p/q 的节点下标，或 LCA，或 -1（都没找到）。 */
    private int lca(int idx, int p, int q) {
        if (idx == -1) {
            em.push(idx);
            em.emit(L_NULL, List.of(VizEvent.of(VizEvent.EventType.COMPARE,
                    "root = null → 没找到（基线）")), "null：这条路上没有 p 也没有 q");
            em.emit(L_RET_NULL2, List.of(VizEvent.of(VizEvent.EventType.RETURN,
                    "return null —— 弹栈")), "空手而归");
            em.pop();
            return -1;
        }
        em.push(idx);
        int val = em.nodes.get(idx)[0];
        em.emit(L_CALL, List.of(VizEvent.of(VizEvent.EventType.CALL,
                "调用 lca(" + em.refText(idx) + ") —— 压栈")),
                "进入 " + em.refText(idx) + "：找 p=" + p + " 或 q=" + q);
        if (val == p || val == q) {
            foundMessage = "找到目标节点 " + em.refText(idx) + "（val=" + val + "）";
            em.emit(L_HIT, List.of(VizEvent.of(VizEvent.EventType.MARK_SORTED,
                    foundMessage, em.refText(idx))),
                    "命中 p 或 q：这个节点就是答案（或答案的祖先）");
            em.emit(L_RET_P, List.of(VizEvent.of(VizEvent.EventType.RETURN,
                    "return " + em.refText(idx) + " —— 向上传递")), "把发现报告给父调用");
            em.pop();
            return idx;
        }
        int left = lca(em.nodes.get(idx)[1], p, q);
        int right = lca(em.nodes.get(idx)[2], p, q);
        if (left != -1 && right != -1) {
            em.emit(L_BOTH, List.of(VizEvent.of(VizEvent.EventType.MARK_SORTED,
                    "p 和 q 分居两侧 → " + em.refText(idx) + " 就是 LCA！", em.refText(idx))),
                    "一边找到一个 → 当前节点是它们最后相聚的地方");
            em.emit(L_RET_P, List.of(VizEvent.of(VizEvent.EventType.RETURN,
                    "return " + em.refText(idx) + "（LCA）")), "LCA 确定");
            em.pop();
            return idx;
        }
        if (left != -1) {
            em.emit(L_LEFT_SIDE, List.of(VizEvent.of(VizEvent.EventType.SET,
                    "左边找到 " + em.refText(left) + " → 向上传递", em.refText(left))),
                    "答案在左子树，原样上传");
            em.emit(L_RET_P, List.of(VizEvent.of(VizEvent.EventType.RETURN,
                    "return " + em.refText(left))), "上传");
            em.pop();
            return left;
        }
        if (right != -1) {
            em.emit(L_RIGHT_SIDE, List.of(VizEvent.of(VizEvent.EventType.SET,
                    "右边找到 " + em.refText(right) + " → 向上传递", em.refText(right))),
                    "答案在右子树，原样上传");
            em.emit(L_RET_P, List.of(VizEvent.of(VizEvent.EventType.RETURN,
                    "return " + em.refText(right))), "上传");
            em.pop();
            return right;
        }
        em.emit(L_RET_NULL2, List.of(VizEvent.of(VizEvent.EventType.RETURN,
                "两边都没找到 → return null")), "这棵子树里没有 p 也没有 q");
        em.pop();
        return -1;
    }

    private Trace.TraceMeta meta() {
        StringBuilder sb = new StringBuilder();
        for (int k = 0; k < INPUT.length; k++) {
            if (k > 0) sb.append(',');
            sb.append(INPUT[k]);
        }
        return new Trace.TraceMeta(id(), title(), codeResource(),
                sb + " | p=" + P_VAL + ", q=" + Q_VAL, INPUT.length);
    }

    @Override
    public com.algoviz.engine.Complexity complexity() {
        return new com.algoviz.engine.Complexity("O(n) 每节点一次", "O(h) 递归栈",
                "递归遍历整棵树，每个节点一次；栈深 = 树高");
    }
}
