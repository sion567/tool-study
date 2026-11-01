package com.algoviz.engine.pattern;

import com.algoviz.engine.Intent;
import com.algoviz.engine.Layer;
import com.algoviz.engine.Loc;
import com.algoviz.engine.Step;
import com.algoviz.engine.Value;
import com.algoviz.engine.VizEvent;
import com.algoviz.engine.ds.SeqView;
import com.algoviz.engine.mem.Heap;
import com.algoviz.engine.mem.HeapLayer;
import com.algoviz.engine.mem.HeapObject;
import com.algoviz.engine.mem.StackFrame;
import com.algoviz.engine.mem.StackLayer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 模式演示共用的发射器：seq.main 图层（泳道+消息时间轴）+ mem.heap（对象图）+ main 栈帧。 */
final class PatternEmitter {

    final List<Step> steps = new ArrayList<>();
    final List<SeqView.Participant> participants;
    final List<SeqView.Message> messages = new ArrayList<>();
    final Map<String, Map<String, Value>> objects = new LinkedHashMap<>();
    final Map<String, String> types = new LinkedHashMap<>();
    final Map<String, Value> mainLocals = new LinkedHashMap<>();
    int activeIndex = -1;

    private final String invariant;

    PatternEmitter(List<SeqView.Participant> participants, String invariant) {
        this.participants = participants;
        this.invariant = invariant;
    }

    /** 重复 produce 前必须清零（Registry 缓存实例，produce 会被多次调用）。 */
    void reset() {
        steps.clear();
        messages.clear();
        objects.clear();
        types.clear();
        mainLocals.clear();
        activeIndex = -1;
    }

    void addObject(String id, String type) {
        types.put(id, type);
        objects.put(id, new LinkedHashMap<>());
    }

    void setField(String id, String field, Value v) {
        objects.get(id).put(field, v);
    }

    /** 发一条消息：消息进时间轴，本步激活它。 */
    void message(int line, int from, int to, String label, String kind, String why) {
        messages.add(new SeqView.Message(from, to, label, kind));
        activeIndex = messages.size() - 1;
        SeqView.Participant f = participants.get(from);
        SeqView.Participant t = participants.get(to);
        emit(line, List.of(VizEvent.of(VizEvent.EventType.MESSAGE,
                f.label() + " → " + t.label() + "：" + label, f.label(), t.label())), why);
    }

    /** 非消息步：对象图变化 / 汇总说明（activeIndex 保持不动）。 */
    void state(int line, VizEvent.EventType type, String detail, List<String> targets, String why) {
        emit(line, List.of(VizEvent.of(type, detail, targets.toArray(new String[0]))), why);
    }

    private void emit(int line, List<VizEvent> events, String why) {
        StackFrame frame = new StackFrame("main",
                Collections.unmodifiableMap(new LinkedHashMap<>(mainLocals)), null);
        Map<String, Layer> layers = new LinkedHashMap<>();
        layers.put("mem.stack", new StackLayer(List.of(frame)));
        layers.put("mem.heap", new HeapLayer(snapshotHeap()));
        layers.put("seq.main", snapshotSeq());
        steps.add(new Step(steps.size(), Loc.line(line, why),
                Collections.unmodifiableMap(layers), List.copyOf(events),
                new Intent(why, invariant)));
    }

    private Heap snapshotHeap() {
        Map<String, HeapObject> objs = new LinkedHashMap<>();
        for (Map.Entry<String, Map<String, Value>> e : objects.entrySet()) {
            objs.put(e.getKey(), new HeapObject(e.getKey(), e.getKey(), types.get(e.getKey()),
                    Collections.unmodifiableMap(new LinkedHashMap<>(e.getValue())), List.of()));
        }
        return new Heap(Collections.unmodifiableMap(objs));
    }

    private SeqView snapshotSeq() {
        return new SeqView("pattern", List.copyOf(participants), List.copyOf(messages), activeIndex);
    }
}
