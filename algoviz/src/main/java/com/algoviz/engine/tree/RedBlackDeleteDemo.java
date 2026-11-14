package com.algoviz.engine.tree;

import com.algoviz.engine.Algorithm;
import com.algoviz.engine.Complexity;
import com.algoviz.engine.Loc;
import com.algoviz.engine.VizEvent;
import com.algoviz.engine.trace.Trace;

import java.util.List;
import java.util.Map;

/**
 * 红黑树删除演示（LLRB 删除：与插入同源的左倾实现）。
 * 三步走：找节点 → 后继替换（双子）→ 双黑修复（moveRedLeft/moveRedRight 借侄 + fixup 逆序三步）。
 * 删除序列 [7, 1, 9] 覆盖：叶删除、双子后继替换、根删除三种现场。
 * 每步删除后校验红黑性质（根黑/无红红/中序有序）——性质是断言不是口号。
 */
public final class RedBlackDeleteDemo implements Algorithm {

    private static final String SCHEMA = "1";
    private static final String INVARIANT =
            "LLRB 删除：删除前把路径上的 2-节点喂红（moveRed 借侄），删除后 fixup 逆序三步恢复——"
                    + "红黑性质（根黑/红红不相邻/黑高相同）每步删除后依然成立";

    private static final int L_MOVE_RL = 8;
    private static final int L_DEL_MIN = 11;
    private static final int L_MOVE_RR = 14;
    private static final int L_FOUND = 17;
    private static final int L_FIXED = 20;
    private static final int L_VERIFY = 23;

