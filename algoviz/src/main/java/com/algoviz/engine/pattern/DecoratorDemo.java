package com.algoviz.engine.pattern;

import com.algoviz.engine.Algorithm;
import com.algoviz.engine.Value;
import com.algoviz.engine.VizEvent;
import com.algoviz.engine.ds.SeqView;
import com.algoviz.engine.trace.Trace;

import java.util.List;

/** 装饰器模式演示：同一接口层层包裹，洋葱式执行——调用入口不变。 */
public final class DecoratorDemo implements Algorithm {

    private static final String SCHEMA = "1";
    private static final String INVARIANT =
            "装饰器与组件同接口：包几层，调用入口不变——层层转发，洋葱式执行";

    private static final int L_ASSEMBLE = 23;
    private static final int L_DRAW = 24;
    private static final int L_BORDER_D = 15;
    private static final int L_GRID_D = 19;
    private static final int L_PLAIN_BODY = 6;

    private PatternEmitter em;

    public DecoratorDemo() {
        em = new PatternEmitter(List.of(
                new SeqView.Participant("client", "client"),
                new SeqView.Participant("border", "border : BorderFrame"),
                new SeqView.Participant("grid", "grid : GridFrame"),
                new SeqView.Participant("plain", "plain : PlainFrame")), INVARIANT);
    }

    @Override
    public String id() {
        return "pattern.decorator";
    }

    @Override
    public String title() {
        return "装饰器模式";
    }

    @Override
    public String codeResource() {
        return "/code/DecoratorDemo.java";
    }

    @Override
    public String docResource() {
        return "/markdown/pattern-decorator.md";
    }

    @Override
    public Trace produce() {
        em.reset();
        em.addObject("@1", "BorderFrame");
        em.addObject("@2", "GridFrame");
        em.addObject("@3", "PlainFrame");
        em.setField("@1", "inner", Value.ref("@2"));
        em.setField("@2", "inner", Value.ref("@3"));
        em.mainLocals.put("f", Value.ref("@1"));
        em.state(L_ASSEMBLE, VizEvent.EventType.INIT,
                "装配：@1{inner=@2{inner=@3}}——三层洋葱，全部实现同一个 Frame 接口", List.of("@1"),
                "看堆面板：每个装饰器都握着 inner 引用——包一层，加一点行为");
        em.message(L_DRAW, 0, 1, "f.draw()", "CALL", "调用入口只有一个：最外层的 draw()——加了几层调用方根本不知道");
        em.message(L_BORDER_D, 1, 2, "inner.draw()", "CALL", "边框画一半，把活转交给里层");
        em.message(L_GRID_D, 2, 3, "inner.draw()", "CALL", "再转交——洋葱剥到芯");
        em.message(L_PLAIN_BODY, 3, 2, "画柱状图", "RETURN", "核心工作在原始组件里完成");
        em.message(L_GRID_D, 2, 1, "返回", "RETURN", "网格收尾，向外层交还");
        em.message(L_BORDER_D, 1, 0, "返回", "RETURN", "边框最后收口——层层退出");
        em.state(L_DRAW, VizEvent.EventType.RETURN,
                "调用方全程只调了一次 f.draw()——加了三层行为，一行代码没多", List.of("@1"),
                "继承加行为的静态版；运行时想再加一层？new 一个包上去即可");
        return new Trace(SCHEMA, meta(new int[0]), em.steps);
    }
}
