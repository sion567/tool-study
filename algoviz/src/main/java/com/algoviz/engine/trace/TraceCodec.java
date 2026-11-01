package com.algoviz.engine.trace;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/** Gson 序列化。v0 只做「写出 + golden 比对」；读回（离线回放）随 M2 加。 */
public final class TraceCodec {

    private static final Gson GSON = new GsonBuilder()
            .disableHtmlEscaping()
            .setPrettyPrinting()
            .create();

    private TraceCodec() {
    }

    public static String toJson(Trace trace) {
        return GSON.toJson(trace);
    }
}
