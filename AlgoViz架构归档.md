# AlgoViz 架构归档

> 蓝图 §5.4「MVC / MVP / MVVM」的归档说明：**本工具本身就是这三者的活案例**。
> 本文用真实代码库回答一个问题——同一个项目，三种架构镜头各看到什么？
> （截至 2026-11：158 个 Java 文件 / 19,396 行 / 102 个注册演示 / 102 golden / 347 测试）

## 一、总览

```
┌─────────────────────── Controller / 组合根 ───────────────────────┐
│  MainApp.java (937 行, JavaFX Application)                        │
│  · 组装 SplitPane(目录│画布│Tab)  · 键盘/点击事件                    │
│  · 预测状态机(ARMED/REVEALED)  · 渲染分发 if/else 链                │
└──────────┬──────────────────────────────────────┬─────────────────┘
           │ 调用                                  │ 读取
┌──────────▼─────────────── View ────────────────▼─────────────────┐
│  ui/ 10 个渲染器: Bar/List/Tree/Graph/Sequence/Matrix/Bitmap/     │
│        Plot/BTree/Hash —— 每个只认 Step，往 Canvas 上画            │
│  ui/ 面板: CodePanel / MarkdownView / PredictionPanel /           │
│        DictationPanel / IntentBar / VarsPanel / ComplexityPanel   │
│  ui/ 辅助: DiffHighlighter(帧间diff) / Theme / PlaybackController │
└──────────┬──────────────────────────────────────┬─────────────────┘
           │ Step (不可变快照)                      │
┌──────────▼──────────────────── Model ────────────▼────────────────┐
│  engine/  —— 零 JavaFX import，纯 JVM 可跑                         │
│  · Algorithm 接口: produce() → Trace                              │
│  · Trace = List<Step>；Step = 图层快照 + 事件 + 意图                │
│  · 图层 records: ds.array/list/tree/graph/hash/btree +             │
│    seq.* / math.matrix/bitmap/plot + mem.stack/heap               │
│  · 102 个 Algorithm 实现（按域分包: sort/tree/graph/math/…）        │
│  · TraceCodec(Gson) —— golden 测试的序列化通道                      │
└───────────────────────────────────────────────────────────────────┘
```

## 二、三种镜头

| 镜头 | AlgoViz 里的对应 | 吻合度 |
|---|---|---|
| **MVC** | Model=`engine`（状态+业务），View=`ui` 渲染器（只读 Step），Controller=`MainApp`（事件→状态变更→触发渲染） | ★★★★ 核心骨架就是它 |
| **MVP** | `MainApp` 兼任 Presenter：直接操作视图组件（`codePanel.highlight(...)`、`predictPanel.setResult(...)`），View 没有接口抽象 | ★★★★ 实际上是"无接口版 MVP" |
| **MVVM** | 缺 ViewModel 层；JavaFX 属性绑定只用于进度条，Trace 的呈现是命令式的 `render()` | ★★ 只有影子 |

**结论：MVC 变体 + 无接口 MVP 的呈现逻辑。** 三个镜头不是三选一——同一份代码里 Controller 的
"事件处理"部分是 MVP 的 Presenter，而"组装根"部分是纯 MVC 的 Controller。

## 三、这个架构最值钱的一条铁律

> **`engine/` 零 JavaFX 依赖，`test/` 零 JavaFX 依赖。**

由此直接兑现的三个能力：

1. **golden 测试纯 JVM 跑**：347 个测试不需要启动 JavaFX 平台，CI 友好、毫秒级
2. **时间旅行免费**：Step 是不可变快照，回退 = 换下标，View 重画即可——不需要"撤销栈"
3. **行为判卷可行**：白纸代码编译运行时不会被 UI 状态污染

反过来说，任何时候发现自己在 `engine/` 里想 `import javafx.`，就是架构在报警。

## 四、与教科书 MVC 的三个偏差（诚实记录）

