public class DecoratorDemo {
    // ===== 组件接口 =====
    interface Frame { void draw(); }                       // 反向引用：渲染层的 CanvasRenderer
    // ===== 原始组件 =====
    static class PlainFrame implements Frame {
        public void draw() { System.out.println("画柱状图"); }
    }
    // ===== 装饰器基类：持有同接口的引用 =====
    static abstract class FrameDecorator implements Frame {
        protected final Frame inner;                        // ← 包一层
        FrameDecorator(Frame inner) { this.inner = inner; }
    }
    static class BorderFrame extends FrameDecorator {
        BorderFrame(Frame inner) { super(inner); }
        public void draw() { System.out.print("画边框 → "); inner.draw(); }
    }
    static class GridFrame extends FrameDecorator {
        GridFrame(Frame inner) { super(inner); }
        public void draw() { System.out.print("画网格 → "); inner.draw(); }
    }
    // ===== 调用方：层层包，调用入口不变 =====
    public static void main(String[] args) {
        Frame f = new BorderFrame(new GridFrame(new PlainFrame()));
        f.draw();                                          // 边框 → 网格 → 柱状图
    }
}
