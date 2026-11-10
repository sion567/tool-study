package com.algoviz.engine.math;

import com.algoviz.engine.Algorithm;
import com.algoviz.engine.Complexity;
import com.algoviz.engine.Intent;
import com.algoviz.engine.Layer;
import com.algoviz.engine.Loc;
import com.algoviz.engine.OrderedMaps;
import com.algoviz.engine.Step;
import com.algoviz.engine.Value;
import com.algoviz.engine.VizEvent;
import com.algoviz.engine.math.MathView;
import com.algoviz.engine.mem.Heap;
import com.algoviz.engine.mem.HeapLayer;
import com.algoviz.engine.mem.HeapObject;
import com.algoviz.engine.mem.StackFrame;
import com.algoviz.engine.mem.StackLayer;
import com.algoviz.engine.trace.Trace;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 汉诺塔：递归分治的祖师爷。n 个盘从 A 挪到 C，借助 B。
 * hanoi(n) = hanoi(n−1, A→B) + 挪第 n 盘 A→C + hanoi(n−1, B→C)——共 2ⁿ−1 步。
 * 画面：三根柱子（matrix 三列），盘子从上到下越大；每步一根柱子顶端的盘飞走。
 */
public final class HanoiDemo implements Algorithm {

    private static final String SCHEMA = "1";
    private static final String INVARIANT =
            "大盘永不上小盘——递归式：把 n−1 个盘让开、挪最大盘、再把 n−1 个盘盖上；步数恒为 2ⁿ−1";

    private static final int N = 5;
    private static final String[] PEG_NAMES = {"A", "B", "C"};

    private final List<Step> steps = new ArrayList<>();
    private final List<Deque<Integer>> pegs = new ArrayList<>();
    private int moves = 0;
    private int curDisk = -1;
    private int[] curCell = {-1, -1};

    @Override public String id() { return "math.hanoi"; }
    @Override public String title() { return "汉诺塔"; }
    @Override public String codeResource() { return "/code/HanoiDemo.java"; }
    @Override public String docResource() { return "/markdown/hanoi.md"; }

    @Override
    public Trace produce() {
        steps.clear();
        pegs.clear();
        for (int i = 0; i < 3; i++) {
            pegs.add(new ArrayDeque<>());
        }
        for (int d = N; d >= 1; d--) {
            pegs.get(0).push(d);   // A 柱：底=最大
        }
        moves = 0;
        curDisk = -1;
        emit(4, List.of(VizEvent.of(VizEvent.EventType.INIT,
                N + " 个盘在 A 柱（上小下大），目标全挪到 C——大盘永远不能压小盘")),
                "直接想 5 个盘会头晕——递归把它翻译成 3 个更小的同款问题");
        hanoi(N, 0, 2, 1);
        emit(9, List.of(VizEvent.of(VizEvent.EventType.RETURN,
                "完成：" + moves + " 步 = 2⁵−1（任何 n 个盘恰好 2ⁿ−1 步，多一步少一步都不可能）")),
                "递归三问：①子问题同构（挪 n−1 个盘）②出口（n=0 什么都不做）③步数递推 T(n)=2T(n−1)+1 → 2ⁿ−1");
        return new Trace(SCHEMA, meta(), steps);
    }

    private void hanoi(int n, int from, int to, int via) {
        if (n == 0) {
            return;
        }
        hanoi(n - 1, from, via, to);        // ① 让开：上面的 n−1 个盘挪到中转柱
        int disk = pegs.get(from).pop();    // ② 挪第 n 盘（最大的那个）
        pegs.get(to).push(disk);
        moves++;
        curDisk = disk;
        int col = to;
        int row = N - pegs.get(to).size();  // 落点在柱内的行号（顶端）
        curCell[0] = row;
        curCell[1] = col;
        emit(6, List.of(VizEvent.of(VizEvent.EventType.SET,
                "第 " + moves + " 步：盘 " + disk + " " + PEG_NAMES[from] + " → " + PEG_NAMES[to])),
                "递归每层只负责一个动作：挪自己这一层的盘——让开与盖上交给递归");
        hanoi(n - 1, via, to, from);        // ③ 盖上：n−1 个盘从中转柱挪到目标柱
    }

    private void emit(int line, List<VizEvent> events, String why) {
        StackFrame frame = new StackFrame("hanoi",
                OrderedMaps.of("步数", Value.prim(moves + " / " + ((1 << N) - 1)),
                        "本步", Value.prim(curDisk > 0 ? "盘 " + curDisk : "-")), null);
        Map<String, Layer> layers = new LinkedHashMap<>();
        layers.put("mem.stack", new StackLayer(List.of(frame)));
        layers.put("mem.heap", new HeapLayer(snapshotHeap()));
        layers.put("math.matrix", snapshotMatrix());
        steps.add(new Step(steps.size(), Loc.line(line, why),
                Collections.unmodifiableMap(layers), List.copyOf(events), new Intent(why, INVARIANT)));
    }

    private Heap snapshotHeap() {
        Map<String, Value> fields = new LinkedHashMap<>();
        for (int i = 0; i < 3; i++) {
            List<Integer> disks = new ArrayList<>(pegs.get(i));
            Collections.reverse(disks);   // 显示自底向上
            fields.put(PEG_NAMES[i] + " 柱", Value.prim(disks.toString()));
        }
        Map<String, HeapObject> objs = new LinkedHashMap<>();
        objs.put("@1", new HeapObject("@1", "pegs", "HanoiState",
                Collections.unmodifiableMap(fields), List.of()));
        return new Heap(Collections.unmodifiableMap(objs));
    }

    private MathView snapshotMatrix() {
        List<String> vals = new ArrayList<>();
        Set<Integer> done = new LinkedHashSet<>();
        Set<Integer> active = new LinkedHashSet<>();
        for (int row = 0; row < N; row++) {          // row 0 = 顶端
            for (int col = 0; col < 3; col++) {
                Deque<Integer> peg = pegs.get(col);
                int idx = row * 3 + col;
                int depthFromBottom = N - row;        // 该行距柱底 1..N
                if (peg.size() >= depthFromBottom) {
                    int disk = (int) peg.toArray()[peg.size() - depthFromBottom];
                    vals.add(String.valueOf(disk));
                    done.add(idx);
                    if (curCell[0] == row && curCell[1] == col) {
                        active.add(idx);
                    }
                } else {
                    vals.add("");
                }
            }
        }
        MathView.Grid grid = new MathView.Grid("hanoi", "三柱（数字=盘大小，上小下大）", N, 3,
                Collections.unmodifiableList(vals), null, null,
                Collections.unmodifiableSet(active), Collections.unmodifiableSet(done));
        return new MathView("汉诺塔", List.of(grid));
    }

    private Trace.TraceMeta meta() {
        return new Trace.TraceMeta(id(), title(), codeResource(), "5 盘 A→C", N);
    }

    @Override
    public java.util.List<String> dataLines() {
        return java.util.List.of("int n = " + N + ";   // A → C，借助 B");
    }

    @Override
    public Complexity complexity() {
        return new Complexity("O(2ⁿ) 步数恒定 2ⁿ−1", "O(n) 递归栈",
                "传说 64 个金盘挪完世界终结——2⁶⁴−1 步 ≈ 5849 亿年（每秒 1 步）。指数增长就是'递归的复利'");
    }
}
