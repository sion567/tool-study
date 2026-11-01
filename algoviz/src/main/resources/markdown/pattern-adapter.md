# adapter

## 一句话

接口不兼容的两个人合作：中间加一个翻译层，双方代码零改动

## 不变量（意图层）

- Adapter 实现 Target、包装 Adaptee——client 与 Adaptee 至死不见面

## 🔗 本项目哪里用到了

- InputStreamReader（字节流转字符流）、Spring MVC HandlerAdapter

## 怎么练

- 🎯 预测页答两题 → 白纸默写完整清单 → 对照代码页 → 隔天再默
- 改一层救两边；也可以用两参数适配器（写适配器适配另一个适配器）

## 变体

- 变体=类适配器（继承实现双管）、对象适配器（组合，推荐）、双向适配器
