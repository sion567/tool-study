package com.algoviz;

import javafx.application.Application;

/** 给 jpackage / java -jar 用的启动器（绕开 JavaFX 模块检查，M6 打包时用）。 */
public class Main {

    public static void main(String[] args) {
        Application.launch(MainApp.class, args);
    }
}
