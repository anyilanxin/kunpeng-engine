# Kunpeng 发行版启动说明

`dist` 模块将引擎装配为可独立运行的发行版：**同一个入口（`com.anyilanxin.Application`）支持单独启动 broker、单独启动 gateway、broker + gateway 一起启动三种形态**，可产出 fat jar、带启动脚本的分发包以及 Docker 镜像。

## 环境要求

- JDK 25
- 构建依赖私仓凭据（环境变量 `ALIYUN_REPO_USERNAME` / `ALIYUN_REPO_PASSWORD` 等，见 `buildSrc/common-config.gradle`）

## 构建产物

```bash
# 可执行 fat jar：dist/build/libs/kunpeng-<version>.jar
./gradlew :dist:bootJar

# 分发目录/压缩包（bin/kunpeng 启动脚本 + lib + config），当前版本 2026.9.0-alpha1
./gradlew :dist:installDist
./gradlew :dist:distZip

# Docker 镜像（jib，本地镜像 kunpeng:<version>）
./gradlew :dist:jibDockerBuild
```

## 必需 JVM 参数

由于 agrona 限制，必须添加：

```text
--add-opens=java.base/jdk.internal.misc=ALL-UNNAMED
```

分发包 `bin/kunpeng` 脚本与 jib 镜像已内置该参数；直接 `java -jar` 启动 fat jar 时必须手动加上，否则启动失败。

## 启动形态

同一个发行包支持三种启动形态，由 `start.type` 与内嵌 gateway 开关（`kunpeng.broker.gateway.enable`，默认 `true`）决定：

| 形态 | 启动方式 | 说明 |
|------|----------|------|
| broker + gateway 一起启动（默认） | 不设 `start.type` 或 `-Dstart.type=broker` | 单进程，broker 内嵌 gateway，同时提供引擎能力与 gRPC 客户端接入 |
| 单独启动 broker | `-Dstart.type=broker -Dkunpeng.broker.gateway.enable=false` | 关闭内嵌 gateway，纯引擎/存储节点，客户端接入走独立 gateway |
| 单独启动 gateway | `-Dstart.type=gateway` | 独立接入层进程，加入已有 broker 集群，转发客户端请求 |

`start.type` 三种指定方式按 Spring 优先级取值（命令行参数 > 系统属性 > 环境变量）：

```bash
java ... --start.type=gateway -jar kunpeng-<version>.jar   # 命令行参数
java -Dstart.type=gateway ... -jar kunpeng-<version>.jar   # 系统属性
START_TYPE=gateway java ... -jar kunpeng-<version>.jar     # 环境变量
```

### 1. broker + gateway 一起启动（默认形态）

```bash
java --add-opens=java.base/jdk.internal.misc=ALL-UNNAMED -jar kunpeng-2026.9.0-alpha1.jar
```

内嵌 gateway 默认开启，gRPC 监听 `kunpeng.broker.gateway.network.port`（默认 2024）。

### 2. 单独启动 broker

```bash
java -Dstart.type=broker -Dkunpeng.broker.gateway.enable=false \
  --add-opens=java.base/jdk.internal.misc=ALL-UNNAMED \
  -jar kunpeng-2026.9.0-alpha1.jar
```

关闭内嵌 gateway 后不再监听 2024 端口，客户端需通过独立 gateway 接入。

### 3. 单独启动 gateway

需要已有 broker 集群；gateway 复用 `kunpeng.cluster.*` 配置（node-id、seed-nodes、start-port）加入集群，gRPC 监听 `kunpeng.gateway.network.port`（默认 2024）：

```bash
java -Dstart.type=gateway \
  -Dserver.port=2021 -Dmanagement.server.port=9601 \
  -Dkunpeng.cluster.node-id=gateway01 \
  -Dkunpeng.cluster.network.start-port=2085 \
  -Dkunpeng.gateway.network.port=2084 \
  --add-opens=java.base/jdk.internal.misc=ALL-UNNAMED \
  -jar kunpeng-2026.9.0-alpha1.jar
```

与 broker 同机部署时注意错开端口：`start-port`（本例 2085，默认 seed `127.0.0.1:2026` 指向 broker node01 的 membership 端口）、gRPC 端口（本例 2084，避开 broker 内嵌 gateway 的 2024）。

## 默认端口与运行时目录

默认形态（broker + 内嵌 gateway）单机单节点启动后的监听端口：

