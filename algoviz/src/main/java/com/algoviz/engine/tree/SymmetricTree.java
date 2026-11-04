package com.algoviz.engine.tree;

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
 * 对称二叉树判断：左子树与右子树是否镜像。
 * 双指针（leftWalker / rightWalker）同步走：left 走左时 right 走右。
 */
public final class SymmetricTree implements Algorithm {

    private static final String SCHEMA = "1";
    private static final String INVARIANT =
            "对称 ⟺ 每对镜像位置的值相等，且 left.left = right.right、left.right = right.left";

    private static final int[] INPUT = {5, 3, 8, 1, 4, 8, 3, 1, 4};   // 最后四个构成镜像

    private TreeEmitter em;

    @Override public String id() { return "tree.symmetric"; }
    @Override public String title() { return "对称二叉树"; }
    @Override public String codeResource() { return "/code/SymmetricTree.java"; }
    @Override public String docResource() { return "/markdown/symmetric-tree.md"; }

    @Override
    public Trace produce() {
        // 手工构建对称树：root 5, 左 3(1,null), 右 3(1,null)... 简化用数组索引手工指定
        em = new TreeEmitter(new int[0], "symmetric", INVARIANT, Map.of());
        // 构建对称树：        5
        //                   /   \
        //                  3     3
        //                 / \   / \
        //                1   4 4   1
        int root = em.newNode(5);
        int l3 = em.newNode(3);
        int r3 = em.newNode(3);
        int ll1 = em.newNode(1);
        int lr4 = em.newNode(4);
        int rl4 = em.newNode(4);
        int rr1 = em.newNode(1);
        em.nodes.get(root)[1] = l3;
        em.nodes.get(root)[2] = r3;
        em.nodes.get(l3)[1] = ll1;
        em.nodes.get(l3)[2] = lr4;
        em.nodes.get(r3)[1] = rl4;
        em.nodes.get(r3)[2] = rr1;

        em.push(root);
        em.emit(2, List.of(VizEvent.of(VizEvent.EventType.INIT,
                "isMirror(root.left, root.right)：两根指针从 3 和 3 出发")),
                "对称 ⟺ 左子树与右子树互为镜像——双指针同步走");

        boolean result = isMirror(l3, r3);

        em.emit(10, List.of(VizEvent.of(VizEvent.EventType.RETURN,
                "结果：" + (result ? "对称 ✓" : "不对称 ✗"))),
                result ? "每一对镜像位置都相等 → 对称" : "存在不相等的镜像位置 → 不对称");
        em.pop();
        return new Trace(SCHEMA, meta(), em.steps);
    }

    private boolean isMirror(int a, int b) {
        em.push(a);
        // 注意右指针在栈上显示为 b
        if (a == -1 && b == -1) {
            em.emit(3, List.of(VizEvent.of(VizEvent.EventType.COMPARE,
                    "两个都 null → 对称（基线）")), "null 对 null = 对称");
            em.pop();
            return true;
        }
        if (a == -1 || b == -1) {
            em.emit(4, List.of(VizEvent.of(VizEvent.EventType.COMPARE,
                    "一边 null 一边非 null → 不对称")),
                    "结构不同 → 不对称");
            em.pop();
            return false;
        }
        int av = em.nodes.get(a)[0];
        int bv = em.nodes.get(b)[0];
        em.emit(5, List.of(VizEvent.of(VizEvent.EventType.COMPARE,
                em.refText(a) + ".val=" + av + " vs " + em.refText(b) + ".val=" + bv
                        + (av == bv ? " ✓ 相等" : " ✗ 不相等"),
                em.refText(a), em.refText(b))),
                "镜像位置的值必须相等");
        if (av != bv) {
            em.pop();
            return false;
        }
        em.emit(7, List.of(VizEvent.of(VizEvent.EventType.SET,
                "递归：外外侧 (" + em.refText(em.nodes.get(a)[1]) + ", "
                        + em.refText(em.nodes.get(b)[2]) + ") + 内内侧 ("
                        + em.refText(em.nodes.get(a)[2]) + ", " + em.refText(em.nodes.get(b)[1]) + ")")),
                "镜像递归：left.left 对 right.right，left.right 对 right.left");
        boolean outer = isMirror(em.nodes.get(a)[1], em.nodes.get(b)[2]);
        if (!outer) {
            em.pop();
            return false;
        }
        boolean inner = isMirror(em.nodes.get(a)[2], em.nodes.get(b)[1]);
        em.pop();
        return inner;
    }

    private Trace.TraceMeta meta() {
        return new Trace.TraceMeta(id(), title(), codeResource(), "对称 7 节点", 7);
    }

    @Override
    public com.algoviz.engine.Complexity complexity() {
        return new com.algoviz.engine.Complexity("O(n) 每节点一次", "O(h) 递归栈",
                "双指针同步走，每个节点只访问一次；栈深 = 树高");
    }
}
