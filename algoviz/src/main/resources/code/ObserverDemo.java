public class ObserverDemo {
    // ===== 观察者接口：统一的通知形状 =====
    interface Observer {
        void update(String step);
    }
    // ===== 被观察者 —— 只维护订阅名单，广播时按订阅顺序逐个通知 =====
    static class Engine {
        private final java.util.List<Observer> observers = new java.util.ArrayList<>();
        void subscribe(Observer o) { observers.add(o); }        // ← 名单变长
        void emitStep(String step) {
            for (Observer o : observers) o.update(step);        // ← 一发，多方响应
        }
    }
    // ===== 观察者一：动画面板 =====
    static class CanvasPanel implements Observer {
        public void update(String step) { System.out.println("画布刷新"); }
    }
    // ===== 观察者二：变量面板 =====
    static class VarsPanel implements Observer {
        public void update(String step) { System.out.println("变量面板刷新"); }
    }
    // ===== 调用方：随时加订阅者，Engine 一行不改 =====
    public static void main(String[] args) {
        Engine engine = new Engine();
        engine.subscribe(new CanvasPanel());
        engine.subscribe(new VarsPanel());
        engine.emitStep("step42");
    }
}
