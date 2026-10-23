# Kunpeng 快速上手：启动与端口指南

面向使用发行包部署、启动 Kunpeng 引擎的用户：如何构建产物、选择启动形态、规划端口以及用客户端接入。构建与装配的更多细节见 [dist/README.md](../../dist/README.md)。

[English](getting-started.md) | 简体中文

## 1. 环境要求

- JDK 25
- 从源码构建需要私仓凭据（环境变量 `ALIYUN_REPO_USERNAME` / `ALIYUN_REPO_PASSWORD` 等，见 `buildSrc/common-config.gradle`）

## 2. 构建产物

```bash
# 可执行 fat jar：dist/build/libs/kunpeng-<version>.jar
./gradlew :dist:bootJar

# 分发目录/压缩包（bin/kunpeng 启动脚本 + lib + config）
./gradlew :dist:installDist
./gradlew :dist:distZip

# Docker 镜像（jib，本地镜像 kunpeng:<version>）
./gradlew :dist:jibDockerBuild
```

## 3. 必需 JVM 参数

由于 agrona 限制，直接 `java -jar` 启动 fat jar 时必须加上以下两条（`--add-opens` 按模块/包粒度生效，一条只开一个包，互不涵盖）：

```text
--add-opens=java.base/jdk.internal.misc=ALL-UNNAMED
--add-opens=java.base/java.util.zip=ALL-UNNAMED
```

分发包 `bin/kunpeng` 脚本与 jib 镜像已内置这两条参数；缺失时启动即抛 `InaccessibleObjectException`。

## 4. 端口模型

### 4.1 默认端口

单机默认形态（broker + 内嵌 gateway）启动后的监听端口：

| 端口 | 用途 | 协议 | 配置项 | 取值方式 |
|------|------|------|--------|----------|
| 2024 | HTTP（REST / OpenAPI） | HTTP（TCP） | `server.port` | 固定默认 |
| 9600 | Actuator（健康检查、指标、Prometheus） | HTTP（TCP） | `management.server.port` | 固定默认 |
| 24108 | gRPC 客户端接入（网关） | gRPC（HTTP/2，TCP） | `kunpeng.gateway.network.port`（内嵌网关为 `kunpeng.broker.gateway.network.port`） | 推导：start-port − port-offset |
| 24109 | 集群 membership（SWIM 成员协议） | Aeron（可靠 UDP） | `kunpeng.cluster.start-port` | 锚点端口 |
| 24110 | broker 业务（raft）通信 | Aeron（可靠 UDP） | `kunpeng.broker.network.port` | 推导：start-port + port-offset |

### 4.2 端口推导规则：显式优先、缺省推导

引擎端口以 `kunpeng.cluster.start-port`（默认 24109）为锚点、`kunpeng.cluster.port-offset`（默认 1）为偏移量推导：

```text
membership（SWIM）  = start-port            （24109）
broker 业务（raft） = start-port + offset   （24110）
gateway gRPC        = start-port − offset   （24108）   ← 与业务端口对称，一个加一个减
```

- **显式优先**：任一平面显式配置了 `port`，该平面不再推导，使用显式值；其余平面继续推导。
- **缺省推导**：只改 `start-port`（或 `port-offset`），三个引擎端口整体平移，无需逐个配置。
- `port-offset` 必须 ≥ 1（默认 1，启动时校验）；同时要求 `start-port − port-offset > 0` 且 `start-port + port-offset < 65535`。
- 同机多节点部署时，建议各节点 `start-port` 步进 ≥ 10：每个节点实际占用 `start-port − 1` 到 `start-port + 1` 三个端口。

## 5. 启动形态

同一个发行包支持三种启动形态，由 `start.type` 与内嵌 gateway 开关（`kunpeng.broker.gateway.enable`，默认 `true`）决定：

| 形态 | 启动方式 | 说明 |
|------|----------|------|
| broker + gateway 一起启动（默认） | 不设 `start.type` 或 `-Dstart.type=broker` | 单进程，broker 内嵌 gateway，同时提供引擎能力与 gRPC 客户端接入 |
| 单独启动 broker | `-Dstart.type=broker -Dkunpeng.broker.gateway.enable=false` | 关闭内嵌 gateway，纯引擎/存储节点，客户端接入走独立 gateway |
| 单独启动 gateway | `-Dstart.type=gateway` | 独立接入层进程，加入已有 broker 集群，转发客户端请求 |

`start.type` 支持命令行参数（`--start.type=gateway`）、系统属性（`-Dstart.type=gateway`）、环境变量（`START_TYPE=gateway`）三种指定方式，按 Spring 优先级取值。

### 5.1 默认形态（broker + 内嵌 gateway）

```bash
java --add-opens=java.base/jdk.internal.misc=ALL-UNNAMED --add-opens=java.base/java.util.zip=ALL-UNNAMED \
  -jar kunpeng-<version>.jar
```

