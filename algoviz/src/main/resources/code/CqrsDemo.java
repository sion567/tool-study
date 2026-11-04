import java.util.*;

public class CqrsDemo {
    /** 写模型：规范化订单库——事务、约束、不变量都在这侧。 */
    record Order(String id, String user, int cents, String status) {}
    static final List<Order> writeDb = new ArrayList<>();

    /** 读模型：反规范化宽表——按查询的形状存，0 join 可缓存。 */
    record OrderRow(String user, String summary) {}
    static final List<OrderRow> readDb = new ArrayList<>();

    public static void main(String[] args) {
        // ===== 命令：有业务意图的写 =====
        createOrder("张三", 19900);
        // ===== 查询：全走读模型 =====
        System.out.println("我的订单 → " + queryOrders("张三"));
        // 写侧保一致，读侧保速度；事件投影负责同步（接受毫秒级最终一致）
    }

    static void createOrder(String user, int cents) {
        Order o = new Order(UUID.randomUUID().toString(), user, cents, "待发货");
        writeDb.add(o);                                       // ① 落库（事务内）
        publish(new Object[]{"OrderCreated", o});             // ② 发事件（不直接改读库）
    }

    static void publish(Object[] evt) {
        Order o = (Order) evt[1];
        readDb.add(new OrderRow(o.user(), o.user() + " ¥" + o.cents() / 100 + " " + o.status()));   // ③ 投影到宽表
    }

    static List<String> queryOrders(String user) {
        return readDb.stream().filter(r -> r.user().equals(user)).map(OrderRow::summary).toList();
    }
}
