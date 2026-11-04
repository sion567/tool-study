public class RefactorParamDemo {
    public static void main(String[] args) {
        // ===== 反例：5 参齐飞，加字段全链改 =====
        // register("张三", 25, "北京", "13800000000", "z@x.com");
        //
        // ===== 正例：打包成 UserInfo，签名稳定 =====
        UserInfo u = new UserInfo("张三", 25, "北京", "13800000000", "z@x.com");
        register(u);           // 加「微信号」字段：只改 UserInfo，这里一行不动
        System.out.println(u.name() + " / " + u.phone());
    }
    record UserInfo(String name, int age, String addr, String phone, String email) {}
    static void register(UserInfo u) { /* 签名从此不变 */ }
}
