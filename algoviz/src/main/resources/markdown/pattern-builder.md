# builder

## 一句话

分步装配同一个对象：每步只改一个字段，build 之前产品不完整也不可用

## 不变量（意图层）

- 返回 this 支持链式；参数名即方法名，自文档化

## 🔗 本项目哪里用到了

- StringBuilder、Lombok @Builder、OkHttp Request.Builder

## 怎么练

- 🎯 预测页答两题 → 白纸默写完整清单 → 对照代码页 → 隔天再默
- 对比 8 参数构造器：顺序错、类型近的参数互换编译器不吭声
