package com.algoviz.ui;

import javafx.scene.paint.Color;

/**
 * 画布绘制调色板（tailwind 式色彩名，一色一常量）。
 * 收编 11 个 renderer 里散落的 Color.web("#...") 字面量（原 186 处）——
 * 改主题色只动这里；Color.web 每次调用都新建对象，常量引用顺带省分配。
 * 命名按色相（GRAY/AMBER/BLUE…），javadoc 注典型用途；语义层（"比较色""交换色"）由调用处注释表达。
 * 画布绘制配色两态通用（暗/亮主题共用同一组），控件样式见 Theme。
 */
public final class Palette {

    // ---- 灰阶：文字与结构线 ----
    /** 近黑：标题/强文字 */
    public static final Color INK_STRONG = Color.web("#111827");
    /** 深灰：正文 */
    public static final Color INK = Color.web("#374151");
    /** 中灰：次要文字/轴标签 */
    public static final Color MUTED = Color.web("#6b7280");
    /** 灰：刻度/浅注 */
    public static final Color FAINT = Color.web("#9ca3af");
    /** 蓝灰：坐标轴 */
    public static final Color AXIS = Color.web("#94a3b8");
    /** 蓝灰深：图表系列 */
    public static final Color SLATE = Color.web("#334155");
    /** 蓝灰浅：次级系列 */
    public static final Color SLATE_LIGHT = Color.web("#64748b");
    /** 浅灰：网格线 */
    public static final Color GRID = Color.web("#d1d5db");
    /** 更浅灰：网格柔线 */
    public static final Color GRID_SOFT = Color.web("#e5e7eb");
    /** 极浅灰：面板底 */
    public static final Color MIST = Color.web("#e2e8f0");
    /** 浅蓝灰：边框 */
    public static final Color BORDER = Color.web("#cbd5e1");
    /** 冷白：卡片底 */
    public static final Color CARD = Color.web("#eef2f7");
    /** 近冷白：表头底 */
    public static final Color CARD_ALT = Color.web("#e8eefc");
    /** 纯白：斑马纹亮行 */
    public static final Color WHITE = Color.web("#ffffff");
    /** 半透明白（33%）：B+树键高亮描边（ARGB 字面量直通） */
    public static final Color WHITE_GHOST = Color.web("#ffffff55");
    /** 半透明白（20%）：B+树普通描边（ARGB 字面量直通） */
    public static final Color WHITE_WISP = Color.web("#ffffff33");

    // ---- 高亮色系：状态与强调 ----
    /** 琥珀：比较/指针 */
    public static final Color AMBER = Color.web("#f59e0b");
    /** 琥珀浅：比较底色 */
    public static final Color AMBER_SOFT = Color.web("#fde68a");
    /** 黄：当前活动 */
    public static final Color YELLOW = Color.web("#eab308");
    /** 黄浅：黄底标记 */
    public static final Color YELLOW_SOFT = Color.web("#fde047");
    /** 蓝：主色/链路 */
    public static final Color BLUE = Color.web("#3b82f6");
    /** 蓝深：选中/强调蓝 */
    public static final Color BLUE_DEEP = Color.web("#1d4ed8");
    /** 蓝极深：表头强调 */
    public static final Color BLUE_INK = Color.web("#1e3a8a");
    /** 蓝 600：链接 */
    public static final Color BLUE_MID = Color.web("#2563eb");
    /** 蓝极浅：蓝底 */
    public static final Color BLUE_SOFT = Color.web("#dbeafe");
    /** 天蓝：信息标记 */
    public static final Color SKY = Color.web("#0ea5e9");
    /** 橙：写入 */
    public static final Color ORANGE = Color.web("#ea580c");
    /** 亮橙：粒子/轨迹 */
    public static final Color ORANGE_BRIGHT = Color.web("#f97316");
    /** 棕橙：警示文字 */
    public static final Color RUST = Color.web("#b45309");
    /** 紫：辅助系列 */
    public static final Color VIOLET = Color.web("#8b5cf6");
    /** 紫深：次级强调 */
    public static final Color VIOLET_DEEP = Color.web("#9333ea");
    /** 粉：特殊标记 */
    public static final Color PINK = Color.web("#ec4899");
    /** 玫红：错误/危险 */
    public static final Color ROSE = Color.web("#e11d48");

    // ---- 绿色系：成功/完成 ----
    /** 绿：成功/完成 */
    public static final Color GREEN = Color.web("#22c55e");
    /** 绿深：确认 */
    public static final Color GREEN_DEEP = Color.web("#16a34a");
    /** 墨绿：排序完成 */
    public static final Color GREEN_INK = Color.web("#15803d");
    /** 极深绿：已定稿 */
    public static final Color GREEN_DARK = Color.web("#166534");
    /** 深绿蓝：青系系列 */
    public static final Color TEAL = Color.web("#0891b2");
    /** 深青：已知解 */
    public static final Color TEAL_DEEP = Color.web("#059669");
    /** 绿极浅：绿底 */
    public static final Color GREEN_SOFT = Color.web("#dcfce7");
    /** 红：交换/删除 */
    public static final Color RED = Color.web("#ef4444");
    /** 酒红：悬崖曲线 */
    public static final Color RED_DEEP = Color.web("#7f1d1d");

    private Palette() {
    }
}
