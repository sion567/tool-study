# factorymethod

## 一句话

把 new 的决定权下放给子类：父类骨架用产品，造什么由 create() 说

## 不变量（意图层）

- Logistics.plan() 用 Transport；SeaLogistics 决定造 Ship

## 🔗 本项目哪里用到了

- JDBC DriverManager、Spring FactoryBean

## 怎么练

- 🎯 预测页答两题 → 白纸默写完整清单 → 对照代码页 → 隔天再默
- 新增产品线 = 新增一对（产品+工厂），骨架零改动
