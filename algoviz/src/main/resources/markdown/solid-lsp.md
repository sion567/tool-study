# SOLID·L 里氏替换（LSP）

## 一句话

任何父类出现的地方，子类塞进去必须**照样工作**——"is-a"要经得起行为检验。

## 反例痛点

`Square extends Rectangle`：

```
Rectangle r = new Square();
r.setWidth(5); r.setHeight(4);
r.area() → 16 ≠ 20   ← 契约被悄悄撕了
```

Square 重写 setter 强行宽=高，调用方拿父类引用算错账 →
被迫 `instanceof` 特判 → 继承形同虚设。

## 正例

正方形不是长方形的**特化**，是 Shape 的**平行实现**：
两者各自实现 `area()`，可互换、互不撕约。

## 常见破约现场

- 子类方法抛 `UnsupportedOperationException`
- 忽略/篡改入参（如 Square 的 setWidth）
- 收紧返回值范围、加前置条件

## 怎么练

- 检验法：把子类实例传进**所有**父类测试用例，必须全绿
- 思考：Ostrich extends Bird { fly() 抛异常 }——怎么改？（拆 Flyable，和 ISP 配合）
