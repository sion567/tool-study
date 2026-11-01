public class ChainDemo {
    abstract static class Handler {
        protected Handler next;                          // 链：我处理不了就传给 next
        Handler link(Handler n) { next = n; return n; }
        abstract void handle(String req);
    }
    static class FrontDesk extends Handler {
        void handle(String req) {
            if (req.equals("问路")) System.out.println("前台解决");
            else if (next != null) next.handle(req);      // 传下去
        }
    }
    static class Manager extends Handler {
        void handle(String req) {
            if (req.equals("投诉")) System.out.println("经理解决");
            else if (next != null) next.handle(req);
        }
    }
    public static void main(String[] args) {
        Handler chain = new FrontDesk(); chain.link(new Manager());
        chain.handle("投诉");                            // 请求进链，自己找终点
    }
}
