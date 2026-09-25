# sink 运行时架构参考

模块 `kunpeng:sink:sink`，包 `com.anyilanxin.kunpeng.sink.*`。依赖 sink-api 与 cluster/configuration/eventlog/protocol/repository/scheduler/structpack/utils（Jackson 3 用于配置绑定与 DebugLogSink）。

## 组件与协作

```
EventLog(日志流) ─▶ SinkService(actor, 每分区一个)
                     ├─ RecordDispatcher ──▶ SinkSlot ──▶ RecordSink(用户实现)
                     ├─ SinkSlot ...（每个已配置 Sink 一个）
                     ├─ PositionBroadcaster ──▶ PartitionMessagingService（leader 广播/follower 应用）
                     ├─ SinkMetrics（micrometer）
                     └─ RepositoryMutableSink（位置/元数据持久化，repository().sink()）
SinkRegistry（broker 启动时按 BrokerCfg.sinks 构建；className 加载或外部 jar）─▶ SinkDescriptor ─▶ SinkServiceContext
```

| 组件 | 职责 |
|---|---|
| `SinkService extends Actor` | 每分区的外发服务：读日志、分发、暂停/恢复、启用/禁用、leader/follower 角色、健康上报。控制 API：`startAsync(scheduler)/stopAsync/pause/softPause/resume/enableSink(WithRetry)/disableSink/getPhase/getLowestPosition` |
| `SinkSlot implements SinkController` | 单个 Sink 实例的簿记：committed/delivered 位置、软暂停缓冲（pendingPosition/pendingMetadata）、匹配器、在 Sink 类加载器上下文执行全部回调 |
| `RecordDispatcher` | 一条原始条目 → 复用的类型化记录 → 按槽位顺序分发；失败时记住续传点（nextSlot），重试从失败槽位继续，不重复投递已成功槽位。槽位列表变化后必须 `resetResumePoint()` |
| `PositionBroadcaster` + `SinkPositionsMessage` | leader 按 `broadcastInterval`（默认 15s）广播全量位置快照；follower 单调应用（只前进）。leader 切换后新 leader 从广播位置续传 |
| `SinkRegistry` | broker 配置/外部 jar → `SinkDescriptor`；登记前用抛弃型实例（`PARTITION_ID_UNSET` + `SimpleMeterRegistry`）跑一次 `initialize` 校验，校验后关闭该实例 |
| `SinkDescriptor` | id + 参数 + `SinkFactory`。**按 id 判等**——id 是 Sink 在集群与重启间的身份，参数允许变化 |
| `SinkFactory` / `ReflectSinkFactory` | 实例来源抽象：默认反射工厂只调无参构造器；自定义工厂适合依赖注入或按分区维护状态的 Sink |
| `SinkInitInfo` | 启用 Sink 时的状态初始化说明：`fresh()`（从头）或 `inheritFrom(旧id)`（继承前身 position/metadata）；`metadataVersion` 高于已存储版本时才重新初始化 |
| `SinkRuntimeContext` | `SinkContext` 实现：CompositeMeterRegistry 按 `partitionId + sinkId` 打标；matcher 缺省全放行（null 回退） |
| `SinkMetrics(+Doc)` | 全部指标（见下） |
| `DebugLogSink` | 内置调试 Sink：记录按 JSON 打日志，配置 logLevel/prettyPrint；`defaultSinkId()/defaultConfig()` 供引擎默认登记 |
| `SinkLoggers` | 日志命名空间 `com.anyilanxin.kunpeng.sink`，每 Sink 子日志器 `.<sinkId>` |
| `MapSinkConfig` | broker 配置参数的 `SinkConfiguration` 实现：宽松绑定（大小写不敏感、单引号、忽略未知属性、数字键 Map→List） |

## 记录流转与性能设计

