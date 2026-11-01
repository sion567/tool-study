package com.algoviz.ui;

/** 暗色主题（M6）：开关 + 两组内联样式。画布绘制配色两态通用，不动；控件色由 -fx-base 派生。 */
public final class Theme {

    private static boolean dark;

    private Theme() {
    }

    public static boolean isDark() {
        return dark;
    }

    public static void setDark(boolean d) {
        dark = d;
    }

    public static boolean toggle() {
        dark = !dark;
        return dark;
    }

    public static String rootStyle() {
        return dark ? "-fx-base:#1e1e2e; -fx-background:#1e1e2e;" : "";
    }

    public static String canvasPaneStyle() {
        return dark
                ? "-fx-background-color:#181825; -fx-border-color:#313244;"
                : "-fx-background-color:white; -fx-border-color:#e5e7eb;";
    }

    public static String intentBarStyle() {
        return dark
                ? "-fx-background-color:#313244; -fx-padding:6 10; -fx-background-radius:4;"
                : "-fx-background-color:#f3f4f6; -fx-padding:6 10; -fx-background-radius:4;";
    }

    public static String intentTextFill() {
        return dark ? "-fx-text-fill:#cdd6f4;" : "-fx-text-fill:#374151;";
    }

    public static String codeNormal() {
        String mono = "-fx-font-family:'Consolas','Courier New',monospace; -fx-font-size:13px;";
        return dark ? "-fx-text-fill:#cdd6f4;" + mono : "-fx-text-fill:#333;" + mono;
    }

    public static String codeHighlight() {
        String mono = "-fx-font-family:'Consolas','Courier New',monospace; -fx-font-size:13px;";
        return dark
                ? "-fx-text-fill:#f9e2af; -fx-background-color:#45475a;" + mono
                : "-fx-text-fill:#111; -fx-background-color:#fff3c4;" + mono;
    }

    public static String codeComment() {
        String mono = "-fx-font-family:'Consolas','Courier New',monospace; -fx-font-size:13px;";
        return dark ? "-fx-text-fill:#7f849c;" + mono : "-fx-text-fill:#9ca3af;" + mono;
    }

    public static String cellText() {
        return dark ? "-fx-text-fill:#cdd6f4;" : "-fx-text-fill:#333;";
    }

    public static String cellTextChanged() {
        return dark ? "-fx-text-fill:#f9e2af; -fx-font-weight:bold;" : "-fx-text-fill:#b45309; -fx-font-weight:bold;";
    }

    public static String resultStyle() {
        return dark
                ? "-fx-text-fill:#cdd6f4; -fx-background-color:#313244; -fx-padding:6; -fx-background-radius:4;"
                : "-fx-text-fill:#111; -fx-background-color:#f3f4f6; -fx-padding:6; -fx-background-radius:4;";
    }

    public static String mutedText() {
        return dark ? "-fx-text-fill:#a6adc8;" : "-fx-text-fill:#6b7280;";
    }

    public static String questionText() {
        return dark ? "-fx-text-fill:#cdd6f4; -fx-font-size:13px;" : "-fx-text-fill:#111; -fx-font-size:13px;";
    }

    public static String pickedText() {
        return dark ? "-fx-text-fill:#c4b5fd; -fx-font-size:13px;" : "-fx-text-fill:#6d28d9; -fx-font-size:13px;";
    }

    public static String scoreText() {
        return dark ? "-fx-text-fill:#9399b2; -fx-font-size:12px;" : "-fx-text-fill:#6b7280; -fx-font-size:12px;";
    }
}
