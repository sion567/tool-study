public class StateDemo {
    interface State { void play(Player ctx); }          // 行为随状态变
    static class Playing implements State {
        public void play(Player ctx) { ctx.setState(new Paused()); }  // 播放中→暂停
    }
    static class Paused implements State {
        public void play(Player ctx) { ctx.setState(new Playing()); } // 暂停→播放
    }
    static class Player {                               // 反向引用：播放器的 ▶/⏸ 按钮
        private State state = new Paused();
        void setState(State s) { state = s; }
        void pressPlay() { state.play(this); }          // 同一个按钮，行为随状态变
    }
    public static void main(String[] args) {
        Player player = new Player();
        player.pressPlay();                              // 暂停中按 → Playing
        player.pressPlay();                              // 播放中按 → Paused
        player.pressPlay();                              // 又回到 Paused
    }
}
