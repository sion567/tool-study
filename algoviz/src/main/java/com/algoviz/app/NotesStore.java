package com.algoviz.app;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * JSON 笔记持久化：预测得分 + 默写记录，按算法 id 存取。
 * 文件位置：algoviz-notes.json（工作目录下），Gson 读写。
 */
public final class NotesStore {

    private static final Path FILE = Path.of("algoviz-notes.json");
    private static Map<String, NoteEntry> cache;

    public static class NoteEntry {
        public int predictionAsked;
        public int predictionCorrect;
        public int predictionStreak;
        public String lastDictation = "";
        public String lastDictationDiff = "";
        public String behaviorVerdict = "";
    }

    public static NoteEntry get(String algorithmId) {
        load();
        return cache.computeIfAbsent(algorithmId, k -> new NoteEntry());
    }

    public static void save(String algorithmId, NoteEntry entry) {
        load();
        cache.put(algorithmId, entry);
        write();
    }

    private static void load() {
        if (cache != null) return;
        cache = new LinkedHashMap<>();
        if (!java.nio.file.Files.exists(FILE)) return;
        try {
            String json = Files.readString(FILE, StandardCharsets.UTF_8);
            var gson = new com.google.gson.Gson();
            var type = new com.google.gson.reflect.TypeToken<Map<String, NoteEntry>>() {}.getType();
            Map<String, NoteEntry> loaded = gson.fromJson(json, type);
            if (loaded != null) cache.putAll(loaded);
        } catch (IOException | com.google.gson.JsonSyntaxException e) {
            System.err.println("笔记加载失败（忽略，从空开始）: " + e.getMessage());
        }
    }

    private static void write() {
        try {
            var gson = new com.google.gson.GsonBuilder().setPrettyPrinting().create();
            Files.writeString(FILE, gson.toJson(cache), StandardCharsets.UTF_8);
        } catch (IOException e) {
            System.err.println("笔记保存失败: " + e.getMessage());
        }
    }
}
