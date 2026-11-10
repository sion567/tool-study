# AlgoViz 踩坑记录

> 项目全程犯过的错与教训——**每条都是真实踩过、付出过返工代价的**。
> 下次动手前先扫一眼对应章节；新增坑请追加到文末（含日期与修复 commit 可查）。

---

## 一、构建与依赖

### 1. `Map.of` / `Set.of` 的随机迭代序（杀伤力：测试全盘抖动）
- **现象**：golden 基线首跑生成、次跑必挂——JSON 字段顺序每轮不同。
- **根因**：JDK 的 `Map.of` 迭代顺序带每次 JVM 启动随机化的 SALT。
- **正确姿势**：多键场合一律用 `OrderedMaps.of` / `LinkedHashMap` / `LinkedHashSet`。
- **检查法**：`grep -rn "Map\.of(" src/main --include="*.java"`——单键可用，多键必改。

### 2. PATH 上的"另一个 jpackage"（杀伤力：exe 打不开，且极难排查）
- **现象**：打包"成功"，双击 exe 闪退；`runtime/release` 里 `JAVA_VERSION="17.0.6"`。
- **根因**：机器 PATH 上有 jdk-17 的 `jpackage.exe`，脚本裸调 `jpackage` 时被它抢先——**jpackage 会把自己所属 JDK 的运行时打进镜像**，而代码是 JDK 21 编译的（class 65 vs runtime 61）。
- **正确姿势**：永远 `"%JAVA_HOME%\bin\jpackage.exe"` 绝对路径调用；打包后校验 `runtime\release` 的 `JAVA_VERSION`。
- **通用教训**：任何"看似确定"的工具调用，多版本共存时都不可信 PATH。

### 3. 主 jar 不在 `--input` 目录（杀伤力：打包随机失败）
- **现象**：`配置的主 jar 在输入目录中不存在`。
- **根因**：maven-jar-plugin 默认产出到 `target/`，而 jpackage 的 `--input target/deploy` 里只有 `libs/`。之前能成功纯属历史布局巧合。
- **正确姿势**：pom 里显式 `<outputDirectory>${project.build.directory}/deploy</outputDirectory>`；脚本加 jar 存在性守卫。

### 4. XML 注释里不能出现 `--`（杀伤力：pom 直接解析失败）
- **现象**：`ModelParseException` 指向一行看似正常的注释。
- **根因**：注释里写了 `jpackage --input`——XML 规范禁止注释内出现双连字符。
- **正确姿势**：XML 注释里描述命令行参数时写"input 目录"而非 `--input`。

## 二、脚本与工具链

### 5. 批处理(.cmd)禁止中文注释（杀伤力：脚本输出乱码命令）
- **现象**：`'"E:\...\"' 不是内部或外部命令`——注释被 GBK 代码页绞碎成命令执行。
- **正确姿势**：`.cmd` 文件全 ASCII（注释用英文）；中文说明放配套 md。

### 6. python 写 Windows 脚本的转义连环坑（杀伤力：脚本自身损坏）
- 三次真实事故：`\a` 被解释成 BEL 响铃符（文件名里的字母 a 被吃掉）；`\r` 变成真实回车；正则替换串里的 `\j` 抛 `PatternError`。
- **正确姿势**：
  - 写 `.cmd`/含反斜杠路径 → 用原始字符串 `r'...'` 或 `chr(92)` 拼接；
  - 复杂补丁别用 `re.sub` 的字符串替换参数 → 用 lambda；
  - **改完立刻 `cat -A` 检查**是否有控制字符（`^G` = BEL）。

### 7. python 大块文本替换失败却静默继续（杀伤力：改了一半的烂摊子）
- **现象**：`assert old in s` 抛异常时，前面的替换已经写盘 / 或根本没写盘但编译已过——两种状态都出现过。
- **正确姿势**：补丁脚本所有 `replace` 前必须 `assert`；**任何大改前先 `git add` 一次**（哪怕不 commit），随时可回滚；改坏了直接 `git checkout HEAD -- 文件` 重来，别在残骸上打补丁。

### 8. git 大文件（杀伤力：push 被拒 + 历史膨胀）
- **现象**：`bt_sudoku.golden.json` 180MB 进了历史，GitHub 拒收。
- **根因**：trace 步数无预算意识——数独 51 空全展开 41,811 步，每步全量快照。
- **修复**：未推送区间用 `reset --soft` 重做提交 + `git gc --prune=now` 清 blob；**治本是产品侧瘦身**（剪枝按格聚合 + 加提示数 → 149 步 / 714KB）。
- **正确姿势**：新 demo 先看步数量级再写 golden；`du -m` 定期扫大文件；仓库必须有 `.gitignore`（target/、*.exe）。

## 三、Java 代码模式