- **drain 批处理**：单个 actor 任务内 while 循环连续投递，每 512 条（`DRAIN_BATCH_SIZE`）让出线程；分发成功走 retry-future `isDone()` 快路径内联继续，不入 actor 队列。
- **skip 内联**：被过滤记录直接在循环内确认（metrics 记 skipped，各槽位 `skipUpTo` 推进）。
- **过滤预计算**：全部槽位匹配器预先 OR 成 RecordType/ValueType/intent 三张布尔表（intent 枚举经 `ValueLifeCycle.INTENT_CLASSES` 展开），组装为 `EntryFilter` 在读取循环判流；无槽位时过滤器直接全拒。
- **对象复用**：RecordMetadata/TypedRecordReader/记录 value 实例跨记录复用；SBE 编解码零拷贝。
- **不可映射记录**：`RecordValueMapper` 找不到 lifecycle 对应的 value 缓存实例时该条目不投递、直接放行（dispatchable=false）。
- 失败处理：Sink 抛异常 → `BackOffRetryStrategy`（初始退避 10s）重试同一记录；解码异常 → `UnrecoverableException` → 服务失败（不可恢复）→ 关闭 actor + 健康报告（unhealthy/dead）+ 通知 FailureListener。
- Sink 全部被禁用 → 服务 idle（关读取器释放日志段、退订广播、取消广播定时器）；再启用 → `wakeUp` 重建对应角色模式。
- 启动时 `dropStateOfRemovedSinks`：持久化状态里已不在配置中的 Sink id，其位置记录一并删除。

## wire 协议（leader→follower 位置快照）

- 传输：`PartitionMessagingService`，topic `sink-positions-<partitionId>`。
- 编码：SBE，schema id=**5**、template id=**1**、version=**3**，littleEndian；消息 `SinkPositions`，重复组 `entry{position:uint64, sinkId:utf8变长, metadata:raw变长}`。
- **字节布局刻意与上一代实现保持一致**（schema 文件为独立编写的 `sink-protocol.xml`，见其头注），滚动升级期间新旧 broker 可继续互相解码位置快照。改 wire 布局前必须先解决兼容性。
- Java 侧编解码类由 schema 生成：`SinkPositionsEncoder/Decoder`，经 `SbeMessage` 基类处理 8 字节 SBE 消息头。

## 指标（命名空间 kunpeng.sink.*）

| meter | 类型 | 标签 | 含义 |
|---|---|---|---|
| `kunpeng.sink.records` | counter | action=delivered/skipped, valueType | 处理的记录数 |
| `kunpeng.sink.pickup.latency` | timer | valueType | 记录写入→被取起投递的时延 |
| `kunpeng.sink.process.duration` | timer | sink, valueType | 单个 Sink 处理单条记录耗时 |
| `kunpeng.sink.position.delivered` | gauge | sink | 已投递位置 |
| `kunpeng.sink.position.committed` | gauge | sink | 已提交（确认）位置 |
| `kunpeng.sink.phase` | gauge | - | 服务阶段：0 运行/1 暂停/2 软暂停/3 关闭 |

## broker 接入（尚未完成，接入时参考）

引擎侧需新写一个 partition transition step（旧 `ExporterProcessServiceTransitionStep` 已随 exporter 命名清理删除），用 `SinkServiceContext` 流式装配：

```java
new SinkService(
    new SinkServiceContext()
        .actorName(Actor.buildActorName("Sink", partitionId))
        .eventLog(eventLog)
        .repository(repository)
        .messaging(messagingService)
        .sinks(descriptorInitInfoMap)      // Map<SinkDescriptor, SinkInitInfo>
        .role(SinkRole.LEADER 或 FOLLOWER) // 按分区角色
        .broadcastInterval(Duration)       // 默认 15s
        .positionsToSkip(entryFilter)      // 跳过指定位置（如快照边界）
        .meterRegistry(partitionMeterRegistry)
        .clock(InstantSource.system()),
    初始 SinkPhase);
```

注意：与旧实现的差异——位置确认真实持久化（旧实现注释掉了持久化调用）；broker 配置装配用 `SinksConfig(enableDebugSink, SinkRegistry)`；broker 配置入口为 `BrokerCfg.sinks`（`Map<String, SinkCfg>`：className/jarPath/args）。

## sink-common 通用件

- `SinkEntityCache<K,V>`：Sink 解析流内引用（流程定义 key、父实例 id……）用的本地有界缓存抽象；`get/getOrLoad/put/invalidate/clear/estimatedSize/close`，实现需线程安全。
- `CaffeineEntityCache`：默认实现，容量淘汰（默认 10000 条，可传自定义 Caffeine 规格）；loader 返回 null 不缓存。
- `EnumLookup`：`resolve/resolveOrDefault/map`，忽略大小写、连字符、下划线、空格的枚举归一查找。
