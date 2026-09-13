# 集群调度一期（CLUSTER_INIT）实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 实现集群初始化调度（CLUSTER_INIT）全链路闭环：admin 分区 Leader 当选自动触发 → 业务计划生成（BOOTSTRAP 增加分区语义）→ 逐条下发执行 + ACK 回流 → 计划 COMPLETED / FAILED + 业务/管理集群元数据落库。

**Architecture:** 全部状态流转由 admin 分区日志驱动：处理器（processor）消费 COMMAND 并写新 COMMAND/EVENT，EVENT 经 applier 落 RocksDB（repository）。任意时刻每计划至多一条执行明细在途（单线程 actor + ACK 推进保证）。失败语义：任一明细 ACK 失败 → 整个计划 FAILING/FAILED，剩余明细不再下发。

**Tech Stack:** Java 17+（switch/lambda `_` 占位）、Gradle 多模块、RocksDB 列族存储、structpack 记录序列化、Actor 并发模型。

**上游文档:** [CLUSTER_DISPATCH_PHASE1_DEV.md](./CLUSTER_DISPATCH_PHASE1_DEV.md)（详设与 T1–T7 拆解、§8 已知风险）。

---

## 与默认流程的差异（必读）

1. **无 TDD**：`cluster-dispatch`、`admin-repository`、`protocol-admin-impl` 模块没有任何测试基建；每个任务以 **编译通过 + spotlessApply** 为验收，端到端验证归入 Task 8 人工清单。
2. **不主动 commit**：按项目 CLAUDE.md 约定，全部任务完成后展示 `git diff` 供用户审核，用户确认后才提交。
3. **代码风格**：标识符英文、注释中文；提交前必须 `./gradlew spotlessApply`。

## 全局事实表（executor 必读，均已核实）

| 事实 | 说明 |
|---|---|
| `LogEventWriter.addCommand(key, lifeCycle, requestId, value)` | 写 COMMAND，由对应处理器消费 |
| `LogEventWriter.addEvent(key, lifeCycle, requestId, value)` | 写 EVENT **并**同步调 applier `applyState(key, value)` 持久化 |
| `LogEventWriter.getDispatchClient()` | 已存在，返回 `ClusterDispatchClient`（无需新增 getter） |
| `LogEventWriter.nextKey()` / `millis()` | 生成全局键 / 当前时间戳 |
| `LogRecord<T>` | `getKey()` / `getValue()` / `getRequestId()` |
| `ClusterDispatchClient.send(MemberId, PartitionType, PartitionExecutionType, T)` | 异步发执行消息，返回 `ActorFuture<Void>`（有 `onComplete(BiConsumer)`） |
| `ExecutionRecordSerialize.encode/decode` | 执行消息负载序列化；`decode(bytes, PartitionExecutionType)` 按类型还原 |
| `DispatchPlanState` 枚举 | **仅** `WAIT / SUCCESS / FAIL`（与生命周期枚举是两套，勿混淆） |
| `BusinessDispatchPlanExecutionRecord` 默认 | `dispatchState=WAIT`、`dispatchStartTime=-1`、`dispatchEndTime=-1` |
| `AdminImmutableRepository` | `repositoryAdmin()` / `repositoryBusiness()` 两个入口 |
| 明细生命周期（Command） | EXECUTING→EXECUTED→ACKNOWLEDGE→SUCCEED/FAILED；SUCCEED/FAILED 也作为 EVENT 落库 |
| 计划生命周期（Command） | INITIALIZE→CREATING→CREATED→COMPLETING→COMPLETED / FAILING→FAILED |
| repository 单例缓冲 | 各 repository 持有共享 key/value 缓冲实例，仅在引擎 actor 单线程下使用（现状模式，勿加锁） |

**处理器/注册已就绪，无需改动注册器**：业务侧 `BusinessDispatch{Initialize,Create,Complete,Fail}Processor` 与 `BusinessDispatchExecution{Executing,Acknowledge,AcknowledgeTimeOut}Processor`、`BusinessClusterMeta{Create,Update,Delete}Processor` 均已按生命周期注册。

**明确不改动的文件（保留空壳作挂点）**：`BusinessDispatchExecutionAcknowledgeTimeOutProcessor`、`BusinessClusterMetaUpdateProcessor`、`BusinessClusterMetaDeleteProcessor`、`AdminDispatchCreateProcessor`、`AdminDispatchFailProcessor`、admin 侧三个 execution 处理器、`AdminClusterMetaUpdateProcessor`。

