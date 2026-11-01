package com.algoviz.engine;

/**
 * 值的三态（蓝图 §1.2 第 4 张图）：基本类型存值，对象存引用 id，null 存空。
 * 正好对应脑内底板：栈格子里要么是值，要么是地址。
 */
public record Value(Kind kind, String text, String refId) {

    public enum Kind { PRIM, REF, NULL }

    public static final Value NULL = new Value(Kind.NULL, "null", null);

    public static Value prim(Object v) {
        return new Value(Kind.PRIM, String.valueOf(v), null);
    }

    public static Value ref(String id) {
        return new Value(Kind.REF, id, id);
    }

    /** 显示用：基本类型显示值，引用显示 @id。 */
    public String display() {
        return switch (kind) {
            case PRIM -> text;
            case REF -> refId;
            case NULL -> "null";
        };
    }
}
