# sink SPI 完整参考

包：`com.anyilanxin.kunpeng.sink.api`（模块 `kunpeng:sink:sink-api`，MPL-2.0）。
依赖仅需 protocol（Record/RecordType/ValueType/ValueLifeCycle）与 micrometer；实现者只需引入本模块。

## RecordSink（用户实现的主接口）

```java
public interface RecordSink {
  default void initialize(SinkContext context) throws Exception {}
  default void start(SinkController controller) {}
  void sink(Record<?> record);
  default void pause() {}
  default void resume() {}
  default void close() {}
  default void purge() throws Exception {}
  default void onPartitionRemoved() throws Exception {}
}
```

| 方法 | 调用时机 | 语义与注意 |
|---|---|---|
| `initialize` | ① 登记校验（抛弃型实例，`PARTITION_ID_UNSET`，校验完实例即被 close）；② 真实实例启动/被启用时 | 校验与安装 `RecordMatcher`；抛异常阻止登记/启动。两次调用在不同实例上，勿分配重型资源 |
| `start` | 第一条记录投递前一次（leader 上带退避重试直到成功） | 分配资源；可 `controller.readMetadata()` 恢复检查点 |
| `sink` | 每条命中过滤的记录，日志顺序 | 正常返回=成功；抛异常=同一条记录退避重试。记录内存复用，跨调用持有需 `copyOf()` |
| `pause` | 引擎级暂停后（不再有 sink 调用） | 可刷缓冲/释放独占资源；抛异常使暂停操作失败。软暂停不触发 |
| `resume` | 硬暂停恢复时，先于下一条 sink | 重建 pause 释放的资源；抛异常使恢复操作失败 |
| `close` | 服务关闭/Sink 被禁用时一次 | 之后不再有任何回调 |
| `purge` | 集群清理操作 | 删除本 Sink 已写出的全部数据；必须幂等，可能被重试 |
| `onPartitionRemoved` | 分区从集群移除 | 清理仅属于该分区的数据；必须幂等 |

实现必须提供公有无参构造器。

## SinkContext（initialize 的参数）

```java
public interface SinkContext {
  MeterRegistry getMeterRegistry();      // 已按 sinkId+partition 打标
  Logger getLogger();                    // 以 Sink id 命名的专属日志器（com.anyilanxin.kunpeng.sink.<id>）
  InstantSource getClock();              // 引擎时间源，测试可控
  SinkConfiguration getConfiguration();
  int getPartitionId();                  // 校验阶段为 PARTITION_ID_UNSET
  int PARTITION_ID_UNSET = Integer.MIN_VALUE;
  void setRecordMatcher(RecordMatcher);  // 可重复调用替换；传 null 回退为全放行
}
```

## RecordMatcher（记录过滤）

```java
public interface RecordMatcher {
  boolean acceptsRecordType(RecordType recordType);   // COMMAND/EVENT/...
  boolean acceptsValueType(ValueType valueType);      // PROCESS_INSTANCE/JOB/...
  default boolean acceptsIntent(ValueLifeCycle intent) { return true; }
}
```

三个条件同时满足才投递。被拒记录由引擎直接确认（Sink 侧零开销）；规则修改只影响之后的记录，已确认记录不会重新投递。引擎会把全部 Sink 的匹配器预计算成 RecordType/ValueType/intent 三张布尔表，在日志读取循环就完成过滤。

## SinkController（start 的参数，确认句柄）

```java
public interface SinkController {
  void updatePosition(long position);
  void updatePosition(long position, byte[] metadata);
  long getPosition();
  CancellableTask scheduleTask(Duration delay, Runnable task);
  Optional<byte[]> readMetadata();
}
```

- `updatePosition`：确认到该位置（含）为止全部处理完成；**单调递增**，回退被忽略；一律转到 actor 线程执行（任意线程可调用）。带 metadata 的重载把检查点随位置持久化，重启后 `readMetadata()` 可取回。
- `scheduleTask`：延时任务，Sink 线程执行；关闭时未执行的定时器被丢弃。
- 从不确认 = 阻塞日志压缩（数据不丢，但磁盘不释放）。

## SinkConfiguration（配置视图）

```java
public interface SinkConfiguration {
  String getId();
  Map<String, Object> getArguments();
  <T> T createSettings(Class<T> settingsClass);
}
```

`createSettings` 宽松绑定：属性名/枚举值大小写不敏感、单引号可用、未知属性忽略、数字键 Map→List。无参数时返回默认实例。

## CancellableTask / SinkException

```java
public interface CancellableTask { void cancel(); }   // 重复取消是空操作

public class SinkException extends RuntimeException { ... }  // 运行时基础异常
```

## 运行时对 SPI 的保证与限制

- **串行**：同一 Sink 实例的所有回调串行执行，无并发。
- **顺序**：sink 严格按日志位置递增；失败记录重试成功前不投递下一条。
- **线程上下文**：外部 jar 加载的 Sink，全部回调在其自身类加载器上下文中执行。
- **重复投递边界**：至多一次投递到确认位置；未确认部分在重启后重投。sink 成功但尚未确认的记录在崩溃后会再来一次——目标系统需按位置幂等或以确认点为界。
