# sink 滞后闸门（SinkBackpressureGate）详细设计

> 配图：[sink-backpressure-flow.svg](images/sink-backpressure-flow.svg)（业务面闭环）· [hysteresis.svg](images/hysteresis.svg)（双阈值迟滞）
> 状态：设计定稿，未实施。管理面不设此闸门，范围仅业务面。

## 动机：两个不同的问题

1. **可见性问题（读写一致性）**：sink 是异步最终一致投影，"我的申请"类查询以 sink 出的数据为准时，提交后必然存在不可见窗口（≈ 积压量 ÷ sink 排空速率）。闸门把它变成"过载时提交被明确拒绝"，但**读路径的根治**靠业务侧"提交即落库"（申请单权威记录写在业务库事务里，sink 数据只做进度/审计），不是引擎侧手段。
2. **系统生存问题**：sink 滞后会卡死日志压缩——压缩门槛 `getCompactionFloorPosition()` 就是全部 sink 的最低确认位置，sink 堵 = raft journal 无法压缩 = 磁盘与恢复时间无界增长。所以本闸门无论读路径怎么改都必须有。

阈值换算公式：`rejectBacklog ≈ 可接受不可见时长 × sink 排空速率`（例：SLA 10s、sink 稳态 2,000 events/s → 20,000）。

## 信号定义（每分区）

```
lag = lastWrittenPosition − lowestSinkPosition
```

- `lastWrittenPosition`：`EventLog.getFlowControl().lastWrittenPosition()`（`kunpeng/eventlog/.../impl/flow/FlowController.java:167`，原子读），每次准入实时读。
- `lowestSinkPosition`：`SinkRepository.getLowestPosition()`（`kunpeng/repository/business-repository/.../modules/sink/SinkRepository.java:106`，遍历全部 sink 状态取 min）。闸门内 volatile + 短 TTL（~250ms）读穿透缓存，避免每条命令打 RocksDB。
- 与日志压缩门槛**同一权威口径**（`PositionSupplierTransitionStep.java:79` 的 supplier 同源）。
- **无 sink 接入 → 闸门不存在**：`getLowestPosition()` 返回 −1（未配置任何 sink）时闸门常开。运行时判定，非配置开关，与压缩门槛「−1 = 无约束」同语义。

## 双阈值迟滞状态机

| 状态 | 进入条件 | 行为 |
|---|---|---|
| 放行（初始） | — | 正常准入 |
| 拒绝 | `lag ≥ rejectBacklog`（默认 50,000） | 业务面 USER_COMMAND 全部回 `code=2` 失败应答 |
| 恢复放行 | 拒绝状态下 `lag ≤ resumeBacklog`（默认 40,000） | 两阈值之间**维持原状态**，防边界抖动 |

约束：`resumeBacklog < rejectBacklog`，装配时校验。`enabled=false`（配置）= 闸门不存在。

## 行为细节

- 检查位置：`CommandApiHandleImpl.handleRequest` 内，磁盘水位检查同款位置（`kunpeng/broker/broker/.../commandapi/CommandApiHandleImpl.java:160`），超限即 `errorResponse(2, ...)` 立即回拒，走现有统一失败应答路径（不悬挂、不等超时）。
- 只拦业务面 USER_COMMAND 入口；引擎 INTERNAL 写入、raft 复制、sink 排空不受限——下游消化不停，lag 单调下降，闸门必然重开（闭环收敛）。
- job 完成等命令同样会被拒，worker 侧重试即可（标准背压语义，上游 Zeebe 同款）。
- 每分区一个闸门实例（lag 是分区维度），装配于 `CommandApiServiceTransitionStep` 注册处理器时（`:40`），supplier 经 `BusinessTransitionContent` 获取。

## 9 点位改造清单（均已在当前工作区核实）

| # | 点位 | 位置 | 改动 |
|---|---|---|---|
| 1 | 新建闸门类 | broker `commandapi` 包 `SinkBackpressureGate` | 双阈值迟滞状态机；volatile + TTL（~250ms）读穿透缓存 lowestSinkPosition |
| 2 | 准入检查 | `CommandApiHandleImpl.java:160`（磁盘检查同款位置） | 超阈值即 `errorResponse(2, ...)` |
| 3 | 错误码集中 | broker-client core，如 `BrokerResponseCodes` | 收拢 `APPEND_REJECTED=1`（现两处局部常量），新增 `SINK_BACKPRESSURE=2` |
| 4 | 装配 | `CommandApiServiceTransitionStep.java:40`（registerHandlers 处） | 传入两个 `LongSupplier`（written 位来自 `EventLog.getFlowControl()`；sink 最低位与 `PositionSupplierTransitionStep` 同源）+ 闸门配置 |
| 5 | 配置 | `FlowControlCfg`（kunpeng/configuration）加 `sinkBackpressure` 字段；新增 `broker/backpressure/SinkBackpressureCfg.java`（enabled / rejectBacklog / resumeBacklog） | 用户自行启停与调参 |
| 6 | gRPC 映射 | `GrpcErrorHandle.java:56` | code=2 → `RESOURCE_EXHAUSTED`；顺带把 code=1 纠正为 `RESOURCE_EXHAUSTED`（它本就是流控拒绝） |
| 7 | REST 映射 | 遵守 HTTP 200 + `Result.fail(code)` | `BrokerCall`/`GlobalRestExceptionHandler` code 透传，无需结构改动 |
| 8 | 客户端 SDK（可选后置） | client-java 错误映射 | code=2 → 专用可重试异常 |
| 9 | 指标 + 面板 | 闸门状态 gauge + 拒绝计数（`kunpeng.commandapi.backpressure.*`） | 按 monitor-dashboards 铁律连带补面板，check-panels.py 覆盖归零 |

wire 层与协议编号池零改动（错误码走 `BrokerResponse.code` int 通道）。

## 量化示例（数字为假设示例，实测定参）

sink 稳态 2,000 events/s、写入 5,000 events/s → 积压每秒 +3,000；`rejectBacklog=50,000` ≈ 过载 17s 后开始拒绝。业务侧感知从"提交成功但 10 分钟查不到"变为"提交被拒、几秒后重试即恢复"。

## 测试要点

- 迟滞状态机单测：跨 reject 转 reject、带内维持、跨 resume 恢复；`enabled=false` 常开；无 sink（−1）常开；TTL 缓存到期刷新。
- 装配单测：supplier 注入、`resumeBacklog ≥ rejectBacklog` 拒绝启动。
- 拒绝应答路径：code=2 立即回包、请求登记清理（不悬挂）。
- 回归：AIMD 窗口行为不受影响（两者独立）。

## 配套但独立的后续项

- **sink 排空速率提升**（rdbms sink 批量写、RecordMatcher 过滤减量）：提高天花板，持续超载时本闸门仍不可替代。
- **业务侧提交即落库**：可见性的正解，零引擎改动，业务架构决定。
- 引擎侧直查（RocksDB 强一致读 + 查询面）：与读模型分离的架构定案相悖，除非有强一致查询刚需否则不做。
