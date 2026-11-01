package com.algoviz.engine;

/**
 * 代码高亮区间：多行语句、条件命中都要能标（蓝图 §2.1）。
 * 行号对齐各算法 codeResource() 指向的源码文件。
 */
public record Loc(int startLine, int endLine, String note) {

    public static Loc line(int line, String note) {
        return new Loc(line, line, note);
    }
}
