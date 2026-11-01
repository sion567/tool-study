public class AdapterDemo {
    interface Target { String request(); }              // 调用方认的接口
    static class Adaptee {                              // 现成的、接口不兼容的类
        String specificRequest() { return "EUR 100"; }
    }
    static class Adapter implements Target {            // 中间翻译层
        private final Adaptee adaptee;
        Adapter(Adaptee a) { this.adaptee = a; }
        public String request() {                       // 翻译：欧 → 人民币
            return "CNY " + Integer.parseInt(adaptee.specificRequest().split(" ")[1]) * 8;
        }
    }
    public static void main(String[] args) {
        Target t = new Adapter(new Adaptee());
        System.out.println(t.request());                // 调用方全程只认 Target
    }
}
