---
name: eventlog-design
description: Use when working with kunpeng/eventlog, the engine's partition-level event log — understanding the append pipeline (lock-free position sequencer, pending queue, AIMD flow control), the "EL" batch frame format, reader/seek semantics, the EventStore SPI bridging to Raft, crash recovery gaps, or checking performance baselines
---

# eventlog 模块（事件日志流）

`kunpeng/eventlog`（事件日志流：定序、批帧编解码、流控、拉读、commit 通知、恢复）的设计知识库。

## 文档

- [01-eventlog-design.md](./01-eventlog-design.md) —— 设计目标与总体架构；EL 批帧 v1 格式（varint，§3.3 字节布局实例可手工复算）；无锁定序器（CAS 预约 + watermark 提交链 + 5s 看门狗烧毁）；三态水位 + AIMD 窗口/令牌桶/积压调节流控；读路径 seek 族语义与 gap 容忍；EventStore SPI（Raft 桥接点，cluster 模块零改动）；风险与验证基线
- [02-eventlog-performance-estimate.md](./02-eventlog-performance-estimate.md) —— 写/读路径逐操作成本核算、帧体积逐字节账目（2 条目批 134B 确定性）、多写者可并行区分析、流控热路径 O(1) 常数、实测基线留档（2026-08-17）

## 速查

- 模块：`kunpeng/eventlog`，包 `com.anyilanxin.kunpeng.eventlog`；仅依赖 structpack/agrona/slf4j/micrometer
- 帧：magic `45 4C`（"EL"，structpack 的 "KP" 已占）+ version + varint 批帧；条目 position = `firstPosition + 批内下标`；sourcePosition 支持批内回指（sourceIndex）
- 写路径：流控准入 → `getAndAdd` CAS 预约 position → PendingAppendQueue（64 槽）fulfill → drain 按 firstPosition 升序 `store.append`；失败/超期区间烧毁产生 gap，position 只进不退
- 流控：InflightRing（1024 槽，O(1) 记账）三态 APPENDING→WRITTEN→COMMITTED；AIMD 窗口仅约束 USER_COMMAND；`FlowControlParams.disabled()` 一键退化
- 桥接：broker 侧 `RaftEventStore/RaftEventStoreReader/RaftAppendListenerAdapter` 实现 EventStore SPI；first/lastPosition 映射 `SerializedApplicationEntry` 的 lowest/highestPosition
- 依赖护栏：`DependencyGuardTest` 保证不引入 protocol-impl；指标前缀 `eventlog.*`
- 排查：`./gradlew :kunpeng:eventlog:test`
- 本技能为设计文档唯一副本（原 `kunpeng/eventlog/docs/` 归档后已删除，修改设计直接改本技能）
