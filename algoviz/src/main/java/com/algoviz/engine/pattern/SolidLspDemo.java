package com.algoviz.engine.pattern;

import com.algoviz.engine.Algorithm;
import com.algoviz.engine.Loc;
import com.algoviz.engine.Step;
import com.algoviz.engine.VizEvent;
import com.algoviz.engine.ds.SeqView;
import com.algoviz.engine.trace.Trace;

import java.util.List;

/**
 * SOLID·L 里氏替换：任何父类出现的地方，子类塞进去必须照样工作。
 * 反例：Square extends Rectangle——setWidth(5)+setHeight(4) 后宽=高=4，面积契约被打破，
 * 调用方被迫 instanceof 特判（继承关系名存实亡）。
 * 正例：二者平行为 Shape 的两个实现，area() 各自成立，替换无忧。
 */
public final class SolidLspDemo implements Algorithm {

    private static final String SCHEMA = "1";
    private static final String INVARIANT = "子类不得收紧父类的契约——「is-a」要经得起行为检验，不只是概念上像";

    private static final int L_BAD1 = 6;
    private static final int L_BAD2 = 8;
    private static final int L_GOOD = 14;

    private PatternEmitter em;

    @Override public String id() { return "solid.lsp"; }
    @Override public String title() { return "SOLID·里氏替换 LSP"; }
    @Override public String codeResource() { return "/code/SolidLspDemo.java"; }
    @Override public String docResource() { return "/markdown/solid-lsp.md"; }

    @Override
    public Trace produce() {
        em = new PatternEmitter(List.of(
                new SeqView.Participant("client", "client"),
                new SeqView.Participant("rect", "Rectangle"),
                new SeqView.Participant("sq", "Square（反例：extends Rectangle）"),
                new SeqView.Participant("shape", "Shape 接口")), INVARIANT);
        em.reset();
        // ===== 反例：继承破坏契约 =====
        em.message(L_BAD1, 0, 2, "setWidth(5); setHeight(4)（以 Rectangle 的方式用）", "CALL",
                "反例：Square 重写 setter 强行保持宽=高——5 和 4 被悄悄吞成 4 和 4");
        em.state(L_BAD2, VizEvent.EventType.COMPARE,
                "面积 20？实际 16 —— Rectangle 的契约被 Square 撕了", List.of(),
                "调用方拿着父类引用却算错账 → 只能 instanceof 特判 → 继承形同虚设");
        // ===== 正例：平行实现 =====
        em.message(L_GOOD, 0, 3, "shape.area()：Rectangle/Square 各自成立，可互换", "CALL",
                "正例：正方形不是长方形的特化，是 Shape 的平行实现——「is-a」靠行为定义");
        em.state(L_GOOD, VizEvent.EventType.RETURN,
                "任何 Shape 引用塞进任何实现，行为都符合接口承诺", List.of(),
                "LSP 检验法：把子类实例传进所有父类测试用例，必须全绿");
        return new Trace(SCHEMA, meta(), em.steps);
    }

    private Trace.TraceMeta meta() {
        return new Trace.TraceMeta(id(), title(), codeResource(), "面积 20 vs 16", 2);
    }

    @Override
    public java.util.List<String> dataLines() {
        return List.of("// 反例: square.setWidth(5); setHeight(4); area()=16≠20 → 平行实现 Shape");
    }

    @Override
    public com.algoviz.engine.Complexity complexity() {
        return new com.algoviz.engine.Complexity("O(1) 结构原则", "O(1)",
                "常见破约现场：子类抛 UnsupportedOperationException、忽略参数、收紧取值范围——都是 LSP 违例");
    }
}
