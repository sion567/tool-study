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
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 机器学习·朴素贝叶斯（垃圾邮件分类）：先验 × 条件概率的逐词乘法现场。
 * 训练 10 封（6 spam / 4 ham），词频：免费(3,0)/中奖(2,0)/会议(0,3)/报告(1,2)——拉普拉斯平滑 +1。
 * 未知邮件「免费 会议」：P(spam)·P(免费|spam)·P(会议|spam) vs P(ham)·P(免费|ham)·P(会议|ham)
 * ——逐词乘出两个分数，大者胜。"朴素" = 假设词与词独立（现实里不真，但好用）。
 * 画面（math.matrix 复用）：网格 = 概率表（词 / P|spam / P|ham）+ 判定行。
 */
public final class NaiveBayesDemo implements Algorithm {

    private static final String SCHEMA = "1";
    private static final String INVARIANT =
            "P(类|邮件) ∝ P(类)·Π P(词|类)：'朴素'假设词与词条件独立——乘法链把每封信压成一个分数；"
                    + "拉普拉斯平滑 +1 让没见过的词有非零概率（不会一票否决）";

    private static final String[] WORDS = {"免费", "中奖", "会议", "报告"};
    private static final int[] SPAM_FREQ = {3, 2, 0, 1};    // 词在 6 封 spam 中的出现次数
    private static final int[] HAM_FREQ = {0, 0, 3, 2};     // 词在 4 封 ham 中
    private static final double P_SPAM = 0.6, P_HAM = 0.4;
    private static final String MAIL = "免费 中奖";

    private final List<Step> steps = new ArrayList<>();
    private double scoreSpam, scoreHam;
    private String verdict;

    @Override public String id() { return "ml.naivebayes"; }
    @Override public String title() { return "朴素贝叶斯分类"; }
    @Override public String codeResource() { return "/code/NaiveBayesDemo.java"; }
    @Override public String docResource() { return "/markdown/naivebayes.md"; }

    @Override
    public Trace produce() {
        steps.clear();
        scoreSpam = P_SPAM;
        scoreHam = P_HAM;
        verdict = "";
        emit(7, List.of(VizEvent.of(VizEvent.EventType.INIT,
                String.format("训练 10 封（6 spam / 4 ham）：先验 P(spam)=%.1f、P(ham)=%.1f——未知邮件「%s」",
                        P_SPAM, P_HAM, MAIL))),
                "先验 = '不看内容时押谁'——后面每个词都在修正这个押注");
        for (int w = 0; w < WORDS.length; w++) {
            double pSpamWord = (SPAM_FREQ[w] + 1) / (6.0 + 4);      // 拉普拉斯平滑：+1 / (词表+类数)
            double pHamWord = (HAM_FREQ[w] + 1) / (4.0 + 4);
            boolean inMail = MAIL.contains(WORDS[w]);
            if (inMail) {
                scoreSpam *= pSpamWord;
                scoreHam *= pHamWord;
            }
            emit(12, List.of(VizEvent.of(VizEvent.EventType.SET,
                    String.format("「%s」%s：P|spam=%.3f，P|ham=%.3f → 累计 score_spam=%.4f vs score_ham=%.4f",
                            WORDS[w], inMail ? "在邮件中" : "不在", pSpamWord, pHamWord, scoreSpam, scoreHam))),
                    inMail ? "在邮件里的词参与乘法链——'免费'在 spam 中常见而 ham 从未出现，分数比被撕开"
                            : "不在邮件里的词不参与乘法——朴素贝叶斯只看'出现了什么'");
        }
        verdict = scoreSpam > scoreHam ? "spam（垃圾邮件）" : "ham（正常邮件）";
        emit(16, List.of(VizEvent.of(VizEvent.EventType.MARK_SORTED,
                String.format("判定：%s（score_spam=%.4f vs score_ham=%.4f）", verdict, scoreSpam, scoreHam))),
                "分数比 = 似然比：>1 押 spam、<1 押 ham——归一化成百分比只是换个说法");
        emit(18, List.of(VizEvent.of(VizEvent.EventType.RETURN,
                "拉普拉斯平滑：没在垃圾邮件里出现过的'会议'获得 1/(6+4) 的非零概率——不会一票否决")),
                "朴素贝叶斯(1950s)至今仍是邮件过滤的基线：快、可解释、小数据也能跑");
        return new Trace(SCHEMA, meta(), steps);
    }