| 端口 | 用途 |
|------|------|
| 2020 | HTTP（REST / OpenAPI） |
| 2024 | gRPC 客户端接入（broker 内嵌 gateway 或独立 gateway） |
| 9600 | Actuator（健康检查、指标、Prometheus） |
| 2026 ~ 2028 | 集群内部端口，由 `start-port`（2026）按偏移 1 依次分配：membership / client / business |

- 数据目录：基于 `basedir`（默认当前目录）；
- 日志文件：`kunpeng-<启动类型>.log`；
- 默认激活 `standalone` profile；dev/test profile 下数据写入临时目录并在停机时删除。

## 本机多节点 demo（4 节点）

node01 的 membership 端口为 2026，与默认 seed（`127.0.0.1:2026`）一致，其余节点保持默认 seed 即可加入集群：

```bash
# node01
java -Dserver.port=2021 -Dmanagement.server.port=9601 -Dkunpeng.cluster.node-id=node01 -Dkunpeng.cluster.network.start-port=2026 -Dkunpeng.broker.gateway.network.port=2024 --add-opens=java.base/jdk.internal.misc=ALL-UNNAMED -jar kunpeng-2026.9.0-alpha1.jar

# node02
java -Dserver.port=2022 -Dmanagement.server.port=9602 -Dkunpeng.cluster.node-id=node02 -Dkunpeng.cluster.network.start-port=2035 -Dkunpeng.broker.gateway.network.port=2034 --add-opens=java.base/jdk.internal.misc=ALL-UNNAMED -jar kunpeng-2026.9.0-alpha1.jar

# node03
java -Dserver.port=2023 -Dmanagement.server.port=9603 -Dkunpeng.cluster.node-id=node03 -Dkunpeng.cluster.network.start-port=2045 -Dkunpeng.broker.gateway.network.port=2044 --add-opens=java.base/jdk.internal.misc=ALL-UNNAMED -jar kunpeng-2026.9.0-alpha1.jar

# node04
java -Dserver.port=2024 -Dmanagement.server.port=9604 -Dkunpeng.cluster.node-id=node04 -Dkunpeng.cluster.network.start-port=2055 -Dkunpeng.broker.gateway.network.port=2054 --add-opens=java.base/jdk.internal.misc=ALL-UNNAMED -jar kunpeng-2026.9.0-alpha1.jar
```

## Docker 启动

```bash
docker run -d --name kunpeng \
  -p 2020:2020 -p 2024:2024 -p 9600:9600 -p 2026-2028:2026-2028 \
  -v $PWD/data:/kunpeng/data -v $PWD/logs:/kunpeng/logs \
  -e TZ=Asia/Shanghai \
  kunpeng:2026.9.0-alpha1
```

## 常用配置项

配置可通过 `config/application-*.yml`、命令行 `-D` 系统属性或 `--` 参数指定（Spring 松散绑定）。

| 配置项 | 默认值 | 说明 |
|--------|--------|------|
| `start.type` | `broker` | 启动类型：`broker` / `gateway` |
| `server.port` | `2020` | HTTP 端口 |
| `management.server.port` | `9600` | Actuator 端口 |
| `kunpeng.cluster.node-id` | `node01` | 节点 ID，集群内唯一 |
| `kunpeng.cluster.seed-nodes` | `127.0.0.1:2026` | 种子节点地址列表 |
| `kunpeng.cluster.network.start-port` | `2026` | 集群起始端口（membership=起始值，client=+偏移，business=+2×偏移，偏移默认 1） |
| `kunpeng.cluster.network.advance-host` | 自动探测 | 对外通告地址 |
| `kunpeng.broker.gateway.enable` | `true` | 是否启用 broker 内嵌 gateway |
| `kunpeng.broker.gateway.network.host` / `port` | `127.0.0.1` / `2024` | broker 内嵌 gateway 的 gRPC 监听 |
| `kunpeng.gateway.network.host` / `port` | 自动探测 / `2024` | 独立 gateway 的 gRPC 监听 |
| `kunpeng.broker.raft.partitioning.partitions-count` | `1` | 分区数 |
| `kunpeng.broker.raft.partitioning.replication-factor` | `1` | 副本数 |
| `kunpeng.broker.threads.cpu-thread-count` / `io-thread-count` | `2` / `2` | broker CPU / IO 线程数 |
