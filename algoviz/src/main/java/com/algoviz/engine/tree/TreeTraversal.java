package com.algoviz.engine.tree;

import com.algoviz.engine.Algorithm;
import com.algoviz.engine.VizEvent;
import com.algoviz.engine.trace.Trace;

import java.util.List;
import java.util.Map;

/**
 * 递归遍历生产者（前/中/后序，同一类不同 Mode）：
 * 调用栈逐层压弹（含 null 调用），调用树 = 树本身 + 访问序号徽章（蓝图 M4 双视图）。
 * 行号常量与 /code/Traversal{Pre,In,Post}.java 对齐。
 */
public final class TreeTraversal implements Algorithm {

    public enum Mode { PRE, IN, POST }

    private static final String SCHEMA = "1";
    private static final String INVARIANT =
            "任何时刻：调用栈是从根到当前节点的路径；已访问节点按序号构成遍历序";

    private static final int L_METHOD = 2;
    private static final int L_IF = 3;
    private static final int L_RET = 4;
    private static final int L_END = 9;

    private final Mode mode;
    private final int[] input;
    private TreeEmitter em;

    private final int visitLine;
    private final int leftLine;
    private final int rightLine;

    public TreeTraversal(Mode mode, int[] input) {
        this.mode = mode;
        this.input = input.clone();
        this.em = TreeEmitter.bst(this.input, "traverse", INVARIANT, Map.of());
        switch (mode) {
            case PRE -> {
                visitLine = 6;
                leftLine = 7;
                rightLine = 8;
            }
            case IN -> {
                leftLine = 6;
                visitLine = 7;
                rightLine = 8;
            }
            default -> {
                leftLine = 6;
                rightLine = 7;
                visitLine = 8;
            }
        }
    }

    @Override
    public String id() {
        return "tree.traversal." + mode.name().toLowerCase();
    }

    @Override
    public String title() {
        return switch (mode) {
            case PRE -> "前序遍历";
            case IN -> "中序遍历";
            case POST -> "后序遍历";
        };
    }

    @Override
    public String codeResource() {
        return "/code/Traversal" + mode.name().charAt(0) + mode.name().substring(1).toLowerCase() + ".java";
    }

    @Override
    public String docResource() {
        return "/markdown/tree-traversal.md";
    }

    @Override
    public Trace produce() {
        em = TreeEmitter.bst(input.clone(), "traverse", INVARIANT, Map.of());   // 重复 produce：重建状态
        traverse(0);
        return new Trace(SCHEMA, meta(input), em.steps);
    }

    private void traverse(int idx) {
        em.push(idx);
        em.emit(L_METHOD,
                List.of(VizEvent.of(VizEvent.EventType.CALL,
                        "调用 traverse(" + em.refText(idx) + ") —— 压栈")),
                "进入 traverse(" + em.refText(idx) + ")：调用栈多了一帧（右面板从下往上数）");

        if (idx == -1) {
            em.emit(L_IF,
                    List.of(VizEvent.of(VizEvent.EventType.COMPARE, "node = null → return（基线）")),
                    "node == null：空树也是树，这是递归的出口");
            em.emit(L_RET,
                    List.of(VizEvent.of(VizEvent.EventType.RETURN,
                            "traverse(null) 返回 —— 弹栈（什么都不留下）")),
                    "基线返回：弹掉栈顶，回到父调用");
            em.pop();
            return;
        }

        em.emit(L_IF,
                List.of(VizEvent.of(VizEvent.EventType.COMPARE,
                        "node = @" + (idx + 1) + " ≠ null → 继续递归", "@" + (idx + 1))),
                "node ≠ null：站在 @" + (idx + 1) + " 上，按「" + orderName() + "」处理");

        switch (mode) {
            case PRE -> {
                visit(idx);
                traverse(em.nodes.get(idx)[1]);
                traverse(em.nodes.get(idx)[2]);
            }
            case IN -> {
                traverse(em.nodes.get(idx)[1]);
                visit(idx);
                traverse(em.nodes.get(idx)[2]);
            }
            case POST -> {
                traverse(em.nodes.get(idx)[1]);
                traverse(em.nodes.get(idx)[2]);
                visit(idx);
            }
        }

        em.emit(L_END,
                List.of(VizEvent.of(VizEvent.EventType.RETURN,
                        "traverse(" + em.refText(idx) + ") 返回 —— 弹栈")),
                "@" + (idx + 1) + " 的整棵子树处理完毕，弹栈回到父调用");
        em.pop();
    }

    private void visit(int idx) {
        int order = em.visit(idx);
        em.emit(visitLine,
                List.of(VizEvent.of(VizEvent.EventType.VISIT,
                        "visit(" + em.refText(idx) + ")：第 " + order + " 个访问", em.refText(idx))),
                switch (mode) {
                    case PRE -> "前序位：先访问根 @" + (idx + 1) + "，再进左右子树";
                    case IN -> "中序位：左子树处理完了，轮到根 @" + (idx + 1);
                    case POST -> "后序位：左右子树都完了，收尾 @" + (idx + 1);
                });
    }

    private String orderName() {
        return switch (mode) {
            case PRE -> "根左右";
            case IN -> "左根右";
            case POST -> "左右根";
        };
    }
}
