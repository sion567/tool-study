package com.algoviz.engine.tree;

import com.algoviz.engine.Algorithm;
import com.algoviz.engine.VizEvent;
import com.algoviz.engine.trace.Trace;

import java.util.List;
import java.util.Map;

/**
 * BST 插入生产者：比大小往下走，空位上造盒子挂上去。
 * 行号常量与 /code/BstInsert.java 对齐；画面核心：橙圈 = 当前比较节点，绿 = 新挂上的节点。
 */
public final class BstInsert implements Algorithm {

    private static final String SCHEMA = "1";
    private static final String INVARIANT =
            "走到哪里都对：左子树 < 根 < 右子树（BST 性质），新值最终挂在搜索路径尽头的空位上";

    private static final int L_METHOD = 2;
    private static final int L_IF_NULL = 3;
    private static final int L_NEW = 4;
    private static final int L_IF_LT = 6;
    private static final int L_ASSIGN_L = 7;
    private static final int L_IF_GT = 8;
    private static final int L_ASSIGN_R = 9;
    private static final int L_RET = 12;

    private final int[] input;
    private final int insertVal;
    private TreeEmitter em;

    public BstInsert(int[] input, int insertVal) {
        this.input = input.clone();
        this.insertVal = insertVal;
        this.em = TreeEmitter.bst(this.input, "insert", INVARIANT, Map.of("val", String.valueOf(insertVal)));
    }

    @Override
    public String id() {
        return "tree.bst.insert";
    }

    @Override
    public String title() {
        return "BST 插入";
    }

    @Override
    public String codeResource() {
        return "/code/BstInsert.java";
    }

    @Override
    public String docResource() {
        return "/markdown/bst-insert.md";
    }

    @Override
    public Trace produce() {
        em = TreeEmitter.bst(input.clone(), "insert", INVARIANT,
                Map.of("val", String.valueOf(insertVal)));   // 重复 produce：重建状态
        if (em.nodes.isEmpty()) {
            int root = em.newNode(insertVal);
            em.push(root);
            em.emit(L_NEW, List.of(VizEvent.of(VizEvent.EventType.SET,
                    "new Node(" + insertVal + ") = @" + (root + 1) + " —— 空树上造根", "@" + (root + 1))),
                    "空树：新节点直接当根");
            em.emit(L_RET, List.of(VizEvent.of(VizEvent.EventType.RETURN, "return @" + (root + 1))),
                    "返回新根");
            em.pop();
            return new Trace(SCHEMA, meta(input), em.steps);
        }
        insert(0);
        return new Trace(SCHEMA, meta(input), em.steps);
    }

    private int insert(int idx) {
        em.push(idx);
        em.emit(L_METHOD, List.of(VizEvent.of(VizEvent.EventType.CALL,
                        "调用 insert(root=" + em.refText(idx) + ", val=" + insertVal + ") —— 压栈")),
                "进入 insert(" + em.refText(idx) + ")：带着要插的值 " + insertVal + " 走搜索路径");

        if (idx == -1) {
            em.emit(L_IF_NULL, List.of(VizEvent.of(VizEvent.EventType.COMPARE,
                    "root = null → 落位（基线）")), "root == null：搜索路径尽头就是空位");
            int newIdx = em.newNode(insertVal);
            em.keepVisited(newIdx);
            em.emit(L_NEW, List.of(VizEvent.of(VizEvent.EventType.SET,
                    "new Node(" + insertVal + ") = @" + (newIdx + 1) + " —— 空位上造新盒子",
                    "@" + (newIdx + 1))),
                    "return new Node(val)：堆里造盒子，搜索路径决定它的位置");
            em.emit(L_RET, List.of(VizEvent.of(VizEvent.EventType.RETURN,
                    "return @" + (newIdx + 1) + " —— 弹栈")),
                    "把新盒子作为返回值交回给父调用");
            em.pop();
            return newIdx;
        }

        int curVal = em.nodes.get(idx)[0];
        if (insertVal < curVal) {
            em.emit(L_IF_LT, List.of(VizEvent.of(VizEvent.EventType.COMPARE,
                    insertVal + " < @" + (idx + 1) + ".val=" + curVal + " → 往左走",
                    em.refText(idx))),
                    "比根小：插入位置只可能在左子树");
            int child = insert(em.nodes.get(idx)[1]);
            em.nodes.get(idx)[1] = child;
            em.emit(L_ASSIGN_L, List.of(VizEvent.of(VizEvent.EventType.SET,
                    "@" + (idx + 1) + ".left ← " + em.refText(child) + "（挂上/接住）",
                    "@" + (idx + 1) + ".left")),
                    "接住子调用的返回值，挂到 left 指针上");
        } else if (insertVal > curVal) {
            em.emit(L_IF_LT, List.of(VizEvent.of(VizEvent.EventType.COMPARE,
                    insertVal + " < @" + (idx + 1) + ".val=" + curVal + "？否", em.refText(idx))),
                    "不比根小，接着问右边");
            em.emit(L_IF_GT, List.of(VizEvent.of(VizEvent.EventType.COMPARE,
                    insertVal + " > @" + (idx + 1) + ".val=" + curVal + " → 往右走",
                    em.refText(idx))),
                    "比根大：插入位置只可能在右子树");
            int child = insert(em.nodes.get(idx)[2]);
            em.nodes.get(idx)[2] = child;
            em.emit(L_ASSIGN_R, List.of(VizEvent.of(VizEvent.EventType.SET,
                    "@" + (idx + 1) + ".right ← " + em.refText(child) + "（挂上/接住）",
                    "@" + (idx + 1) + ".right")),
                    "接住子调用的返回值，挂到 right 指针上");
        } else {
            em.emit(L_IF_LT, List.of(VizEvent.of(VizEvent.EventType.COMPARE,
                    insertVal + " < @" + (idx + 1) + ".val=" + curVal + "？否", em.refText(idx))),
                    "不比根小，接着问右边");
            em.emit(L_IF_GT, List.of(VizEvent.of(VizEvent.EventType.COMPARE,
                    insertVal + " > @" + (idx + 1) + ".val=" + curVal + "？否（相等）",
                    em.refText(idx))),
                    "相等：集合语义不重复插，直接返回原节点");
        }

        em.emit(L_RET, List.of(VizEvent.of(VizEvent.EventType.RETURN,
                "return @" + (idx + 1) + " —— 弹栈")),
                "本层子树的根不变，原样交回父调用");
        em.pop();
        return idx;
    }
}
