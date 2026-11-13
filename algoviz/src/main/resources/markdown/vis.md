# 线程·可见性与CAS

## 一句话

JMM 三现场：**无 volatile 看不见 → volatile 强制可见 → count++ 丢更新靠 CAS 自旋救**。

## 不变量

- 无同步：写留在工作副本、读从主存拿旧值——"看不见"是 JMM 的**合法行为**，不是 bug
- volatile 写：强制刷主存 + 读绕过副本——只治可见性，**不治原子性**
- count++ = 读-改-写三步：两个线程撞同一个旧值 → 丢失更新（两次 ++ 只加 1）
- CAS(compareAndSet)：期望值对才交换——失败不写回，自旋重读再试

## 画面读法

- 泳道 = 写线程 / 主内存 / 读线程：消息 = 读写动作
- 第一幕：writer 副本 true、主存 false、reader 副本 false——三个值同屏对比
- 第三幕：两条 CAS 消息一成功一失败——失败方的"重读再试"就是 AtomicInteger 的内核

## 悲观 vs 乐观

- synchronized：悲观——先拿锁再操作，别人等着
- CAS：乐观——先试，撞了重来。低竞争下 CAS 快，高竞争下自旋空耗
- AtomicInteger/LongAdder/非阻塞队列——java.util.concurrent 的底层全是 CAS

## 怎么练

- 跑模板代码：count < 20000 的实感（每次跑丢的数量都不同——竞态的非确定性）
- 思考：volatile int 能不能救 count++？（不能——可见性≠原子性，三步照样撞）
- 延伸：LongAdder 分段累加——高竞争下把一个热点拆成多个
