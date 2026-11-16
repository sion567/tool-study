package com.algoviz.ui;

import com.algoviz.engine.Algorithm;
import com.algoviz.engine.Step;
import com.algoviz.app.NotesStore;
import com.algoviz.engine.ds.HashView;
import com.algoviz.engine.ds.ListView;
import com.algoviz.engine.ds.TreeView;
import com.algoviz.engine.math.MathView;
import com.algoviz.engine.math.PlotView;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * 预测状态机（架构归档 §七债务① 的收官）：
 * ARMED（出题/点选）→ REVEALED（揭晓+计分）→ 下一题。
 * 从 MainApp 剥离的全部预测职责：导航漂移重出题、点选切换、
 * 判卷（truthIndices）、答案命名（fmtIdx）、分数持久化。
 * MainApp 只保留：画布点击坐标换下标（CanvasDispatch）、render() 读 armed/picked。
 */
public final class PredictionController {

    public enum Phase { ARMED, REVEALED }

    private Phase phase;   // null = 尚未开始（刚换算法）
    private int armedIndex = -1;
    private final Set<Integer> picked = new LinkedHashSet<>();
    private int asked;
    private int correct;
    private int streak;

    private final PlaybackController playback;
    private final PredictionPanel panel;
    private final Supplier<Algorithm> algo;
    private final Runnable rerender;

    public PredictionController(PlaybackController playback,
                                PredictionPanel panel,
                                Supplier<Algorithm> algo,
                                Runnable rerender) {
        this.playback = playback;
        this.panel = panel;
        this.algo = algo;
        this.rerender = rerender;
    }

    // ---------- 查询（render 用） ----------

    public boolean armed() {
        return phase == Phase.ARMED;
    }

    public Set<Integer> picked() {
        return picked;
    }

    // ---------- 生命周期 ----------

    /** 换算法：预测状态清零（phase=null → 下次导航自动重新出题）。 */
    public void onAlgorithmLoaded() {
        phase = null;
        picked.clear();
    }

    /** 每帧导航后调用：漂移则重新出题，ARMED 则刷新题目面板。 */
    public void onNavChanged() {
        int idx = playback.index();
        boolean keepArmed = phase == Phase.ARMED && idx == armedIndex;
        boolean keepRevealed = phase == Phase.REVEALED && idx == armedIndex + 1;
        if (!keepArmed && !keepRevealed) {
            arm(phase != Phase.REVEALED);
            return;
        }
        if (phase == Phase.ARMED) {
            refreshQuestion();
        }
    }

    public void arm(boolean clearResult) {
        phase = Phase.ARMED;
        armedIndex = playback.index();
        picked.clear();
        if (clearResult) {
            panel.setResult("");
        }
    }

