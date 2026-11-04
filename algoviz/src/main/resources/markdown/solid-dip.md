# SOLID·D 依赖倒置（DIP）

## 一句话

高层策略与低层细节**都**依赖抽象——谁也不焊死谁。

## 反例痛点

`UserService` 里 `new MysqlConn()`：

- 业务类被数据库实现焊死——依赖箭头指向实现类
- 换 PostgreSQL、上测试桩 → 都要改 UserService 本体
- 测试都连真库，跑得慢还脏数据

## 正例

```
UserService ──依赖──> UserDao 接口 <──实现── PgConn / FakeConn
```

- 编译期：UserService 只见接口，源码里再无 Mysql 字样
- 运行期：构造器注入——生产注 PG、测试注内存桩，**同一份业务代码**

## 判据

识别信号：业务类里出现 `new 具体实现类()`——那就是该倒置的依赖点。
（工厂、Spring 容器都是"把 new 搬出去"的搬运工。）

## 怎么练

- 搜项目里的 `new XxxConn/new XxxClient`，在业务类里的都标出来
- 思考：为什么叫"倒置"？（传统：高层→低层；倒置后：低层反过来实现高层的抽象）
