---
name: cluster-dispatch-design
description: Use when working with kunpeng/cluster/cluster-dispatch, the cluster scheduling subsystem — understanding dispatch plan lifecycles (CHANGE_REPLICATION/CHANGE_PARTITION/CLUSTER_BALANCE type-driven entry, PLAN_CREATING/PLAN_DELAYED/PLAN_CREATED flow), plan generation (maker/generator + PartitionTopologyDiff), execution detail dispatch & ACK handling, the delayed watchdog retry mechanism, source-id governance, adminInit boot commands, commandapi/scheduling/distributor infrastructure, or checking known risks
---

# cluster-dispatch 模块（集群调度子系统）

`kunpeng/cluster/cluster-dispatch`（集群调度：计划生命周期、拓扑 diff 计划生成、执行明细下发与 ACK、延迟看门狗重试、source 治理、集群初始化）的设计知识库。

## 文档

- [01-cluster-dispatch-design.md](./01-cluster-dispatch-design.md) —— 总体设计：分层架构（commandapi/计划层/执行层/planner/source/delayed/api/scheduling）、调度类型（admin 1 类 + business 3 类）、统一生命周期链路、关键机制概览（拓扑 diff、延迟看门狗、ACK 回流、source 治理、adminInit 7 条初始命令）、协作模块与风险表
- [02-cluster-dispatch-detail.md](./02-cluster-dispatch-detail.md) —— 详细设计：端到端时序、计划制定（maker/generator/PartitionTopologyDiff 推导规则/三类生成器语义）、执行明细链路（计划级与明细级 EXECUTING、Acknowledge 幂等/失败重试/成功推进）、延迟四分支、source 治理处理器表、adminInit 命令明细、commandapi/distributor/scheduling 外围、状态机（含枚举数值）、代码点位索引、已知风险

## 速查

- 模块：`kunpeng/cluster/cluster-dispatch`，包 `com.anyilanxin.kunpeng.cluster.dispatch`；协作：cluster-admin（Leader 接线 + 业务节点执行）、broker-admin-client（DefaultClusterDispatchClient）、protocol-admin(-impl)（记录/枚举事实源）、admin-repository（applier 落库）
- 类型驱动：无 INITIALIZE 命令；admin `CHANGE_REPLICATION` / business `CHANGE_PARTITION`+`CHANGE_REPLICATION`+`CLUSTER_BALANCE` 即生命周期入口命令；全量初始化 = CHANGE_PARTITION + oldMeta 为空
- 计划链路：类型命令 → `PLAN_CREATING` →（成员不足 `PLAN_DELAYED` 延迟等待重入 | `createPlan` → `PLAN_CREATED`）→ applyPlan → 计划级 `EXECUTING` → `EXECUTED` → 明细串行 → `COMPLETING` → `COMPLETED`（+ ClusterMeta CREATING/UPDATING）
- 明细粒度：操作级（拓扑 diff 的 Bootstrap/Join/Leave 逐成员一条），非每分区一条；缩容多阶段 leave→DATA_MERGE→STOP→SOURCE_TRANSFER
- 失败语义：明细失败 ACK → 延迟重试（**不终止计划**）；ack 看门狗 = 每次下发注册 `DelayedType.{面}_EXECUTION` 延迟任务，到期未终态重发 EXECUTING；AcknowledgeTimeOut 处理器是空壳
- 计划生成：`BusinessDispatchPlanMaker`（EnumMap 策略上下文）+ 3 generator；`AdminDispatchPlanMaker` 仅 CHANGE_REPLICATION；操作序列统一由 `PartitionTopologyDiff.diff(当前,目标)` 推导（分区 ID/成员 ID 升序确定性输出）
- source 治理：`NodeSource`（maxNodeSourceId+1 分配）/`PartitionSource`（APPLYING→TRANSFERRING_ADD→TRANSFERRED_ADD 迁移台账）；bootstrap 明细执行期来源重写（追加 BOOTSTRAP_SOURCE_* 明细重编 order）
- 初始化：`DispatchProcessService.adminInit()` 守卫 `!isInitiator()`（无 DB 幂等守卫），7 条命令：source 台账×2 + source meta×2 + AdminClusterMeta + admin 计划 CHANGE_REPLICATION + business 计划 CHANGE_PARTITION
- 消息：ack topic `CLUSTER_DISPATCH_ACK`、node-source topic `CLUSTER_NODE_SOURCE`；执行消息 topic = `PartitionType-ExecutionType`（PartitionExecutionType 共 11 值）；客户端 5×2s 退避重试
- 两套状态勿混：生命周期枚举（EXECUTING/EXECUTED/ACKNOWLEDGE/SUCCEED/FAILED）vs 明细字段 `DispatchExecutionState`（WAIT/EXECUTED/SUCCEED/FAILED）
- 本技能为设计文档唯一副本（原 `kunpeng/cluster/cluster-dispatch/docs/` 归档后已删除，修改设计直接改本技能）
