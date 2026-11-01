public class BuilderDemo {
    static class Dialog {                               // 产品：字段多
        String title; boolean border; double opacity;
        public String toString() { return "Dialog{" + title + ",边框=" + border + ",透明=" + opacity + "}"; }
    }
    static class Builder {                              // 分步装配同一个对象
        private final Dialog d = new Dialog();
        Builder title(String t) { d.title = t; return this; }
        Builder border(boolean b) { d.border = b; return this; }
        Builder opacity(double o) { d.opacity = o; return this; }
        Dialog build() { return d; }
    }
    public static void main(String[] args) {
        Dialog dlg = new Builder()
                .title("AlgoViz")
                .border(true)
                .opacity(0.8)
                .build();                               // 四步凑齐，一次成形
        System.out.println(dlg);
    }
}
