public class SolidSrpDemo {
    public static void main(String[] args) {
        // ===== 反例：三个变化方向挤一类 =====
        // class Employee {
        //     double calcPay() {...}        // 税率会变
        //     String genReport() {...}      // 报表格式会变
        //     void save() {...}             // 表结构会变
        // }  → 改报表要回归测试薪资！
        //
        // ===== 正例：按变化方向拆 =====
        SalaryCalc pay = new SalaryCalc();
        ReportGen report = new ReportGen();
        EmployeeRepo store = new EmployeeRepo();
        System.out.println(pay.calc(8000));
        System.out.println(report.render("张三"));
        store.save("张三");          // 改任何一件都只碰一个类
    }
}
class SalaryCalc {
    double calc(double base) { return base * 0.85; }   // 只管税率变化
}
class ReportGen {
    String render(String name) { return "报表:" + name; }  // 只管格式变化
}
class EmployeeRepo {
    void save(String name) { System.out.println("存库:" + name); }  // 只管表结构变化
}
