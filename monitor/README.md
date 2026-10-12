# Kunpeng Monitor

Kunpeng 引擎的本地监控观测栈：Prometheus 采集 + Grafana 面板，一条命令拉起。

引擎自身（gateway / broker）通过 Spring Boot Actuator 在**管理端口（默认 9600）**的 `/actuator/prometheus` 暴露全部 Micrometer 指标；本目录只负责采集与展示，不包含引擎进程。

## 目录结构

```
monitor/
├── docker-compose.yml                  # Prometheus + Grafana 容器编排
├── Makefile                            # up / down / restart / clean / logs / open
├── .env                                # Grafana 地址与账号（compose 与 Makefile 共用）
├── prometheus/
│   └── prometheus.yml                  # 抓取配置（engine 节点列表在此维护）
└── grafana/
    ├── provisioning/
    │   ├── datasources/datasources.yml # Prometheus 数据源（自动装配）
    │   └── dashboards/dashboard.yml    # 面板文件供给（Kunpeng 目录，自动加载）
    └── dashboards/
        └── kunpeng.json                # 全系统单面板（六个分节，80 个图表）
```

## 快速开始

```bash
cd monitor
make up          # 拉起 Prometheus(9090) + Grafana(3000)
make open        # 打开 Grafana（默认账号 admin / kunpeng，见 .env）
make down        # 停止（保留数据卷）
make clean       # 停止并清空数据卷
```

面板放在 Grafana 的 **Kunpeng** 目录下，随 provisioning 自动加载，修改 JSON 文件后约 30s 自动热更新。

## 接入引擎节点

1. 引擎节点开启指标暴露（dist 发行版默认已开）：管理端口 `management.server.port`（默认 9600）+ `/actuator/prometheus`。
2. 单机多节点需给每个节点分配不同管理端口（如 gateway 9600、broker-1/2/3 用 9601/9602/9603，在各节点配置覆盖）。
3. 编辑 `prometheus/prometheus.yml` 的 target：
   - 引擎跑在宿主机：保持 `host.docker.internal:<port>`（compose 已为 Linux 映射 host-gateway）；
   - 引擎跑在其他主机/容器：替换为对应 `host:port`，可增删 job。

面板顶部的 `job` / `partition` / `sink_id` 变量按 Prometheus label 自动发现，无需改面板。

## 面板结构

全系统为**一块面板** `Kunpeng`（uid=`kunpeng`），按行分六个分节，自上而下覆盖整个引擎：

| 分节 | 覆盖内容 |
|------|---------|
| 总览 Overview | 集群成员数、Leader 分区、任期、角色时间线、日志水位、追加延迟、提交速率、EventLog 水位、Sink 交付、复制滞后、选举、堆使用 |
| Raft 复制与共识 | 按 Follower 的追加延迟/速率、未复制条目、复制滞后字节、心跳、选举、投票、Raft 消息、journal（追加/flush/段数/大小）、快照复制与安装、压缩、bootstrap/join、再平衡 |
| EventLog 事件日志 | written/committed/processed 三水位与水位差、定序/提交速率、AIMD 拥塞窗口、在途与驱逐、窗口/非法拒绝、烧毁 position、按上下文与原因的拆分 |
| 调度器 Scheduler | actor 生命周期、队列深度、失败数、定时器、阻塞外包、job 池；引擎流程定时器耗时/滞后/排队/去重表 |
| Sink 日志外发 | 按 sink 的交付/跳过速率、拾起延迟、处理耗时、交付/确认水位、外发积压、运行相位 |
| JVM 与运行时 | 堆/非堆、GC、线程、CPU、HTTP 请求（REST 面）、日志事件、文件描述符、系统负载 |

面板顶部变量：`job`（节点角色）、`partition_group` / `partition`（分区）、`sink_id`（外发通道），均按 Prometheus label 自动发现。

## 指标来源

面板全部基于引擎 Micrometer 指标（各模块 `*MetricsDoc` 定义），关键命名约定：

- Timer（如 `atomix_append_entries_latency`）导出为 `*_seconds_count / *_seconds_sum / *_seconds_max`，延迟均值用 `_sum/_count` 比值；
- Counter 导出带 `_total` 后缀（如 `eventlog_append_count_total`）；
- Raft 角色 gauge 数值映射：0=INACTIVE 1=PASSIVE 2=PROMOTABLE 3=FOLLOWER 4=CANDIDATE 5=LEADER；
- Sink 相位 gauge：0=running 1=paused 2=soft paused 3=closed。

**加指标的标准**：新增/修改引擎指标必须连带补对应面板（有指标就有面板），收口跑覆盖检查：

```bash
python3 .agents/skills/monitor-dashboards/check-panels.py   # 退出码 0 = 指标/面板全对齐
```

面板撰写规范见技能 `.agents/skills/monitor-dashboards/`。
