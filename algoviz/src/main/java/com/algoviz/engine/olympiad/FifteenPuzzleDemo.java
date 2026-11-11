package com.algoviz.engine.olympiad;

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

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * 不变量·15 谜题可解性：4×4 滑块（15 牌 + 1 空格）——哪些局面能滑回 1..15？
 * 判据（宽为偶数）：逆序数 + 空格所在行（自底 1 起）为奇 ⇔ 可解。
 * 两幕：①合法打乱 → 数逆序 → 判可解 ②对换两枚牌（合法移动做不到的操作）→ 奇偶翻转 → 永无解。
 * 不变量思想的代表作：合法移动永远带着「奇偶指纹」——1879 年全美寻找解法的狂热被一行数学终结。
 * 画面（math.matrix 复用）：4×4 棋盘 + 1×15 扁平行（数逆序用）双网格同屏。
 */
public final class FifteenPuzzleDemo implements Algorithm {

    private static final String SCHEMA = "1";
    private static final String INVARIANT =
            "横移不改变逆序数；竖移跳过 3 个中间格 → 逆序数变化为偶——「逆序数+空格行」的奇偶是合法移动的不变量；"
                    + "对换两枚牌把奇偶翻转 → 那些局面无论怎么滑都到不了";

    private static final int N = 4;
    private static final int SCRAMBLE_MOVES = 60;

    private final long seed;

    private final List<Step> steps = new ArrayList<>();
    private final int[] board = new int[N * N];       // 0 = 空格
    private final List<String> flatRow = new ArrayList<>();
    private int inversions, blankRowFromBottom;
    private boolean solvable;

    public FifteenPuzzleDemo() {
        this(7);
    }

    /** 变体：换打乱种子（两幕局面随之改变，判定照旧）。 */
    public FifteenPuzzleDemo(long seed) {
        this.seed = seed;
    }

    @Override public String id() { return "olympiad.fifteen"; }
    @Override public String title() { return "不变量·15谜题可解性"; }
    @Override public String codeResource() { return "/code/FifteenPuzzleDemo.java"; }
    @Override public String docResource() { return "/markdown/fifteen.md"; }

    @Override
    public Trace produce() {
        steps.clear();
        scramble();
        setFlat();   // 先拉平：合约要求扁平行全程 15 格
        emit(7, List.of(VizEvent.of(VizEvent.EventType.INIT,
                "局面 A：合法滑动打乱而来（60 步）——它能滑回 1..15 吗？先数逆序数")),
                "1879 年「15 谜题风暴」：全美疯玩一局换两牌的赝品——没人滑得回去，数学最终出庭作证");
        setFlat();
        inversions = 0;
        for (int i = 0; i < flatRow.size() - 1; i++) {
            int mine = Integer.parseInt(flatRow.get(i));
            int smaller = 0;
            for (int j = i + 1; j < flatRow.size(); j++) {
                if (Integer.parseInt(flatRow.get(j)) < mine) {
                    smaller++;
                }
            }
            inversions += smaller;
            emit(12, List.of(VizEvent.of(VizEvent.EventType.COMPARE,
                    String.format("以 %d 为基准：后面比它小的有 %d 个 → 逆序数累计 %d", mine, smaller, inversions))),
                    i % 2 == 0 ? "逐个数「后面的更小对」——O(n²) 但看得见，代价换透明"
                            : "数到一半了：逆序数就是「打乱程度」的精确度量");
        }
        blankRowFromBottom = N - (blankPos() / N);
        solvable = (inversions + blankRowFromBottom) % 2 == 1;
        emit(13, List.of(VizEvent.of(VizEvent.EventType.MARK_SORTED,
                String.format("局面 A：逆序数 %d（偶/奇判定），空格在第 %d 行（自底）→ 和为 %d → %s",
                        inversions, blankRowFromBottom, inversions + blankRowFromBottom,
                        solvable ? "奇 → 可解！" : "偶 → 无解"))),
                "判据：宽为偶时「逆序数+空格行」必须为奇——这就是从初始到终点全部合法移动的指纹和");
        // ---- 第二幕：对换两枚牌（合法移动做不到的操作）----
        int p1 = firstNonBlank(), p2 = nextNonBlank(p1);
        int tmp = board[p1];
        board[p1] = board[p2];
        board[p2] = tmp;
        int before = inversions;
        inversions = countInversions();
        boolean swapSolvable = (inversions + blankRowFromBottom) % 2 == 1;
        solvable = swapSolvable;   // 堆里「判定」字段同步翻转为第二幕结论
        setFlat();
        emit(15, List.of(VizEvent.of(VizEvent.EventType.SWAP,
                String.format("局面 B：对换两枚牌（%d 与 %d）——注意：任何合法滑动都做不到这件事！",
                        tmp, board[p1]))),
                "对换 = 奇偶粉碎机：所有合法移动都保奇偶，唯独这一步在指纹之外");
        emit(16, List.of(VizEvent.of(VizEvent.EventType.MARK_SORTED,
                String.format("局面 B：逆序数 %d → %d（%s），奇偶翻转 → %s——无论怎么滑都回不去",
                        before, inversions, (before % 2) == (inversions % 2) ? "未翻转？" : "已翻转",
                        swapSolvable ? "可解" : "无解"))),
                "数学判决：赝品局的「无解」不是你手笨，是奇偶不变量把门焊死了");
        emit(19, List.of(VizEvent.of(VizEvent.EventType.RETURN,
                "不变量方法论：找一个「所有操作都改不动」的量——局面分成两个永不相通的世界")),
                "同样的思想判死了五色地图的六色情形、判活了棋盘残局——不变量是组合数学的指纹鉴定科");
        return new Trace(SCHEMA, meta(), steps);
    }

