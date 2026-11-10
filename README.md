# AlgoViz —— 算法的"执行现场"

> 一个 JavaFX 桌面应用：把 **109 个算法/模式/数学主题** 变成可逐帧回放的"执行现场"。
> 不是看动画的**电影院**，而是练预测、练默写的**健身房**。

[![Java](https://img.shields.io/badge/Java-21-orange)]() [![JavaFX](https://img.shields.io/badge/JavaFX-21.0.4-blue)]() [![Tests](https://img.shields.io/badge/tests-410%20passing-brightgreen)]() [![Golden](https://img.shields.io/badge/golden%20baselines-121-yellow)]()

---

## 核心理念（为什么这样做）

三个学习闭环，全部建立在一个底层合约上——**每一步 = 不可变快照 + 结构化事件 + 意图**：

| 闭环 | 怎么练 | 在哪个 Tab |
|---|---|---|
| **预测 → 揭晓 → 对比** | 播放暂停在某一步，猜"下一帧哪些格子/节点会变"，提交后高亮真实变化——猜错的地方就是误解所在 | 🎯 预测 |
| **白纸默写 → 双重判卷** | 凭记忆写代码：`📝 对比` 做文本 diff；`▶ 行为判卷` 用 JDK 内置编译器**真的编译运行你的代码**，与参考输出比对——写法不同不扣分，行为一致才通过 | 📝 默写 |
| **复杂度 → 体感** | O(n²) vs O(n log n) 不背口诀，看六种排序吃同一批数据的实测步数曲线 | ⚖ 对比 |

**时间旅行**：因为每帧不可变，任意回退、逐帧步进、 golden 基线逐字节比对，全部免费。

## 内容目录（约 109 个演示）

| 板块 | 内容 |
|---|---|
| **算法**（43） | 排序 6 · 二分 · 链表 8 · 树 13 · 图 8 · 数组技巧 3 · DP 5 · 回溯 3 · 贪心 · KMP |
| **设计模式**（23） | GoF 全家桶：创建 5 / 结构 7 / 行为 11，时序图逐条消息 |
| **SOLID + 重构**（8） | 五原则各有反例→正例两幕；提取方法 / 参数对象 / 多态取代条件 |
| **并发**（3） | 生产者-消费者 / 读写锁 / 线程池——脚本化交错保证确定性 |
| **分布式**（3） | 断路器三态机 / 服务发现 / CQRS |
| **数学**（32） | 数与计算 10 · 微积分 6 · 几何坐标 5 · 线性代数 3 · 概率统计 3 · 图论离散 7 |

每个演示自带：高亮代码（行号跟随）· 课件 markdown · 复杂度横幅 · 部分支持 🎲 随机数据（33 个）。

## 快速开始

```bash
# 依赖：JDK 21 + Maven（Windows）
cd algoviz
mvn test              # 410 个测试（纯 JVM，不需要图形环境）
mvn javafx:run        # 开发运行

# 一键打包（产物：免安装 exe，自带 JRE，目标机器无需 Java）
package.cmd           # 或 package.cmd test 先跑测试
# 产物 → target\jpackage\AlgoViz\AlgoViz.exe
# 发行 = zip 整个 AlgoViz 文件夹
```

## 架构一览

```
MainApp（Controller/组合根）
   ├── ui/  12 种渲染形态（Bar/List/Tree/Graph/BTree/Hash/Sequence/Matrix/Bitmap/Plot/Scatter）+ 面板
   └── engine/（Model，零 JavaFX 依赖）—— Algorithm.produce() → Trace(不可变 Step 时间轴)
                                     com.algoviz.app/（JavaRunner 行为判卷等纯逻辑）
```

- **`engine/` 与测试零 JavaFX 依赖** → 410 个测试纯 JVM 毫秒级跑完
- 121 个 golden 基线逐字节锁定每条 trace——任何行为变化都看得见
- 详见 [AlgoViz架构归档.md](AlgoViz架构归档.md)：MVC/MVP/MVVM 三镜头映射、新图层 7 步 checklist、关键决策日志

## 项目结构

```
algoviz/
├── src/main/java/com/algoviz/
│   ├── engine/          # Model：Algorithm 接口 + Trace/Step + 12 域图层 + 109 个实现
│   ├── ui/              # View：渲染器 + 面板 + PlaybackController + DiffHighlighter
│   ├── app/             # 纯逻辑：JavaRunner（行为判卷引擎）、笔记持久化
│   └── MainApp.java     # Controller：组合根 + 预测状态机
├── src/main/resources/
│   ├── code/            # 每个演示的可运行代码模板（默写参考答案）
│   └── markdown/        # 每个演示的课件
└── src/test/            # 410 测试 + TraceAsserts 合约检查 + golden 基线
package.cmd              # 一键打包
```

## 测试

```bash
mvn test    # 410 tests：合约检查（TraceAsserts）+ 幂等性 + 121 golden 逐字节 + 行为断言
```

Golden 基线的哲学：**任何一行产出的变化都看得见**。有意变更时删除对应 `.golden.json` 重跑即自动重建。

## 学习路线建议

1. **排序**先练「🎯 预测」：暂停在冒泡的某一步，猜哪两个元素会交换
2. **链表/树**默写后点「▶ 行为判卷」——写法不同不扣分，跑对才算对
3. **数学馆**按子域逛：微积分从"导数=割线极限"开始，比定义式直观十倍
4. 每个演示页面的 🎲 / n-滑杆 换数据再看一遍——参数换了规律还在，才是真懂

## 相关文档

- [AlgoViz学习蓝图.md](AlgoViz学习蓝图.md) —— 从一页愿景到全部落地的完整蓝图（状态标记）
- [AlgoViz架构归档.md](AlgoViz架构归档.md) —— 自身架构的 MVC 案例归档 + 决策日志
- [AlgoViz踩坑记录.md](AlgoViz踩坑记录.md) —— 全程真实错误与教训（动手前先扫一眼）
- [TODO.md](TODO.md) —— 跨会话待办池（P1/P2/P3 分级 + 完成归档）
