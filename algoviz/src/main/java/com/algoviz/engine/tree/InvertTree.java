package com.algoviz.engine.tree;

import com.algoviz.engine.Algorithm;
import com.algoviz.engine.Intent;
import com.algoviz.engine.Layer;
import com.algoviz.engine.Loc;
import com.algoviz.engine.Step;
import com.algoviz.engine.VizEvent;
import com.algoviz.engine.trace.Trace;

import java.util.List;
import java.util.Map;

/**
 * 翻转二叉树：每个节点的左右孩子互换（递归）。
 * 不变量：处理完的节点，其子树内部（翻转后的）仍然是一棵合法 BST 的镜像。
 */
public final class InvertTree implements Algorithm {

    private static final String SCHEMA = "1";
    private static final String INVARIANT =
            "每个节点只做一件事：左右孩子指针互换——递归处理完，整棵树镜像";

    /** 与 /code/InvertTree.java 的行号对齐 */
    private static final int L_CALL = 2;
    private static final int L_NULL = 3;
    private static final int L_RET_NULL = 4;
    private static final int L_SWAP = 6;
    private static final int L_REC_L = 7;
    private static final int L_REC_R = 8;
    private static final int L_RET = 10;

    private static final int[] INPUT = {5, 3, 8, 1, 4, 7, 9};

    private TreeEmitter em;

    @Override public String id() { return "tree.invert"; }
    @Override public String title() { return "翻转二叉树"; }
    @Override public String codeResource() { return "/code/InvertTree.java"; }
    @Override public String docResource() { return "/markdown/invert-tree.md"; }

    @Override
    public Trace produce() {
        em = TreeEmitter.bst(INPUT.clone(), "invert", INVARIANT, Map.of());   // 重复 produce：重建
        invert(0);
        return new Trace(SCHEMA, meta(INPUT), em.steps);
    }

    private int invert(int idx) {
        if (idx == -1) {
            em.push(idx);
            em.emit(L_NULL, List.of(VizEvent.of(VizEvent.EventType.COMPARE,
                    "root = null（基线）")), "空树无需翻转");
            em.emit(L_RET_NULL, List.of(VizEvent.of(VizEvent.EventType.RETURN,
                    "return null —— 弹栈")), "基线返回");
            em.pop();
            return -1;
        }
        em.push(idx);
        em.emit(L_CALL, List.of(VizEvent.of(VizEvent.EventType.CALL,
                "调用 invert(" + em.refText(idx) + ") —— 压栈")),
                "进入 invert(" + em.refText(idx) + ")：翻转以它为根的子树");
        // 递归翻转左右子树（先处理孩子，最后换自己——后序思路）
        int left = em.nodes.get(idx)[1];
        int right = em.nodes.get(idx)[2];
        em.emit(L_SWAP, List.of(VizEvent.of(VizEvent.EventType.COMPARE,
                "先递归处理两个孩子，再交换本节点的左右指针")),
                "递归信任：孩子的事交给递归，我只管本层");
        int newLeft = invert(left);
        em.nodes.get(idx)[1] = newLeft;
        int newRight = invert(right);
        em.nodes.get(idx)[2] = newRight;
        // 交换本节点左右
        em.nodes.get(idx)[1] = newRight;
        em.nodes.get(idx)[2] = newLeft;
        em.emit(L_SWAP, List.of(VizEvent.of(VizEvent.EventType.SWAP,
                "@" + (idx + 1) + ".left ↔ .right 互换（"
                        + em.refText(newLeft) + " ↔ " + em.refText(newRight) + "）",
                "@" + (idx + 1))),
                "核心动作：左右指针互换——子树跟着指针一起翻");
        em.emit(L_RET, List.of(VizEvent.of(VizEvent.EventType.RETURN,
                "return @" + (idx + 1) + " —— 弹栈")),
                "翻转后的子树根交回父调用");
        em.pop();
        return idx;
    }
}