---

### Task 1: Repository 层修复与补齐（admin-repository）

现状缺陷：① `getDispatchPlanExecution` 查错列族（查了 plan 列族）；② 私有 `saveDispatchPlanExecution` 只 wrap 没 put，**明细从未真正落库**；③ 缺少单条明细更新的公开方法（ACK 后状态推进需要）。

**Files:**
- Modify: `kunpeng/repository/admin-repository/src/main/java/com/anyilanxin/kunpeng/repository/admin/modules/business/RepositoryBusiness.java`
- Modify: `kunpeng/repository/admin-repository/src/main/java/com/anyilanxin/kunpeng/repository/admin/modules/business/MutableRepositoryBusiness.java`
- Modify: `kunpeng/repository/admin-repository/src/main/java/com/anyilanxin/kunpeng/repository/admin/modules/admin/RepositoryAdmin.java`
- Modify: `kunpeng/repository/admin-repository/src/main/java/com/anyilanxin/kunpeng/repository/admin/modules/admin/MutableRepositoryAdmin.java`

- [ ] **Step 1.1: 确认 gradle 模块路径**

Run: `./gradlew projects | grep -E "admin-repository|cluster-dispatch"`
Expected: 列出 `:kunpeng:repository:admin-repository` 与 `:kunpeng:cluster:cluster-dispatch`（后续命令按实际输出调整；若路径不同，以输出为准）。

- [ ] **Step 1.2: 修复并补齐 RepositoryBusiness**

`RepositoryBusiness.java` 三处修改：

(a) `saveDispatchPlan` 改为调用公开的单条更新方法，并删除私有 `saveDispatchPlanExecution`：

```java
  @Override
  public void saveDispatchPlan(final BusinessDispatchPlanRecord dispatchPlan) {
    updateDispatchPlan(dispatchPlan);
    for (final BusinessDispatchPlanExecutionRecord planExecutionRecord :
        dispatchPlan.executionPlan()) {
      updateDispatchPlanExecution(planExecutionRecord);
    }
  }
```

(b) 新增公开方法（替代原私有方法，补上缺失的 `put`）：

```java
  @Override
  public void updateDispatchPlanExecution(
      final BusinessDispatchPlanExecutionRecord dispatchPlanExecution) {
    planExecutionDbKey.wrapLong(dispatchPlanExecution.getDispatchPlanExecutionId());
    dispatchPlanExecutionDbValue.wrap(dispatchPlanExecution);
    dispatchPlanExecutionColumnFamily.put(planExecutionDbKey, dispatchPlanExecutionDbValue);
  }
```

(c) `getDispatchPlanExecution` 修复查错列族（原来查的是 `dispatchPlanColumnFamily` 且用了 plan 的 key）：

```java
  @Override
  public BusinessDispatchPlanExecutionRecord getDispatchPlanExecution(final long key) {
    planExecutionDbKey.wrapLong(key);
    if (dispatchPlanExecutionColumnFamily.get(planExecutionDbKey) != null) {
      return dispatchPlanExecutionDbValue.unwrap(dispatchPlanExecutionBuffer);
    }
    return null;
  }
```

- [ ] **Step 1.3: MutableRepositoryBusiness 增加接口方法**

```java
  void updateDispatchPlanExecution(
      final BusinessDispatchPlanExecutionRecord dispatchPlanExecution);
```

- [ ] **Step 1.4: RepositoryAdmin 做完全相同的四处修复**

`RepositoryAdmin.java`：`saveDispatchPlan` 循环改调 `updateDispatchPlanExecution`、删除私有 `saveDispatchPlanExecution`、新增 `updateDispatchPlanExecution`（同 1.2(b)，类型换为 `AdminDispatchPlanExecutionRecord`）、`getDispatchPlanExecution` 改查 `dispatchPlanExecutionColumnFamily`（同 1.2(c)，类型换为 `AdminDispatchPlanExecutionRecord`）。

`MutableRepositoryAdmin.java` 增加：

```java
  void updateDispatchPlanExecution(
      final AdminDispatchPlanExecutionRecord dispatchPlanExecution);
```

- [ ] **Step 1.5: 编译验证**

Run: `./gradlew :kunpeng:repository:admin-repository:compileJava`
Expected: BUILD SUCCESSFUL

---

### Task 2: Applier 持久化实现（EVENT → RocksDB）

所有 `applyState` 目前为空壳。按下表逐个实现（均为单行委托，构造器与字段已存在，不要改动文件其它部分）。

