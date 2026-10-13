---
name: backpressure-design
description: System-wide backpressure and flow-control design for the business write path (disk watermark gate, AIMD append window, sink-lag backlog gate), the drain closed-loop model, gateway error semantics (RESOURCE_EXHAUSTED / Result.fail), and the configuration surface where every threshold and enable/disable switch lives in kunpeng/configuration. Use when changing or tuning backpressure/rate-limiting behavior, adding admission gates, or diagnosing rejected commands and sink backlog.
---

# Skill: backpressure-design（全系统背压控流设计）

kunpeng 业务面写路径的准入控制体系。设计立场：下游（磁盘、处理、sink 外发）过载时**快速拒绝**新命令，而不是接受后静默堆积——保护 raft journal 的可压缩性与恢复时间，把"提交成功却长时间查不到"变成"提交被拒、退避重试"。

## 设计图（全部 SVG，不用文字流程图）

| 图 | 文件 | 内容 |
|---|---|---|
| 全系统控制点总览 | [overview.svg](images/overview.svg) | 三道闸门 + 信号源 + 配置注入面 + 排空闭环 + 拒绝错误链 |
| 业务面闭环详图 | [sink-backpressure-flow.svg](images/sink-backpressure-flow.svg) | 调用方 → gateway → broker 准入链 → 处理 → sink 排空 → 闸门重开的完整闭环 |
| 双阈值迟滞 | [hysteresis.svg](images/hysteresis.svg) | lag 时间线与闸门状态翻转（防边界抖动） |

## 三道准入闸门（业务面 USER_COMMAND，按序经过）

| # | 闸门 | 信号 | 拒绝条件 | 配置（均可启停） | 状态 |
|---|---|---|---|---|---|
| ① | 磁盘水位 | 剩余空闲空间（周期监测） | 低于 freeSpace 阈值 | `disk.enableMonitoring` + `disk.freeSpace.*` | 现有 |
| ② | sink 滞后 | `lag = lastWrittenPosition − lowestSinkPosition` | `lag ≥ rejectBacklog`（迟滞恢复） | `flowControl.sinkBackpressure.*` | 设计定稿，未实施 |
| ③ | AIMD 在途窗口 | 在途 USER_COMMAND 数 + 处理 RTT | 窗口耗尽 | `flowControl.request.enabled` + `aimd.*` | 现有 |

## 硬语义（不可违背）

1. **无 sink 接入 → sink 侧背压不存在**：`SinkRepository.getLowestPosition()` 返回 −1（未配置任何 sink）时闸门常开。这是**运行时判定**，不是配置开关；与日志压缩门槛「−1 = 无约束」同语义。
2. **全部阈值可配、每项可启停**：三道闸门的阈值与 `enabled` 都在 `kunpeng/configuration` 模块（`DiskCfg` / `FlowControlCfg.request` / `FlowControlCfg.sinkBackpressure`）。禁用的语义是"该闸门不存在"，不是"放宽"。
3. **只拦业务面 USER_COMMAND 入口**：引擎 INTERNAL 写入、raft 复制、sink 排空永不受限——下游消化不停，闭环必然收敛。管理面不设 sink 闸门。
4. **拒绝 = 快速失败可重试**：统一 `errorResponse(code)` 立即应答；gateway 映射 gRPC `RESOURCE_EXHAUSTED` / REST `Result.fail(code)`，客户端退避重试。
5. **双阈值迟滞**：`resumeBacklog < rejectBacklog`，两阈值之间维持当前状态，防边界抖动。

## 参考文档

- **[01-overview.md](./01-overview.md)** — 全系统总览：各控制点落点与行为、配置总表（含 yaml 示例与配置类位置）、错误码与网关映射、指标与面板要求
- **[02-sink-backpressure.md](./02-sink-backpressure.md)** — sink 滞后闸门详细设计：动机、信号定义、迟滞状态机、9 点位改造清单（file:line）、量化与测试要点

## 何时读本技能

- 改动 / 调优背压与流控行为，或新增准入闸门
- 排查命令被拒（`RESOURCE_EXHAUSTED` / append rejected / 磁盘水位）
- 排查 sink 堆积、journal 无法压缩
- 给 `kunpeng/configuration` 新增流控配置项