    /** ARMED 态刷新题目：末帧/plot 类禁用，其余按图层出题。 */
    private void refreshQuestion() {
        int idx = playback.index();
        int size = playback.size();
        if (idx >= size - 1) {
            panel.setQuestion("已到最后一步，没有下一帧了。按 ⏮ 回到开头再练一轮。");
            panel.setPicked("");
            panel.setButtons(false, false, "提交答案");
            return;
        }
        Step cur = playback.current();
        boolean plotMode = cur != null && cur.plot() != null;
        if (plotMode && !predictablePlot(cur)) {
            panel.setQuestion("此演示是连续图像型（曲线/几何），暂不支持点选预测——"
                    + "已开放：线性代数·3D 旋转（猜转轴）、二分法（猜半区）；"
                    + "其余点选类演示（排序/链表/树/图/矩阵）练起来更顺手。");
            panel.setPicked("");
            panel.setButtons(false, false, "跳过");
            return;
        }
        if (plotMode) {
            if (PlotView.WIRE3D.equals(cur.plot().kind()) && cur.plot().angle() < 0) {
                panel.setQuestion("本帧不可预测（开场/万向锁现场/收尾——看懂它本身就是收获）。");
                panel.setPicked("");
                panel.setButtons(false, false, "跳过");
                return;
            }
            String target = cur.plot().kind().equals(PlotView.WIRE3D)
                    ? "这一步绕哪根轴转？（点轴线选择）"
                    : "中点分半后，根在左半还是右半？（点半个区间选择）";
            panel.setQuestion("第 " + (idx + 1) + " 步 → 第 " + (idx + 2) + " 步：" + target);
            panel.setPicked(picked.isEmpty() ? "已选：（无）" : "已选：" + fmtIdx(picked, cur));
            panel.setButtons(true, true, "提交答案");
            return;
        }
        boolean listMode = cur != null && cur.list() != null;
        boolean treeMode = cur != null && cur.tree() != null;
        boolean graphMode = cur != null && cur.graph() != null;
        boolean hashMode = cur != null && cur.hash() != null;
        boolean btreeMode = cur != null && cur.btree() != null;
        boolean seqMode = cur != null && cur.seq() != null;
        boolean mathMode = cur != null && cur.math() != null && cur.bitmap() == null;
        boolean bitmapMode = cur != null && cur.bitmap() != null;
        String target = bitmapMode ? "BitSet 上哪些格子会翻转？（点格子）"
                : mathMode ? "矩阵上哪些格子会变？（点格子选择）"
                : seqMode ? "下一条消息会经过哪些泳道？（点泳道头选择）"
                : graphMode || treeMode || hashMode || btreeMode ? "哪些节点会变？（点节点选择）"
                : listMode ? "哪些节点的 next 箭头会变？（点盒子选择）"
                : "哪些数组格子会变？（点柱子选择）";
        String noneCase = bitmapMode ? "没有格子翻转"
                : mathMode ? "本步没有格子写数"
                : seqMode ? "没有消息要发"
                : graphMode ? "没有节点变化" : treeMode ? "没有任何节点变化"
                : hashMode ? "链没动" : btreeMode ? "树纹丝不动"
                : listMode ? "一根箭头都不断" : "数组不变";
        panel.setQuestion("第 " + (idx + 1) + " 步 → 第 " + (idx + 2) + " 步：" + target
                + "\n全不选 = 预测「" + noneCase + "」");
        panel.setPicked(picked.isEmpty() ? "已选：（无）" : "已选：" + fmtIdx(picked, cur));
        panel.setButtons(true, true, "提交答案");
    }

    // ---------- 点选 ----------

    /** ARMED 态点击画布元素（idx 由 CanvasDispatch.pick 算出）：切换选中并重绘。 */
    public void onCanvasClick(int pickIdx) {
        if (phase != Phase.ARMED || pickIdx < 0) {
            return;
        }
        if (!picked.add(pickIdx)) {
            picked.remove(pickIdx);
        }
        rerender.run();
    }

    // ---------- 提交 / 跳过 ----------

    public void submit() {
        if (phase == Phase.REVEALED) {
            arm(false);
            rerender.run();
            return;
        }
        int idx = playback.index();
        if (idx >= playback.size() - 1) {
            return;
        }
        Step cur = playback.current();
        Step next = playback.trace().steps().get(idx + 1);
        Set<Integer> truth = truthIndices(cur, next);
        boolean listMode = cur.list() != null;
        boolean treeMode = cur.tree() != null;
        boolean graphMode = cur.graph() != null;
        boolean mathMode = cur.math() != null && cur.bitmap() == null;
        boolean bitmapMode = cur.bitmap() != null;

        asked++;
        List<Integer> missed = truth.stream().filter(t -> !picked.contains(t)).toList();
        List<Integer> extra = picked.stream().filter(p -> !truth.contains(p)).toList();
        boolean full = missed.isEmpty() && extra.isEmpty();
        if (full) {
            correct++;
            streak++;
        } else {
            streak = 0;
        }

        String truthStr = truth.isEmpty()
                ? (bitmapMode ? "没有格子翻转（只是读了一下）"
                : mathMode ? "本步没有格子写数（只是移动了读取的行列）"
                        : treeMode || graphMode ? "没有节点变化（只动了栈/表）"
                        : cur.hash() != null ? "链一条都没变（变的是桶高亮/变量）"
                        : cur.btree() != null ? "树没变（只动了下探路径）"
                        : listMode ? "一根箭头都没断（变的是栈里的指针变量！）"
                        : "数组一个格子都没变（变的是栈里的变量！）")
                : fmtIdx(truth, cur);
        StringBuilder sb = new StringBuilder(full ? "✅ 全对！实际变化：" : "❌ 实际变化：");
        sb.append(truthStr);
        if (!missed.isEmpty()) {
            sb.append("；漏了 ").append(fmtIdx(missed, cur));
        }
        if (!extra.isEmpty()) {
            sb.append("；多选了 ").append(fmtIdx(extra, cur));
        }
        panel.setResult(sb.toString());
        panel.setScore(asked + " 题 · " + correct + " 全对 · 连对 " + streak);
        Algorithm current = algo.get();
        if (current != null) {
            var entry = NotesStore.get(current.id());
            entry.predictionAsked = asked;
            entry.predictionCorrect = correct;
            entry.predictionStreak = streak;
            NotesStore.save(current.id(), entry);
        }

        phase = Phase.REVEALED;
        playback.go(idx + 1);   // 揭晓：跳到下一帧（触发 slider 监听 → render）
        panel.setButtons(true, true, "下一题 ▶");
    }