**Files（均在 `kunpeng/repository/admin-repository/src/main/java/com/anyilanxin/kunpeng/repository/admin/modules/` 下）:**

| 文件 (applier/impl/) | lifeCycle | applyState 实现 |
|---|---|---|
| `business/.../BusinessDispatchPlanCreatedApplierImpl.java` | Plan.CREATED | `repositoryBusiness.saveDispatchPlan(recordValue);` |
| `business/.../BusinessDispatchPlanCompletedApplierImpl.java` | Plan.COMPLETED | `repositoryBusiness.updateDispatchPlan(recordValue);` |
| `business/.../BusinessDispatchPlanFailedApplierImpl.java` | Plan.FAILED | `repositoryBusiness.updateDispatchPlan(recordValue);` |
| `business/.../BusinessDispatchPlanExecutionExecutedApplierImpl.java` | Execution.EXECUTED | `repositoryBusiness.updateDispatchPlanExecution(recordValue);` |
| `business/.../BusinessDispatchPlanExecutionSucceedApplierImpl.java` | Execution.SUCCEED | `repositoryBusiness.updateDispatchPlanExecution(recordValue);` |
| `business/.../BusinessDispatchPlanExecutionFailedApplierImpl.java` | Execution.FAILED | `repositoryBusiness.updateDispatchPlanExecution(recordValue);` |
| `business/.../BusinessClusterMetaCreatedApplierImpl.java` | ClusterMeta.CREATED | `repositoryBusiness.updateClusterMeta(recordValue);` |
| `admin/.../AdminDispatchPlanCreatedApplierImpl.java` | Plan.CREATED | `repositoryAdmin.saveDispatchPlan(recordValue);` |
| `admin/.../AdminDispatchPlanCompletedApplierImpl.java` | Plan.COMPLETED | `repositoryAdmin.updateDispatchPlan(recordValue);` |
| `admin/.../AdminClusterMetaCreatedApplierImpl.java` | ClusterMeta.CREATED | `repositoryAdmin.updateClusterMeta(recordValue);` |

- [ ] **Step 2.1: 按上表实现 10 个 applyState**

示例（业务计划 CREATED，其余同型）：

```java
  @Override
  public void applyState(final long key, final BusinessDispatchPlanRecord recordValue) {
    repositoryBusiness.saveDispatchPlan(recordValue);
  }
```

不实现的 applier（一期不触达，保留空壳）：`AdminClusterMetaUpdatedApplierImpl`、`BusinessClusterMetaUpdatedApplierImpl`、`BusinessClusterMetaDeletedApplierImpl`、`AdminDispatchPlanFailedApplierImpl`。

- [ ] **Step 2.2: 编译验证**

Run: `./gradlew :kunpeng:repository:admin-repository:compileJava`
Expected: BUILD SUCCESSFUL

---

### Task 3: T1 业务计划生成（INITIALIZE → CREATING）

**Files:**
- Modify: `kunpeng/cluster/cluster-dispatch/src/main/java/com/anyilanxin/kunpeng/cluster/dispatch/command/admin/dispatch/processor/AdminDispatchInitializeProcessor.java`
- Modify: `kunpeng/cluster/cluster-dispatch/src/main/java/com/anyilanxin/kunpeng/cluster/dispatch/command/business/dispatch/processor/BusinessDispatchInitializeProcessor.java`

- [ ] **Step 3.1: AdminDispatchInitializeProcessor 给业务计划携带拓扑快照**

业务计划生成依赖 admin 拓扑，必须把 admin 计划的 `meta` 传递过去。将业务计划构造改为：

```java
    // 业务节点初始化调度
    final BusinessDispatchPlanRecord businessDispatchPlanRecord =
        new BusinessDispatchPlanRecord()
            .setApplyPlan(true)
            .setDispatchPlanId(writer.nextKey())
            .setInitDispatch(true)
            .setMeta(value.getMeta())
            .setDispatchPlanType(BusinessDispatchType.CLUSTER_INIT);
```

- [ ] **Step 3.2: 实现 BusinessDispatchInitializeProcessor**

替换 `processRecord` 方法体并补充 import（`encode` 静态导入、`PartitionBootstrapRecord`、`PartitionExecutionType`、`PartitionType`、`DispatchPlanState`、`PartitionInfoMetaRecord`）：