### 5.2 单独启动 broker

```bash
java -Dstart.type=broker -Dkunpeng.broker.gateway.enable=false \
  --add-opens=java.base/jdk.internal.misc=ALL-UNNAMED --add-opens=java.base/java.util.zip=ALL-UNNAMED \
  -jar kunpeng-<version>.jar
```

### 5.3 单独启动 gateway

需要已有 broker 集群；gateway 复用 `kunpeng.cluster.*` 配置（node-id、seed-nodes、start-port）加入集群。与 broker 同机部署时错开 `start-port` 即可，gRPC 端口随 `start-port − offset` 自动推导：

```bash
java -Dstart.type=gateway \
  -Dserver.port=2025 -Dmanagement.server.port=9601 \
  -Dkunpeng.cluster.node-id=gateway01 \
  -Dkunpeng.cluster.start-port=24219 \
  --add-opens=java.base/jdk.internal.misc=ALL-UNNAMED --add-opens=java.base/java.util.zip=ALL-UNNAMED \
  -jar kunpeng-<version>.jar
```

上例 gateway 的 gRPC 端口推导为 24218（24219 − 1），membership 端口 24219；如需固定 gRPC 端口，显式配置 `kunpeng.gateway.network.port`。

## 6. 分区配置与集群引导

### 6.1 分区配置

引擎内部分为两个 raft 平面，分区相关配置如下：

| 平面 | 配置位置 | 关键配置 | 默认值 |
|------|----------|----------|--------|
| 业务面（流程数据分区） | `kunpeng.broker.raft.partitioning.*` | `partitions-count` / `replication-factor` / `scheme`（`round-robin` / `fixed`） | 1 / 1 / `round-robin` |
| 管理面（集群元数据 raft） | `kunpeng.broker.manage.*` | `replication-factor` / `init-broker-id` | 1 / 必填无默认 |

- `partitions-count` 与两个 `replication-factor` 启动时校验 ≥ 1；副本数建议奇数（偶数仅告警不阻断）；
- 业务分区 ID 由 `partitions-count` 派生为 1..N；`scheme: fixed` 时需在 `fixed` 中给出完备的 broker→分区映射。

### 6.2 全新系统的引导流程

1. 集群首次启动时，node-id 等于 `kunpeng.broker.manage.init-broker-id` 的节点成为初始节点，创建管理面 raft 分区（初始单成员，自身为 leader）；
2. 管理分区就绪后自动写入初始化记录：管理面按 `manage.replication-factor` 扩副本；业务面按 `partitions-count × replication-factor` 生成分区放置（round-robin 或 fixed）；
3. 调度子系统把分区执行明细下发到各 broker，各自启动 raft 分区与事件日志，leader 选举完成后集群可用；
4. 其余节点以 joining 方式加入已有集群；之后的扩缩容、分区再均衡都复用同一条调度计划链路。

### 6.3 init-broker-id 配置约束

- 必须填**某个真实节点的裸 node-id**（如 `node01`），不要带 `broker@` 前缀；
- **全集群所有节点必须配同一个值**：它只在初始引导时由初始节点消费，但每个节点的配置里都要有（空白会启动失败）；
- 配错值（不指向任何节点）时不会报错，未匹配节点会一直等待初始节点引导，集群无法形成——部署前务必核对。

## 7. 本机多节点 demo（4 节点）

node01 的 membership 端口为 24109，与默认种子（`127.0.0.1:24109`）一致，其余节点保持默认种子即可加入集群。全部节点使用发行包默认的 `init-broker-id: node01`（初始引导节点），无需额外配置。每节点只需设置 `node-id`、`start-port` 与 HTTP/actuator 端口，gRPC 与业务端口自动推导：

| 节点 | node-id | start-port | gRPC（−1） | membership | 业务（+1） | HTTP | actuator |
|------|---------|-----------|-----------|------------|-----------|------|----------|
| node01 | node01（默认） | 24109（默认） | 24108 | 24109 | 24110 | 2024（默认） | 9600（默认） |
| node02 | node02 | 24119 | 24118 | 24119 | 24120 | 2025 | 9601 |
| node03 | node03 | 24129 | 24128 | 24129 | 24130 | 2026 | 9602 |
| node04 | node04 | 24139 | 24138 | 24139 | 24140 | 2027 | 9603 |

