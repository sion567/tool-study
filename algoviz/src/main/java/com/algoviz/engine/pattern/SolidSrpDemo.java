package com.algoviz.engine.pattern;

import com.algoviz.engine.Algorithm;
import com.algoviz.engine.Loc;
import com.algoviz.engine.Step;
import com.algoviz.engine.VizEvent;
import com.algoviz.engine.ds.SeqView;
import com.algoviz.engine.trace.Trace;

import java.util.List;

/**
 * SOLID·S 单一职责：一个类只对**一个变化原因**负责。
 * 反例：Employee 一类包揽算薪+报表+存储——改报表格式也要回归测试薪资逻辑。
 * 正例：拆成三个小类，各自独立演化；类数变多但每个的变化原因只剩一个。
 */
public final class SolidSrpDemo implements Algorithm {

    private static final String SCHEMA = "1";
    private static final String INVARIANT = "一个类只对一个变化原因负责——拆类的标准是「变化的方向」，不是代码行数";

    private static final int L_BAD = 4;
    private static final int L_PAIN = 6;
    private static final int L_GOOD = 12;
    private static final int L_GAIN = 16;

    private PatternEmitter em;

    @Override public String id() { return "solid.srp"; }
    @Override public String title() { return "SOLID·单一职责 SRP"; }
    @Override public String codeResource() { return "/code/SolidSrpDemo.java"; }
    @Override public String docResource() { return "/markdown/solid-srp.md"; }

    @Override
    public Trace produce() {
        em = new PatternEmitter(List.of(
                new SeqView.Participant("client", "client"),
                new SeqView.Participant("god", "Employee（反例）"),
                new SeqView.Participant("pay", "SalaryCalc"),
                new SeqView.Participant("repo", "ReportGen"),
                new SeqView.Participant("store", "EmployeeRepo")), INVARIANT);
        em.reset();
        // ===== 反例 =====
        em.message(L_BAD, 0, 1, "算薪资 + 报表 + 存库（全在一个类里）", "CALL",
                "反例：三个变化方向挤在一个类——税率动、报表格式动、表结构动，改谁都碰它");
        em.state(L_PAIN, VizEvent.EventType.COMPARE,
                "改报表格式 → 重新编译+回归测试「薪资」逻辑——无辜模块被连坐", List.of(),
                "痛感的来源：职责多了，测试范围 = 整个类");
        // ===== 正例 =====
        em.message(L_GOOD, 0, 2, "算薪资", "CALL", "正例：按变化方向拆——谁会变，谁就单独一类");
        em.message(L_GOOD, 0, 3, "生成报表", "CALL", "报表格式再怎么改，只碰 ReportGen 一个文件");
        em.message(L_GOOD, 0, 4, "存取数据库", "CALL", "存储换 MySQL→PG 也只动 EmployeeRepo");
        em.state(L_GAIN, VizEvent.EventType.RETURN,
                "类从 1 个变 3 个——但每个类的测试范围都缩到 1/3", List.of(),
                "SRP 的收益不是类少，是**改动的影响半径**小");
        return new Trace(SCHEMA, meta(), em.steps);
    }

    private Trace.TraceMeta meta() {
        return new Trace.TraceMeta(id(), title(), codeResource(), "1 类 → 3 类", 3);
    }

    @Override
    public java.util.List<String> dataLines() {
        return List.of("// 反例: Employee{calcPay(); genReport(); save();}  → 拆成 3 个各管一摊");
    }

    @Override
    public com.algoviz.engine.Complexity complexity() {
        return new com.algoviz.engine.Complexity("O(1) 结构原则", "O(类数) 略增",
                "判据：问「这个类有几个变化的原因？」——超过 1 个就该拆。与代码长短无关");
    }
}
