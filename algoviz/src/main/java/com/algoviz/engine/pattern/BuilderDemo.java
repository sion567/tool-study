package com.algoviz.engine.pattern;

import com.algoviz.engine.Algorithm;
import com.algoviz.engine.Value;
import com.algoviz.engine.VizEvent;
import com.algoviz.engine.ds.SeqView;
import com.algoviz.engine.trace.Trace;

import java.util.List;

/** 建造者演示：分步装配同一个对象——每步只改一个字段，build 之前产品不可用。 */
public final class BuilderDemo implements Algorithm {

    private static final String SCHEMA = "1";
    private static final String INVARIANT = "分步装配同一个对象：每步只改一个字段，build 之前产品不完整也不可用";

    private static final int L_NEW = 14;
    private static final int L_TITLE = 15;
    private static final int L_BORDER = 16;
    private static final int L_OPACITY = 17;
    private static final int L_BUILD = 18;
    private static final int L_USE = 19;

    private PatternEmitter em;

    @Override public String id() { return "pattern.builder"; }
    @Override public String title() { return "建造者模式"; }
    @Override public String codeResource() { return "/code/BuilderDemo.java"; }
    @Override public String docResource() { return "/markdown/pattern-builder.md"; }

    @Override
    public Trace produce() {
        em = new PatternEmitter(List.of(
                new SeqView.Participant("client", "client"),
                new SeqView.Participant("builder", "builder : Builder"),
                new SeqView.Participant("dialog", "dlg : Dialog")), INVARIANT);
        em.addObject("@1", "Builder");
        em.addObject("@2", "Dialog");
        em.setField("@1", "d", Value.ref("@2"));
        em.setField("@2", "title", Value.prim("null"));
        em.setField("@2", "border", Value.prim("false"));
        em.setField("@2", "opacity", Value.prim("0.0"));
        em.mainLocals.put("builder", Value.ref("@1"));
        em.state(L_NEW, VizEvent.EventType.INIT,
                "new Builder()：内部产品 @2 就位——三个字段全是默认值", List.of("@2"),
                "字段多到构造器装不下（telescoping constructor）时，装配过程自己成为对象");
        em.message(L_TITLE, 0, 1, "title(\"AlgoViz\")", "CALL", "第一步：设置标题（返回 this，可以链着写）");
        em.setField("@2", "title", Value.prim("AlgoViz"));
        em.state(L_TITLE, VizEvent.EventType.SET, "@2.title ← AlgoViz（其余仍是默认）", List.of("@2"),
                "每步只动一个字段——装配过程清晰可读");
        em.message(L_BORDER, 0, 1, "border(true)", "CALL", "第二步：边框");
        em.setField("@2", "border", Value.prim("true"));
        em.state(L_BORDER, VizEvent.EventType.SET, "@2.border ← true", List.of("@2"), "可链式：return this 的秘密");
        em.message(L_OPACITY, 0, 1, "opacity(0.8)", "CALL", "第三步：透明度");
        em.setField("@2", "opacity", Value.prim("0.8"));
        em.state(L_OPACITY, VizEvent.EventType.SET, "@2.opacity ← 0.8", List.of("@2"), "参数名即方法名：自文档化");
        em.message(L_BUILD, 0, 1, "build()", "CALL", "最后一步：产品成型");
        em.state(L_BUILD, VizEvent.EventType.SET, "build() 返回装配完的 @2——之前它不该被使用", List.of("@2"),
                "build 之前字段可以任意顺序补齐；build 之后产品不可变更佳");
        em.state(L_USE, VizEvent.EventType.RETURN, "打印：Dialog{AlgoViz,边框=true,透明=0.8}", List.of("@2"),
                "对比 8 参数构造器：顺序错、类型近的参数互换了编译器都不吭声");
        return new Trace(SCHEMA, meta(new int[0]), em.steps);
    }
}
