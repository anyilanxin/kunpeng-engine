---
name: kunpeng-skills
description: Use as the top-level entry to discover kunpeng project's coding/generation skills. Lists all skill modules (iteration, record-standards, enum-standards, repository-standards, structpack, scheduler, eventlog-design, cluster-dispatch-design, commit-standards, sink-design, workjob) with one-line descriptions and links to each module's entry file.
---

# kunpeng Skills 总览

本项目 `.agents/skills/` 下所有技能模块索引。每个模块是一个子目录、一个可自动发现的技能：有自己的 `SKILL.md`（frontmatter `name` + `description`，description 用英文便于技能匹配，正文中文；目录/文件名一律英文）。

## 模块列表

| 模块目录 | 主题 | 何时用 | 入口 |
|----------|------|--------|------|
| `iteration/` | 全链路迭代开发专家：跨模块/跨层既有功能改动的一次性编排（影响排查→计划确认→固定顺序执行→契约自检），大改动分流（广而浅→总控派发 / 深而窄→设计先行），协议兼容变更先评估 | 改动跨 protocol/repository/engine/clients 多层的字段、校验、bug、枚举、状态机；"改一下XX/加个字段/迭代一下" | [iteration/SKILL.md](./iteration/SKILL.md) |
| `record-standards/` | 协议 Record 类（`*Record extends UnifiedRecordValue`）编写规范：类骨架、字段类型、key、getter/setter、集合与 addXxx、wrap/unwrap、嵌套对象 setter | 给 `*RecordValue` 接口实现 `*Record` 类；新增字段、修改 setter、加 addXxx 方法 | [record-standards/SKILL.md](./record-standards/SKILL.md) |
| `enum-standards/` | LifeCycle/State 枚举规范：`CommandValueLifeCycle` vs `CommandApiValueLifeCycle` 契约、`PROCESS_INDEX_*`/`RECORD_INDEX_*` 编号池分配 | 新建/修改 LifeCycle 枚举、增删状态、申请索引编号 | [enum-standards/SKILL.md](./enum-standards/SKILL.md) |
| `repository-standards/` | repository Entity 规范（record 规则与 repository 规则整合）：`*Entity extends UnpackedObject implements DbValue`、必须 wrap/unwrap、字段映射与裁剪 | 新建/修改与 protocol Record 对应的持久化 Entity、同步 wrap/unwrap | [repository-standards/SKILL.md](./repository-standards/SKILL.md) |
| `structpack/` | structpack 序列化层（引擎唯一 Record 序列化）：实现原理、id 强制身份管理、性能演进、document 解码 | 理解 Record 序列化机制、排查序列化问题、查性能数据、开发新 Record | [structpack/Index.md](./structpack/Index.md) |
| `scheduler/` | scheduler Actor 调度器：并发协议（gate+wakeTickets）、执行模型、相位机、future 语义、指标、使用契约（含历史设计与性能分析归档） | 理解调度原理、排查调度/启动链问题、写 actor 消费方代码、查指标 | [scheduler/Index.md](./scheduler/Index.md) |
| `eventlog-design/` | eventlog 事件日志流：append 管线（无锁定序器 + pending 队列 + AIMD 流控）、EL 批帧 v1 格式、读路径 seek 族语义、EventStore SPI 桥接 Raft、崩溃恢复 | 理解/改动 eventlog、排查定序/流控/恢复问题、查写读路径性能基线 | [eventlog-design/SKILL.md](./eventlog-design/SKILL.md) |
| `cluster-dispatch-design/` | cluster-dispatch 集群调度子系统：调度计划生命周期（admin/business 三类入口、PLAN 状态流）、拓扑 diff 计划生成、执行明细下发与 ACK、延迟看门狗重试、source 治理、adminInit | 理解/改动 cluster-dispatch、排查调度计划与执行明细问题 | [cluster-dispatch-design/SKILL.md](./cluster-dispatch-design/SKILL.md) |
| `commit-standards/` | 提交规范：spotlessApply 必须根目录整库执行（模块级任务不存在会报错）、commit message 规范（英文 type 前缀分条、无 Claude 署名尾行）、先审核再提交流程 | 改动准备提交前、跑 spotlessApply 前、写 commit message 时 | [commit-standards/SKILL.md](./commit-standards/SKILL.md) |
| `sink-design/` | sink 日志外发体系：RecordSink SPI 全生命周期、位置确认与重试语义、SinkService 运行时架构（分发/leader-follower 位置广播）、SBE wire 协议与指标 | 实现/评审/接入 sink、查 sink API 用法、排查位置确认或暂停恢复行为、broker 接入 sink 服务时 | [sink-design/SKILL.md](./sink-design/SKILL.md) |
| `workjob/` | 分布式 Job 推拉系统设计 v1.5：注册同步（快照对账+增量）、竞争消费、broker 层三原语能力服务、deadline 簿记与 Resolve、故障矩阵、gRPC/Netty 双段协议 | 理解/改动 gateway/broker job 派发链路、排查 job 投递问题、演进 job 推拉系统时 | [workjob/SKILL.md](./workjob/SKILL.md) |

> 后续添加新模块（规范类或知识库类）：在 `.agents/skills/` 下新建子目录 + 该目录的 `SKILL.md`，在上表加一行。

## 目录结构

```text
.agents/skills/
├── SKILL.md               ← 本文件(总览)
├── record-standards/      ← 协议 Record 类编写规范
├── enum-standards/        ← LifeCycle 枚举规范
├── repository-standards/  ← repository 持久化 Entity 编写规范
├── commit-standards/      ← 提交规范(spotlessApply + git commit)
├── iteration/             ← 全链路迭代开发专家(跨模块改动编排)
├── structpack/            ← 序列化层知识库
├── scheduler/             ← Actor 调度器知识库
├── eventlog-design/       ← 事件日志流知识库
├── cluster-dispatch-design/ ← 集群调度子系统知识库
├── sink-design/           ← sink 日志外发体系知识库
└── workjob/               ← 分布式 Job 推拉系统设计知识库
```

## 通用原则（跨模块）

- **接口契约优先**：实现类严格遵守对应接口的方法签名，不"顺便"加接口没有的字段/方法
- **常量复用**：能用现有常量（`BusinessRecordConstant` 等）的就用，没有就写字面量，**不要**自己往公共常量类加新常量
- **编译验证**：任何代码生成后都要跑对应模块的 `./gradlew :<module>:compileJava` 确认通过

## 如何使用

1. 看上表找到相关模块（每个顶层目录即自动发现技能）
2. 读其 `SKILL.md` / `Index.md` 拿章节地图
3. 跳到具体章节
4. 完成代码后跑该模块规定的验证命令

## 扩展指南

要加新模块：

1. `mkdir .agents/skills/<新模块名>/`（英文 kebab-case）
2. 写 `SKILL.md`（frontmatter `name` + `description`；规范型参考 `record-standards/SKILL.md`，知识库型参考 `scheduler/SKILL.md`）；多文档模块再加 `Index.md`
3. 把具体章节 md 文件加进去
4. 回到本文件，在"模块列表"表格里加一行
5. （可选）如有跨模块的通用约定，更新"通用原则"章节
