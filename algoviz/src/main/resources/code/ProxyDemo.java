public class ProxyDemo {
    interface Vault { String open(); }
    static class RealVault implements Vault {
        public String open() { return "金库里的宝藏"; }
    }
    static class VaultProxy implements Vault {           // 与 RealVault 同接口
        private final RealVault real = new RealVault();
        private boolean loggedIn = false;
        void login() { loggedIn = true; }
        public String open() {
            if (!loggedIn) return "拒绝：请先登录";       // ← 代理多干的事：控制访问
            return real.open();                           // 检查过了才转发
        }
    }
    public static void main(String[] args) {
        Vault v = new VaultProxy();
        System.out.println(v.open());                    // 拒绝：请先登录
    }
}
