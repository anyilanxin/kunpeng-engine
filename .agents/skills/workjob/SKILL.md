---
name: workjob
description: Distributed job push/pull dispatch design (Gateway <-> Broker messaging) v1.6. Covers registration sync, competing consumers, broker capability primitives (queryStreamTargets/pushToStream/broadcastConsumable), partitioned data plane (per-partition leader bindings, per-(jobType,partition) watermarks), deadline bookkeeping with Resolve, fault matrix, wire protocol and Java pseudocode. Use when understanding or changing gateway/broker job dispatch, debugging job delivery, or evolving the job push/pull system.
---

# workjob — 分布式 Job 推拉系统设计知识库

job 实时流推送（push）与长连接拉取（long-polling pull）完整链路的设计文档（当前 v1.6）。实现落点：broker-client `com.anyilanxin.kunpeng.broker.client.job`（wire 契约）、broker `com.anyilanxin.kunpeng.broker.job`（能力服务）、gateway-job（Hub/Transport）、bpmn-engine `EngineJobSource`（raft 业务源）。

## 入口文档

| 文档 | 内容 |
|------|------|
| [job-dispatch-design.md](./job-dispatch-design.md) | 设计全文（v1.6） |

## 章节地图（job-dispatch-design.md）

- §1 背景与目标 — 延迟/吞吐/可靠性指标，明确不做（业务侧职责）
- §2 总体架构与角色 — client/gateway/broker(raft) 分层，STREAM 与 PULL 两种消费模式
- §3 核心抽象与数据模型
- §4 控制面：注册同步 — 会话为源/路由投影、全节点广播等值副本、快照对账+增量事件、ack 捎带各分区 leader 表、TTL 摘除
- §5 数据面：broker 层能力服务与 raft 业务层编排 — 三原语（查询可用消费流/推送/可消费广播）、可消费广播语义、同 jobType 竞争消费、§5.3 分区维度（数据面按分区分片水平扩展）
- §6 Deadline 簿记与 Resolve 请求（业务层职责）— at-most-once 边界
- §7 故障处理矩阵
- §8 协议设计 — client↔gateway（gRPC）、gateway↔broker（cluster/messaging Netty）
- §9 Java 伪代码 — broker 层服务、raft 业务层装配、gateway 注册表/挂起表、gRPC 实现、client SDK
- §10 低延迟/高吞吐优化清单；§11 监控与容量；§12 演进方向

## 关键定稿（v1.6）

- 交付物是 **broker 层能力服务**，不到 raft 层；raft 及其上的业务编排是使用方
- leader 语义由 raft 层按分区 `bind(partitionId, ...)` 注入，broker 层自身零 raft 依赖
- 整体派发原则：有实时流 → 业务层查询消费流并推送；无实时流 → 广播分区可消费信号，gateway 反向拉取
- 竞争消费：一个 job 恰好派给一个 worker；可靠性边界 at-most-once
- v1.7：deadline 落引擎事件溯源状态机（ACTIVATED 事件 + 到期索引列族 + TIME_OUT 回 READY），broker 侧易失簿记（DeadlineBook）已删除
- v1.8：传输层对齐原创参考实现——仅 snapshot 对账/push 应答/job-ready 广播三个专属触点，拉取走标准 commandapi 激活命令、完成走 CompleteJob、推送失败本地 WITHDRAW 回退；九主题与会话/路由/微批三件套删除
- v1.9：聚合订阅（数组水位线）——每 (gateway,jobType) 一条 Aggregate(jobType,workers[])，下标即流位置、长度即水位线，broker 视图 O(type)；推送带 targetIndex+worker（引擎按 workers[index] 落 lockOwner），网关下标定位/同 worker 兜底/跨 worker 不凑合；starter 空 name 生成指纹 beanName#methodName
- v1.10：wire 编解码统一 SBE（schema id=11，嵌套 group 表达聚合数组，JobRecord 作 blob 段零拷贝），手写 Writer/Reader 删除
- v1.11：稳定会话 ID 映射替代数组水位线——Aggregate(jobType, sessions[]{sessionId,worker})，会话 ID 网关自增永不复用、下线只删自身项（零重排）；StreamPush/PushPoint 携带 sessionId，网关 ConcurrentHashMap O(1) 直达；watermark 派生字段删除；YIELD 更名 WITHDRAW（JobWithdrawProcessor / PushFailureFallback，短值 10 / PROCESS_INDEX_145 不变）
- v1.12：broker 选点扁平化——byType 直接聚合为跨网关合并的 (gateway,sessionId,worker) 会话列表，单游标取模轮转（每会话精确等份额，消除两级轮转向小网关的系统性倾斜），AggregateTarget/跨网关游标删除；注册传输为网关源点对点单播（ClusterCommunicationService，逐成员 ACK），SWIM 仅成员发现与 MEMBER_REMOVED 摘桶、不携带注册数据
- v1.13：注册分发 gossip 反熵落地（千级规模正解）——网关快照只发 3 个随机种子 broker（ZoneType.BROKER 过滤），broker 间每 1s 与 3 个随机对端交换 digest（(gateway,generation,refreshedAt) 清单）：落后方应答回传清单→gossipBatch 按需回拉、领先方主动推、同代次传播新鲜度（TTL 依据）；桶 60s 未刷新摘除兜底，误摘的活网关由对端 gossip 复活；SBE 新增 gossipDigest/gossipDigestReply/gossipBatch（message id 5/6/7；data-only group 可行）；broker-client 增 configuration 依赖（ZoneType）
- v1.14：注册分发迁移 SWIM 元数据通道（用户定案：其 SWIM 对大 property 有版本戳+owner 直拉+反熵加强，子系统不自建 gossip）——网关快照（SBE base64）写本地成员属性 job-stream-snapshot，broker 成员监听 METADATA_CHANGED 解析喂协调器 applySnapshot（代次幂等合并）；v1.13 gossip 三件套（digest/digestReply/batch 消息与机制）、种子发送、SNAPSHOT 请求-应答与 SnapshotAck 全部删除（非法快照 broker 记日志丢弃）；稳态零流量、万级收敛 ~3s；观察项=高频变更 owner 拉取热点（集群层扩 candidates 缓解）
- 数据面分区化（v1.6）：数据面消息/DeliveredJob 携带分区号，broker 按分区持 leader 绑定与落点，gateway 水位细分 (jobType, 分区) 轮询选区、resolve/失败回报按分区记忆精确改投；job-push 与流注册保持全局
- 装配：JobStreamBootstrapStep（broker bootstrap）构建 JobStreamCoordinator / CoordinatorJobStreamer / PushFailureFallback 并 submitActor，包为 JobStreamDispatcher 挂到启动上下文（ClusterRaftStep 消费），SWIM 成员移除自动摘桶
