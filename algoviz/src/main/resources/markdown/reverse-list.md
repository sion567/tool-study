# 链表反转

## 一句话

三个指针（prev / curr / next）走一遍，把每根 `next` 箭头逐个掉头。

## 生活故事

一列火车车厢要掉头：你一次只能抓一节。先记住后面那节（`next`），把当前这节的挂钩解开来接 到已翻好的那截上（`curr.next = prev`），然后整体右挪一格。**全程只有「断一根、接一根」这两种动作。**

## 不变量（意图层）

- **prev 开头的链，永远是已反转完成的部分；curr 开头的链是待处理部分**
- 三段式口诀（蓝图 §1.3）：谁在指谁？断哪根？接哪根？——每一步只回答这三句
- `next` 这行（①）必须先于 `curr.next = prev`（②）：**先记住，再断箭**——顺序反了后半截链就丢了

## 万能三段式（对照帧看）

| 帧 | 谁在指谁 | 断哪根 | 接哪根 |
|---|---|---|---|
| `next = curr.next` | curr → @k | 无 | 栈里多一个 next 标签 |
| `curr.next = prev` | curr 原来指向 @k | **@curr.next 这根** | 接到 prev（或 null） |
| `prev = curr` / `curr = next` | 标签挪位 | 无 | 标签换盒子 |

## 怎么练（健身房，蓝图 §2.5-C）

1. 🎯 预测页：问的是「哪些节点的 next 箭头会变」——注意 `prev = curr`、`curr = next` 这两帧**箭头一根都不断**，只有标签挪位，最容易骗到你
2. 看帧时只盯一根箭头：`curr.next`——蓝图 §3.2 的帧序列就是这个画面
3. 白纸默写四行循环体 → 对照 `/code/ReverseList.java` → 隔天再默

## 🔗 本项目哪里用到

- `engine/list/ReverseList.java`：生产者；`mem.heap` 放 Node 盒子（`val`/`next` 字段），`ds.list` 放箭头视图
- `ui/ListRenderer.java`：盒子 + 箭头渲染器——直线 = 前向，下方弧线 = 掉头
- `traces/reverse_list.golden.json`：行为快照