```java
  @Override
  public void processRecord(final LogRecord<BusinessDispatchPlanRecord> record) {
    final BusinessDispatchPlanRecord value = record.getValue();
    final PartitionInfoMetaRecord meta = value.getMeta();
    // 初始化调度语义为增加分区（BOOTSTRAP）：admin 拓扑中的每个分区组生成一条执行明细
    final long dispatchPlanExecutionId = writer.nextKey();
    final PartitionBootstrapRecord bootstrapRecord =
        new PartitionBootstrapRecord()
            .setPartitionType(PartitionType.BUSINESS)
            .setExecutionType(PartitionExecutionType.BOOTSTRAP)
            .setPartitionMeta(meta)
            .setDispatchPlanId(value.getDispatchPlanId())
            .setDispatchPlanExecutionId(dispatchPlanExecutionId);
    final BusinessDispatchPlanExecutionRecord execution = value.executionPlan().add();
    execution
        .setDispatchPlanExecutionId(dispatchPlanExecutionId)
        .setExecutionOrder(0)
        .setPlanData(encode(bootstrapRecord))
        .setDispatchMemberId(meta.getPartitionMembers().get(0).getMemberId())
        .setDispatchState(DispatchState.WAIT);
    // 计划指定完成，流转 CREATING（落库后启动执行）
    writer.addCommand(
        value.getDispatchPlanId(),
        BusinessDispatchPlanLifeCycle.CREATING,
        record.getRequestId(),
        value);
  }
```

要点：`partitionType` 必须显式置 `BUSINESS`——业务节点 ACK 会回传该值，`ClusterDispatchService` 按它路由到 business 分支；缺省值是 `ADMIN` 会导致 ACK 丢失。

- [ ] **Step 3.3: 编译验证**

Run: `./gradlew :kunpeng:cluster:cluster-dispatch:compileJava`
Expected: BUILD SUCCESSFUL

---

### Task 4: T2 计划落库与启动（CREATING → CREATED → 首条 EXECUTING）

**Files:**
- Create: `kunpeng/cluster/cluster-dispatch/src/main/java/com/anyilanxin/kunpeng/cluster/dispatch/command/business/BusinessDispatchPlanSupport.java`
- Modify: `kunpeng/cluster/cluster-dispatch/src/main/java/com/anyilanxin/kunpeng/cluster/dispatch/command/business/dispatch/processor/BusinessDispatchCreateProcessor.java`

- [ ] **Step 4.1: 新建明细选取工具（Create 与 Acknowledge 两个处理器共用）**

```java
/*
 * Copyright © 2026 anyilanxin zxh(anyilanxin@aliyun.com)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.anyilanxin.kunpeng.cluster.dispatch.command.business;

import com.anyilanxin.kunpeng.protocol.admin.impl.record.command.business.BusinessDispatchPlanExecutionRecord;
import com.anyilanxin.kunpeng.protocol.admin.impl.record.command.business.BusinessDispatchPlanRecord;
import com.anyilanxin.kunpeng.protocol.admin.record.command.DispatchPlanState;

/** 业务调度计划执行明细的公共选取逻辑 */
public final class BusinessDispatchPlanSupport {

  private BusinessDispatchPlanSupport() {}

  /** 找出 executionOrder 最小、尚未开始执行（WAIT 且未记录开始时间）的执行明细 */
  public static BusinessDispatchPlanExecutionRecord findNextExecution(
      final BusinessDispatchPlanRecord plan) {
    BusinessDispatchPlanExecutionRecord next = null;
    for (final BusinessDispatchPlanExecutionRecord detail : plan.executionPlan()) {
      if (detail.getDispatchState() == DispatchState.WAIT
          && detail.getDispatchStartTime() == -1
          && (next == null || detail.getExecutionOrder() < next.getExecutionOrder())) {
        next = detail;
      }
    }
    return next;
  }
}
```

- [ ] **Step 4.2: 实现 BusinessDispatchCreateProcessor**

替换 `processRecord`（删除 `System.out.println`），补充 import（`BusinessDispatchPlanExecutionRecord`、`PartitionSourceLifeCycle`、`BusinessDispatchPlanSupport`）：

```java
  @Override
  public void processRecord(final LogRecord<BusinessDispatchPlanRecord> record) {
    final BusinessDispatchPlanRecord value = record.getValue();
    // 计划与执行明细落库（CREATED 事件经 applier 持久化）
    writer.addEvent(
        value.getDispatchPlanId(),
        BusinessDispatchPlanLifeCycle.CREATED,
        record.getRequestId(),
        value);
    // 触发首条执行明细
    final BusinessDispatchPlanExecutionRecord first =
        BusinessDispatchPlanSupport.findNextExecution(value);
    if (first != null) {
      writer.addCommand(
          first.getDispatchPlanExecutionId(),
          BusinessDispatchPlanExecutionLifeCycle.EXECUTING,
          record.getRequestId(),
          first);
    }
  }
```

