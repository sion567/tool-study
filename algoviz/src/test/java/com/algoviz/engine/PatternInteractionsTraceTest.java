package com.algoviz.engine;

import com.algoviz.engine.pattern.ObserverDemo;
import com.algoviz.engine.pattern.TemplateMethodDemo;
import com.algoviz.engine.trace.Trace;
import com.algoviz.engine.ds.SeqView;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.algoviz.engine.PatternDemosTraceTest.golden;
import static com.algoviz.engine.TraceAsserts.assertContract;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PatternInteractionsTraceTest {

    /** 行为：观察者——最后两帧必须是对两个订阅者的广播（一发，多方响应）。 */
    @Test
    void observerBroadcastsToBothSubscribers() {
        Trace t = new ObserverDemo().produce();
        assertContract(t);
        SeqView last = t.steps().get(t.steps().size() - 1).seq();
        int n = last.messages().size();
        SeqView.Message toCanvas = last.messages().get(n - 2);
        SeqView.Message toVars = last.messages().get(n - 1);
        assertEquals("canvas", last.participants().get(toCanvas.to()).id());
        assertEquals("vars", last.participants().get(toVars.to()).id());
        assertEquals("engine", last.participants().get(toCanvas.from()).id());
    }

    @Test
    void observerGoldenMatches() throws Exception {
        golden("pattern_observer", new ObserverDemo().produce());
    }

    /** 行为：模板方法——消息顺序必须严格 load → compute → render。 */
    @Test
    void templateMethodOrderIsLocked() {
        Trace t = new TemplateMethodDemo().produce();
        assertContract(t);
        SeqView last = t.steps().get(t.steps().size() - 1).seq();
        List<String> labels = last.messages().stream().map(SeqView.Message::label).toList();
        assertTrue(labels.stream().anyMatch(l -> l.startsWith("load()")));
        assertTrue(labels.stream().anyMatch(l -> l.startsWith("compute()")));
        assertTrue(labels.stream().anyMatch(l -> l.startsWith("render()")));
        int load = indexOfStartsWith(labels, "load()");
        int compute = indexOfStartsWith(labels, "compute()");
        int render = indexOfStartsWith(labels, "render()");
        assertTrue(load < compute && compute < render, "骨架顺序必须是 load → compute → render");
    }

    @Test
    void templateMethodGoldenMatches() throws Exception {
        golden("pattern_templatemethod", new TemplateMethodDemo().produce());
    }

    private static int indexOfStartsWith(List<String> labels, String prefix) {
        for (int i = 0; i < labels.size(); i++) {
            if (labels.get(i).startsWith(prefix)) {
                return i;
            }
        }
        return -1;
    }
}
