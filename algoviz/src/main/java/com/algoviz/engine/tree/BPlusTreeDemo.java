package com.algoviz.engine.tree;

import com.algoviz.engine.Algorithm;
import com.algoviz.engine.Complexity;
import com.algoviz.engine.Intent;
import com.algoviz.engine.Layer;
import com.algoviz.engine.Loc;
import com.algoviz.engine.Step;
import com.algoviz.engine.Value;
import com.algoviz.engine.VizEvent;
import com.algoviz.engine.ds.BTreeView;
import com.algoviz.engine.mem.Heap;
import com.algoviz.engine.mem.HeapLayer;
import com.algoviz.engine.mem.HeapObject;
import com.algoviz.engine.mem.StackFrame;
import com.algoviz.engine.mem.StackLayer;
import com.algoviz.engine.trace.Trace;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * B+ 树插入（阶 m=4，每节点最多 3 键）：数据库索引的骨架。
 * 数据 {10,20,5,15,25,30,8,35,6,12,18,22}——3 次叶子分裂 + 1 次内部分裂，树从 1 层长到 3 层。
 * 画面：多槽盒子（内部蓝/叶子绿/刚分裂黄），下探路径橙描边，
 * 本步写入的 key 亮字，底部绿色叶链（范围查询的骨架）。
 * 不变量：数据全在叶子且叶链有序；内部节点只存路标（子树最大键的副本）；
 * children = keys + 1；节点满 3 键才分裂。
 */
public final class BPlusTreeDemo implements Algorithm {

    private static final String SCHEMA = "1";
    private static final String INVARIANT =
            "数据全在叶子、叶链有序；内部节点只是路标——查范围 = 根下探 + 叶链横扫，这就是数据库索引";

    private static final int[] DEFAULT_KEYS = {10, 20, 5, 15, 25, 30, 8, 35, 6, 12, 18, 22};
    private static final int MAX_KEYS = 3;

    private final int[] keys;

    /** 与 /code/BPlusTreeDemo.java 的行号对齐 */
    private static final int L_DESCEND = 6;
    private static final int L_LEAF_INSERT = 8;
    private static final int L_SPLIT = 10;
    private static final int L_COPYUP = 11;

    private final List<Step> steps = new ArrayList<>();

    /** 节点：keys 有序；internal 有 children；leaf 有 next 叶链。 */
    private static final class BNode {
        int id;
        final List<Integer> keys = new ArrayList<>();
        final List<BNode> children = new ArrayList<>();
        BNode next;               // 叶链
        boolean leaf = true;
        String state = "unvisited";   // unvisited / onstack / visited / fresh
        Integer activeKey;

        BNode(int id) {
            this.id = id;
        }

        String nodeId() {
            return "@" + id;
        }
    }

    private BNode root;
    private int alloc = 0;
    private final List<BNode> allNodes = new ArrayList<>();

    public BPlusTreeDemo() {
        this(DEFAULT_KEYS);
    }

    public BPlusTreeDemo(int[] keys) {
        this.keys = keys.clone();
    }

    @Override public String id() { return "tree.bplus"; }
    @Override public String title() { return "B+ 树插入"; }
    @Override public String codeResource() { return "/code/BPlusTreeDemo.java"; }
    @Override public String docResource() { return "/markdown/bplus-tree.md"; }

    @Override
    public Trace produce() {
        steps.clear();
        allNodes.clear();
        alloc = 0;
        root = newNode();
        root.state = "visited";
        emit(4, List.of(VizEvent.of(VizEvent.EventType.INIT, "空树：一个叶子根（阶 m=4，每节点 ≤3 键）")),
                "B+ 树满 3 键才分裂——矮胖树 = 磁盘少读几层，这就是它当数据库索引的原因");
        for (int k : keys) {
            insert(k);
            for (BNode n : allNodes) {
                if (!"onstack".equals(n.state)) n.state = "visited";
                n.activeKey = null;
            }
        }
        // 收尾：统计
        int leaves = 0;
        for (BNode n : allNodes) if (n.leaf) leaves++;
        emit(14, List.of(VizEvent.of(VizEvent.EventType.RETURN,
                keys.length + " 键就位：" + leaves + " 个叶子串成链，树高 " + height() + " 层")),
                "范围查询 = 根下探一次 + 叶链横扫——SQL 的 WHERE id BETWEEN 走的就是这条");
        return new Trace(SCHEMA, meta(), steps);
    }

    private int height() {
        int h = 1;
        BNode n = root;
        while (!n.leaf) {
            h++;
            n = n.children.get(0);
        }
        return h;
    }

    private BNode newNode() {
        BNode n = new BNode(++alloc);
        allNodes.add(n);
        return n;
    }

