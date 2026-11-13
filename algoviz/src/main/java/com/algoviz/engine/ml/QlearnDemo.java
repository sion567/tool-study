package com.algoviz.engine.ml;

import com.algoviz.engine.Algorithm;
import com.algoviz.engine.Complexity;
import com.algoviz.engine.Intent;
import com.algoviz.engine.Layer;
import com.algoviz.engine.Loc;
import com.algoviz.engine.OrderedMaps;
import com.algoviz.engine.Step;
import com.algoviz.engine.Value;
import com.algoviz.engine.VizEvent;
import com.algoviz.engine.math.MathView;
import com.algoviz.engine.mem.Heap;
import com.algoviz.engine.mem.HeapLayer;
import com.algoviz.engine.mem.HeapObject;
import com.algoviz.engine.mem.StackFrame;
import com.algoviz.engine.mem.StackLayer;
import com.algoviz.engine.trace.Trace;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 强化学习·Q-Learning 走迷宫：5×4 迷宫（S 起点、G 终点、2 墙）——TD 更新的逐帧现场。
 * Q(s,a) ← Q(s,a) + lr·(r + γ·max Q(s') − Q(s,a))：奖励沿经验反向传播——
 * 走得多的格子 Q 值被"抬"起来，贪婪策略沿 max Q 走出最短路径。
 * 脚本化动作序列（确定性）：12 步更新（含撞墙负奖励、到 G 正奖励）→ 贪婪路径帧。
 * 画面（math.matrix 复用）：网格 = 每格 max Q（保留 2 位），墙显示 #，G 显示终点；黄圈 = 本步更新格。
 */
public final class QlearnDemo implements Algorithm {

    private static final String SCHEMA = "1";
    private static final String INVARIANT =
            "Q(s,a) ← Q(s,a) + lr·(r + γ·max Q(s') − Q(s,a))：TD 误差把奖励沿经验反向传播——"
                    + "γ<1 让远处的奖励打折，贪婪策略沿 max Q 走；探索(ε)保证没走过的格子也有机会";

    private static final int W = 5, H = 4;
    private static final double LR = 0.5, GAMMA = 0.9;
    private static final double STEP_R = -0.04, GOAL_R = 1.0, WALL_R = -1.0;
    private static final int GOAL = W - 1 + (H - 1) * W;   // (4,3)
    private static final Set<Integer> WALLS = Set.of(2 + W, 3 + W * 2);   // (2,1) 与 (3,2)
    // 脚本化动作序列（0=右 1=下 2=左 3=上；确定性演示）
    private static final int[] SINGLE_ROUND = {0, 0, 0, 0, 1, 1, 1};   // 一轮：右×4 下×3 到 G
    private static final int ROUNDS = 12;
    private static final int[] ACTIONS = buildActions();

    /** 12 轮动作拼接：奖励沿经验反向传播约每轮 1 格——12 轮足以照亮整条路径。 */
    private static int[] buildActions() {
        int[] acts = new int[SINGLE_ROUND.length * ROUNDS];
        for (int i = 0; i < acts.length; i++) {
            acts[i] = SINGLE_ROUND[i % SINGLE_ROUND.length];
        }
        return acts;
    }

    private final List<Step> steps = new ArrayList<>();
    private final double[][] q = new double[H * W][4];
    private int pos, curQ, curA;
    private double curR;
    private final Set<Integer> path = new LinkedHashSet<>();

    @Override public String id() { return "ml.qlearn"; }
    @Override public String title() { return "强化学习·Q-Learning"; }
    @Override public String codeResource() { return "/code/QlearnDemo.java"; }
    @Override public String docResource() { return "/markdown/qlearn.md"; }

    @Override
    public Trace produce() {
        steps.clear();
        for (double[] row : q) {
            Arrays.fill(row, 0);
        }
        pos = 0;
        curQ = -1;
        emit(7, List.of(VizEvent.of(VizEvent.EventType.INIT,
                "5×4 迷宫：S=(0,0)→G=(4,3)，墙×2；Q 表全 0——奖励还没被'传播'到任何格子")),
                "Q-Learning 不需要环境模型：'试出来的经验'就是唯一的老师——无模型的强化学习");
        for (int stepNo = 0; stepNo < ACTIONS.length; stepNo++) {
            int a = ACTIONS[stepNo];
            int next = move(pos, a);
            boolean wall = WALLS.contains(next) || blockedByEdge(pos, a);
            if (wall) {
                next = pos;
            }
            curQ = pos;
            curA = a;
            curR = next == GOAL ? GOAL_R : wall ? WALL_R : STEP_R;
            double maxNext = -Double.MAX_VALUE;
            for (int i = 0; i < 4; i++) {
                maxNext = Math.max(maxNext, q[next][i]);
            }
            if (next == GOAL) {
                maxNext = 0;
            }
            double old = q[pos][a];
            q[pos][a] = old + LR * (curR + GAMMA * maxNext - old);
            if (stepNo % 7 != 6 && stepNo != ACTIONS.length - 1) {
                pos = next;
                if (next == GOAL) {
                    pos = 0;
                }
                continue;
            }
            emit(13, List.of(VizEvent.of(VizEvent.EventType.SET,
                    String.format("step%d：s(%d,%d) 动作%d → r=%.2f，Q ← %.2f + %.1f·(%.2f+%.1f·%.2f−%.2f)=%.2f",
                            stepNo + 1, pos % W, pos / W, a, curR, old, LR, curR, GAMMA, maxNext, old, q[pos][a]),
                    String.format("轮%d", stepNo / 7 + 1))),
                    curR == GOAL_R ? "+1 奖励入账：终点格子被点亮，奖励开始沿经验倒灌"
                            : wall ? "撞墙 −1：负奖励也是老师——'此路不通'被记进 Q 表"
                            : "小步奖励 −0.04：走得越久越亏——逼着策略找捷径");
            pos = next;
            if (next == GOAL) {
                pos = 0;
            }
        }
        curQ = -1;
        // 贪婪路径：从 S 沿 max Q 走
        path.clear();
        pos = 0;
        for (int guard = 0; guard < 12 && pos != GOAL; guard++) {
            path.add(pos);
            int best = 0;
            for (int i = 1; i < 4; i++) {
                if (q[pos][i] > q[pos][best]) {
                    best = i;
                }
            }
            int next = move(pos, best);
            if (WALLS.contains(next) || blockedByEdge(pos, best)) {
                next = pos;
            }
            pos = next;
        }
        path.add(GOAL);
        emit(16, List.of(VizEvent.of(VizEvent.EventType.MARK_SORTED,
                "贪婪路径：从 S 沿 max Q 走——奖励已从终点倒灌到起点")),
                "Q 值 = '从这里出发的未来总奖励期望'——max Q 一路指向 G，路径自己浮出来");
        emit(18, List.of(VizEvent.of(VizEvent.EventType.RETURN,
                "ε-贪婪探索：训练期以 ε 概率随机走（保证没走过的格子也有机会）—— exploitation vs exploration 的永恒平衡")),
                "Q-Learning(1989) → Deep Q-Network(2013)：同一个 Q 函数，用神经网络代替表格就登上了 Atari");
        return new Trace(SCHEMA, meta(), steps);
    }

    private int move(int p, int a) {
        int r = p / W, c = p % W;
        return switch (a) {
            case 0 -> Math.min(W - 1, c + 1) + r * W;
            case 1 -> Math.min(H - 1, r + 1) * W + c;
            case 2 -> Math.max(0, c - 1) + r * W;
            default -> Math.max(0, r - 1) * W + c;
        };
    }

    private boolean blockedByEdge(int p, int a) {
        int r = p / W, c = p % W;
        return switch (a) {
            case 0 -> c == W - 1;
            case 1 -> r == H - 1;
            case 2 -> c == 0;
            default -> r == 0;
        };
    }

    private void emit(int line, List<VizEvent> events, String why) {
        StackFrame frame = new StackFrame("qlearn",
                OrderedMaps.of("位置", Value.prim(String.format("(%d,%d)", pos % W, pos / W)),
                        "本步 Q", Value.prim(curQ >= 0 ? String.format("%.2f", q[curQ][curA]) : "-")), null);
        Map<String, Layer> layers = new LinkedHashMap<>();
        layers.put("mem.stack", new StackLayer(List.of(frame)));
        layers.put("mem.heap", new HeapLayer(snapshotHeap()));
        layers.put("math.matrix", snapshotMath());
        steps.add(new Step(steps.size(), Loc.line(line, why),
                Collections.unmodifiableMap(layers), List.copyOf(events), new Intent(why, INVARIANT)));
    }

    private Heap snapshotHeap() {
        Map<String, Value> fields = new LinkedHashMap<>();
        fields.put("lr / γ", Value.prim("0.5 / 0.9"));
        fields.put("Q 表大小", Value.prim("20 格 × 4 动作"));
        fields.put("终点奖励", Value.prim("+1（步 −0.04 / 撞墙 −1）"));
        Map<String, HeapObject> objs = new LinkedHashMap<>();
        objs.put("@1", new HeapObject("@1", "qlearn", "QTable",
                Collections.unmodifiableMap(fields), List.of()));
        return new Heap(Collections.unmodifiableMap(objs));
    }

    private MathView snapshotMath() {
        List<String> vals = new ArrayList<>();
        Set<Integer> active = new LinkedHashSet<>();
        for (int r = 0; r < H; r++) {
            for (int c = 0; c < W; c++) {
                int idx = c + r * W;
                if (WALLS.contains(idx)) {
                    vals.add("#");
                    continue;
                }
                double maxQ = -Double.MAX_VALUE;
                for (int i = 0; i < 4; i++) {
                    maxQ = Math.max(maxQ, q[idx][i]);
                }
                if (idx == GOAL) {
                    vals.add("G");
                } else {
                    vals.add(String.format("%.2f", maxQ == -Double.MAX_VALUE ? 0 : maxQ));
                }
            }
        }
        if (curQ >= 0) {
            active.add(curQ);
        }
        for (int p : path) {
            active.add(p);
        }
        MathView.Grid grid = new MathView.Grid("maze", "迷宫 max Q（格值 = 未来奖励期望；# 墙；G 终点）",
                H, W, vals, null, null, active, Set.of());
        return new MathView("Q-Learning", List.of(grid));
    }

    private Trace.TraceMeta meta() {
        return new Trace.TraceMeta(id(), title(), codeResource(),
                "5×4 迷宫 lr=0.5", ROUNDS + 3);
    }

    @Override
    public java.util.List<String> dataLines() {
        return java.util.List.of("double lr = 0.5, gamma = 0.9;   //@DATA 学习率与折扣因子");
    }

    @Override
    public Complexity complexity() {
        return new Complexity("O(经验数) 每步一次 TD 更新", "O(状态×动作) Q 表",
                "Q-Learning 是价值迭代的最简形态：Deep Q-Network 用网络近似 Q 表——"
                        + "表格放不下的时候，'函数逼近'接棒");
    }
}
