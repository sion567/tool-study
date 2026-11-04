# 并发·读写锁

## 一句话

读-读共享、读-写互斥、写-写互斥——按"谁真的会冲突"分派并发权。

## 不变量

锁状态机三态：**空闲 / 读(n) / 写(1)**。
acquireRead 在空闲或读态都立即成功；acquireWrite 必须等读计数归零。

## 画面读法

- A、B 先后 acquireRead → mode=读(2)：两人并行读
- 写者 acquireWrite → 挂起（读写互斥）
- A、B 退出 → 读计数 0 → 唤醒写者 → mode=写（独占）
- releaseWrite → 回到空闲

## 红利算术

99% 读 1% 写的场景：普通互斥锁把 99 个读者也串行化；
读写锁让读者并行 → 吞吐近似乘上核数。

**写太频繁反而亏**：维护读计数、写者饥饿都成问题——JDK8+ 看 `StampedLock` 乐观读。

## 怎么练

- 白纸画状态机：给出一串 acquire/release 序列，标出每步 mode 与 readers
- 思考：写者等待期间新读者到达，放不放？（写饥饿问题——ReentrantReadWriteLock 默认非公平）