    private void insert(int k) {
        BNode leaf = descend(root, k);
        insertSorted(leaf, k);
        emit(L_LEAF_INSERT, List.of(VizEvent.of(VizEvent.EventType.SET,
                "叶子" + nodeTag(leaf) + " ← " + k + "（插入后 " + leaf.keys + " 键）", leaf.nodeId())),
                "数据只进叶子——插入后键数 <3 就收工");
        if (leaf.keys.size() > MAX_KEYS) {
            splitLeaf(leaf);
        }
    }

    /** 下探：内部节点按"路标 ≤ k 走右、> k 走左"选孩子——沿路 onstack 高亮。 */
    private BNode descend(BNode node, int k) {
        node.state = "onstack";
        if (node.leaf) {
            return node;
        }
        int i = 0;
        while (i < node.keys.size() && k >= node.keys.get(i)) {
            i++;
        }
        String turn = i < node.keys.size()
                ? k + " < " + node.keys.get(i) + " → 走第 " + (i + 1) + " 棵子树"
                : k + " ≥ 全部路标 " + node.keys + " → 走最右子树";
        emit(L_DESCEND, List.of(VizEvent.of(VizEvent.EventType.VISIT,
                turn, node.nodeId())),
                "内部节点 = 路标：k 比路标小就往左拐——二分/线性都比磁盘读便宜");
        BNode leaf = descend(node.children.get(i), k);
        node.state = "visited";
        return leaf;
    }

    private void insertSorted(BNode leaf, int k) {
        int i = 0;
        while (i < leaf.keys.size() && leaf.keys.get(i) < k) i++;
        leaf.keys.add(i, k);
        leaf.activeKey = k;
    }

    /** 叶子分裂：右半搬新叶，最小键**复制**上去（叶子里还得留着真身）。 */
    private void splitLeaf(BNode leaf) {
        int mid = leaf.keys.size() / 2;
        BNode right = newNode();
        right.state = "fresh";
        right.keys.addAll(leaf.keys.subList(mid, leaf.keys.size()));
        leaf.keys.subList(mid, leaf.keys.size()).clear();
        right.next = leaf.next;
        leaf.next = right;
        int up = right.keys.get(0);
        emit(L_SPLIT, List.of(VizEvent.of(VizEvent.EventType.SWAP,
                "叶子满 → 分裂：" + nodeTag(leaf) + " 保留 " + leaf.keys
                        + "，新叶" + nodeTag(right) + " 拿走 " + right.keys, leaf.nodeId())),
                "分裂 = 满载才发生——B+ 树永远至少半满，树不会一边倒");
        emit(L_COPYUP, List.of(VizEvent.of(VizEvent.EventType.CALL,
                "复制 " + up + " 上浮给父节点（叶子里的真身不走）", right.nodeId())),
                "与 B 树的区别：上浮的是**副本**——数据仍完整躺在叶子，叶链照样扫得到");
        pushUp(parent(leaf, allNodes), leaf, right, up);
    }

    /** 内部节点上浮：中位键**搬走**（内部不存真数据）。 */
    private void pushUp(BNode parent, BNode leftChild, BNode rightChild, int key) {
        if (parent == null) {
            BNode newRoot = newNode();
            newRoot.leaf = false;
            newRoot.state = "fresh";
            newRoot.keys.add(key);
            newRoot.children.add(leftChild);
            newRoot.children.add(rightChild);
            root = newRoot;
            emit(L_COPYUP, List.of(VizEvent.of(VizEvent.EventType.MARK_SORTED,
                    "没有父节点 → 新根" + nodeTag(newRoot) + " ← " + key + "（树长高一层）")),
                    "树高的唯一增长方式：根分裂——所有叶子同步下沉一层，永远等高");
            return;
        }
        int i = 0;
        while (i < parent.keys.size() && parent.keys.get(i) < key) i++;
        parent.keys.add(i, key);
        parent.children.add(i + 1, rightChild);
        if (parent.keys.size() > MAX_KEYS) {
            emit(L_SPLIT, List.of(VizEvent.of(VizEvent.EventType.SWAP,
                    "内部节点满 → 分裂：" + nodeTag(parent) + " " + parent.keys, parent.nodeId())),
                    "内部也装不下 → 继续向上分裂");
            int mid = parent.keys.size() / 2;
            int up = parent.keys.get(mid);
            BNode right = newNode();
            right.leaf = false;
            right.state = "fresh";
            right.keys.addAll(parent.keys.subList(mid + 1, parent.keys.size()));
            List<BNode> movedChildren = new ArrayList<>(parent.children.subList(mid + 1, parent.children.size()));
            parent.keys.subList(mid, parent.keys.size()).clear();
            parent.children.subList(mid + 1, parent.children.size()).clear();
            right.children.addAll(movedChildren);
            emit(L_COPYUP, List.of(VizEvent.of(VizEvent.EventType.CALL,
                    "中位 " + up + " **搬**上浮（内部节点不留副本）", right.nodeId())),
                    "内部节点只是路标，路标上浮就搬家——和数据无关");
            pushUp(parent(parent, allNodes), parent, right, up);
        } else {
            emit(L_COPYUP, List.of(VizEvent.of(VizEvent.EventType.SET,
                    nodeTag(parent) + " ← 路标 " + key + "（现 " + parent.keys + "）", parent.nodeId())),
                    "路标插进父节点——父子指针对齐（children = keys+1）");
        }
    }

