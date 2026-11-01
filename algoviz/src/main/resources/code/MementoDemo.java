public class MementoDemo {
    static class Editor {                                // 原发器
        String text = "";
        Snapshot save() { return new Snapshot(text); }   // 存档
        void restore(Snapshot s) { text = s.text; }      // 读档
    }
    record Snapshot(String text) { }                     // 备忘录：不可变快照
    public static void main(String[] args) {
        Editor ed = new Editor();
        Snapshot save1 = ed.save();                      // 存档：text = ""
        ed.text = "画到一半";                             // 继续编辑
        ed.restore(save1);                               // 读档：一键回到过去
        System.out.println(ed.text);                     // ""
    }
}
