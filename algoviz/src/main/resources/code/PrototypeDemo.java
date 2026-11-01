public class PrototypeDemo {
    static class Sheet implements Cloneable {           // 反向引用：Java 的 clone() 协议
        String owner; java.util.List<String> cells = new java.util.ArrayList<>();
        public String toString() { return "Sheet{" + owner + "," + cells + "}"; }
        protected Sheet clone() {
            Sheet copy = new Sheet();                   // 按模板复制
            copy.owner = owner;
            copy.cells = new java.util.ArrayList<>(cells);
            return copy;
        }
    }
    public static void main(String[] args) {
        Sheet tpl = new Sheet();
        tpl.owner = "模板"; tpl.cells.add("A1");
        Sheet copy = tpl.clone();                        // 从原型复制，不从头 new
        copy.owner = "副本"; copy.cells.set(0, "改过");   // 改副本
        System.out.println(tpl + "  |  " + copy);        // 模板不受影响
    }
}