- [ ] **Step 4.3: 编译验证**

Run: `./gradlew :kunpeng:cluster:cluster-dispatch:compileJava`
Expected: BUILD SUCCESSFUL

---

### Task 5: T3 执行下发（EXECUTING → EXECUTED → send）

**Files:**
- Modify: `kunpeng/cluster/cluster-dispatch/src/main/java/com/anyilanxin/kunpeng/cluster/dispatch/command/business/execution/processor/BusinessDispatchExecutionExecutingProcessor.java`

- [ ] **Step 5.1: 实现 BusinessDispatchExecutionExecutingProcessor**

替换 `processRecord`，补充 import（静态 `decode`、`MemberId`、`PartitionBootstrapRecord`、`PartitionExecutionType`、`PartitionType`、`AdminRepositoryLoggers`、`Logger`）：

```java
  private static final Logger LOGGER = Loggers.CLUSTER_DISPATCH;
```

（加为类字段。）

```java
  @Override
  public void processRecord(final LogRecord<BusinessDispatchPlanExecutionRecord> record) {
    final BusinessDispatchPlanExecutionRecord value = record.getValue();
    // 记录开始时间并落库（EXECUTED 事件经 applier 持久化）
    value.setDispatchStartTime(writer.millis());
    writer.addEvent(
        value.getDispatchPlanExecutionId(),
        BusinessDispatchPlanExecutionLifeCycle.EXECUTED,
        record.getRequestId(),
        value);
    // 向目标成员下发 BOOTSTRAP 执行消息（planData 为序列化的 PartitionBootstrapRecord）
    final PartitionBootstrapRecord bootstrap =
        decode(value.getPlanData(), PartitionExecutionType.BOOTSTRAP);
    final long dispatchPlanExecutionId = value.getDispatchPlanExecutionId();
    final String dispatchMemberId = value.getDispatchMemberId();
    writer
        .getDispatchClient()
        .send(
            MemberId.from(dispatchMemberId),
            PartitionType.BUSINESS,
            PartitionExecutionType.BOOTSTRAP,
            bootstrap)
        .onComplete(
            (_, throwable) -> {
              if (throwable != null) {
                LOGGER.error(
                    "Failed to send bootstrap dispatch execution {} to member {}",
                    dispatchPlanExecutionId,
                    dispatchMemberId,
                    throwable);
              }
            });
  }
```

要点：lambda 内只引用局部变量（`value` 底层缓冲可能被复用）；发送为异步火后不管，失败仅记日志——明细在途的兜底（ACK 超时）为二期范围（开发文档 §4.4.6）。

- [ ] **Step 5.2: 编译验证**

Run: `./gradlew :kunpeng:cluster:cluster-dispatch:compileJava`
Expected: BUILD SUCCESSFUL

---

### Task 6: T4/T6 ACK 消费与推进（成功推进 / 失败整单终止）

**Files:**
- Modify: `kunpeng/cluster/cluster-dispatch/src/main/java/com/anyilanxin/kunpeng/cluster/dispatch/api/ClusterDispatchService.java`
- Modify: `kunpeng/cluster/cluster-dispatch/src/main/java/com/anyilanxin/kunpeng/cluster/dispatch/command/business/execution/processor/BusinessDispatchExecutionAcknowledgeProcessor.java`

- [ ] **Step 6.1: ClusterDispatchService 把失败 ACK 的结果带进日志**

现状问题：`writeCommand` 收到 ACK 后直接回查 repository 中的明细落 COMMAND，**丢弃了 `success/errorMessage`**，消费侧无从判定成败。修改 business 分支（admin 分支一期不动）：

```java
    } else {
      recordValue =
          repositoryBusiness.getDispatchPlanExecution(record.getDispatchPlanExecutionId());
      if (recordValue != null && !record.isSuccess()) {
        // 失败 ack：将失败状态与原因写入执行明细，供 ACK 消费侧判定
        recordValue.setDispatchState(DispatchState.FAIL);
        recordValue.setDispatchMessage(record.getErrorMessage());
      }
      recordMetadata =
          new AdminRecordMetadata()
              .recordType(RecordType.COMMAND)
              .recordVersion(1)
              .brokerVersion(BROKER_VERSION)
              .valueLifeCycle(BusinessDispatchPlanExecutionLifeCycle.ACKNOWLEDGE)
              .valueType(AdminValueType.BUSINESS_DISPATCH_EXECUTION);
    }
```

