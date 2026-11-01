# AlgoViz

蓝图见 [`../AlgoViz学习蓝图.md`](../AlgoViz学习蓝图.md)。
当前进度：**M6+ 完成** —— **40 个演示**：**算法×15**（排序×4 含快排、查找×1、链表×2 含判环、树×6 含层序、图×3 含 BFS/DFS）+ **GoF 设计模式 23 个全（完整可编译代码清单）** + **数学域（矩阵乘法）**；帧 diff + 预测模式全域 + 暗色主题 + 代码页（行号/自动滚动/注释浮窗/横向滚动）+ 可拖拽 SplitPane + golden 测试（40 份，含幂等性测试）+ jpackage 打包 exe。
下一批候选：归并/堆排序、合并有序链表、KMP、DP 背包。

## 打包 exe（免装 JDK 的绿色版）

```bash
JAVA_HOME="D:/sdk/Java/jdk-21.0.10" mvn -f pom.xml package -DskipTests
cp target/algoviz-0.1.0-SNAPSHOT.jar target/deploy/
"D:/sdk/Java/jdk-21.0.10/bin/jpackage" --type app-image --name AlgoViz \
  --input target/deploy --main-jar algoviz-0.1.0-SNAPSHOT.jar \
  --main-class com.algoviz.Main --dest target/installer
# 产物：target/installer/AlgoViz/AlgoViz.exe（双击即用；重打包前先关掉正在运行的实例）
```

## 快捷键

`←`/`→` 单步，`空格` 播放/暂停，`N`/`P` 下一个/上一个算法。

## 模式演示的代码是完整可编译清单（接口+实现+上下文+调用方），消息高亮对齐真实行号；代码页带行号、注释弱化、高亮行自动滚动。
注：Dijkstra 由蓝图原定的 WebView+HTML 改为 ds.graph 图层 + Canvas——为了接预测模式与 golden 测试（原则 4：只做健身房）。

## 运行（桌面应用）

需要 JDK 21 + Maven 3.9（本机 JDK 21 在 `D:/sdk/Java/jdk-21.0.10`）。

```bash
# Git Bash
JAVA_HOME="D:/sdk/Java/jdk-21.0.10" mvn javafx:run

# PowerShell
$env:JAVA_HOME="D:\sdk\Java\jdk-21.0.10"; mvn javafx:run
```

快捷键：`←`/`→` 单步，`空格` 播放/暂停。

**预测模式（右侧「🎯 预测」页）**：先看当前帧 → 猜「下一帧哪些格子会变」（点柱子选，全不选 = 预测数组不变）→ 提交揭晓 + diff → 答错的地方就是底板上的洞（蓝图 §2.5-A）。

**帧 diff**：与上一帧相比变化的格子有黄圈；变量面板里变化的变量黄字加粗（视图 C，蓝图 §2.2）。

## 测试

```bash
JAVA_HOME="D:/sdk/Java/jdk-21.0.10" mvn test
```

- golden 首跑自动生成 `src/test/resources/traces/bubble_sort.golden.json`，之后逐字节比对；
  **有意变更行为时，删掉 golden 文件重新生成**。
- 不变量测试（蓝图 §2.4）：每步校验「尾部已就位区间必须有序」。

## 结构（对应蓝图 §2.1 / §6.1）

```
engine/   引擎层（纯 Java，无 UI 依赖，只是生产者之一）
  Step/Layer/Event/Intent   ← 通用底层合约：一步 = 各图层不可变快照 + 结构化事件 + 意图
  mem/    图层 mem.stack / mem.heap（栈帧、堆盒子，@id 引用）
  ds/     图层 ds.array / ds.list / ds.tree / ds.graph（数组、链表、树、带权图的语义视图）
  list/   生产者：ReverseList（链表反转，栈堆+引用箭头现场）
  tree/   生产者：TreeTraversal（前/中/后序递归）+ BstInsert / BstDelete（TreeEmitter 共享）
  graph/  生产者：Dijkstra（出圈+松弛，距离表实物化为堆盒子）
  pattern/ 生产者：策略/观察者/模板方法/装饰器/命令/迭代器/状态（PatternEmitter 共享）
  math/   生产者：MatrixMultiply（math.matrix 图层——数学域的第一个现场）
  trace/  Trace 合约（版本化 JSON）
  sort/   生产者：BubbleSort（行号对齐 /code/BubbleSort.java）
ui/       渲染层（只订阅图层渲染，不含算法逻辑）
  PlaybackController  时间旅行 = 改 index
  BarRenderer / CodePanel / VarsPanel / IntentBar
```

> M1 说明：UI 用纯代码搭建（少一层 FXML 出错面），FXML 化到 M2 顺手做。
> 未来加内容 = 加图层（`seq.*` 设计模式、`math.*` 数学）+ 加渲染器，播放器不动。