    public void reveal() {
        if (phase == Phase.REVEALED) {
            return;
        }
        int idx = playback.index();
        if (idx >= playback.size() - 1) {
            return;
        }
        Step cur = playback.current();
        Step next = playback.trace().steps().get(idx + 1);
        Set<Integer> truth = truthIndices(cur, next);
        panel.setResult("（跳过，未计分）实际变化："
                + (truth.isEmpty() ? (cur.list() != null ? "箭头没变" : "数组没变") : fmtIdx(truth, cur)));
        phase = Phase.REVEALED;
        playback.go(idx + 1);
        panel.setButtons(true, true, "下一题 ▶");
    }

    // ---------- 真值与命名（原 MainApp 静态工具平移） ----------

    /** 预测的真值：与下一帧相比，变的是哪些下标。 */
    /** plot 类白名单：数据驱动真值的两演示（wire3d angle 编码轴、二分 rects[0]=[a,mid,b]）。 */
    private static boolean predictablePlot(Step cur) {
        PlotView pv = cur.plot();
        if (pv == null) {
            return false;
        }
        if (PlotView.WIRE3D.equals(pv.kind())) {
            return true;
        }
        return PlotView.NEWTON.equals(pv.kind()) && !pv.rects().isEmpty() && pv.rects().get(0).size() == 3;
    }

    private static Set<Integer> truthIndices(Step cur, Step next) {
        if (cur == null || next == null) {
            return Set.of();
        }
        if (cur.plot() != null && predictablePlot(cur)) {
            PlotView pv = cur.plot();
            PlotView npv = next.plot();
            if (PlotView.WIRE3D.equals(pv.kind())) {
                int axis = (int) Math.round(npv.angle());
                return axis >= 0 && axis <= 2 ? Set.of(axis) : Set.of();
            }
            // 二分：下一帧 rects[0]（区间左端 a）变大 = mid 归入 a = 根在右半
            boolean goRight = npv.rects().get(0).get(0) > pv.rects().get(0).get(0);
            return Set.of(goRight ? 1 : 0);
        }
        if (cur.array() != null) {
            return DiffHighlighter.changedIndices(cur.array(), next.array());
        }
        if (cur.list() != null) {
            Set<Integer> out = new LinkedHashSet<>();
            List<ListView.NodeBox> nodes = cur.list().nodes();
            for (String nodeId : DiffHighlighter.changedArrows(cur.list(), next.list())) {
                for (int i = 0; i < nodes.size(); i++) {
                    if (nodes.get(i).nodeId().equals(nodeId)) {
                        out.add(i);
                        break;
                    }
                }
            }
            return out;
        }
        if (cur.tree() != null) {
            Set<Integer> out = new LinkedHashSet<>();
            var nodes = cur.tree().nodes();
            for (String nodeId : DiffHighlighter.changedTreeNodes(cur.tree(), next.tree())) {
                for (int i = 0; i < nodes.size(); i++) {
                    if (nodes.get(i).nodeId().equals(nodeId)) {
                        out.add(i);
                        break;
                    }
                }
            }
            return out;
        }
        if (cur.btree() != null) {
            Set<Integer> out = new LinkedHashSet<>();
            var nodes = cur.btree().nodes();
            for (String nodeId : BTreeRenderer.changedNodes(cur.btree(), next.btree())) {
                for (int i = 0; i < nodes.size(); i++) {
                    if (nodes.get(i).nodeId().equals(nodeId)) {
                        out.add(i);
                        break;
                    }
                }
            }
            return out;
        }
        if (cur.hash() != null) {
            Set<Integer> out = new LinkedHashSet<>();
            var nodes = cur.hash().nodes();
            for (String nodeId : HashRenderer.changedArrows(cur.hash(), next.hash())) {
                for (int i = 0; i < nodes.size(); i++) {
                    if (nodes.get(i).nodeId().equals(nodeId)) {
                        out.add(i);
                        break;
                    }
                }
            }
            return out;
        }
        if (cur.math() != null) {
            Set<Integer> out = new LinkedHashSet<>();
            var grids = cur.math().grids();
            for (String key : DiffHighlighter.changedMatrixCells(cur.math(), next.math())) {
                if (key.startsWith("bits:")) {
                    continue;
                }
                int i = Integer.parseInt(key);
                out.add(i);
            }
            return out;
        }
        if (cur.seq() != null) {
            Set<Integer> out = new LinkedHashSet<>();
            var participants = cur.seq().participants();
            for (String label : DiffHighlighter.changedSeqParticipants(cur.seq(), next.seq())) {
                for (int i = 0; i < participants.size(); i++) {
                    if (participants.get(i).label().equals(label)) {
                        out.add(i);
                        break;
                    }
                }
            }
            return out;
        }
        return Set.of();
    }

