public class SolidDipDemo {
    public static void main(String[] args) {
        // ===== 反例：高层亲手 new 细节 =====
        // class UserService {
        //     private MysqlConn conn = new MysqlConn();   // 焊死
        // }
        //
        // ===== 正例：两头都依赖接口，实现注入 =====
        UserDao pg = new PgConn();
        UserService svc = new UserService(pg);       // 生产环境
        UserDao fake = u -> System.out.println("内存桩存 " + u);
        UserService testSvc = new UserService(fake); // 测试环境——同一份业务代码
        svc.save("张三");
        testSvc.save("张三");
    }
}
interface UserDao { void save(String user); }        // 抽象
class UserService {                                   // 高层策略
    private final UserDao dao;
    UserService(UserDao dao) { this.dao = dao; }      // 构造器注入
    void save(String u) { dao.save(u); }
}
class PgConn implements UserDao {                     // 低层细节
    public void save(String u) { System.out.println("PG 存 " + u); }
}
