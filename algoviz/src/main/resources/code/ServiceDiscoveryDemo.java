import java.util.*;
import java.util.concurrent.*;

public class ServiceDiscoveryDemo {
    /** 注册表：地址表 + 心跳续约。90s 没续约 → 摘除（宕机方不会自己报丧）。 */
    static final Map<String, Long> registry = new ConcurrentHashMap<>();

    public static void main(String[] args) throws Exception {
        register("user-svc@10.0.0.1");
        register("user-svc@10.0.0.2");
        System.out.println("lookup → " + lookup("user-svc"));      // [两台]
        // 模拟 @.1 心跳超时被摘除
        registry.remove("user-svc@10.0.0.1");
        System.out.println("lookup → " + lookup("user-svc"));      // 只剩 @.2，流量自动全切
        register("user-svc@10.0.0.1");                             // 恢复 → 重新注册回池子
        System.out.println("lookup → " + lookup("user-svc"));
        // 客户端全程只认名字 user-svc，从不写死 IP
    }

    static void register(String instance) {
        registry.put(instance, System.currentTimeMillis());   // 注册 = 首次续约
    }

    static List<String> lookup(String service) {
        List<String> out = new ArrayList<>();
        registry.keySet().stream().filter(k -> k.startsWith(service + "@")).forEach(out::add);
        Collections.sort(out);                                // 调用方负载均衡挑一个
        return out;
    }
}
