package com.algoviz.engine.pattern;

import com.algoviz.engine.Algorithm;
import com.algoviz.engine.Loc;
import com.algoviz.engine.Step;
import com.algoviz.engine.VizEvent;
import com.algoviz.engine.ds.SeqView;
import com.algoviz.engine.trace.Trace;

import java.util.List;

/**
 * SOLID·D 依赖倒置：高层策略与低层细节都依赖抽象。
 * 反例：UserService 里 new MysqlConn()——业务类被数据库实现焊死。
 * 正例：依赖 UserDao 接口，实现从构造器注入——换 PG 零改业务。
 */
public final class SolidDipDemo implements Algorithm {

    private static final String SCHEMA = "1";
    private static final String INVARIANT = "高层不碰低层，两头都碰接口——「策略」和「细节」谁也不焊死谁";

    private static final int L_BAD1 = 5;
    private static final int L_BAD2 = 7;
    private static final int L_GOOD1 = 13;
    private static final int L_GOOD2 = 15;

    private PatternEmitter em;

    @Override public String id() { return "solid.dip"; }
    @Override public String title() { return "SOLID·依赖倒置 DIP"; }
    @Override public String codeResource() { return "/code/SolidDipDemo.java"; }
    @Override public String docResource() { return "/markdown/solid-dip.md"; }

    @Override
    public Trace produce() {
        em = new PatternEmitter(List.of(
                new SeqView.Participant("client", "UserService（高层）"),
                new SeqView.Participant("mysql", "new MysqlConn()（反例）"),
                new SeqView.Participant("iface", "UserDao 接口"),
                new SeqView.Participant("pg", "PgConn / FakeConn（可注入）")), INVARIANT);
        em.reset();
        // ===== 反例：硬编码细节 =====
        em.message(L_BAD1, 0, 1, "save() → 内部 new MysqlConn()", "CALL",
                "反例：高层业务亲手 new 低层细节——依赖箭头指向实现类");
        em.state(L_BAD2, VizEvent.EventType.COMPARE,
                "换 PostgreSQL / 上测试桩 → 都要改 UserService 本体", List.of(),
                "业务代码焊死在 MySQL 上——测试都连真库");
        // ===== 正例：面向接口 + 注入 =====
        em.message(L_GOOD1, 0, 2, "UserService 依赖 UserDao 接口（编译期）", "CALL",
                "正例：依赖箭头指向接口——UserService 的源码里再无 Mysql 字样");
        em.message(L_GOOD2, 0, 3, "构造器注入 PgConn / 测试桩 FakeConn（运行期）", "CALL",
                "细节从外面塞进来：生产注 PG、测试注内存桩——同一份业务代码");
        em.state(L_GOOD2, VizEvent.EventType.RETURN,
                "依赖方向「倒置」了：细节反过来依赖抽象，而不是抽象依赖细节", List.of(),
                "DIP 是 Spring 依赖注入的全部理论根基——先懂 DIP 再看框架都是明牌");
        return new Trace(SCHEMA, meta(), em.steps);
    }

    private Trace.TraceMeta meta() {
        return new Trace.TraceMeta(id(), title(), codeResource(), "new 1 处 → 注入 0 处", 2);
    }

    @Override
    public java.util.List<String> dataLines() {
        return List.of("// 反例: this.conn = new MysqlConn(); → 正例: UserDao conn（构造器注入）");
    }

    @Override
    public com.algoviz.engine.Complexity complexity() {
        return new com.algoviz.engine.Complexity("O(1) 结构原则", "O(1)",
                "识别信号：业务类里出现 new 具体实现类——那就是该倒置的依赖点（工厂/容器是它的搬运工）");
    }
}