### 9. emit 的括号错位（本项目最高频错误，出现过 7+ 次）
- **症状**：`推论变量 E 具有不兼容的上限` / `需要')'或','`。
- **病根**：`emit(x, List.of(VizEvent.of(detail, targets)), why)` 三层嵌套里，`targets` 参数和 `why` 参数写成同样的 `)...,` 结尾，肉眼极难分辨。
- **正确姿势**：
  1. 记住结构：`VizEvent.of(type, detail, targets...)` 在 `List.of(...)` 里；`why` 永远在 `List.of(...)` **外面**；
  2. 复制**已通过编译的** emit 块改内容，不徒手敲结构；
  3. 编译报"推论变量"九成是这里。

### 10. 静态常量转实例字段的初始化顺序（杀伤力：5 个文件连环编译错）
- **症状**：`可能尚未初始化变量 N`。
- **病根**：把 `static final int N` 参数化后，字段声明处仍写着 `= new int[N]`——实例初始化器在构造器之前跑，此时 N 还没赋值。
- **正确姿势**：依赖构造参数的字段**只声明不初始化**，赋值全部搬进构造器。

### 11. `List.of` 混装类型（int 与 double）
- `List.of(x, 0)`（x 是 double）→ `List<? extends Number&Comparable...>` 推断失败。
- **正确姿势**：字面量写全 `0.0`；循环变量 `(double) k` 显式转型。

### 12. record 无参构造 / 构造器冲突
- LcsDemo 原有 no-arg 构造器里给 final 字段赋值，参数化后出现两个 no-arg → 冲突。
- **正确姿势**：参数化时检查**所有**既有构造器；final 字段的赋值路径必须唯一。

## 四、演示内容与教学数字

### 13. "拍脑袋数字"必被测试逮（出现过 3 次）
- 黎曼和课件表 3 个数是编的（n=32 实为 0.3179 非 0.3203）；RNN h₅ 手写 0.66 实为 0.63；CNN 池化断言想当然。
- **正确姿势**：**课件里的每个数字先用可运行代码跑出来再抄**；行为测试断言用理论公式（如左端点误差 = 1/(2n)−1/(6n²)）定容差，不抄课件。

### 14. 断言读事件文本太脆（出现过 2 次）
- `detail().replaceAll(".*=", "")` 解析出 "2×1.5" 这样的非数字 → NumberFormatException。
- **正确姿势**：断言优先读**结构化数据**（heap 字段 / grid 值 / layer 记录），事件文案只做 contains 级别的弱断言。

### 15. 剧本类 demo 忘挂 ds/seq/math 图层（出现过 2 次）
- TraceAsserts 的"缺图层"合约每次都拦住了——但都是提交前最后一刻。
- **正确姿势**：新 demo 的 emit 里 `layers.put` 至少三层（stack/heap/主图层），写完先本地跑一遍 contract 测试再继续。

### 16. 事件剧本的下标/参数传错（杀伤力：教学叙事断裂）
- 七桥第一次"走出欧拉路径"时桥下标重复用了 3 号——集合永远差一个，"最后一座"事件永不触发；赌博机游走的 MAX_STEPS(500) 小于真实结局(1194 步)。
- **正确姿势**：**剧本先在 main 里空跑打印，确认终局事件真的发生**，再固化成断言；涉及随机/长程的先探测（固定种子跑一遍记下步数）。

## 五、资源与注册一致性

### 17. 新增 demo 的"三件套"漏项（自检抓出 9 处）
- 实现 ↔ 注册 ↔ code 模板 ↔ md 课件，四者靠手工同步：Lis/Avl/Bitmap 缺模板、红黑树等 6 个缺课件、HuffmanDemo 整个没注册（模板课件都写好了）。
- **正确姿势**：
  1. 写新 demo 按 checklist 走：实现 → 注册 → 模板 → 课件 → golden → 行为断言，**一口气做完**；
  2. 定期跑一致性探针（Audit.java 模式：注册项 × codeResource/docResource 逐一 openStream）。

### 18. 孤儿检测脚本自身有 bug
- bash `basename` 后变量被路径前缀污染，输出一堆假"未注册"。
- **教训**：检查工具本身先验证——先用已知状态（如必注册的 BubbleSort）校准探针，再信它的输出。

## 六、快照阅读（快速索引）

| 坑 | 一句话预防 |
|---|---|
| Map.of 随机序 | 多键用 OrderedMaps |
| jpackage 抢跑 | 绝对路径 + 版本守卫 |
| jar 不在 input | pom 显式 outputDirectory |
| cmd 中文 | 脚本全 ASCII |
| python 转义 | 原始字符串 + cat -A 验收 |
| emit 括号 | 复制可用块改，why 在 List.of 外 |
| 字段初始化序 | 依赖参数的字段在构造器里赋值 |
| 编数字 | 先跑代码再抄进课件 |
| 断言读文本 | 优先 heap/grid 结构化字段 |
| 忘挂图层 | emit 三层起步，先过合约测试 |
| 剧本下标错 | 空跑打印确认终局事件 |
| 三件套漏项 | checklist 一口气 + 定期 Audit |

---

## 追加区

> 格式：`### YYYY-MM-DD 坑名`，含现象/根因/姿势三段。修复 commit 号可追溯。
