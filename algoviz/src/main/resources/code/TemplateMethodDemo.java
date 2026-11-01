public class TemplateMethodDemo {
    // ===== 抽象骨架 —— 流程定死在父类，子类只允许填钩子 =====
    abstract static class AlgorithmRunner {
        public final void run() {          // ← final：顺序锁死，子类碰不了
            load();
            compute();                     // 钩子：变化点
            render();
        }
        void load() { System.out.println("加载输入"); }
        abstract void compute();           // 只留这一步给子类
        void render() { System.out.println("渲染帧"); }
    }
    // ===== 子类：只填变化点 =====
    static class BubbleSortRunner extends AlgorithmRunner {
        void compute() { System.out.println("冒泡：逐帧吐 Step"); }
    }
    // ===== 调用方 =====
    public static void main(String[] args) {
        AlgorithmRunner runner = new BubbleSortRunner();
        runner.run();                      // 顺序永远 load → compute → render
    }
}
