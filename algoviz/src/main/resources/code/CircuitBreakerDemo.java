import java.util.concurrent.atomic.*;
import java.util.function.*;

public class CircuitBreakerDemo {
    enum State { CLOSED, OPEN, HALF_OPEN }

    static final int THRESHOLD = 3;
    static final long COOLDOWN_MS = 5_000;
    static State state = State.CLOSED;
    static AtomicInteger failures = new AtomicInteger();
    static volatile long openedAt;

    public static void main(String[] args) throws Exception {
        Supplier<String> call = () -> "支付结果";
        for (int i = 1; i <= 6; i++) {
            String r = call(call, i);
            System.out.println("req#" + i + " → " + r + "  [breaker=" + state + "]");
            if (i == 4) {
                state = State.OPEN; openedAt = System.currentTimeMillis();   // 三连败熔断
            }
            if (i == 5) {
                Thread.sleep(COOLDOWN_MS);   // 演示用 5s；生产可配
                state = State.HALF_OPEN;
            }
            if (i == 6 && state == State.HALF_OPEN) {
                state = State.CLOSED; failures.set(0);   // 试探成功 → 复位
            }
        }
    }

    /** 所有请求都过这一道闸——OPEN 时根本不打服务（fail fast）。 */
    static String call(Supplier<String> svc, int n) {
        if (state == State.OPEN) return "拒绝（快速失败）";
        try {
            String r = svc.get();
            failures.set(0);
            return r;
        } catch (Exception e) {
            if (failures.incrementAndGet() >= THRESHOLD) state = State.OPEN;
            return "500 失败(" + failures.get() + ")";
        }
    }
}
