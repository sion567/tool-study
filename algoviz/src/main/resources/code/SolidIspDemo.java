public class SolidIspDemo {
    public static void main(String[] args) {
        // ===== 反例：胖接口逼实现者撒谎 =====
        // interface Worker { void work(); void eat(); }
        // class Robot implements Worker {
        //     public void work() {...}
        //     public void eat() { throw new UnsupportedOperationException(); }  // 被迫
        // }
        //
        // ===== 正例：按使用者切能力 =====
        Workable robot = () -> System.out.println("机器人干活");
        robot.work();          // Robot 只有 work 能力
        Eatable human = () -> System.out.println("员工吃饭");
        human.eat();           // 人有 eat，机器没有——互不强迫
    }
}
interface Workable { void work(); }
interface Eatable { void eat(); }
