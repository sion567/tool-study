package com.algoviz.ui;

import com.algoviz.engine.Step;
import com.algoviz.engine.trace.Trace;

import java.util.ArrayList;
import java.util.List;

/** 播放状态机：时间旅行 = 改 index。UI 组件都订阅它。 */
public final class PlaybackController {

    private Trace trace;
    private int index;
    private final List<Runnable> listeners = new ArrayList<>();

    public void load(Trace trace) {
        this.trace = trace;
        this.index = 0;
        fire();
    }

    public Trace trace() {
        return trace;
    }

    public int index() {
        return index;
    }

    public int size() {
        return trace == null ? 0 : trace.steps().size();
    }

    public Step current() {
        return trace == null || trace.steps().isEmpty() ? null : trace.steps().get(index);
    }

    public void next() {
        go(index + 1);
    }

    public void prev() {
        go(index - 1);
    }

    public void go(int i) {
        if (trace == null || trace.steps().isEmpty()) {
            return;
        }
        int clamped = Math.max(0, Math.min(trace.steps().size() - 1, i));
        if (clamped != index) {
            index = clamped;
            fire();
        }
    }

    public void addListener(Runnable r) {
        listeners.add(r);
    }

    private void fire() {
        for (Runnable r : listeners) {
            r.run();
        }
    }
}
