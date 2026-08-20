# scheduler 模块技能索引

`kunpeng/scheduler`（Actor 模型调度器）的设计知识库。

## 文档

| 文档 | 内容 | 何时用 |
|------|------|--------|
| [01-设计说明.md](./01-设计说明.md) | 当前设计：API 面（21 类型）、ActorCell/Envelope 数据结构、SchedulingGate + wakeTickets 并发协议、执行循环、相位机、定时器、阻塞外包、指标、使用契约、测试锚点 | 理解调度器原理、排查调度/启动链问题、扩展 API、写消费方代码 |

## 速查

- 模块：`kunpeng/scheduler`，包 `com.anyilanxin.kunpeng.scheduler`
- 核心入口：`ActorScheduler`（builder）→ `ActorSchedulingService.submitActor`
- 消费方契约：单 actor 串行；actor 线程禁止阻塞 get()；close 后 submit 报 `"Actor is closed"`；外部队列上限 10000
- 排查启动链：看 `Startup <步骤名>` / `completed` / `still running after 30s` 日志三元组
