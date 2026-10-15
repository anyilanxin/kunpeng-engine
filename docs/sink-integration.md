# Sink 对接指南

本文面向要给 kunpeng 引擎实现日志外发 Sink 的开发者：如何实现 SPI、如何正确打包为 **uber jar**、如何在 broker 上注册启用。参考实现见 [sinks/sink-rdbms](../sinks/sink-rdbms/)（RDBMS 落库）与引擎内置的 `DebugLogSink`。

## 1. 打包要求（最重要）

### 必须打包为 uber jar

外部 Sink 以**独立 jar** 形式被 broker 加载（`jarPath` 配置），运行在专属类加载器中。它必须是一个 **uber jar**（也叫 fat jar / JAR with dependencies）——你自己编译的类和所有第三方依赖**打进同一个 jar**，否则运行期会找不到依赖类。

使用 [`com.gradleup.shadow`](https://gradleup.com/shadow/) 插件打包，`build.gradle` 骨架（对照 [sinks/sink-rdbms/build.gradle](../sinks/sink-rdbms/build.gradle)）：

```groovy
plugins {
    id 'com.gradleup.shadow'
}

dependencies {
    // 引擎运行期已提供的包：只参与编译，绝不打进 uber jar
    compileOnly project(':kunpeng:sink:sink-api')
    compileOnly project(':kunpeng:bpm-model:bpm-common')
    compileOnly project(':kunpeng:protocol:protocol-business')
    compileOnly project(':kunpeng:protocol:protocol-common')
    compileOnly 'tools.jackson.core:jackson-databind'
    compileOnly 'io.micrometer:micrometer-core'

    // 你自己的依赖（数据库驱动、连接池、HTTP 客户端等）正常声明，会被打进 uber jar
    implementation 'com.alibaba:druid'
    implementation 'org.mybatis:mybatis'
    // ...
}

shadowJar {
    // 默认配置即可；需要排除/重定位时在此配置
}
```

独立仓库（不在本 monorepo 内）实现时，把 `compileOnly project(...)` 换成对应 Maven 坐标（group `com.anyilanxin.kunpeng`，如 `kunpeng-sink-api`、`kunpeng-protocol-business` 等）。

构建命令：

```bash
./gradlew shadowJar      # 产物在 build/libs/ 下，带 -all 后缀（或按 archiveName 配置）
```

### 为什么引擎的包必须 compileOnly

| 后果 | 原因 |
|------|------|
| 类冲突 / 版本错位 | 引擎 classpath 上已有这些包；uber jar 再带一份，两个类加载器各见一套，转型失败、静态状态分裂 |
| 体积膨胀 | jackson、micrometer 等体积不小，重复打包毫无收益 |
| 升级脱钩 | compileOnly 保证你只依赖引擎 SPI 的编译期签名，引擎升级这些依赖不需要重发你的 jar |

**判断标准：凡是上表 compileOnly 清单里的坐标，一律不进 uber jar；其余依赖全部打进去。**

## 2. 实现 RecordSink SPI

```java
public final class MySink implements RecordSink {

  private SinkController controller;
  private MySettings settings;          // 自动绑定 yaml 里的 args

  @Override
  public void initialize(final SinkContext context) {
    settings = context.getConfiguration().createSettings(MySettings.class);
    // 只做配置校验；重型资源（连接池等）留给 start
    context.setRecordMatcher(new RecordMatcher() {      // 可选：过滤，减少投递
      @Override public boolean acceptsRecordType(final RecordType t) { return t == RecordType.EVENT; }
      @Override public boolean acceptsValueType(final ValueType v) { return v == ValueType.PROCESS_INSTANCE; }
    });
  }

  @Override
  public void start(final SinkController controller) {
    this.controller = controller;
    // 建立连接；可用 controller.readMetadata() 恢复上次检查点
  }

  @Override
  public void sink(final Record<?> record) {
    // 写目标系统；批量攒批的话，定期 controller.updatePosition(pos, checkpoint)
  }

  @Override
  public void pause() { flush(); }      // 硬暂停时刷出缓冲

  @Override
  public void resume() { /* 重建 pause 释放的资源 */ }

  @Override
  public void close() { flush(); disconnect(); }
}
```

### 生命周期硬约束

- **必须提供公有无参构造器**；除回调外不要依赖构造器做事。
- **`initialize` 会被调用两次、在不同实例上**：第一次在注册校验时（抛弃型实例，`context.getPartitionId()` 为 `PARTITION_ID_UNSET`，校验完即 close），第二次在真实实例参与外发前。抛异常 = 拒绝注册。因此 initialize 里**不要分配重型资源**。
- **`sink` 失败即重试**：正常返回 = 成功；抛异常 = 同一条记录按退避重试直到成功或服务关闭。引擎**绝不丢数据**——代价是持续失败会拖住日志压缩。
- **`pause`/`resume` 只由硬暂停触发**（软暂停对 Sink 不可见）。
- **`close` 之后不再有任何回调**；`purge`/`onPartitionRemoved` 要求幂等。
- 所有回调在 Sink 线程**串行**执行，无需考虑回调间并发。
- **`Record` 包装引擎复用内存**：跨调用持有必须 `Record#copyOf()`。

### 位置确认（checkpoint 语义）

- `controller.updatePosition(position[, metadata])`：**单调递增**，任意线程可调用；位置+元数据持久化，重启续传。
- 引擎替你跳过被匹配器拒绝的记录，但绝不越过在途记录。
- **从不确认的 Sink 会永久阻塞日志压缩**——这是引擎背压体系的一环，挂住的 Sink 会把磁盘水位顶上去。

## 3. 分区本地查询引擎（SinkQueryService）

Sink 处理记录时经常需要补查上下文（流程定义内容、变量值）。`SinkContext.getQueryRepository()` 提供分区本地 RocksDB 直查，**不产生日志记录、不走命令链路**：

```java
// 流程定义（含 BPMN XML 资源内容）；不存在返回 null
ProcessDefinitionRecordValue definition = context.getQueryRepository().getProcessDefinition(1125899906842627L);

// 流程实例（流程作用域）的全部变量；无变量返回空映射
Map<String, Object> processVariables = context.getQueryRepository().getVariable(processInstanceId);

// 某活动可见的变量：活动作用域起沿父实例链向上合并，子作用域优先
Map<String, Object> visible = context.getQueryRepository().getVariable(processInstanceId, activityInstanceId);
```

约束与语义：

- **线程安全**，任意线程可调用（含你自建的刷盘工作线程）；流程定义查询命中缓存无锁。
- 流程定义**一经部署内容不变**（引擎内置缓存）；**变量随流程执行变更**，变量查询每次读最新已提交值、不走缓存。
- 返回的流程定义是**只读视图**（缓存正本），跨调用持有或需要修改请自行拷贝；两个变量查询返回的 `Map` 为当次调用独享。
- 配置校验阶段（分区未绑定）`getQueryRepository()` 返回 **null**——校验期实例没有分区存储，别在校验路径上调它。
- 查询只反映**已提交**状态；引擎批次内的在途写入不可见。

## 4. broker 注册与配置

```yaml
sinks:
  my-sink:
    className: com.example.MySink     # 实现类全名
    jarPath: /path/to/my-sink-all.jar # 外部 uber jar；不配则从 broker classpath 加载
    args:
      endpoint: http://...
      flushInterval: 5s
```

- `sinks.<id>` 的 key 即 Sink id；`args` 是原始参数，经 `context.getConfiguration().createSettings(MySettings.class)` 宽松绑定（属性名/枚举值大小写不敏感、单引号可用、未知属性忽略、数字键 Map 自动转 List）。
- 配置在 broker 运行期变更（增/删/改 args）会被感知：新 Sink 走注册校验后启用，被移除的 Sink 优雅关闭。

## 5. 常见错误

| 错误 | 正确做法 |
|------|---------|
| uber jar 里打进了 sink-api/jackson 等引擎包 | 全部改 `compileOnly`（见第 1 节清单） |
| `initialize` 里建连接池 | 留给 `start`（initialize 会跑两次） |
| 持有 `Record` 未 `copyOf()` | 跨调用持有必须拷贝，引擎复用该内存 |
| 位置确认回退或从不确认 | 只前进；无法处理时抛异常走重试，别静默吞 |
| 校验阶段调用 `getQueryRepository()` | 该阶段为 null，查询放 `start`/`sink` 之后 |
| 构造器里读配置/起线程 | 公有无参构造器保持干净，一切从 `initialize` 开始 |