    private void emit(int line, List<VizEvent> events, String why) {
        StackFrame frame = new StackFrame("naivebayes",
                OrderedMaps.of("邮件", Value.prim("「" + MAIL + "」"),
                        "score_spam", Value.prim(String.format("%.4f", scoreSpam)),
                        "score_ham", Value.prim(String.format("%.4f", scoreHam))), null);
        Map<String, Layer> layers = new LinkedHashMap<>();
        layers.put("mem.stack", new StackLayer(List.of(frame)));
        layers.put("mem.heap", new HeapLayer(snapshotHeap()));
        layers.put("math.matrix", snapshotMath());
        steps.add(new Step(steps.size(), Loc.line(line, why),
                Collections.unmodifiableMap(layers), List.copyOf(events), new Intent(why, INVARIANT)));
    }

    private Heap snapshotHeap() {
        Map<String, Value> fields = new LinkedHashMap<>();
        fields.put("先验", Value.prim("P(spam)=0.6 P(ham)=0.4"));
        fields.put("score_spam", Value.prim(String.format("%.4f", scoreSpam)));
        fields.put("score_ham", Value.prim(String.format("%.4f", scoreHam)));
        fields.put("判定", Value.prim(verdict.isEmpty() ? "演算中" : verdict));
        Map<String, HeapObject> objs = new LinkedHashMap<>();
        objs.put("@1", new HeapObject("@1", "nb", "BayesState",
                Collections.unmodifiableMap(fields), List.of()));
        return new Heap(Collections.unmodifiableMap(objs));
    }

    private MathView snapshotMath() {
        List<String> vals = new ArrayList<>();
        Set<Integer> active = new LinkedHashSet<>();
        vals.add("词");
        vals.add("P|spam");
        vals.add("P|ham");
        for (int w = 0; w < WORDS.length; w++) {
            double ps = (SPAM_FREQ[w] + 1) / (6.0 + 4);
            double ph = (HAM_FREQ[w] + 1) / (4.0 + 4);
            vals.add(WORDS[w] + (MAIL.contains(WORDS[w]) ? " ✓" : ""));
            vals.add(String.format("%.3f", ps));
            vals.add(String.format("%.3f", ph));
            if (MAIL.contains(WORDS[w])) {
                active.add((w + 1) * 3);
                active.add((w + 1) * 3 + 1);
                active.add((w + 1) * 3 + 2);
            }
        }
        vals.add("判定");
        vals.add(verdict.isEmpty() ? "…" : verdict);
        vals.add(String.format("%.2f", scoreSpam / Math.max(scoreHam, 1e-12)));
        if (!verdict.isEmpty()) {
            active.add((WORDS.length + 1) * 3);
            active.add((WORDS.length + 1) * 3 + 1);
        }
        MathView.Grid grid = new MathView.Grid("prob", "概率表（拉普拉斯平滑；黄 = 参与乘法链的词）",
                WORDS.length + 2, 3, vals, null, null, active, Set.of());
        return new MathView("朴素贝叶斯", List.of(grid));
    }

    private Trace.TraceMeta meta() {
        return new Trace.TraceMeta(id(), title(), codeResource(),
                "10 封 训练", WORDS.length + 2);
    }

    @Override
    public java.util.List<String> dataLines() {
        return java.util.List.of("String mail = \"免费 会议\";   //@DATA 待分类邮件");
    }

    @Override
    public Complexity complexity() {
        return new Complexity("O(词表) 一次乘法链", "O(词表×类数) 概率表",
                "朴素贝叶斯 = '独立假设 + 贝叶斯定理'：小数据也能跑、训练秒级——"
                        + "它常被当作分类任务的 first baseline");
    }
}
