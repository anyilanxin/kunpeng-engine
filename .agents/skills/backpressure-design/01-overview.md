# 全系统背压控流总览

> 配图：[overview.svg](images/overview.svg)（三道闸门 + 信号源 + 配置注入面 + 排空闭环 + 拒绝错误链）

## 总体模型

业务面写路径是一条**闭环反馈控制**：

```
写入侧（准入三闸门）→ eventlog/raft → 引擎处理（不受限）→ sink 排空（不受限）
        ▲                                                    │
        └────────── lag 信号（sink 确认位置前移）←────────────┘
```

- 拦截面只有一个：broker 业务面命令入口 `CommandApiHandleImpl.handleRequest`（actor 线程、每分区）。
- 拒绝是**快速失败**：走统一 `errorResponse(code)` 应答路径，调用方立即拿到可重试错误。
- 排空侧永不受限，因此积压必然单调消化、闸门自动重开——闭环收敛有结构保证。

## 控制点 ①：磁盘水位闸门（现有）

| 项 | 内容 |
|---|---|
| 检查位置 | `CommandApiHandleImpl.handleRequest` 内 `isDiskSpaceAvailable`（`kunpeng/broker/broker/.../commandapi/CommandApiHandleImpl.java:160`） |
| 信号源 | `DiskSpaceUsageListener` 周期监测（间隔 `disk.monitoringInterval`，默认 1s），经 `CommandApiServiceImpl.onDiskSpaceNotAvailable/Available` 转入 actor |
| 拒绝行为 | `RuntimeException("Broker is out of disk space")`，请求立即失败 |
| 配置 | `DiskCfg`（`kunpeng/configuration/.../broker/DiskCfg.java`）：`enableMonitoring`（默认 true；**false 时阈值置 0 = 永不触发**）、`freeSpace.processing`（默认 2GB）、`freeSpace.replication`（默认 1GB） |
| 已知边界 | 管理面 `AdminCommandApiHandleImpl` 的同名字段是写死 `true` 的退化版，不感知磁盘 |

## 控制点 ③：AIMD 在途窗口（现有）

| 项 | 内容 |
|---|---|
| 实现 | eventlog 模块 `FlowController` / `AimdWindow` / `InflightRing`（`kunpeng/eventlog/.../impl/flow/`） |
| 约束范围 | 仅 `WriteContext.USER_COMMAND`；引擎 `INTERNAL`、跨分区、调度写入直接放行 |
| 窗口释放 | 由**处理进度**驱动：`EngineProcessService` / `DispatchProcessService` 处理完调用 `onProcessed(position)` 采样 RTT 并释放占位——与 sink 进度无关 |
| 拒绝行为 | `AppendResult.Rejected(REQUEST_WINDOW_EXHAUSTED)` → broker 应答 `ERROR_CODE_APPEND_REJECTED = 1`（局部常量，`CommandApiHandleImpl.java:65`） |
| 配置 | `FlowControlCfg.request`（`LimitCfg`，`kunpeng/configuration/.../broker/backpressure/LimitCfg.java`）：`enabled`（默认 true，**false → 窗口退化 Integer.MAX_VALUE**）、`algorithm`（AIMD/FIXED）、`aimd.{initialLimit, minLimit, maxLimit}` |
| 装配映射 | `BusinessLogStreamPartitionTransitionStep.toParams`（`:123-146`）→ eventlog `FlowControlParams` |
| 注意 | `FlowControlCfg.write`（`RateLimitCfg`）配置面存在但 eventlog **未消费**，是预留项，不算生效闸门；eventlog 设计文档中的 `BacklogThrottle`/`TokenBucket` 仅存在于文档，代码未实现 |

## 控制点 ②：sink 滞后闸门（设计定稿，未实施）

→ 完整设计见 [02-sink-backpressure.md](./02-sink-backpressure.md)。要点：信号 `lag = lastWrittenPosition − lowestSinkPosition`，双阈值迟滞（reject 50k / resume 40k，均可配），无 sink（−1）时闸门不存在。

