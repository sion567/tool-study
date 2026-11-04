public class RefactorPolyDemo {
    public static void main(String[] args) {
        // ===== 反例：switch 裂殖（getPrice 和 getPoints 各一份）=====
        // double getPrice(String type) { switch(type){case "vip":...; case "svip":...;} }
        //
        // ===== 正例：分支变子类 =====
        Member v = new Vip(), s = new Svip(), n = new Normal();
        System.out.println(v.getPrice() + " " + s.getPrice() + " " + n.getPrice());
        // 新增 SVVip = 新增一个类，这里零修改
    }
}
interface Member { double getPrice(); }
class Vip implements Member { public double getPrice() { return 100 * 0.8; } }
class Svip implements Member { public double getPrice() { return 100 * 0.6; } }
class Normal implements Member { public double getPrice() { return 100; } }