    /** 从还原态出发做合法滑动打乱（合法移动保奇偶 → 打乱必可解）。 */
    private void scramble() {
        for (int i = 0; i < N * N - 1; i++) {
            board[i] = i + 1;
        }
        board[N * N - 1] = 0;
        Random r = new Random(seed);
        int blank = N * N - 1;
        for (int i = 0; i < SCRAMBLE_MOVES; i++) {
            List<Integer> options = new ArrayList<>();
            int row = blank / N, col = blank % N;
            if (row > 0) {
                options.add(blank - N);
            }
            if (row < N - 1) {
                options.add(blank + N);
            }
            if (col > 0) {
                options.add(blank - 1);
            }
            if (col < N - 1) {
                options.add(blank + 1);
            }
            int pick = options.get(r.nextInt(options.size()));
            board[blank] = board[pick];
            board[pick] = 0;
            blank = pick;
        }
    }

    private int blankPos() {
        for (int i = 0; i < N * N; i++) {
            if (board[i] == 0) {
                return i;
            }
        }
        return N * N - 1;
    }

    private void setFlat() {
        flatRow.clear();
        for (int v : board) {
            if (v != 0) {
                flatRow.add(String.valueOf(v));
            }
        }
    }

    private int countInversions() {
        setFlat();
        int count = 0;
        for (int i = 0; i < flatRow.size() - 1; i++) {
            int mine = Integer.parseInt(flatRow.get(i));
            for (int j = i + 1; j < flatRow.size(); j++) {
                if (Integer.parseInt(flatRow.get(j)) < mine) {
                    count++;
                }
            }
        }
        return count;
    }

    private int firstNonBlank() {
        for (int i = 0; i < N * N; i++) {
            if (board[i] != 0) {
                return i;
            }
        }
        return 0;
    }

    private int nextNonBlank(int from) {
        for (int i = from + 1; i < N * N; i++) {
            if (board[i] != 0) {
                return i;
            }
        }
        return from;
    }

    private void emit(int line, List<VizEvent> events, String why) {
        StackFrame fr = new StackFrame("fifteen",
                OrderedMaps.of("逆序数", Value.prim(String.valueOf(inversions)),
                        "空格行(自底)", Value.prim(String.valueOf(blankRowFromBottom)),
                        "判定", Value.prim(solvable ? "可解" : "无解")), null);
        Map<String, Layer> layers = new LinkedHashMap<>();
        layers.put("mem.stack", new StackLayer(List.of(fr)));
        layers.put("mem.heap", new HeapLayer(snapshotHeap()));
        layers.put("math.matrix", snapshotMath());
        steps.add(new Step(steps.size(), Loc.line(line, why),
                Collections.unmodifiableMap(layers), List.copyOf(events), new Intent(why, INVARIANT)));
    }

    private Heap snapshotHeap() {
        Map<String, Value> fields = new LinkedHashMap<>();
        fields.put("逆序数", Value.prim(String.valueOf(inversions)));
        fields.put("奇偶指纹", Value.prim((inversions + blankRowFromBottom) % 2 == 1 ? "奇（可解世界）" : "偶（无解世界）"));
        fields.put("判据", Value.prim("逆序数+空格行 为奇 ⇔ 可解"));
        Map<String, HeapObject> objs = new LinkedHashMap<>();
        objs.put("@1", new HeapObject("@1", "fifteen", "ParityState",
                Collections.unmodifiableMap(fields), List.of()));
        return new Heap(Collections.unmodifiableMap(objs));
    }

    private MathView snapshotMath() {
        List<String> values = new ArrayList<>();
        for (int v : board) {
            values.add(v == 0 ? "" : String.valueOf(v));
        }
        Set<Integer> active = new LinkedHashSet<>();
        MathView.Grid boardGrid = new MathView.Grid("board", "4×4 滑块局面（空格=空白）",
                N, N, values, null, null, active, Set.of());
        MathView.Grid flatGrid = new MathView.Grid("flat", "扁平行（忽略空格，数逆序用）",
                1, 15, List.copyOf(flatRow), null, null, Set.of(), Set.of());
        return new MathView("15 谜题奇偶判决", List.of(boardGrid, flatGrid));
    }

    private Trace.TraceMeta meta() {
        return new Trace.TraceMeta(id(), title(), codeResource(),
                "打乱种子 " + seed, 26);
    }

    @Override
    public java.util.List<String> dataLines() {
        return java.util.List.of(String.format("java.util.Random rnd = new java.util.Random(%d);   //@DATA 打乱种子", seed));
    }

    @Override
    public Complexity complexity() {
        return new Complexity("O(n²) 数逆序", "O(n) 棋盘",
                "魔方「拧不回」的判定、扑克牌洗牌可达性、图灵机的不可达配置——"
                        + "把状态空间劈成两个世界的不是暴力搜索，是一个改不动的数");
    }
}