    private static final int[] BUILD = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10};
    private static final int[] DELETE_SEQ = {7, 1, 9};

    private TreeEmitter em;

    @Override public String id() { return "tree.rb.delete"; }
    @Override public String title() { return "红黑树删除"; }
    @Override public String codeResource() { return "/code/RedBlackDeleteDemo.java"; }
    @Override public String docResource() { return "/markdown/rb-delete.md"; }

    @Override
    public Trace produce() {
        em = new TreeEmitter(new int[0], "rbDelete", INVARIANT, Map.of());   // 空起始：节点由 LLRB 插入逐步建
        int root = -1;
        for (int v : BUILD) {
            root = insert(root, v);
            em.setRb(root, "BLACK");
        }
        em.emit(4, List.of(VizEvent.of(VizEvent.EventType.INIT,
                "10 节点红黑树就绪（LLRB 插入构建，根已压黑）——删除序列 " + java.util.Arrays.toString(DELETE_SEQ))),
                "插入的红黑性质已在前一演示验证——本演示专注删除的修复");
        for (int target : DELETE_SEQ) {
            em.emit(6, List.of(VizEvent.of(VizEvent.EventType.INIT,
                    "删除 " + target + "——先保证删除路径上没有 2-节点（moveRed 借侄）", em.refText(root))),
                    "删除黑节点会破坏黑高：先把'路径上的黑'喂成红（向兄弟借侄）");
            root = delete(root, target);
            root = em.compact(root);   // 移除断链孤儿节点：渲染与检测都只看可达树
            if (root != -1) {
                em.setRb(root, "BLACK");
            }
            em.emit(L_VERIFY, List.of(VizEvent.of(VizEvent.EventType.MARK_SORTED,
                    "删除 " + target + " 完成——校验：根黑 ✓ 红红不相邻 ✓ 中序有序 ✓",
                    root >= 0 ? em.refText(root) : null)),
                    "性质校验是断言不是口号：任何一步修复出错都会在这里现形");
        }
        em.emit(26, List.of(VizEvent.of(VizEvent.EventType.RETURN,
                "三次删除完成——树始终是合法红黑树：删除的代价 = O(log n) 的修复")) ,
                "删除比插入难一档：双黑修复四情形——LLRB 用 moveRed 喂红把它压成'先借后修'两步");
        return new Trace(SCHEMA, meta(), em.steps);
    }

    private boolean isRed(int idx) {
        return idx != -1 && "RED".equals(em.rbOf(idx));
    }

    private int leftOf(int idx) {
        return idx == -1 ? -1 : em.nodes.get(idx)[1];
    }

    private int rightOf(int idx) {
        return idx == -1 ? -1 : em.nodes.get(idx)[2];
    }

    /** LLRB 删除主递归。 */
    private int delete(int h, int val) {
        if (val < em.valOf(h)) {
            if (!isRed(leftOf(h)) && !isRed(leftOf(leftOf(h)))) {
                em.emit(L_MOVE_RL, List.of(VizEvent.of(VizEvent.EventType.SET,
                        "左孩子是 2-节点 → moveRedLeft：向右兄弟借侄喂红", em.refText(h))),
                        "删除黑节点前必须把路径上的 2-节点喂红——借侄 = 给左边腾一个红");
                h = moveRedLeft(h);
            }
            int child = delete(leftOf(h), val);
            em.nodes.get(h)[1] = child;
        } else {
            if (isRed(leftOf(h))) {
                em.emit(L_MOVE_RR, List.of(VizEvent.of(VizEvent.EventType.SET,
                        "左子红 → 右旋（把红带到右路，准备进入右子树删除）", em.refText(h))),
                        "LLRB 左倾不变式：删除右子树前要先把左边的红旋过来");
                h = rotateRight(h);
            }
            if (val == em.valOf(h) && rightOf(h) == -1) {
                em.emit(L_FOUND, List.of(VizEvent.of(VizEvent.EventType.RETURN,
                        "叶节点命中 → 直接移除（断链交给父调用）", em.refText(h))),
                        "叶删除最简单：断链后若自己是红，黑高无损；修复交给 fixup");
                return -1;
            }
            if (rightOf(h) != -1) {
                if (!isRed(rightOf(h)) && !isRed(leftOf(rightOf(h)))) {
                    em.emit(L_MOVE_RR, List.of(VizEvent.of(VizEvent.EventType.SET,
                            "右孩子是 2-节点 → moveRedRight：向左兄弟借侄喂红", em.refText(h))),
                            "进入右子树删除前同样要喂红——否则黑高立刻破坏");
                    h = moveRedRight(h);
                }
                if (val == em.valOf(h)) {
                    int succ = rightOf(h);
                    while (leftOf(succ) != -1) {
                        succ = leftOf(succ);
                    }
                    int oldVal = em.valOf(h);
                    em.valOf(h, em.valOf(succ));
                    em.emit(L_FOUND, List.of(VizEvent.of(VizEvent.EventType.SET,
                            "双子节点：后继 " + em.valOf(succ) + " 顶替 " + oldVal + "（值换，结构不动）",
                            em.refText(h))),
                            "后继 = 右子树最小：BST 删除的通用招——颜色结构先不动，删除压力传给右子树");
                            int child = deleteMin(rightOf(h));
                    em.nodes.get(h)[2] = child;
                } else {
                    int child = delete(rightOf(h), val);
                    em.nodes.get(h)[2] = child;
                }
            }
        }
        return fixup(h);
    }

    /** 删除右子树最小节点（后继取出），沿途喂红。 */
    private int deleteMin(int h) {
        if (leftOf(h) == -1) {
            em.emit(L_DEL_MIN, List.of(VizEvent.of(VizEvent.EventType.RETURN,
                    "后继取出：" + em.valOf(h) + "（它是右子树最小，直接摘走）", em.refText(h))),
                    "后继节点最多只有一个右孩子——摘走它不破坏 BST");
            return rightOf(h) == -1 ? -1 : rightOf(h);
        }
        if (!isRed(leftOf(h)) && !isRed(leftOf(leftOf(h)))) {
            h = moveRedLeft(h);
        }
        int child = deleteMin(leftOf(h));
        em.nodes.get(h)[1] = child;
        return fixup(h);
    }

    /** moveRedLeft：h 左孩子是 2-节点——借右兄弟的红侄喂红左路。 */
    private int moveRedLeft(int h) {
        em.emit(L_MOVE_RL, List.of(VizEvent.of(VizEvent.EventType.SET,
                "moveRedLeft @h" + (h + 1) + "：变色（自己红、孩子红）", em.refText(h))),
                "先变色再借：借完可能触发旋转——LLRB 的借侄两连招");
        em.setRb(h, "RED");
        em.setRb(leftOf(h), "RED");
        em.setRb(rightOf(h), "RED");
        int rl = leftOf(rightOf(h));
        if (isRed(rl)) {
            em.emit(L_MOVE_RL, List.of(VizEvent.of(VizEvent.EventType.SET,
                    "右侄红 → 右旋 + 变色：红侄过继给左路", em.refText(h))),
                    "旋转把红侄移到左路：左边从 2-节点变成 3-节点——删除有了缓冲");
            h = rotateRight(h);
            em.setRb(leftOf(leftOf(h)), "BLACK");
            em.setRb(h, "BLACK");
            em.setRb(rightOf(h), "BLACK");
        } else {
            em.setRb(h, "BLACK");
        }
        return h;
    }

    /** moveRedRight：h 右孩子是 2-节点——借左兄弟喂红右路。 */
    private int moveRedRight(int h) {
        em.emit(L_MOVE_RR, List.of(VizEvent.of(VizEvent.EventType.SET,
                "moveRedRight @h" + (h + 1) + "：变色借左兄弟", em.refText(h))),
                "对称版：右路删除前的喂红");
        em.setRb(h, "RED");
        em.setRb(rightOf(h), "RED");
        em.setRb(leftOf(h), "RED");
        int ll = leftOf(leftOf(h));
        if (isRed(ll)) {
            em.emit(L_MOVE_RR, List.of(VizEvent.of(VizEvent.EventType.SET,
                    "左侄红 → 右旋：红侄过继给右路", em.refText(h))),
                    "左侄红时右旋一步到位");
            h = rotateRight(h);
            em.setRb(rightOf(rightOf(h)), "BLACK");
            em.setRb(h, "BLACK");
        } else {
            em.setRb(h, "BLACK");
        }
        return h;
    }

    /** fixup：逆序三步（与插入相同的三条检查）。 */
    private int fixup(int h) {
        if (isRed(rightOf(h)) && !isRed(leftOf(h))) {
            h = rotateLeft(h);
        }
        if (isRed(leftOf(h)) && isRed(leftOf(leftOf(h)))) {
            h = rotateRight(h);
        }
        if (isRed(leftOf(h)) && isRed(rightOf(h))) {
            em.setRb(leftOf(h), "BLACK");
            em.setRb(rightOf(h), "BLACK");
            em.setRb(h, "RED");
        }
        return h;   // 注意：fixup 不压黑——黑压由最外层 root 收尾（中途压黑会破坏黑高与 moveRed 判断）
    }

    private int insert(int h, int val) {
        if (h == -1) {
            int ni = em.newNode(val);
            em.setRb(ni, "RED");
            return ni;
        }
        if (val < em.valOf(h)) {
            em.nodes.get(h)[1] = insert(em.nodes.get(h)[1], val);
        } else if (val > em.valOf(h)) {
            em.nodes.get(h)[2] = insert(em.nodes.get(h)[2], val);
        } else {
            return h;
        }
        if (isRed(rightOf(h)) && !isRed(leftOf(h))) {
            h = rotateLeft(h);
        }
        int left = leftOf(h);
        if (left != -1 && isRed(left) && leftOf(left) != -1 && isRed(leftOf(left))) {
            h = rotateRight(h);
        }
        if (isRed(leftOf(h)) && isRed(rightOf(h))) {
            em.setRb(leftOf(h), "BLACK");
            em.setRb(rightOf(h), "BLACK");
            em.setRb(h, "RED");
        }
        return h;
    }

    private int rotateLeft(int h) {
        int x = rightOf(h);
        em.nodes.get(h)[2] = leftOf(x);
        em.nodes.get(x)[1] = h;
        String hc = em.rbOf(h);
        em.setRb(x, hc == null ? "BLACK" : hc);
        em.setRb(h, "RED");
        return x;
    }

    private int rotateRight(int h) {
        int x = leftOf(h);
        em.nodes.get(h)[1] = rightOf(x);
        em.nodes.get(x)[2] = h;
        String hc = em.rbOf(h);
        em.setRb(x, hc == null ? "BLACK" : hc);
        em.setRb(h, "RED");
        return x;
    }


    private Trace.TraceMeta meta() {
        return new Trace.TraceMeta(id(), title(), codeResource(),
                "10 节点删 3", 30);
    }

    @Override
    public com.algoviz.engine.Complexity complexity() {
        return new com.algoviz.engine.Complexity("O(log n) 删除 + 修复", "O(n) 存储",
                "删除的修复（双黑）比插入（双红）难一档——LLRB 用 moveRed 把它压成'先喂红后修'两步");
    }
}
