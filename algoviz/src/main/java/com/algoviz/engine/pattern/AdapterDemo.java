package com.algoviz.engine.pattern;

import com.algoviz.engine.Algorithm;
import com.algoviz.engine.Value;
import com.algoviz.engine.VizEvent;
import com.algoviz.engine.ds.SeqView;
import com.algoviz.engine.trace.Trace;

import java.util.List;

/** 适配器演示：接口不兼容的两个人合作——中间加一个翻译层，双方代码零改动。 */
public final class AdapterDemo implements Algorithm {

    private static final String SCHEMA = "1";
    private static final String INVARIANT = "接口不兼容的两个人合作：中间加一个翻译层，双方代码零改动";

    private static final int L_NEW = 12;
    private static final int L_REQ = 13;
    private static final int L_TRANSLATE = 9;
    private static final int L_ADAPTEE_BODY = 4;

    private PatternEmitter em;

    @Override public String id() { return "pattern.adapter"; }
    @Override public String title() { return "适配器模式"; }
    @Override public String codeResource() { return "/code/AdapterDemo.java"; }
    @Override public String docResource() { return "/markdown/pattern-adapter.md"; }

    @Override
    public Trace produce() {
        em = new PatternEmitter(List.of(
                new SeqView.Participant("client", "client"),
                new SeqView.Participant("adapter", "t : Adapter"),
                new SeqView.Participant("adaptee", "adaptee : Adaptee")), INVARIANT);
        em.addObject("@1", "Adapter");
        em.addObject("@2", "Adaptee");
        em.setField("@1", "adaptee", Value.ref("@2"));
        em.mainLocals.put("t", Value.ref("@1"));
        em.state(L_NEW, VizEvent.EventType.INIT, "装配：@1 包住 @2——Target 在左，Adaptee 在右", List.of("@1"),
                "已有的类不能改（库/历史代码），包一层让它说 Target 的话");
        em.message(L_REQ, 0, 1, "request()", "CALL", "调用方按 Target 接口发请求——它以为自己在调「原生」方法");
        em.message(L_TRANSLATE, 1, 2, "specificRequest()", "CALL", "翻译层转手：调的是被适配者的原名");
        em.message(L_ADAPTEE_BODY, 2, 1, "EUR 100", "RETURN", "旧接口的返回形状");
        em.message(L_TRANSLATE, 1, 0, "CNY 800", "RETURN", "适配器把返回值也翻译了——欧换人民币");
        em.state(L_REQ, VizEvent.EventType.RETURN, "调用方拿到 CNY：Adaptee 与 Client 至死不见面", List.of("@1"),
                "适配器 = 接口转换器：改一层，救两边");
        return new Trace(SCHEMA, meta(new int[0]), em.steps);
    }
}
