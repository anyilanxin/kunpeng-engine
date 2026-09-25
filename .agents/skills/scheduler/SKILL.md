---
name: scheduler
description: Use when working with the kunpeng/scheduler Actor dispatcher — understanding the scheduling design (gate + wakeTickets concurrency protocol, ActorCell execution model, phase machine, futures, metrics), debugging scheduling or startup-chain issues, extending the API, or writing actor consumer code
---

# scheduler 模块（Actor 调度器）

`kunpeng/scheduler`（Actor 模型调度器）的设计知识库。

核心入口：`ActorScheduler`（builder）→ `ActorSchedulingService.submitActor`。

## 文档

见 [Index.md](./Index.md) 的文档表与速查。主文档：[01-设计说明.md](./01-设计说明.md) —— 当前设计：API 面（21 类型）、ActorCell/Envelope 数据结构、SchedulingGate + wakeTickets 并发协议、执行循环、相位机、定时器、阻塞外包、指标、使用契约、测试锚点。

## 速查

- 模块：`kunpeng/scheduler`，包 `com.anyilanxin.kunpeng.scheduler`
- 消费方契约：单 actor 串行；actor 线程禁止阻塞 get()；close 后 submit 报 `"Actor is closed"`；外部队列上限 10000
- 排查启动链：看 `Startup <步骤名>` / `completed` / `still running after 30s` 日志三元组
