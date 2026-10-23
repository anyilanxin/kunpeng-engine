[English](CHANGELOG.md) | 简体中文

# 更新日志

本文件记录项目的所有显著变更。格式遵循 [Keep a Changelog](https://keepachangelog.com/)，条目与仓库提交约定一致（`feat` / `fix` / `refactor` / `perf`）。

## [Unreleased]

## [2026.9.0-alpha1] - 2026-10-20

首个公开 alpha：自研微服务编排引擎——BPMN 2.0 执行、分区事件日志、Raft 复制、gateway/broker 分离、无关系型数据库、sink 日志外发。

### 新增

**引擎与流程执行**

- 完整业务引擎处理链路：协议 Record 与存储层（structpack key-id 自描述帧、RocksDB 持久化）
- 引擎侧表达式求值命令链
- 跨分区命令派发与并行/串行管线（集群调度子系统）
- 启动恢复链：以持久化 committed position 播种 eventlog 恢复，引擎与调度启动时重放已提交事件
- 数据手术首期：双侧单副本冻结排水与幂等状态迁移
- DMN 引擎与系统脚本模块
- 部署的业务扩展数据（resource 级 extendedData）

**Job 系统**

- 分布式 job 推拉派发：稳定会话聚合订阅、竞争消费、broker 侧 byType 扁平索引轮转、deadline 簿记、`/actuator/jobstreams` 观测端点
- 默认流处理器（stream job worker）

**集群与通信**

- 集群通信底座切换 Aeron（UDP 单播、三平面端口模型）；业务通信面独立端口实例与节点端口注册表；自寻址消息经 Aeron IPC 短路
- SWIM 成员实体统一与搭车成员扩散的元数据通道
- leader 权威元数据读取（raft 分区按模式读取、leader 路由读门面、跨分区命令经统一拓扑服务路由）
- 集群消息可观测性（丢弃的广播与无处理者的 subject）

**快照与引导**

- 跨分区 bootstrap 快照传输链（infoSent 状态机、三层校验、分块 CRC）
- 快照存储统一到 bootstrap/merge/raft 三包，带合并结果通知，raft 快照先行拍摄
- raft 快照元数据携带 process/sink 位置与快照时间，真实位置供应者接入快照提供者

**Journal**

- journal 强化：FilePreallocator 段文件预分配、CRC32c 校验和热路径、建段残留文件"删除后重建"加上界

**网关与 API**

- 完整 gateway REST API（Spring Framework 7 原生版本化 `/api/{version}`）；内嵌 gateway 为默认形态（REST 条件装配）
- gateway gRPC 端口自集群 start-port 锚点推导

**Sink 与背压**

- sink 外发子系统：RecordSink SPI 生命周期、位置确认与重试
- rdbms sink：TableName 注册表命名、列名对齐协议 record key、权威 `SINK_POSITION` 位置表、时序流实例行、`tableNameCase`、auto-ddl 表注释
- sink 分区本地查询引擎（`SinkQueryService`）
- sink uber 打包与对接指南
- 业务面准入的 sink 滞后背压闸门

**观测与文档**

- monitor 观测栈（Prometheus + Grafana，六分节单面板）与指标↔面板覆盖门禁
- 中英双语用户指南（启动形态与端口模型）、README 总览图、sink 对接指南、BPMN 2.0.2 规范 PDF

**生态**

- connector 树迁入引擎并接入构建（REST 与脚本连接器开箱即用、runtime、element-template 生成器）
- 客户端 SDK（client-java + Spring Boot starter）

### 变更

- broker-client 合并为单模块（core/business/admin），统一 envelope 与 SBE 传输 schema
- 网络配置拆分 cluster/broker/gateway 三面；dist 装配 profile 收紧
- rdbms sink 不再把位置回推引擎（由权威位置表承接）
- structpack id 管理改为最小空闲分配（移除标记账本）
- BPMN 元素/类型枚举抽取到 bpm-common；DMN 引擎重构
- 客户端 SDK 包迁移至 `com.anyilanxin.kunpeng`
- 三框架消息层基准定案 Aeron 后删除 cluster-netty 模块
- BOM 依赖整体刷新

### 修复

- 网关绑定与通告地址分离：默认通配绑定 + 显式可达通告地址（修"no reachable gateway"）
- 关机链：驱动 TO_INACTIVE 逐层关闭并授予 checker 关闭权威（修关闭期定时任务触碰已释放 RocksDB 事务导致的 JVM 崩溃）
- 长轮询 job 拉取在客户端截止先于轮询窗口到期时不再误判为失败（DEADLINE_EXCEEDED 按空批处理）
- 批量激活扫描尊重 `maxJobsActivate`；失败的激活探针浮出报告；worker 属性缺省时保留 `streamEnabled`
- ActorCell 睡眠后的复查不再无门扫描订阅列表（票差+队列复查）；lane 池关闭改在调度 actor 上执行；drain 批让位不再饿死 actor 控制命令
- SBE 空变长字段解码（length=0 也必须推进 limit）与 job stream wire 编解码字段序
- structpack 零声明容器纯透传不再丢数据
- 资源分发记账与重试语义重设计（ACK 回填分区 id）
- sink：启动失败按 error 级记录日志、恢复位置钳制到日志尾
- 快照调度配置边界与 entry 触发阈值校验；listener 步骤关机容忍缺失的 raft 分区
- 业务面寻址：本节点直读与远端未就绪的有界重解析
- JobLifeCycle wire lookup 补上缺失的 TIME_OUT 分支（回放不再越界异常）