    private static BNode parent(BNode node, List<BNode> all) {
        for (BNode n : all) {
            if (!n.leaf && n.children.contains(node)) return n;
        }
        return null;
    }

    private String nodeTag(BNode n) {
        return n.leaf ? "L" + n.id : "I" + n.id;
    }

    private void emit(int line, List<VizEvent> events, String why) {
        Map<String, Value> locals = new LinkedHashMap<>();
        locals.put("树高", Value.prim(String.valueOf(height())));
        int leafN = 0;
        for (BNode n : allNodes) if (n.leaf) leafN++;
        locals.put("叶数", Value.prim(String.valueOf(leafN)));
        StackFrame frame = new StackFrame("bplusInsert", Collections.unmodifiableMap(locals), null);
        Map<String, Layer> layers = new LinkedHashMap<>();
        layers.put("mem.stack", new StackLayer(List.of(frame)));
        layers.put("mem.heap", new HeapLayer(snapshotHeap()));
        layers.put("ds.btree", snapshotTree());
        steps.add(new Step(steps.size(), Loc.line(line, why),
                Collections.unmodifiableMap(layers), List.copyOf(events), new Intent(why, INVARIANT)));
    }

    private Heap snapshotHeap() {
        Map<String, HeapObject> objs = new LinkedHashMap<>();
        for (BNode n : allNodes) {
            Map<String, Value> fields = new LinkedHashMap<>();
            fields.put("keys", Value.prim(n.keys.toString()));
            if (!n.leaf) {
                List<String> ch = new ArrayList<>();
                for (BNode c : n.children) ch.add(nodeTag(c));
                fields.put("children", Value.prim(ch.toString()));
            } else {
                fields.put("next", n.next != null ? Value.prim(nodeTag(n.next)) : Value.NULL);
            }
            objs.put("@" + n.id, new HeapObject("@" + n.id, nodeTag(n), n.leaf ? "Leaf" : "Internal",
                    Collections.unmodifiableMap(fields), List.of()));
        }
        return new Heap(Collections.unmodifiableMap(objs));
    }

    private BTreeView snapshotTree() {
        List<BTreeView.BNode> out = new ArrayList<>();
        List<String> chain = new ArrayList<>();
        // 根动态判定：唯一未被任何 children 引用的节点
        java.util.Set<BNode> referenced = new java.util.HashSet<>();
        for (BNode n : allNodes) referenced.addAll(n.children);
        String rootId = null;
        for (BNode n : allNodes) {
            if (!referenced.contains(n)) rootId = "@" + n.id;
        }
        for (BNode n : allNodes) {
            List<String> ch = new ArrayList<>();
            for (BNode c : n.children) ch.add("@" + c.id);
            List<String> childrenOut = n.leaf ? List.of() : Collections.unmodifiableList(ch);
            out.add(new BTreeView.BNode("@" + n.id,
                    Collections.unmodifiableList(new ArrayList<>(n.keys)),
                    childrenOut, n.leaf,
                    n.next != null ? "@" + n.next.id : null,
                    n.state, n.activeKey));
        }
        // 叶链：从最左叶开始
        BNode leftmost = root;
        while (!leftmost.leaf) leftmost = leftmost.children.get(0);
        for (BNode n = leftmost; n != null; n = n.next) chain.add("@" + n.id);
        return new BTreeView("B+ 树（m=4）", rootId,
                Collections.unmodifiableList(out), Collections.unmodifiableList(chain));
    }

    private Trace.TraceMeta meta() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < keys.length; i++) {
            if (i > 0) sb.append(',');
            sb.append(keys[i]);
        }
        return new Trace.TraceMeta(id(), title(), codeResource(), sb.toString(), keys.length);
    }

    @Override
    public java.util.List<String> dataLines() {
        return java.util.List.of("int[] keys = " + java.util.Arrays.toString(keys) + ";   // 阶 m=4");
    }

    @Override
    public Complexity complexity() {
        return new Complexity("O(log n) 树高 = log₄n", "O(n) 全部数据在叶子",
                "百万数据树高 ~10 层 = 10 次磁盘读；范围查询走叶链不用回根——"
                        + "MySQL InnoDB 的主键索引就是一棵 B+ 树");
    }
}
