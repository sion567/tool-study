public class FacadeDemo {
    static class Engine { void start() { System.out.println("引擎启动"); } }
    static class Panels { void refresh() { System.out.println("面板刷新"); } }
    static class Player { void play() { System.out.println("开始播放"); } }
    static class AppFacade {                            // 唯一的门牌
        private final Engine engine = new Engine();
        private final Panels panels = new Panels();
        private final Player player = new Player();
        void open() {                                   // 一个调用 = 一串编排
            engine.start(); panels.refresh(); player.play();
        }
    }
    public static void main(String[] args) {
        new AppFacade().open();                         // client 只认识 Facade
    }
}
