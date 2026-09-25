---
name: sink-design
description: kunpeng engine sink design docs and dev guide. Covers the RecordSink SPI full lifecycle (initialize→start→sink⇄pause/resume→close), position acknowledgement and retry semantics, runtime architecture (SinkService/SinkSlot/dispatch/leader-follower position broadcast), SBE wire protocol and metrics. Use when implementing, reviewing or onboarding sinks for the kunpeng engine, querying the sink API usage, troubleshooting position acknowledgement or pause/resume behavior, or wiring the sink service into the broker.
---

# Kunpeng Sink（日志外发）设计

kunpeng 引擎的日志外发体系，位于仓库 `kunpeng/sink/`。三个模块：

| 模块 | 包 | 协议 | 职责 |
|---|---|---|---|
| `sink-api` | `com.anyilanxin.kunpeng.sink.api` | MPL-2.0 | 用户 SPI：实现 `RecordSink` 即可消费引擎记录 |
| `sink-common` | `...sink.common` | AGPL | 通用件：Caffeine 实体缓存、枚举归一查找 |
| `sink` | `...sink.{runtime,registry,metrics,protocol,debug,config}` | AGPL | 引擎侧外发服务（actor） |

设计背景：整套代码为 clean-room 原创（替代含 Zeebe 衍生代码的旧 exporter 实现，后整体更名为 sink）；wire 字节布局刻意与上一代保持一致，以便滚动升级期间新旧 broker 互相解码位置快照。

## 生命周期（核心）

### 用户侧 RecordSink

```
initialize(SinkContext)   初始化/校验配置
        │
        ▼
start(SinkController)     分配资源，拿到确认句柄
        │
        ▼
sink(Record) ◀─────┐      按日志顺序逐条投递
        │           │
     pause() ─恢复─▶ resume()  引擎级暂停/恢复，可反复多次
        │
        ▼
close()                     释放资源，终态
（按需）purge() / onPartitionRemoved()
```

关键语义：

- **`initialize` 会被调用两次、在不同实例上**：第一次在 `SinkRegistry` 登记校验时（抛弃型实例，分区 id 为 `PARTITION_ID_UNSET`，校验完即 close），第二次在真实实例参与外发前。抛异常 = 拒绝登记/启动失败。因此不要在 `initialize` 分配重型资源，留给 `start`。
- **`sink` 失败即重试**：正常返回 = 成功；抛异常 = 同一条记录按退避重试直到成功或服务关闭。绝不丢数据——代价是持续失败会拖住日志压缩。
- **`pause`/`resume` 只由硬暂停触发**；软暂停（继续投递、仅缓冲位置确认）对 Sink 不可见。回调抛异常会使对应的暂停/恢复操作失败。
- **`close` 后不再有任何回调**；`purge`/`onPartitionRemoved` 要求幂等。
- 传入的 `Record` 包装引擎复用内存；跨调用持有需 `Record#copyOf()`。

### 引擎侧 SinkService（actor）

阶段状态机：

```
RUNNING ──pause──▶ PAUSED ──resume──▶ RUNNING
    │                                        ▲
    └──softPause──▶ SOFT_PAUSED ──resume─────┘
（任一阶段服务关闭后进入终态 CLOSED）
```

启动：`startAsync(scheduler)` → `onActorStarting`（leader 建日志读取器）→ `onActorStarted`（建广播器、逐槽位 init + `initializeSink`、清理已移除 Sink 的残留持久化状态；leader 逐槽位 loadPersistedState + startSink（带重试）→ 从最低已提交位置 seek → 注册记录监听 + 周期广播 → drain；follower 只 loadPersistedState + 订阅广播）。
关闭：`stopAsync` → `onActorCloseRequested`（started=false、关全部槽位即用户 close、关广播器）→ `onActorClosing`（关读取器、注销监听）→ `onActorClosed`（CLOSED 终态）。

服务阶段与用户回调的对应：initSlots/enable → `initialize`；leader 打开槽位 → `start`；drain → `sink`；硬暂停/恢复 → `pause`/`resume`；关闭/禁用 → `close`。

## 位置确认语义

- 确认走 `SinkController.updatePosition(position[, metadata])`，**单调递增**，任意线程可调用，一律转到 actor 线程执行。
- 位置+元数据持久化到 repository（`RepositoryMutableSink`）；重启后从最后确认位置续传（不重复投递）。
- 软暂停期间确认只缓冲（pending），恢复时一并写回。
- 引擎跳过不需要的记录时替 Sink 推进位置，但**绝不越过在途记录**（守卫：`committedPosition >= lastDeliveredPosition` 才允许 skip 推进）。
- `getLowestPosition()`（全部 Sink 最低已提交位置）是日志压缩门槛；从不确认的 Sink 会永久阻塞压缩。
- 新 Sink 可用 `SinkInitInfo.inheritFrom(旧id)` 继承前身的 position/metadata。

## 快速开始：实现一个 Sink

```java
public final class MySink implements RecordSink {

  private SinkController controller;
  private MySettings settings;          // 自动绑定 yaml 里的 args

  @Override
  public void initialize(final SinkContext context) {
    settings = context.getConfiguration().createSettings(MySettings.class);
    // 校验配置；重型资源留给 start
    context.setRecordMatcher(new RecordMatcher() {      // 可选：过滤，减少投递
      @Override public boolean acceptsRecordType(RecordType t) { return t == RecordType.EVENT; }
      @Override public boolean acceptsValueType(ValueType v) { return v == ValueType.PROCESS_INSTANCE; }
    });
  }

  @Override
  public void start(final SinkController controller) {
    this.controller = controller;
    // 连接目标系统；可读取 controller.readMetadata() 恢复上次检查点
  }

  @Override
  public void sink(final Record<?> record) {
    // 写入目标系统；批量攒批的话，定期 controller.updatePosition(pos, checkpoint)
  }

  @Override
  public void pause() { flush(); }        // 硬暂停时刷出缓冲

  @Override
  public void resume() { /* 重建 pause 释放的资源 */ }

  @Override
  public void close() { flush(); disconnect(); }
}
```

broker 配置注册（`BrokerCfg.sinks` 为 `Map<String, SinkCfg>`，key 即 Sink id；`className` 在 classpath 上，或配 `jarPath` 走外部 jar）：

```yaml
sinks:
  my-sink:
    className: com.example.MySink
    args:
      endpoint: http://...
      flushInterval: 5s
```

配置绑定是宽松的：属性名/枚举值大小写不敏感、单引号可用、未知属性忽略、数字键 Map 自动转 List（`{"0":a,"1":b}` → List）。

## 参考文档（按需阅读）

- **[spi.md](./spi.md)** — SPI 完整签名：RecordSink、SinkContext、SinkController、RecordMatcher、SinkConfiguration、CancellableTask、SinkException 的全部方法与语义细节。
- **[runtime.md](./runtime.md)** — 运行时架构：SinkService/SinkSlot/RecordDispatcher/PositionBroadcaster/SinkRegistry/SinkFactory 职责与协作、drain 批处理性能设计、leader/follower 位置广播与 SBE wire 协议（schema id 5、topic 命名）、指标清单、DebugLogSink。

## 硬性约定速查

- 必须提供公有无参构造器；除回调外不要依赖构造器做事。
- 生命周期顺序不可假设 `sink` 与 `pause` 交错之外的任何并发——所有回调都在 Sink 线程串行执行。
- 位置只能前进；旧位置的确认会被忽略。
- 指标经 `context.getMeterRegistry()`（已按 sinkId+partition 打标）。
