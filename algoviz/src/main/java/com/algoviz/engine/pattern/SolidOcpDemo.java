package com.algoviz.engine.pattern;

import com.algoviz.engine.Algorithm;
import com.algoviz.engine.Loc;
import com.algoviz.engine.Step;
import com.algoviz.engine.VizEvent;
import com.algoviz.engine.ds.SeqView;
import com.algoviz.engine.trace.Trace;

import java.util.List;

/**
 * SOLID·O 开闭原则：对扩展开放，对修改关闭。
 * 反例：AreaCalculator 用 switch(shape.type)——每加一种图形都要改它（改历史代码）。
 * 正例：Shape.area() 多态——新图形 = 新类，老代码零修改。
 */
public final class SolidOcpDemo implements Algorithm {

    private static final String SCHEMA = "1";
    private static final String INVARIANT = "新增行为 = 新增类，不改已测试的老代码——扩展点留在抽象上";

    private static final int L_BAD1 = 5;
    private static final int L_BAD2 = 7;
    private static final int L_GOOD1 = 14;
    private static final int L_GOOD2 = 16;

    private PatternEmitter em;

    @Override public String id() { return "solid.ocp"; }
    @Override public String title() { return "SOLID·开闭 OCP"; }
    @Override public String codeResource() { return "/code/SolidOcpDemo.java"; }
    @Override public String docResource() { return "/markdown/solid-ocp.md"; }

    @Override
    public Trace produce() {
        em = new PatternEmitter(List.of(
                new SeqView.Participant("client", "client"),
                new SeqView.Participant("calc", "AreaCalculator（switch 版）"),
                new SeqView.Participant("circle", "Circle"),
                new SeqView.Participant("tri", "Triangle（新增）"),
                new SeqView.Participant("poly", "Shape.area() 多态")), INVARIANT);
        em.reset();
        // ===== 反例：switch 陷进 =====
        em.message(L_BAD1, 0, 1, "area(circle) → case CIRCLE: πr²", "CALL",
                "反例：类型判断写死在计算器里——每种图形一个 case");
        em.message(L_BAD2, 0, 1, "area(triangle) → 必须回去改 switch！新增 case TRIANGLE", "CALL",
            "痛感：加三角形 = 修改已上线的 AreaCalculator——已测试的逻辑重新暴露在风险里");
        // ===== 正例：多态扩展 =====
        em.message(L_GOOD1, 0, 4, "area(shape) → shape.area()（动态分派）", "CALL",
                "正例：计算器只认 Shape 抽象——它对具体图形永远关闭修改");
        em.message(L_GOOD2, 0, 2, "Circle.area()，Triangle.area()（新类自己接上）", "CALL",
                "新图形 = 新增一个类实现 area()——AreaCalculator 一行不动，OCP 达成");
        em.state(L_GOOD2, VizEvent.EventType.RETURN,
                "扩展点从「if/switch 链」挪到「抽象方法」——变化被新类吸收", List.of(),
                "OCP 不是不许改代码，是把「必然发生的变化」赶到新增文件里去");
        return new Trace(SCHEMA, meta(), em.steps);
    }

    private Trace.TraceMeta meta() {
        return new Trace.TraceMeta(id(), title(), codeResource(), "switch 3 分支 → 0 分支", 2);
    }

    @Override
    public java.util.List<String> dataLines() {
        return List.of("// 反例: switch(shape.type){CASE_CIRCLE...} → 正例: shape.area() 多态");
    }

    @Override
    public com.algoviz.engine.Complexity complexity() {
        return new com.algoviz.engine.Complexity("O(1) 结构原则", "O(n) 类数=图形种数",
                "识别信号：新增一种情况要改老方法 → 该把条件变成多态了（和「以多态取代条件」重构互为表里）");
    }
}