    private static String fmtIdx(Collection<Integer> idxs, Step refStep) {
        if (refStep != null && refStep.plot() != null && predictablePlot(refStep)) {
            boolean wire = PlotView.WIRE3D.equals(refStep.plot().kind());
            return idxs.stream().sorted().map(i -> wire
                    ? (i == 0 ? "绕X" : i == 1 ? "绕Y" : "绕Z")
                    : (i == 0 ? "左半" : "右半"))
                    .collect(Collectors.joining(", "));
        }
        boolean listMode = refStep != null && refStep.list() != null;
        boolean treeMode = refStep != null && refStep.tree() != null;
        boolean graphMode = refStep != null && refStep.graph() != null;
        boolean hashMode = refStep != null && refStep.hash() != null;
        boolean btreeMode = refStep != null && refStep.btree() != null;
        boolean seqMode = refStep != null && refStep.seq() != null;
        boolean mathMode = refStep != null && refStep.math() != null && refStep.bitmap() == null;
        boolean bitmapMode = refStep != null && refStep.bitmap() != null;
        return idxs.stream().sorted()
                .map(i -> {
                    if (bitmapMode) {
                        return "bit[" + i + "]";
                    }
                    if (mathMode) {
                        var grids = refStep.math().grids();
                        MathView.Grid c = grids.stream().filter(g -> g.id().equals("C")).findFirst()
                                .orElse(grids.isEmpty() ? null : grids.get(0));
                        if (c != null && i >= 0 && i < c.values().size()) {
                            String name = c.id().equals("C") ? "C" : c.label();
                            return name + "[" + (i / c.cols()) + "][" + (i % c.cols()) + "]";
                        }
                        return "?" + i;
                    }
                    if (seqMode) {
                        var participants = refStep.seq().participants();
                        return i >= 0 && i < participants.size() ? participants.get(i).label() : "?" + i;
                    }
                    if (graphMode) {
                        var vertices = refStep.graph().vertices();
                        return i >= 0 && i < vertices.size() ? vertices.get(i).label() : "?" + i;
                    }
                    if (hashMode) {
                        var nodes = refStep.hash().nodes();
                        return i >= 0 && i < nodes.size() ? "node[" + nodes.get(i).val() + "]" : "?" + i;
                    }
                    if (btreeMode) {
                        var nodes = refStep.btree().nodes();
                        return i >= 0 && i < nodes.size() ? "节点" + nodes.get(i).keys() : "?" + i;
                    }
                    if (treeMode) {
                        var nodes = refStep.tree().nodes();
                        return i >= 0 && i < nodes.size() ? nodes.get(i).nodeId() : "?" + i;
                    }
                    return listMode ? "@" + (i + 1) + ".next" : "a[" + i + "]";
                })
                .collect(Collectors.joining(", "));
    }

}

