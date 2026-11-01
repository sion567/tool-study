public class IteratorDemo {
    public static void main(String[] args) {
        var steps = java.util.List.of("step1", "step2", "step3");
        var it = steps.iterator();                       // 反向引用：播放器遍历 Trace.steps
        while (it.hasNext()) {                           // 还有吗？
            var s = it.next();                           // 取一个，游标右移
            System.out.println("播放 " + s);
        }
    }
}
