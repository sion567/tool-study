public class AbstractFactoryDemo {
    interface Button { void paint(); }
    interface Checkbox { void paint(); }
    static class WinButton implements Button { public void paint() { System.out.println("Win 按钮"); } }
    static class WinCheckbox implements Checkbox { public void paint() { System.out.println("Win 复选"); } }
    static class MacButton implements Button { public void paint() { System.out.println("Mac 按钮"); } }
    static class MacCheckbox implements Checkbox { public void paint() { System.out.println("Mac 复选"); } }
    interface WidgetFactory {                          // 一次拿一整套
        Button createButton();
        Checkbox createCheckbox();
    }
    static class WinFactory implements WidgetFactory {
        public Button createButton() { return new WinButton(); }
        public Checkbox createCheckbox() { return new WinCheckbox(); }
    }
    static class MacFactory implements WidgetFactory {
        public Button createButton() { return new MacButton(); }
        public Checkbox createCheckbox() { return new MacCheckbox(); }
    }
    public static void main(String[] args) {
        WidgetFactory f = new WinFactory();            // 选一次风格
        Button b = f.createButton();                   // 整族都是 Win
        Checkbox c = f.createCheckbox();
    }
}
