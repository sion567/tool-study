public class CommandDemo {
    interface Command { void execute(); }              // 请求 = 对象
    static class AddStepCommand implements Command {
        private final String step;                      // 请求的参数打包进对象
        AddStepCommand(String step) { this.step = step; }
        public void execute() { System.out.println("播放 " + step); }
    }
    static class Invoker {                              // 只认 Command，不知道内容
        private final java.util.List<Command> queue = new java.util.ArrayList<>();
        void submit(Command c) { queue.add(c); }
        void flush() { for (Command c : queue) c.execute(); }   // ← 排队逐个执行
    }
    public static void main(String[] args) {
        Invoker invoker = new Invoker();
        invoker.submit(new AddStepCommand("step1"));     // 请求入队
        invoker.submit(new AddStepCommand("step2"));
        invoker.flush();                                 // 统一执行
    }
}
