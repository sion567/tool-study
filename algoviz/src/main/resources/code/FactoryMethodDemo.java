public class FactoryMethodDemo {
    interface Transport { void deliver(); }           // 产品接口
    static class Ship implements Transport {
        public void deliver() { System.out.println("海运"); }
    }
    static class Truck implements Transport {
        public void deliver() { System.out.println("陆运"); }
    }
    abstract static class Logistics {                 // 工厂方法：new 的决定权下放给子类
        abstract Transport create();
        void plan() { Transport t = create(); t.deliver(); }   // 骨架用产品，不管造什么
    }
    static class SeaLogistics extends Logistics {
        Transport create() { return new Ship(); }
    }
    static class RoadLogistics extends Logistics {
        Transport create() { return new Truck(); }
    }
    public static void main(String[] args) {
        Logistics sea = new SeaLogistics();
        sea.plan();                                    // 工厂造 Ship → 海运
    }
}
