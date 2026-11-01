package com.algoviz.ui;

import com.algoviz.engine.Step;
import com.algoviz.engine.Value;
import com.algoviz.engine.mem.HeapObject;
import com.algoviz.engine.mem.StackFrame;
import javafx.collections.FXCollections;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 底部现场面板（蓝图 §1.2 的底板实物化）：左栈帧局部变量，右堆盒子。
 * 视图 C：与上帧相比变化的行黄字加粗。
 */
public final class VarsPanel extends VBox {

    private final ListView<String> locals = new ListView<>();
    private final ListView<String> heap = new ListView<>();

    private Set<String> changedKeys = Set.of();
    private boolean heapChanged;

    public VarsPanel() {
        locals.setPrefHeight(100);
        heap.setPrefHeight(100);
        locals.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : item);
                setStyle(item != null && isChangedLocal(item)
                        ? Theme.cellTextChanged()
                        : Theme.cellText());
            }
        });
        heap.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : item);
                setStyle(heapChanged
                        ? Theme.cellTextChanged()
                        : Theme.cellText());
            }
        });
        TitledPane localsPane = new TitledPane("栈帧 sort() 局部变量（黄 = 本步变了）", locals);
        TitledPane heapPane = new TitledPane("堆（@编号 = 地址；黄 = 本步变了）", heap);
        localsPane.setCollapsible(false);
        heapPane.setCollapsible(false);
        getChildren().addAll(localsPane, heapPane);
    }

    /** 主题切换后重刷单元格配色。 */
    public void applyTheme() {
        locals.refresh();
        heap.refresh();
    }

    public void render(Step cur, Step prev) {
        changedKeys = DiffHighlighter.changedLocalKeys(
                prev == null ? null : prev.stack(), cur == null ? null : cur.stack());
        heapChanged = DiffHighlighter.heapChanged(
                prev == null ? null : prev.heap(), cur == null ? null : cur.heap());
        locals.setItems(FXCollections.observableList(renderLocals(cur)));
        heap.setItems(FXCollections.observableList(renderHeap(cur)));
        locals.refresh();
        heap.refresh();
    }

    private boolean isChangedLocal(String row) {
        String trimmed = row.trim();
        int eq = trimmed.indexOf(" =");
        String key = eq > 0 ? trimmed.substring(0, eq) : trimmed;
        return changedKeys.contains(key);
    }

    private static List<String> renderLocals(Step step) {
        List<String> out = new ArrayList<>();
        if (step != null && step.stack() != null) {
            for (StackFrame f : step.stack().frames()) {
                out.add("▸ " + f.method() + "()");
                for (Map.Entry<String, Value> e : f.locals().entrySet()) {
                    String suffix = e.getValue().kind() == Value.Kind.REF ? "  ← 地址，不是值" : "";
                    out.add("   " + e.getKey() + " = " + e.getValue().display() + suffix);
                }
            }
        }
        return out;
    }

    private static List<String> renderHeap(Step step) {
        List<String> out = new ArrayList<>();
        if (step != null && step.heap() != null) {
            for (HeapObject o : step.heap().objects().values()) {
                if (!o.elements().isEmpty()) {
                    StringBuilder sb = new StringBuilder(o.id() + "  " + o.type() + "  " + o.label() + " = [");
                    for (int i = 0; i < o.elements().size(); i++) {
                        if (i > 0) {
                            sb.append(", ");
                        }
                        sb.append(o.elements().get(i).display());
                    }
                    out.add(sb.append("]").toString());
                } else {
                    StringBuilder sb = new StringBuilder(o.id() + "  " + o.type() + "  {");
                    boolean first = true;
                    for (Map.Entry<String, Value> f : o.fields().entrySet()) {
                        if (!first) {
                            sb.append(", ");
                        }
                        sb.append(f.getKey()).append("=").append(f.getValue().display());
                        first = false;
                    }
                    out.add(sb.append("}").toString());
                }
            }
        }
        return out;
    }
}
