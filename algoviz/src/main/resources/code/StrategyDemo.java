public class StrategyDemo {
    // ===== 策略接口 —— 「怎么做」抽象成接口，与 Context 配对的另一半 =====
    interface Strategy {
        void execute();
    }
    // ===== 具体策略一 =====
    static class BubbleSort implements Strategy {
        public void execute() { System.out.println("冒泡：逐帧吐 Step"); }
    }
    // ===== 具体策略二 =====
    static class InsertionSort implements Strategy {
        public void execute() { System.out.println("插入：直接搬位"); }
    }
    // ===== 上下文 —— 持有策略，行为委托出去，调用方唯一的入口 =====
    static class Context {
        private Strategy strategy;                              // ← 本体就是这根引用
        Context(Strategy s) { this.strategy = s; }
        void setStrategy(Strategy s) { this.strategy = s; }     // ← 运行时换箭头
        void run() { strategy.execute(); }                      // ← 转发：多态分发
    }
    // ===== 调用方：只认 Context 的 run() =====
    public static void main(String[] args) {
        Context ctx = new Context(new BubbleSort());            // ① 装配：构造器注入
        ctx.run();                                              // ② 转发到冒泡
        ctx.setStrategy(new InsertionSort());                   // ③ 运行时换实现
        ctx.run();                                              // ④ 行为变了，代码没变
    }
}