| 偏差 | 现状 | 为什么接受 |
|---|---|---|
| **Pull 而非 Observer** | 每次 `render()` 由 Controller 触发，渲染器主动读 Step；没有 listener 机制 | 时间轴应用天然是"一帧一快照"的 pull 模型；Observer 反而要维护订阅生命周期 |
| **View 无接口** | MainApp 直接调用具体面板类，无法替换 View 实现 | 单人桌面工具，View 替换需求≈0；接口层的维护成本>收益 |
| **Controller 偏胖** | MainApp 937 行：组合根 + 渲染分发 + 预测状态机 + 复杂度/对比 Tab 内容 | 已知的坏味道。拆分方向见 §六 |

## 五、新图层 checklist（实战沉淀）

加一个可视化域（如 `ds.hash`）时，实际要动 **7 处**——这份清单是本项目最有复用价值的归档：

1. `engine/ds/` 新图层 record（如 `HashView(name, bucketCount, nodes)`）
2. `engine/Step` 加访问器 `hash()`（读 `layers.get("ds.hash")`）
3. `ui/` 新渲染器（`draw` + `hitIndex` + `changedXxx` diff 三件套）
4. `MainApp` 四处分发：render 分支 / 点击 hitIndex / 预测问题文案 / truthIndices + fmtIdx
5. `TraceAsserts` 合约检查（id 唯一、指针不悬空、与 mem.heap 同源一致）
6. 生产者：`layers.put("ds.hash", snapshot())`
7. golden 基线（首跑自动生成，之后逐字节比对）

## 六、关键决策日志

| 决策 | 选择 | 理由 |
|---|---|---|
| 帧的表示 | 不可变 Step 快照（全量拷贝） | 时间旅行、golden 逐字节比对、渲染器无状态——三件事一次解决 |
| 测试策略 | golden 基线 JSON | "任何一行变化都看得见"——比断言最终态强一个数量级 |
| `OrderedMaps` | 替换所有多键 `Map.of` | JDK `Map.of` 迭代序每次 JVM 随机（SALT）→ golden 抖动 |
| 并发演示 | 脚本化交错而非真线程 | 确定性是 golden 的前提；真实竞态交给代码模板让用户自己跑 |
| 负权边豁免 | TraceAsserts 对 Bellman-Ford 放开 | 合约属于算法族，不属于整个图层 |
| 数独 trace 瘦身 | 剪枝按格聚合 + 提示数 51→36 空 | 41,811 步/180MB golden 教训：**演示的步数预算也是设计约束** |
| JavaRunner | JDK 内置 `javax.tools` 内存编译 | 行为级判卷不用外部进程；jpackage runtime 自带 `jdk.compiler` |

## 七、已知债务（2026-11-05 清理后）

1. **MainApp 拆分（仅剩这一项）**：预测状态机（ARMED/REVEALED + 判卷计数）仍在 MainApp，约 250 行——
   提取需引入 listener 回调（状态变更后要触发 render/NotesStore），无 UI 测试兜底，暂缓。
   已完成的部分：Tab 内容 → `ui/TabContents`；渲染分发 → `ui/CanvasDispatch`；MainApp 937 → 843 行。
2. ~~预测模式 hash/btree 点击~~ ✅ **核实为误记**：问题文案/truthIndices/fmtIdx/hitIndex 四层早已接通（CanvasDispatch.pick 统一入口）。
3. ~~引擎与 UI 边界微漏~~ ✅ `JavaRunner`/`DictationJudge`/`DictationDiff`/`NotesStore` 已迁至 `com.algoviz.app`（纯逻辑包，测试同步迁移）。
4. ~~if/else 渲染分发链~~ ✅ `ui/CanvasDispatch` 有序数组分发：每个条目自算 diff、自绘制，新增图层 = 加一个条目。

## 八、一句话总结

> **Model 冻结成不可变时间轴，View 退化成纯函数，Controller 收拢成唯一的状态机** ——
> 这让"可视化 102 个算法"变成了"渲染 10 种图层形状"，也让整个引擎可以被 347 个测试钉死。
