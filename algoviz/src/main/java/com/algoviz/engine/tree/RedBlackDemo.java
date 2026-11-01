package com.algoviz.engine.tree;

import com.algoviz.engine.Algorithm;
import com.algoviz.engine.VizEvent;
import com.algoviz.engine.trace.Trace;

import java.util.List;
import java.util.Map;

/**
 * 红黑树插入演示（LLRB：左倾红黑树，与教材等价的简化实现）。
 * 核心画面：新节点红 → 违例检测 → 变色 / 旋转 —— 三行修复逻辑应对所有情况。
 * 不变量（RB 五性质）：根黑；红节点的孩子必黑（红红不相邻）；每条根到叶路径的黑数相同。
 */
public final class RedBlackDemo implements Algorithm {

    private static final String SCHEMA = "1";
    private static final String INVARIANT =
            "RB 三性质：根必须黑；红红不相邻；所有根到叶路径的黑数相同——插入靠变色与旋转恢复";

    /** 与 /code/RedBlackDemo.java 的行号对齐 */
    private static final int L_INSERT_RED = 11;
    private static final int L_ROT_L = 15;
    private static final int L_ROT_R = 16;
    private static final int L_FLIP = 17;
    private static final int L_ROOT_BLACK = 26;

    private static final int[] INPUT = {10, 5, 1, 8, 7, 9};

    private TreeEmitter em;

    @Override public String id() { return "tree.rb.insert"; }
    @Override public String title() { return "红黑树插入"; }
    @Override public String codeResource() { return "/code/RedBlackDemo.java"; }
    @Override public String docResource() { return "/markdown/red-black-tree.md"; }

    @Override
    public Trace produce() {
        em = new TreeEmitter(new int[0], "rbInsert", INVARIANT, Map.of());   // 重复 produce：重建
        int root = -1;
        for (int v : INPUT) {
            root = insert(root, v);
            em.setRb(root, "BLACK");
            em.emit(L_ROOT_BLACK, List.of(VizEvent.of(VizEvent.EventType.SET,
                    "根必须为黑 → @" + (root + 1) + " 变黑", "@" + (root + 1))),
                    "F7 收尾：无论修复成什么样，根永远压成黑色");
        }
        return new Trace(SCHEMA, meta(INPUT), em.steps);
    }

    private boolean isRed(int idx) {
        return idx != -1 && "RED".equals(em.rbOf(idx));
    }

    private int insert(int h, int val) {
        if (h == -1) {
            int ni = em.newNode(val);
            em.setRb(ni, "RED");
            em.emit(L_INSERT_RED, List.of(VizEvent.of(VizEvent.EventType.SET,
                    "插入 " + val + " = @" + (ni + 1) + "（红色新节点）", "@" + (ni + 1))),
                    "F1：新节点一律红色——黑高度才不会立刻被破坏");
            return ni;
        }
        if (val < em.nodes.get(h)[0]) {
            em.nodes.get(h)[1] = insert(em.nodes.get(h)[1], val);
        } else if (val > em.nodes.get(h)[0]) {
            em.nodes.get(h)[2] = insert(em.nodes.get(h)[2], val);
        } else {
            return h;   // 去重
        }
        // F5/F6：右红左黑 → 左旋（恢复左倾）
        if (isRed(em.nodes.get(h)[2]) && !isRed(em.nodes.get(h)[1])) {
            em.emit(L_ROT_L, List.of(VizEvent.of(VizEvent.EventType.SET,
                    "右子红、左子黑 → 左旋 @" + (h + 1) + "（下一帧看位置互换）", "@" + (h + 1))),
                    "F5：右红破坏左倾——旋转把孩子位置对调，颜色随位互换");
            h = rotateLeft(h);
            em.emit(L_ROT_L, List.of(VizEvent.of(VizEvent.EventType.SET,
                    "左旋完成：@" + (h + 1) + " 接管子树，原根变红", "@" + (h + 1))),
                    "F6：旋转 = 位置交换 + 颜色随位——BST 性质不受影响");
        }
        // F4：左子与左孙双红 → 右旋
        int left = em.nodes.get(h)[1];
        if (left != -1 && isRed(left) && em.nodes.get(left)[1] != -1 && isRed(em.nodes.get(left)[1])) {
            em.emit(L_ROT_R, List.of(VizEvent.of(VizEvent.EventType.SET,
                    "左子与左孙双红 → 右旋 @" + (h + 1) + "（下一帧看位置互换）", "@" + (h + 1))),
                    "F4：双红相连违反「红红不相邻」——右旋把中间节点顶上去");
            h = rotateRight(h);
            em.emit(L_ROT_R, List.of(VizEvent.of(VizEvent.EventType.SET,
                    "右旋完成：@" + (h + 1) + " 接管子树", "@" + (h + 1))),
                    "旋转后仍可能要变色——看下一层检查");
        }
        // F3：左右皆红 → 变色（孩子黑、自己红，红问题抛给爷爷）
        if (isRed(em.nodes.get(h)[1]) && isRed(em.nodes.get(h)[2])) {
            em.emit(L_FLIP, List.of(VizEvent.of(VizEvent.EventType.SET,
                    "左右皆红 → 变色：@" + (h + 1) + " 的两个孩子变黑、自己变红",
                    "@" + (h + 1), em.refText(em.nodes.get(h)[1]), em.refText(em.nodes.get(h)[2]))),
                    "F3：黑数不变地度过一层——红色问题向上抛给爷爷（帧4：递归向上）");
            em.setRb(em.nodes.get(h)[1], "BLACK");
            em.setRb(em.nodes.get(h)[2], "BLACK");
            em.setRb(h, "RED");
            em.emit(L_FLIP, List.of(VizEvent.of(VizEvent.EventType.SET,
                    "变色完成：@" + (h + 1) + " 变红——若它的父亲也是红，下一层会继续修", "@" + (h + 1))),
                    "向上传递：这就是「爷爷变红后可能又违规」的递归修复");
        }
        return h;
    }

    private int rotateLeft(int h) {
        int x = em.nodes.get(h)[2];
        em.nodes.get(h)[2] = em.nodes.get(x)[1];
        em.nodes.get(x)[1] = h;
        String hc = em.rbOf(h);
        em.setRb(x, hc == null ? "BLACK" : hc);
        em.setRb(h, "RED");
        return x;
    }

    private int rotateRight(int h) {
        int x = em.nodes.get(h)[1];
        em.nodes.get(h)[1] = em.nodes.get(x)[2];
        em.nodes.get(x)[2] = h;
        String hc = em.rbOf(h);
        em.setRb(x, hc == null ? "BLACK" : hc);
        em.setRb(h, "RED");
        return x;
    }
}
