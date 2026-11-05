package com.algoviz.app;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.FileObject;
import javax.tools.ForwardingJavaFileManager;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.ToolProvider;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.SecureClassLoader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 行为级判卷的引擎：把白纸上的 Java 代码**内存编译 + 运行**，抓标准输出。
 * 编译用 JDK 自带 javax.tools（jpackage 打包的 runtime 含 jdk.compiler 模块）；
 * 运行在守护线程上，超时 interrupt——死循环代码不拖死应用。
 * 只干三件事：编译（带回错误行号）、运行（抓 stdout）、超时兜底。
 */
public final class JavaRunner {

    /** 一次编译+运行的结果。compiled=false 时 compileErrors 有内容；运行异常时 error 有内容。 */
    public record RunResult(boolean compiled, String compileErrors, String stdout, String error, long ms) {

        public boolean ok() {
            return compiled && error == null;
        }
    }

    private JavaRunner() {
    }

    /** 环境是否带编译器（缺 jdk.compiler 模块时行为判卷置灰）。 */
    public static boolean available() {
        return ToolProvider.getSystemJavaCompiler() != null;
    }

    /**
     * 编译并运行。className 必须与 source 里的 public 类名一致。
     * 运行 = 调 main(String[])，stdout 全量捕获（UTF-8）。
     */
    public static RunResult compileAndRun(String className, String source, long timeoutMs) {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            return new RunResult(false, "当前运行环境缺少 JDK 编译器（jdk.compiler 模块）", "", null, 0);
        }
        long t0 = System.currentTimeMillis();
        DiagnosticCollector<JavaFileObject> diags = new DiagnosticCollector<>();
        MemoryFileManager fm = new MemoryFileManager(
                compiler.getStandardFileManager(diags, Locale.CHINA, StandardCharsets.UTF_8));
        JavaFileObject src = new MemSource(className, source);
        JavaCompiler.CompilationTask task = compiler.getTask(null, fm, diags,
                List.of("-encoding", "UTF-8"), null, List.of(src));
        if (!Boolean.TRUE.equals(task.call())) {
            StringBuilder sb = new StringBuilder();
            for (Diagnostic<? extends JavaFileObject> d : diags.getDiagnostics()) {
                if (d.getKind() == Diagnostic.Kind.ERROR) {
                    sb.append("第 ").append(d.getLineNumber()).append(" 行：")
                            .append(d.getMessage(Locale.CHINA)).append("\n");
                }
            }
            return new RunResult(false, sb.isEmpty() ? "编译失败（无详细诊断）" : sb.toString().strip(), "", null,
                    System.currentTimeMillis() - t0);
        }

        // 运行：守护线程 + stdout 捕获 + 超时 interrupt
        ClassLoader parent = JavaRunner.class.getClassLoader();
        MemoryClassLoader loader = new MemoryClassLoader(fm.classes, parent);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        List<Throwable> err = new ArrayList<>();
        Thread runner = new Thread(() -> {
            java.io.PrintStream old = System.out;
            System.setOut(new java.io.PrintStream(out, true, StandardCharsets.UTF_8));
            try {
                Class<?> main = loader.loadClass(className);
                Method m = main.getMethod("main", String[].class);
                m.setAccessible(true);
                m.invoke(null, (Object) new String[0]);
            } catch (Throwable e) {
                Throwable cause = e instanceof java.lang.reflect.InvocationTargetException ? e.getCause() : e;
                err.add(cause);
            } finally {
                System.setOut(old);
            }
        }, "dictation-run-" + className);
        runner.setDaemon(true);
        runner.start();
        try {
            runner.join(timeoutMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        long ms = System.currentTimeMillis() - t0;
        if (runner.isAlive()) {
            runner.interrupt();
            return new RunResult(true, out.toString(StandardCharsets.UTF_8), out.toString(StandardCharsets.UTF_8),
                    "超时（>" + timeoutMs + "ms）——可能死循环", ms);
        }
        String stdout = out.toString(StandardCharsets.UTF_8);
        String error = err.isEmpty() ? null : err.get(0).toString();
        return new RunResult(true, "", stdout, error, ms);
    }

    /** 白纸代码适配：含 class 声明就按原样编译；否则整段包进 main。 */
    public static String normalize(String code) {
        String c = code.strip();
        if (c.matches("(?s).*\\bclass\\s+\\w+.*")) {
            return c;
        }
        return "public class WhitePaper {\n    public static void main(String[] args) throws Exception {\n"
                + indent(c) + "\n    }\n}\n";
    }

    /** 取 source 里的类名（public 类须与文件名一致）。缺省 WhitePaper。 */
    public static String classNameOf(String source) {
        var m = java.util.regex.Pattern.compile("\\bclass\\s+(\\w+)").matcher(source);
        return m.find() ? m.group(1) : "WhitePaper";
    }

    private static String indent(String c) {
        StringBuilder sb = new StringBuilder();
        for (String line : c.split("\n", -1)) {
            sb.append("        ").append(line.stripLeading()).append("\n");
        }
        return sb.toString();
    }

    // ---------- 内存编译基建 ----------

    private static final class MemSource extends SimpleJavaFileObject {
        private final String code;

        MemSource(String className, String code) {
            super(URI.create("mem:///" + className + ".java"), Kind.SOURCE);
            this.code = code;
        }

        @Override
        public CharSequence getCharContent(boolean ignoreEncoding) {
            return code;
        }
    }

    private static final class MemClass extends SimpleJavaFileObject {
        private final ByteArrayOutputStream buf = new ByteArrayOutputStream();

        MemClass(String className, Kind kind) {
            super(URI.create("mem:///" + className + "." + kind.name().toLowerCase(Locale.ROOT)), kind);
        }

        @Override
        public OutputStream openOutputStream() {
            return buf;
        }

        byte[] bytes() {
            return buf.toByteArray();
        }
    }

    private static final class MemoryFileManager
            extends ForwardingJavaFileManager<javax.tools.JavaFileManager> {

        private final Map<String, MemClass> classes = new HashMap<>();

        MemoryFileManager(javax.tools.JavaFileManager delegate) {
            super(delegate);
        }

        @Override
        public JavaFileObject getJavaFileForOutput(Location location, String className,
                                                   JavaFileObject.Kind kind, FileObject sibling) {
            MemClass mc = new MemClass(className, kind);
            classes.put(className, mc);
            return mc;
        }
    }

    private static final class MemoryClassLoader extends SecureClassLoader {
        private final Map<String, MemClass> classes;

        MemoryClassLoader(Map<String, MemClass> classes, ClassLoader parent) {
            super(parent);
            this.classes = classes;
        }

        @Override
        protected Class<?> findClass(String name) throws ClassNotFoundException {
            MemClass mc = classes.get(name);
            if (mc == null) {
                throw new ClassNotFoundException(name);
            }
            byte[] b = mc.bytes();
            return defineClass(name, b, 0, b.length);
        }
    }
}
