public class MediatorDemo {
    static class ChatMediator {                          // 唯一知道所有人的
        private final java.util.List<String> members = new java.util.ArrayList<>();
        void join(String name) { members.add(name); }
        void send(String from, String msg) {
            System.out.println("[群] " + from + " 说: " + msg);
        }
    }
    static class Colleague {
        private final ChatMediator mediator; private final String name;
        Colleague(ChatMediator m, String n) { mediator = m; name = n; }
        void send(String msg) { mediator.send(name, msg); }   // 只认识中介
    }
    public static void main(String[] args) {
        ChatMediator m = new ChatMediator();
        Colleague a = new Colleague(m, "画布"); Colleague b = new Colleague(m, "变量面板");
        a.send("我刷新了");                                // A 不认识 B，照样通信
    }
}
