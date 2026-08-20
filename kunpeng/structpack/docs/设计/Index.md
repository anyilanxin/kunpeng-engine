# structpack 模块技能索引

`kunpeng/structpack`（引擎 Record 序列化层）的设计知识库。

## 文档

| 文档 | 内容 | 何时用 |
|------|------|--------|
| [01-实现原理.md](./01-实现原理.md) | 五层架构、key-id 自界定帧 v1 格式（字节实例可手工复算）、id 三重守卫、读写路径优化（占位回填/布局缓存）、document 双轨策略、字段演进纪律 | 理解序列化机制、开发新 Record、排查序列化问题 |
| [02-性能报告.md](./02-性能报告.md) | 实测性能基线（帧体积/读写 ns）、设计迭代决策记录、DocumentCodec 性能、JSON 桥接选型 | 查性能数据、格式设计决策追溯 |

## 速查

- 模块：`kunpeng/structpack`，包 `com.anyilanxin.kunpeng.structpack`
- 帧：magic `4B 50`（"KP"）+ 版本 + **id 升序列表** + 值长度前缀自界定
- 演进零仪式：删字段 = 删那一行（标记注释自动退休 id，永不复用）；未知 id 按值长度跳过，新旧版本双向互读互不失败
- document（流程变量）：标准 msgpack 字节透传，`DocumentUtil` 承担 JSON/Map 互转
- 排查：`./gradlew :kunpeng:structpack:test`（117 用例）+ 编码规范技能的 Record 编写规范
