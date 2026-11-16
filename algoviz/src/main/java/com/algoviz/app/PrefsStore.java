package com.algoviz.app;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 用户偏好持久化：主题 / 背景网格 / 画布缩放拖拽视图。
 * 文件位置：algoviz-prefs.json（工作目录下，与 algoviz-notes.json 同居），手写极简 JSON 读写。
 * 启动时 restore()，变化处调 saveXxx 即时写盘（文件 <200B，写盘成本可忽略）。
 */
public final class PrefsStore {

    private static final Path FILE = Path.of("algoviz-prefs.json");

    private static boolean dark;
    private static boolean showGrid = true;
    private static double viewZoom = 1, viewPanX, viewPanY;
    private static boolean loaded;

    private PrefsStore() {
    }

    public static boolean dark() {
        load();
        return dark;
    }

    public static boolean showGrid() {
        load();
        return showGrid;
    }

    public static double viewZoom() {
        load();
        return viewZoom;
    }

    public static double viewPanX() {
        load();
        return viewPanX;
    }

    public static double viewPanY() {
        load();
        return viewPanY;
    }

    /** 任意字段变化后调用：全量写盘（四个字段一起写，免多入口维护）。 */
    public static void save(boolean d, boolean grid, double zoom, double panX, double panY) {
        load();
        dark = d;
        showGrid = grid;
        viewZoom = zoom;
        viewPanX = panX;
        viewPanY = panY;
        write();
    }

    private static void load() {
        if (loaded) {
            return;
        }
        loaded = true;
        if (!Files.exists(FILE)) {
            return;
        }
        try {
            String json = Files.readString(FILE, StandardCharsets.UTF_8);
            dark = json.contains("\"dark\":true");
            showGrid = !json.contains("\"showGrid\":false");   // 缺字段 = 默认显示
            viewZoom = num(json, "viewZoom", 1);
            viewPanX = num(json, "viewPanX", 0);
            viewPanY = num(json, "viewPanY", 0);
            viewZoom = Math.max(0.4, Math.min(6, viewZoom));
        } catch (IOException e) {
            System.err.println("偏好加载失败（忽略，用默认）: " + e.getMessage());
        }
    }

    private static double num(String json, String key, double def) {
        var m = java.util.regex.Pattern.compile("\"" + key + "\":(-?[0-9.]+)").matcher(json);
        return m.find() ? Double.parseDouble(m.group(1)) : def;
    }

    private static void write() {
        try {
            Files.writeString(FILE, String.format(
                    "{\"dark\":%b,\"showGrid\":%b,\"viewZoom\":%.4f,\"viewPanX\":%.2f,\"viewPanY\":%.2f}",
                    dark, showGrid, viewZoom, viewPanX, viewPanY), StandardCharsets.UTF_8);
        } catch (IOException e) {
            System.err.println("偏好保存失败: " + e.getMessage());
        }
    }
}
