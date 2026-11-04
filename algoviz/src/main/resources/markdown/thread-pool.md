# 并发·线程池

## 一句话

线程是资产、任务是消耗品——常驻 worker 死循环 `take → 执行 → 再 take`。

## 不变量

worker 循环永不退出；任务排队等认领。**线程复用**：3 个任务只用 2 根线程，
创建/销毁（~1ms + MB 级栈）被摊薄到零。

## 画面读法

- submit(T1/T2/T3) 全进队列——提交方毫秒级返回（异步的本质：提交与执行解耦）
- worker-1 认领 T1、worker-2 认领 T2——并行度 = worker 数
- T1 完成 → worker-1 不退场，转身 take T3

## 七个参数落到画面上

| 参数 | 画面 |
|---|---|
| corePoolSize | 常驻 worker 数 |
| workQueue | 中间那列队列（削峰） |
| maximumPoolSize + reject | 队列满时的扩容与拒绝策略 |
| keepAliveTime | 空闲 worker 的裁撤 |

懂了这张图，`ThreadPoolExecutor` 的参数就不再是咒语。

## 怎么练

- 白纸推：core=2，队列容量 1，连提 4 个任务——第 4 个去哪了？（扩容到 max → 拒绝）
- 思考：为什么提交比执行快这么多还敢秒回？（Future 把"等"转嫁给调用方）
