package com.algoviz.engine.tree;

import com.algoviz.engine.Complexity;
import com.algoviz.engine.Algorithm;
import com.algoviz.engine.VizEvent;
import com.algoviz.engine.trace.Trace;

import java.util.List;
import java.util.Map;

/**
 * BST 删除生产者：三种断链方式全走一遍（删根 5 = 双子换后继路线）：
 * 叶子/单子 → 直接用孩子顶替；双子 → 后继的值抄进来，再从右子树删掉后继。
 * 注意：内层递归 delete(root.right, min.val) 的目标值是后继的值——每层调用的 val 各自独立。
 * 行号常量与 /code/BstDelete.java 对齐。
 */
public final class BstDelete implements Algorithm {

    private static final String SCHEMA = "1";
    private static final String INVARIANT =
            "删完仍是 BST：左 < 根 < 右；双子删除用「中序后继」补位，等价于把节点和后继交换后删掉";

    private static final int L_METHOD = 2;
    private static final int L_NULL = 3;
    private static final int L_RET_NULL = 4;
    private static final int L_LT = 6;
    private static final int L_ASSIGN_L = 7;
    private static final int L_GT = 8;
    private static final int L_ASSIGN_R = 9;
    private static final int L_FOUND = 10;
    private static final int L_NOLEFT = 11;
    private static final int L_RET_R = 12;
    private static final int L_NORIGHT = 14;
    private static final int L_RET_L = 15;
    private static final int L_MIN = 17;
    private static final int L_WHILE = 18;
    private static final int L_MINL = 19;
    private static final int L_COPY = 21;
    private static final int L_DEL_R = 22;
    private static final int L_RET = 25;

    private final int[] input;
    private final int deleteVal;
    private TreeEmitter em;

    public BstDelete(int[] input, int deleteVal) {
        this.input = input.clone();
        this.deleteVal = deleteVal;
        this.em = TreeEmitter.bst(this.input, "delete", INVARIANT, Map.of());
    }

    @Override
    public String id() {
        return "tree.bst.delete";
    }

    @Override
    public String title() {
        return "BST 删除";
    }

    @Override
    public String codeResource() {
        return "/code/BstDelete.java";
    }

    @Override
    public String docResource() {
        return "/markdown/bst-delete.md";
    }

    @Override
    public Trace produce() {
        em = TreeEmitter.bst(input.clone(), "delete", INVARIANT, Map.of());   // 重复 produce：重建状态
        delete(0, deleteVal);
        return new Trace(SCHEMA, meta(input), em.steps);
    }

