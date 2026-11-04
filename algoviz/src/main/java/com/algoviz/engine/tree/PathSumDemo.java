package com.algoviz.engine.tree;

import com.algoviz.engine.Algorithm;
import com.algoviz.engine.Complexity;
import com.algoviz.engine.Intent;
import com.algoviz.engine.Loc;
import com.algoviz.engine.Step;
import com.algoviz.engine.VizEvent;
import com.algoviz.engine.trace.Trace;

import java.util.List;
import java.util.Map;

/**
 * 路径总和（根→叶）：树 {5,3,8,1,4,7,9}，目标 12。
 * 叶子只有 1/4/7/9 → 根到叶总和 9/12/20/22，恰好 5→3→4 = 12 命中。
 * 画面：递归栈逐层深入（栈帧链 = 当前路径），sum 标签挂在当前节点上，
 * 命中的路径节点保持高亮，未命中分支回溯后灰显。
 * 不变量：sum 恒等于根到当前节点的路径和——递归下传 sum+node.val，叶子处一比即知。
 */
public final class PathSumDemo implements Algorithm {

    private static final String SCHEMA = "1";
    private static final String INVARIANT =
            "sum 恒等于根到当前节点的路径和——递归下传 sum+node.val，叶子处一比即知";

    private static final int[] INPUT = {5, 3, 8, 1, 4, 7, 9};
    private static final int TARGET = 12;

    private TreeEmitter em;
    private int sum;
    private boolean foundAny;
    private String sumTag = "";   // 当前 sum 标签名（换值前先 untag）

    @Override public String id() { return "tree.pathsum"; }
    @Override public String title() { return "路径总和"; }
    @Override public String codeResource() { return "/code/PathSumDemo.java"; }
    @Override public String docResource() { return "/markdown/path-sum.md"; }

    @Override
    public Trace produce() {
        em = TreeEmitter.bst(INPUT.clone(), "pathSum", INVARIANT, Map.of());   // 重复 produce：重建
        sum = 0;
        foundAny = false;
        sumTag = "";
        dfs(0);
        untagSum();
        em.emit(14, List.of(VizEvent.of(VizEvent.EventType.RETURN,
                        foundAny ? "存在根→叶路径和 = " + TARGET + "（栈里那条链就是路径）"
                                : "不存在这样的路径")),
                "所有分支试完——递归把整棵树走了一遍");
        return new Trace(SCHEMA, meta(INPUT), em.steps);
    }

    /** 返回：以 idx 为根的子树里是否存在根→叶路径和 = 剩余目标。 */
    private boolean dfs(int idx) {
        if (idx == -1) {
            em.push(-1);
            em.emit(4, List.of(VizEvent.of(VizEvent.EventType.COMPARE,
                    "node = null（基线）——空节点不是叶子，不算路径")), "空节点不是叶子，不构成路径");
            em.pop();
            return false;
        }
        em.push(idx);
        int val = em.nodes.get(idx)[0];
        sum += val;
        tagSum(idx);
        em.emit(6, List.of(VizEvent.of(VizEvent.EventType.CALL,
                        "进入 " + em.refText(idx) + "(val=" + val + ")，sum ← " + sum)),
                "下传 sum+val——栈帧链就是根到这里的路径");
        boolean isLeaf = em.nodes.get(idx)[1] == -1 && em.nodes.get(idx)[2] == -1;
        if (isLeaf) {
            boolean hit = sum == TARGET;
            if (hit) {
                foundAny = true;
                em.keepVisited(idx);
                em.emit(9, List.of(VizEvent.of(VizEvent.EventType.MARK_SORTED,
                        "叶子 " + val + "：sum = " + sum + " = target ✓ 命中！")),
                        "根→叶路径和恰为目标——栈里那条链就是答案");
            } else {
                em.emit(9, List.of(VizEvent.of(VizEvent.EventType.COMPARE,
                        "叶子 " + val + "：sum = " + sum + " ≠ " + TARGET + "，此路不通")),
                        "只有叶子才是完整路径——中途节点不判");
            }
            em.pop();
            sum -= val;
            retagToTop();
            em.emit(10, List.of(VizEvent.of(VizEvent.EventType.RETURN,
                            "return " + hit + " —— 弹栈，sum 回退到 " + sum)),
                    "回溯：弹出本节点，sum 减回父层值");
            return hit;
        }
        boolean left = dfs(em.nodes.get(idx)[1]);
        boolean right = left || dfs(em.nodes.get(idx)[2]);
        if (right) em.keepVisited(idx);
        em.pop();
        sum -= val;
        retagToTop();
        em.emit(10, List.of(VizEvent.of(VizEvent.EventType.RETURN,
                        "return " + right + " —— 左右都试完，弹栈")),
                "孩子命中则本节点也在路径上（keepVisited）——否则灰显回父层");
        return right;
    }

    private void tagSum(int idx) {
        untagSum();
        sumTag = "sum=" + sum;
        em.tag(sumTag, idx);
    }

    private void untagSum() {
        if (!sumTag.isEmpty()) {
            em.untag(sumTag);
            sumTag = "";
        }
    }

    /** 回溯后把 sum 标签挪回栈顶节点（栈空则撤掉）。 */
    private void retagToTop() {
        untagSum();
        if (!em.callStack.isEmpty() && em.callStack.get(em.callStack.size() - 1) >= 0) {
            sumTag = "sum=" + sum;
            em.tag(sumTag, em.callStack.get(em.callStack.size() - 1));
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
        return java.util.List.of(Algorithm.joinArray(INPUT), "int targetSum = " + TARGET + ";");
    }

    @Override
    public Complexity complexity() {
        return new Complexity("O(n) 每节点恰访问一次", "O(h) 递归栈 = 树高",
                "前序遍历 + 边走边加：sum 作参数下传就不用真存路径；叶子处 O(1) 判定");
    }
}
