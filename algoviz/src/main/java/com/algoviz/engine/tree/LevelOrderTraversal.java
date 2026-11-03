package com.algoviz.engine.tree;

import com.algoviz.engine.Complexity;
import com.algoviz.engine.Algorithm;
import com.algoviz.engine.VizEvent;
import com.algoviz.engine.trace.Trace;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;

/**
 * 层序遍历（BFS）生产者：队列驱动——出队一个、访问、孩子从左到右入队。
 * 不变量：队列里永远最多只有两层的节点——出队顺序就是从上到下、从左到右。
 */
public final class LevelOrderTraversal implements Algorithm {

    private static final String SCHEMA = "1";
    private static final String INVARIANT =
            "队列里最多同时存在两层：出队顺序 = 从上到下、从左到右（和前中后序的递归完全不同路）";

    /** 与 /code/LevelOrderTraversal.java 的行号对齐 */
    private static final int L_OFFER_ROOT = 4;
    private static final int L_WHILE = 5;
    private static final int L_POLL = 6;
    private static final int L_VISIT = 7;
    private static final int L_OFFER_L = 8;
    private static final int L_OFFER_R = 9;
    private static final int L_END = 11;

    private final int[] input;

    public LevelOrderTraversal() {
        this(new int[]{5, 3, 8, 1, 4, 7, 9});
    }

    /** 随机数据：不同值建 BST，形状每次不同。 */
    public LevelOrderTraversal(int[] input) {
        this.input = input.clone();
    }

    private TreeEmitter em;
    private final Deque<Integer> queue = new ArrayDeque<>();

    @Override public String id() { return "tree.levelorder"; }
    @Override public String title() { return "层序遍历"; }
    @Override public String codeResource() { return "/code/LevelOrderTraversal.java"; }
    @Override public String docResource() { return "/markdown/level-order-traversal.md"; }

    @Override
    public Trace produce() {
        em = TreeEmitter.bst(input.clone(), "level", INVARIANT, Map.of());   // 重复 produce：重建状态
        queue.clear();

        // L4: 根入队
        queue.offer(0);
        em.tag("队首", 0);
        em.emit(L_OFFER_ROOT, List.of(VizEvent.of(VizEvent.EventType.INIT,
                "root 入队（@1）——队列是层序的心脏", "@1")),
                "和递归版最大的不同：这里没有任何调用栈，全靠队列排队");

        while (!queue.isEmpty()) {
            int u = queue.poll();
            if (!queue.isEmpty()) {
                em.tag("队首", queue.peek());
            } else {
                em.untag("队首");
            }
            // L6: 出队
            em.emit(L_POLL, List.of(VizEvent.of(VizEvent.EventType.SET,
                    "poll() → @" + (u + 1) + "（队首出队）", "@" + (u + 1))),
                    "队首出队：先进来的先访问——这就是「层」的来历");
            // L7: 访问
            int order = em.visit(u);
            em.emit(L_VISIT, List.of(VizEvent.of(VizEvent.EventType.VISIT,
                    "visit(" + em.refText(u) + ")：第 " + order + " 个访问", em.refText(u))),
                    "访问顺序 = 从上到下、从左到右——对比中序的 1,3,4,5,7,8,9 完全不同");
            // 孩子（下一层）从左到右入队
            int left = em.nodes.get(u)[1];
            if (left != -1) {
                queue.offer(left);
                em.tag("队首", queue.peek());
                em.emit(L_OFFER_L, List.of(VizEvent.of(VizEvent.EventType.SET,
                        "左孩子 @" + (left + 1) + " 入队", "@" + (left + 1))),
                        "下一层从左到右排队——当前层还没出完，它们只能等");
            }
            int right = em.nodes.get(u)[2];
            if (right != -1) {
                queue.offer(right);
                em.tag("队首", queue.peek());
                em.emit(L_OFFER_R, List.of(VizEvent.of(VizEvent.EventType.SET,
                        "右孩子 @" + (right + 1) + " 入队", "@" + (right + 1))),
                        "队首标签永远指着下一个要出队的节点");
            }
        }
        em.emit(L_END, List.of(VizEvent.of(VizEvent.EventType.RETURN,
                "队列空：全部 7 个节点按层访问完毕")), "层序完成：5 → 3,8 → 1,4,7,9——一层一层铺下来");
        return new Trace(SCHEMA, meta(input), em.steps);
    }

    @Override
    public java.util.List<String> dataLines() {
        return java.util.List.of(Algorithm.joinArray(input));
    }

    @Override
    public Complexity complexity() {
        return new Complexity("O(n)", "O(w) 队列，w = 最宽层", "每个节点入队/出队各一次 → O(n)。队列最宽 = 最底层节点数，最坏 n/2");
    }
}
