package com.algoviz.ui;

import com.algoviz.engine.AlgorithmRegistry;
import com.algoviz.engine.Step;
import com.algoviz.engine.trace.Trace;
import javafx.application.Platform;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 渲染烟测：把注册表里每个演示的首/中/末三帧真实渲染一遍（FX 线程），任何异常即失败。
 * 背景：纯 JVM 的 golden 测试只锁 trace 数据，BarRenderer 的 Integer 拆箱 NPE 在
 * 478 个测试全绿的情况下让 8 个演示画布残缺——渲染层的回归必须靠真渲染来抓。
 */
class RendererSmokeTest {

    @BeforeAll
    static void startFx() throws Exception {
        CountDownLatch ready = new CountDownLatch(1);
        Platform.startup(ready::countDown);
        assertTrue(ready.await(30, TimeUnit.SECONDS), "JavaFX toolkit 启动超时");
    }

    @AfterAll
    static void stopFx() {
        Platform.runLater(Platform::exit);
    }

    @Test
    void everyDemoRendersWithoutThrowing() throws Exception {
        AtomicReference<Throwable> failure = new AtomicReference<>();
        CountDownLatch done = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                javafx.scene.canvas.Canvas canvas = new javafx.scene.canvas.Canvas(900, 520);
                for (AlgorithmRegistry.Entry entry : AlgorithmRegistry.all()) {
                    try {
                        renderThreeFrames(canvas, entry.algorithm());
                    } catch (Throwable t) {
                        failure.compareAndSet(null, new RuntimeException(
                                entry.algorithm().id() + " 渲染抛异常: " + t, t));
                        return;
                    }
                }
            } finally {
                done.countDown();
            }
        });
        assertTrue(done.await(120, TimeUnit.SECONDS), "渲染烟测超时");
        if (failure.get() != null) {
            throw new AssertionError(failure.get());
        }
    }

    private static void renderThreeFrames(javafx.scene.canvas.Canvas canvas, com.algoviz.engine.Algorithm alg) {
        Trace trace = alg.produce();
        int n = trace.steps().size();
        int[] picks = {0, n / 2, n - 1};
        for (int idx : picks) {
            Step cur = trace.steps().get(idx);
            Step prev = idx > 0 ? trace.steps().get(idx - 1) : null;
            CanvasDispatch.paint(canvas, prev, cur, java.util.Set.of());
            // 命中检测同样不能抛（预测模式点选路径）
            CanvasDispatch.pick(canvas, cur, canvas.getWidth() / 2, canvas.getHeight() / 2);
        }
    }

    /** 复杂度图冒烟：按演示高亮重载（家族灰线+⭐加粗+n 标记）对三类曲线各画一次不抛异常。 */
    @Test
    void complexityPanelHighlightDraws() throws Exception {
        AtomicReference<Throwable> failure = new AtomicReference<>();
        CountDownLatch done = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                javafx.scene.canvas.Canvas canvas = new javafx.scene.canvas.Canvas(900, 520);
                ComplexityPanel.draw(canvas, 50, java.util.List.of("nsq"), 7, "O(n²)");                       // 多项式族
                ComplexityPanel.draw(canvas, 50, java.util.List.of("constant"), 64, "O(16) 轮 × O(1) 查表轮函数");   // 常数族（n=64 出界不画标记）
                ComplexityPanel.draw(canvas, 50, java.util.List.of("log", "nlogn"), 15, "O(n log n) 平均 / O(n²) 最坏"); // 双高亮
                ComplexityPanel.draw(canvas, 50);   // 无高亮旧签名
                ComplexityPanel.drawEmpirical(canvas, 50);
            } finally {
                done.countDown();
            }
        });
        assertTrue(done.await(30, TimeUnit.SECONDS), "复杂度图烟测超时");
        if (failure.get() != null) {
            throw new AssertionError(failure.get());
        }
    }

    private static void assertTrue(boolean cond, String msg) {
        org.junit.jupiter.api.Assertions.assertTrue(cond, msg);
    }

    /** 课件渲染烟测：AST 遍历（commonmark）装载代表性课件——表格/引用块/新演示三类各一，任何异常即失败。 */
    @Test
    void markdownViewsLoadWithoutThrowing() throws Exception {
        AtomicReference<Throwable> failure = new AtomicReference<>();
        CountDownLatch done = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                MarkdownView view = new MarkdownView();
                for (String doc : new String[]{"/markdown/aes.md", "/markdown/two-pointers.md", "/markdown/log.md"}) {
                    try {
                        view.loadResource(doc);
                    } catch (Throwable t) {
                        failure.compareAndSet(null, new RuntimeException(doc + " 渲染抛异常: " + t, t));
                        return;
                    }
                }
                view.applyTheme();   // 主题重渲染路径
            } finally {
                done.countDown();
            }
        });
        assertTrue(done.await(30, TimeUnit.SECONDS), "课件渲染烟测超时");
        if (failure.get() != null) {
            throw new AssertionError(failure.get());
        }
    }
}