    private int delete(int idx, int target) {
        em.perCallVals.put(em.callStack.size(), String.valueOf(target));
        em.push(idx);
        em.emit(L_METHOD, List.of(VizEvent.of(VizEvent.EventType.CALL,
                        "调用 delete(root=" + em.refText(idx) + ", val=" + target + ") —— 压栈")),
                "进入 delete(" + em.refText(idx) + ")：带着要删的值 " + target + " 走搜索路径");

        if (idx == -1) {
            em.emit(L_NULL, List.of(VizEvent.of(VizEvent.EventType.COMPARE,
                    "root = null（基线）")), "root == null：树里没有这个值");
            em.emit(L_RET_NULL, List.of(VizEvent.of(VizEvent.EventType.RETURN,
                    "return null —— 弹栈")), "把 null 交回父调用（指针不动）");
            em.pop();
            return -1;
        }

        int curVal = em.nodes.get(idx)[0];
        if (target < curVal) {
            em.emit(L_LT, List.of(VizEvent.of(VizEvent.EventType.COMPARE,
                    target + " < @" + (idx + 1) + ".val=" + curVal + " → 往左找", em.refText(idx))),
                    "比根小：目标只可能在左子树");
            int child = delete(em.nodes.get(idx)[1], target);
            em.nodes.get(idx)[1] = child;
            em.emit(L_ASSIGN_L, List.of(VizEvent.of(VizEvent.EventType.SET,
                    "@" + (idx + 1) + ".left ← " + em.refText(child) + "（接住删完的子树）",
                    "@" + (idx + 1) + ".left")),
                    "接住返回值——子树可能已被删改，指针重新挂");
        } else if (target > curVal) {
            em.emit(L_LT, List.of(VizEvent.of(VizEvent.EventType.COMPARE,
                    target + " < @" + (idx + 1) + ".val=" + curVal + "？否", em.refText(idx))),
                    "不比根小，接着问右边");
            em.emit(L_GT, List.of(VizEvent.of(VizEvent.EventType.COMPARE,
                    target + " > @" + (idx + 1) + ".val=" + curVal + " → 往右找", em.refText(idx))),
                    "比根大：目标只可能在右子树");
            int child = delete(em.nodes.get(idx)[2], target);
            em.nodes.get(idx)[2] = child;
            em.emit(L_ASSIGN_R, List.of(VizEvent.of(VizEvent.EventType.SET,
                    "@" + (idx + 1) + ".right ← " + em.refText(child) + "（接住删完的子树）",
                    "@" + (idx + 1) + ".right")),
                    "接住返回值——子树可能已被删改，指针重新挂");
        } else {
            // ===== 找到目标：三种断链 =====
            em.emit(L_FOUND, List.of(VizEvent.of(VizEvent.EventType.COMPARE,
                    "val == @" + (idx + 1) + ".val → 找到目标", em.refText(idx))),
                    "找到 @" + (idx + 1) + "：现在问它有几个孩子");
            int left = em.nodes.get(idx)[1];
            int right = em.nodes.get(idx)[2];
            em.emit(L_NOLEFT, List.of(VizEvent.of(VizEvent.EventType.COMPARE,
                    "root.left == null？" + (left == -1 ? "是 → 用右孩子顶替" : "否（有左孩子 " + em.refText(left) + "）"),
                    em.refText(idx))),
                    "情况一问左：没有左孩子，右孩子（哪怕也是 null）直接顶替自己");
            if (left == -1) {
                em.emit(L_RET_R, List.of(VizEvent.of(VizEvent.EventType.RETURN,
                        "return root.right = " + em.refText(right) + " —— 自己被跳过",
                        em.refText(right))),
                        "断链方式①/②：parent 原本指向我，现在改指我的右孩子");
                em.pop();
                return right;
            }
            em.emit(L_NORIGHT, List.of(VizEvent.of(VizEvent.EventType.COMPARE,
                    "root.right == null？" + (right == -1 ? "是 → 用左孩子顶替" : "否 → 双子删除"),
                    em.refText(idx))),
                    "情况二问右：没有右孩子，左孩子顶替；两边都有 → 最难的第三种");
            if (right == -1) {
                em.emit(L_RET_L, List.of(VizEvent.of(VizEvent.EventType.RETURN,
                        "return root.left = " + em.refText(left) + " —— 自己被跳过",
                        em.refText(left))),
                        "断链方式②：parent 原本指向我，现在改指我的左孩子");
                em.pop();
                return left;
            }
            // 双子：找右子树的最小值（中序后继）
            int minIdx = right;
            em.tag("min", minIdx);
            em.emit(L_MIN, List.of(VizEvent.of(VizEvent.EventType.SET,
                    "min ← root.right = " + em.refText(minIdx) + "（去右子树找最小值）", "min")),
                    "双子删除：右子树的最小值 = 中序后继，它是替代者");
            while (em.nodes.get(minIdx)[1] != -1) {
                em.emit(L_WHILE, List.of(VizEvent.of(VizEvent.EventType.COMPARE,
                        "min.left ≠ null → 继续往左", "min")),
                        "比 min 还小的一定在左边，一路向左走到头");
                minIdx = em.nodes.get(minIdx)[1];
                em.tag("min", minIdx);
                em.emit(L_MINL, List.of(VizEvent.of(VizEvent.EventType.SET,
                        "min ← min.left = " + em.refText(minIdx), "min")),
                        "min 左移一格");
            }
            int succVal = em.nodes.get(minIdx)[0];
            em.nodes.get(idx)[0] = succVal;   // 换值：状态与台词同步（golden 会拦不同步）
            em.emit(L_COPY, List.of(VizEvent.of(VizEvent.EventType.SET,
                    "root.val ← " + succVal + "（后继的值抄进来，@" + (idx + 1) + " 现在装着 "
                            + succVal + "）", "@" + (idx + 1))),
                    "只换值不换节点——结构与指针都不动，BST 性质立刻恢复");
            em.emit(L_DEL_R, List.of(VizEvent.of(VizEvent.EventType.SET,
                    "root.right ← delete(root.right, " + succVal + ")（把后继从右子树里删掉）",
                    "@" + (idx + 1) + ".right")),
                    "值抄完了，真正的删除发生在右子树里：注意这层的 val 变成了 " + succVal);
            em.untag("min");
            int newRight = delete(right, succVal);
            em.nodes.get(idx)[2] = newRight;
            em.emit(L_ASSIGN_R, List.of(VizEvent.of(VizEvent.EventType.SET,
                    "@" + (idx + 1) + ".right ← " + em.refText(newRight) + "（接住删完的右子树）",
                    "@" + (idx + 1) + ".right")),
                    "后继已从右子树摘除，接住剩余部分");
        }

        em.emit(L_RET, List.of(VizEvent.of(VizEvent.EventType.RETURN,
                "return @" + (idx + 1) + " —— 弹栈")),
                "本层子树处理完，原样交回父调用");
        em.pop();
        return idx;
    }

    @Override
    public java.util.List<String> dataLines() {
        return java.util.List.of(Algorithm.joinArray(input));
    }

    @Override
    public Complexity complexity() {
        return new Complexity("O(h)", "O(1) 迭代版", "搜索 O(h) + 可能的后继查找 O(h) → 总计 O(h)。双子删除的后继必无左孩子 → 不会更深");
    }
}