## 配置总表（kunpeng/configuration → BrokerCfg）

```yaml
kunpeng:
  broker:
    flowControl:
      request:                      # 控制点③ AIMD 在途窗口（LimitCfg）
        enabled: true               # false → 窗口退化（Integer.MAX_VALUE），闸门不存在
        algorithm: AIMD             # AIMD / FIXED
        aimd:
          initialLimit: 100
          minLimit: 10
          maxLimit: 1000
      # write: ...                  # 预留（RateLimitCfg，eventlog 未消费）
      sinkBackpressure:             # 控制点② sink 滞后闸门（新增 SinkBackpressureCfg）
        enabled: true               # false → 闸门不存在
        rejectBacklog: 50000        # lag ≥ 此值 → 拒绝（进入拒绝态）
        resumeBacklog: 40000        # lag ≤ 此值 → 恢复放行（须 < rejectBacklog）
    disk:                           # 控制点① 磁盘水位（DiskCfg）
      enableMonitoring: true        # false → 阈值置 0，永不触发
      monitoringInterval: 1s
      freeSpace:
        processing: 2GB             # 处理所需最小空闲
        replication: 1GB            # 复制所需最小空闲
```

| 配置组 | 配置类（kunpeng/configuration） | 挂载点 |
|---|---|---|
| `disk.*` | `broker/DiskCfg.java`（含 `FreeSpaceCfg`） | `BrokerCfg.disk` |
| `flowControl.request` | `broker/backpressure/LimitCfg.java`（含 AIMDCfg 等） | `BrokerCfg.flowControl.request` |
| `flowControl.sinkBackpressure`（新增） | `broker/backpressure/SinkBackpressureCfg.java`（新增，落 backpressure 子包） | `BrokerCfg.flowControl.sinkBackpressure`（FlowControlCfg 加字段） |

配置注入约束：新增 `sinkBackpressure` 后，装配侧（`CommandApiServiceTransitionStep` 注册时经 `BusinessTransitionContent`）负责把配置连同两个位置 supplier 传给闸门；`toParams` 映射保持 eventlog 参数与 sink 闸门参数**各自独立**，不塞进同一个 `FlowControlParams`。

## 错误码与网关映射

- broker 传输级错误码是 `BrokerResponse.code`（int），**不是** `RejectionType`（那是记录级业务拒绝枚举，两套体系不要混用）：
  - `1 = APPEND_REJECTED`：现为实现里的局部常量，收拢到 broker-client 共享常量类（gateway/broker 都要读）
  - `2 = SINK_BACKPRESSURE`：新增
- wire 层零改动：错误码走现有 int 通道，不碰 SBE、不碰协议编号池。
- gRPC：`GrpcErrorHandle.error(StreamObserver, BrokerResponse)`（`gateway-grpc/.../GrpcErrorHandle.java:56`）现状一律 `FAILED_PRECONDITION`；目标行为：code 1/2 → `RESOURCE_EXHAUSTED`（标准可重试语义）。
- REST：遵守现有约定 HTTP 200 + `Result.fail(code)`（`GlobalRestExceptionHandler` / `BrokerCall` 透传 code），业务侧按 code 识别可重试。
- 客户端 SDK（可选后置）：code=2 / `RESOURCE_EXHAUSTED` → 专用可重试异常类型。

## 指标与面板

- lag 观测已有现成面板：monitor/grafana `kunpeng.json`「外发积压」= `eventlog_position_written − kunpeng_sink_position_delivered`（clamp ≥ 0），无需重复建。
- 新增闸门指标必须连带补面板（monitor-dashboards 铁律）：闸门状态 gauge（放行/拒绝）+ 拒绝计数（按分区打标），验收以 check-panels.py 覆盖归零为准。

## 与相邻技能的边界

- eventlog 内部 AIMD 细节（三态水位、InflightRing、看门狗）→ `eventlog-design`
- sink 位置确认 / 压缩门槛语义 → `sink-design`
- 本技能只管**准入控制的设计与配置面**：三道闸门、闭环模型、错误语义