补充 import：`com.anyilanxin.kunpeng.protocol.admin.record.command.DispatchState`。

- [ ] **Step 6.2: 实现 BusinessDispatchExecutionAcknowledgeProcessor**

类字段增加 repository 与 logger，替换 `processRecord`（删除 `System.out.println`），补充 import（`DispatchPlanState`、`PartitionSourceRecord`、`DelayedLifeCycle`、`BusinessDispatchPlanSupport`、`ImmutableRepositoryDelayed`、`AdminRepositoryLoggers`、`Logger`）：

```java
  private static final Logger LOGGER = Loggers.CLUSTER_DISPATCH;
  protected final LogEventWriter writer;
  private final ImmutableRepositoryBusiness repositoryBusiness;

  public BusinessDispatchExecutionAcknowledgeProcessor(final LogEventWriter writer) {
    super(writer);
    this.writer = writer;
    repositoryBusiness = writer.getRepository().repositoryBusiness();
  }
```

```java
  @Override
  public void processRecord(final LogRecord<BusinessDispatchPlanExecutionRecord> record) {
    final BusinessDispatchPlanExecutionRecord value = record.getValue();
    final long requestId = record.getRequestId();
    if (value.getDispatchState() == DispatchState.FAIL) {
      // 任一明细失败 → 整个调度计划失败：失败明细标记 FAILED，剩余明细保持 WAIT 不再下发，直接反馈计划层
      value.setDispatchEndTime(writer.millis());
      writer.addEvent(
          value.getDispatchPlanExecutionId(),
          BusinessDispatchPlanExecutionLifeCycle.FAILED,
          requestId,
          value);
      final BusinessDispatchPlanRecord plan = repositoryBusiness.getDispatchPlan();
      if (plan != null) {
        writer.addCommand(
            plan.getDispatchPlanId(), BusinessDispatchPlanLifeCycle.FAILING, requestId, plan);
      }
      return;
    }
    // 成功：明细完结后推进下一条，全部完成则完结计划
    value.setDispatchState(DispatchState.SUCCESS);
    value.setDispatchEndTime(writer.millis());
    writer.addEvent(
        value.getDispatchPlanExecutionId(),
        BusinessDispatchPlanExecutionLifeCycle.SUCCEED,
        requestId,
        value);
    final BusinessDispatchPlanRecord plan = repositoryBusiness.getDispatchPlan();
    if (plan == null) {
      LOGGER.error(
          "Business dispatch plan not found for execution {}", value.getDispatchPlanExecutionId());
      return;
    }
    final BusinessDispatchPlanExecutionRecord next =
        BusinessDispatchPlanSupport.findNextExecution(plan);
    if (next != null) {
      writer.addCommand(
          next.getDispatchPlanExecutionId(),
          BusinessDispatchPlanExecutionLifeCycle.EXECUTING,
          requestId,
          next);
    } else {
      writer.addCommand(
          plan.getDispatchPlanId(), BusinessDispatchPlanLifeCycle.COMPLETING, requestId, plan);
    }
  }
```

- [ ] **Step 6.3: 编译验证**

Run: `./gradlew :kunpeng:cluster:cluster-dispatch:compileJava`
Expected: BUILD SUCCESSFUL

---

### Task 7: T5/T6 计划完结、元数据落库与 admin 联动

**Files:**
- Modify: `kunpeng/cluster/cluster-dispatch/src/main/java/com/anyilanxin/kunpeng/cluster/dispatch/command/business/dispatch/processor/BusinessDispatchCompleteProcessor.java`
- Modify: `kunpeng/cluster/cluster-dispatch/src/main/java/com/anyilanxin/kunpeng/cluster/dispatch/command/business/dispatch/processor/BusinessDispatchFailProcessor.java`
- Modify: `kunpeng/cluster/cluster-dispatch/src/main/java/com/anyilanxin/kunpeng/cluster/dispatch/command/business/clustermeta/processor/BusinessClusterMetaCreateProcessor.java`
- Modify: `kunpeng/cluster/cluster-dispatch/src/main/java/com/anyilanxin/kunpeng/cluster/dispatch/command/admin/dispatch/processor/AdminDispatchCompleteProcessor.java`

- [ ] **Step 7.1: 实现 BusinessDispatchCompleteProcessor（COMPLETING）**

