# 翻转二叉树

## 一句话

递归交换每个节点的左右孩子：`invert(root) = swap(left, invert(right))`——后序回溯时换。

## 不变量

子树翻转完成后再交换指针——递归信任：孩子的事交给递归，我只管本层交换。

## 三行核心

```java
Node l = invert(root.left);
Node r = invert(root.right);
root.left = r; root.right = l;
```

## 为什么它是名题

Homebrew 作者 Max Howell 因写不出这题被 Google 拒——"我能 reverse 一棵树，却 reverse 不了面试"。
它考的不是难度，是**递归心智模型**：分治边界（null 返回）+ 本层动作（swap）。

## 怎么练

- 白纸画 {5,3,8,1,4,7,9} 翻转前后（中序恰好反序）
- 变形：判断对称树 = 翻转思想的变体（左子树 vs 镜像右子树）