```bash
# node01
java -Dkunpeng.cluster.node-id=node01 -Dkunpeng.cluster.start-port=24109 \
  --add-opens=java.base/jdk.internal.misc=ALL-UNNAMED --add-opens=java.base/java.util.zip=ALL-UNNAMED \
  -jar kunpeng-<version>.jar

# node02
java -Dserver.port=2025 -Dmanagement.server.port=9601 \
  -Dkunpeng.cluster.node-id=node02 -Dkunpeng.cluster.start-port=24119 \
  --add-opens=java.base/jdk.internal.misc=ALL-UNNAMED --add-opens=java.base/java.util.zip=ALL-UNNAMED \
  -jar kunpeng-<version>.jar

# node03
java -Dserver.port=2026 -Dmanagement.server.port=9602 \
  -Dkunpeng.cluster.node-id=node03 -Dkunpeng.cluster.start-port=24129 \
  --add-opens=java.base/jdk.internal.misc=ALL-UNNAMED --add-opens=java.base/java.util.zip=ALL-UNNAMED \
  -jar kunpeng-<version>.jar

# node04
java -Dserver.port=2027 -Dmanagement.server.port=9603 \
  -Dkunpeng.cluster.node-id=node04 -Dkunpeng.cluster.start-port=24139 \
  --add-opens=java.base/jdk.internal.misc=ALL-UNNAMED --add-opens=java.base/java.util.zip=ALL-UNNAMED \
  -jar kunpeng-<version>.jar
```

> demo 默认单副本（`replication-factor: 1`）；多节点生产部署建议配置奇数副本（`kunpeng.broker.raft.partitioning.replication-factor`）。

## 8. Docker 启动

```bash
docker run -d --name kunpeng \
  -p 2024:2024 -p 24108:24108 -p 9600:9600 -p 24109-24110:24109-24110 \
  -v $PWD/data:/kunpeng/data -v $PWD/logs:/kunpeng/logs \
  -e TZ=Asia/Shanghai \
  kunpeng:<version>
```

## 9. 客户端接入

客户端通过网关的 gRPC 端口（默认 `127.0.0.1:24108`）接入。Java SDK 示例：

```java
KunpengClient client =
    KunpengClient.newClientBuilder()
        .grpcAddress(URI.create("http://127.0.0.1:24108"))
        .usePlaintext()
        .build();
```

客户端内置网关发现与负载均衡：只需配置一个网关地址，即可自动发现集群内其余网关并动态分摊负载。SDK 与 Spring Boot starter 的完整说明见 [clients/client-java](../../clients/client-java) 与 [clients/spring-boot-starter-client](../../clients/spring-boot-starter-client)。

## 10. 常用配置项

配置可通过 `config/application-*.yml`、命令行 `-D` 系统属性或 `--` 参数指定（Spring 松散绑定）。发行包 `config/` 目录内含各形态全量配置模板。

| 配置项 | 默认值 | 说明 |
|--------|--------|------|
| `start.type` | `broker` | 启动类型：`broker` / `gateway` |
| `server.port` | `2024` | HTTP 端口（REST / OpenAPI） |
| `management.server.port` | `9600` | Actuator 端口 |
| `kunpeng.cluster.node-id` | `node01` | 节点 ID，集群内唯一 |
| `kunpeng.cluster.cluster-name` | `kunpeng-cluster` | 集群名称 |
| `kunpeng.cluster.seed-nodes` | `127.0.0.1:24109` | 种子节点地址列表（host:membership 端口） |
| `kunpeng.cluster.start-port` | `24109` | 锚点端口：membership = start-port |
| `kunpeng.cluster.port-offset` | `1`（≥1） | 端口偏移量：业务 +offset、gRPC −offset |
| `kunpeng.cluster.advance-host` | 自动探测 | 对外通告地址 |
| `kunpeng.broker.network.port` | 推导：start-port + offset（24110） | broker 业务（raft）通信端口 |
| `kunpeng.gateway.network.port` | 推导：start-port − offset（24108） | 独立网关 gRPC 端口 |
| `kunpeng.broker.gateway.network.port` | 推导：start-port − offset（24108） | 内嵌网关 gRPC 端口 |
| `kunpeng.broker.gateway.enable` | `true` | 是否启用 broker 内嵌 gateway |
| `kunpeng.broker.raft.partitioning.partitions-count` | `1` | 分区数 |
| `kunpeng.broker.raft.partitioning.replication-factor` | `1` | 业务分区副本数（多节点建议奇数） |
| `kunpeng.broker.manage.init-broker-id` | 无（必填） | 初始引导节点的裸 node-id，全集群所有节点配同一值 |
| `kunpeng.broker.manage.replication-factor` | `1` | 管理面 raft 副本数（建议奇数） |
| `kunpeng.broker.threads.cpu-thread-count` / `io-thread-count` | `2` / `2` | broker CPU / IO 线程数 |

## 11. 数据目录、日志与停机

- 数据目录：基于 `basedir`（默认当前目录），数据写入 `data/`；
- 日志文件：`logs/kunpeng-<启动类型>.log`；
- 优雅停机：`spring.lifecycle.timeout-per-shutdown-phase`（默认 30s）控制每个关闭阶段的最长等待；
- 默认激活 `standalone` profile；dev/test profile 下数据写入临时目录并在停机时删除。

## 相关链接

- [发行版装配与构建细节](../../dist/README.md)
- [Java 客户端 SDK](../../clients/client-java)