该文件当前无 `processRecord`，需新增方法与字段。完整目标：

```java
public class BusinessDispatchCompleteProcessor extends AbstractBusinessDispatchProcessor {
  private static final Logger LOGGER = Loggers.CLUSTER_DISPATCH;
  protected final LogEventWriter writer;
  private final ImmutableRepositoryBusiness repositoryBusiness;

  public BusinessDispatchCompleteProcessor(final LogEventWriter writer) {
    super(writer);
    this.writer = writer;
    repositoryBusiness = writer.getRepository().repositoryBusiness();
  }

  @Override
  public void processRecord(final LogRecord<BusinessDispatchPlanRecord> record) {
    final BusinessDispatchPlanRecord value = record.getValue();
    // 校验全部执行明细已完成
    for (final BusinessDispatchPlanExecutionRecord detail : value.executionPlan()) {
      if (detail.getDispatchState() != DispatchState.SUCCESS) {
        LOGGER.error(
            "Business dispatch plan {} has unfinished execution {}",
            value.getDispatchPlanId(),
            detail.getDispatchPlanExecutionId());
        return;
      }
    }
    // 计划完结落库
    writer.addEvent(
        value.getDispatchPlanId(),
        BusinessDispatchPlanLifeCycle.COMPLETED,
        record.getRequestId(),
        value);
    // 初始化场景：业务集群元数据首次创建
    final BusinessClusterMetaRecord oldClusterMeta = repositoryBusiness.getClusterMeta();
    final PartitionInfoMetaRecord meta = value.getMeta();
    final BusinessClusterMetaRecord clusterMeta;
    if (oldClusterMeta == null) {
      clusterMeta =
          new BusinessClusterMetaRecord()
              .setVersion(1)
              .setReplicationFactor(1)
              .setCurrentReplicationFactor(meta.getPartitionMembers().size())
              .setCreateTime(writer.millis())
              .setUpdateTime(writer.millis())
              .setMeta(meta);
    } else {
      clusterMeta =
          new BusinessClusterMetaRecord()
              .setVersion(oldClusterMeta.getVersion() + 1)
              .setReplicationFactor(1)
              .setCurrentReplicationFactor(meta.getPartitionMembers().size())
              .setCreateTime(oldClusterMeta.getCreateTime())
              .setUpdateTime(writer.millis())
              .setMeta(meta);
    }
    writer.addCommand(
        value.getDispatchPlanId(),
        BusinessClusterMetaLifeCycle.CREATING,
        record.getRequestId(),
        clusterMeta);
    // admin 计划联动：业务计划完成后回写 admin 计划 COMPLETED 落库
    final AdminDispatchPlanRecord adminPlan =
        writer.getRepository().repositoryAdmin().getDispatchPlan();
    if (adminPlan != null) {
      writer.addEvent(
          adminPlan.getDispatchPlanId(),
          AdminDispatchPlanLifeCycle.COMPLETED,
          record.getRequestId(),
          adminPlan);
    }
  }

  @Override
  public BusinessDispatchPlanLifeCycle valueLifeCycle() {
    return BusinessDispatchPlanLifeCycle.COMPLETING;
  }
}
```

补充 import：`DispatchPlanState`、`BusinessClusterMetaRecord`、`NodeSourceLifeCycle`、`AdminDispatchPlanRecord`、`AdminDispatchPlanLifeCycle`、`PartitionInfoMetaRecord`、`ImmutableRepositoryDelayed`、`AdminRepositoryLoggers`、`Logger`。

- [ ] **Step 7.2: 实现 BusinessDispatchFailProcessor（FAILING → FAILED 落库）**

补充 import（`LogRecord`、`DelayedLifeCycle` 已有），新增 `processRecord`：

```java
  @Override
  public void processRecord(final LogRecord<BusinessDispatchPlanRecord> record) {
    final BusinessDispatchPlanRecord value = record.getValue();
    // 失败计划落库（FAILED 事件经 applier 持久化）；失败原因在失败执行明细的 dispatchMessage 中
    writer.addEvent(
        value.getDispatchPlanId(),
        BusinessDispatchPlanLifeCycle.FAILED,
        record.getRequestId(),
        value);
  }
```

- [ ] **Step 7.3: 实现 BusinessClusterMetaCreateProcessor（CREATING → CREATED 落库）**

替换 `processRecord`（保留现有写法风格，方法体只有一行落库）：

```java
  @Override
  public void processRecord(final LogRecord<BusinessClusterMetaRecord> record) {
    final BusinessClusterMetaRecord value = record.getValue();
    // 元数据落库（CREATED 事件经 applier 持久化）
    writer.addEvent(-1, BusinessClusterMetaLifeCycle.CREATED, record.getRequestId(), value);
  }
```

注意：**不要**动该文件里已有的其它逻辑（此文件与 admin 侧同名类不同，业务侧原本只有取值语句；直接替换为上述实现）。

- [ ] **Step 7.4: AdminDispatchCompleteProcessor 落库 admin 计划（联动前提）**

业务计划完结时要回写 admin 计划 COMPLETED，前提是 admin 计划已在 repository 中。在 `processRecord` 的 `final AdminDispatchPlanRecord value = record.getValue();` 之后插入：

```java
    // 管理计划落库（CREATED 事件经 applier 持久化），供业务计划完结后回写 COMPLETED
    writer.addEvent(
        value.getDispatchPlanId(),
        AdminDispatchPlanLifeCycle.CREATED,
        record.getRequestId(),
        value);
```

（`AdminDispatchPlanLifeCycle` 该文件已 import，无需新增。）

- [ ] **Step 7.5: 编译验证**

Run: `./gradlew :kunpeng:cluster:cluster-dispatch:compileJava :kunpeng:cluster:cluster-admin:compileJava`
Expected: BUILD SUCCESSFUL（cluster-admin 一并编译确认 ACK 接收面改动无涟漪）

---

### Task 8: T7 全量验证

- [ ] **Step 8.1: 格式化**

Run: `./gradlew spotlessApply`
Expected: BUILD SUCCESSFUL；若有文件被格式化，复查 diff 确认无语义变化。

- [ ] **Step 8.2: 受影响模块全量编译**

Run: `./gradlew :kunpeng:repository:admin-repository:compileJava :kunpeng:cluster:cluster-dispatch:compileJava :kunpeng:cluster:cluster-admin:compileJava`
Expected: BUILD SUCCESSFUL

- [ ] **Step 8.3: 既有测试回归（cluster 模块是唯一有测试基建的相邻模块）**

Run: `./gradlew :kunpeng:cluster:cluster:test`
Expected: BUILD SUCCESSFUL（全部通过）

- [ ] **Step 8.4: 展示改动供审核**

Run: `git status && git diff --stat`
向用户展示改动文件清单与 diff 摘要。**不 commit**——等用户确认后按 CLAUDE.md 规范提交（英文 commit message、type 前缀、无 Co-Authored-By）。

- [ ] **Step 8.5: 端到端人工验证清单（交用户执行，逐项记录结果）**

1. 单节点：启动 → admin Raft 就绪 → 自动初始化 → 业务分区创建 → `repositoryBusiness` 中 `BusinessClusterMetaRecord` 存在且 version=1、`repositoryAdmin` 中 `AdminClusterMetaRecord` 存在且 version=1 → admin/业务计划终态 COMPLETED。
2. 多节点（≥3）：任意节点当选 Leader（含非发起者）均能完成闭环。
3. 执行中途重启 admin Leader：日志重放后闭环可继续（EXECUTING 命令重消费 → 重新下发）。
4. 重复启动（已初始化集群）：允许重复触发（一期已知行为，开发文档 §8-1）。
5. 失败路径说明：业务节点侧当前只在成功时回 ACK（`executionAck` 固定 `setSuccess(true)`），真实失败注入需业务节点失败 ACK 支撑，属二期范围；一期失败链路（ACK 失败 → 整单 FAILING/FAILED、剩余明细不再下发）已按代码路径就绪。

---

## 已知边界（实现中刻意保留的现状）

| # | 事项 | 处理 |
|---|---|---|
| 1 | 业务分区复用 admin 拓扑的 group/id（`PartitionInfoMetaRecord` 原样透传） | 按开发文档 §4.3 从简；若运行时业务/管理分区目录冲突，二期调整分区号规则 |
| 2 | `getNextDispatchPlanExecution` 仍返回 null | 一期未使用（选取逻辑走 `BusinessDispatchPlanSupport`），二期实现 |
| 3 | `ackDispatchPlanExecution`（按 key 删除明细）未被调用 | 保留现状，二期决定在途明细的清理时机 |
| 4 | `CLUSTER_DISPATCH_TOPIX` 拼写 | 内部一致使用，二期顺手更名（开发文档 §8-7） |
| 5 | admin `UPDATING`/`DELETING` 链路与对应 applier | 二期范围，保留空壳 |
