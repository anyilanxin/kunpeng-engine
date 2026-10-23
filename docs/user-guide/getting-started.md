# Kunpeng Getting Started: Startup & Ports Guide

For users deploying and starting the Kunpeng engine from the distribution: how to build artifacts, choose a startup form, plan ports, and connect clients. See [dist/README.md](../../dist/README.md) for full assembly details.

English | [简体中文](getting-started.zh-CN.md)

## 1. Requirements

- JDK 25
- Building from source requires private repository credentials (environment variables `ALIYUN_REPO_USERNAME` / `ALIYUN_REPO_PASSWORD`, see `buildSrc/common-config.gradle`)

## 2. Build Artifacts

```bash
# Executable fat jar: dist/build/libs/kunpeng-<version>.jar
./gradlew :dist:bootJar

# Distribution directory/archive (bin/kunpeng launch script + lib + config)
./gradlew :dist:installDist
./gradlew :dist:distZip

# Docker image (jib, local image kunpeng:<version>)
./gradlew :dist:jibDockerBuild
```

## 3. Mandatory JVM Arguments

Due to agrona constraints, launching the fat jar directly with `java -jar` requires the following two arguments (`--add-opens` works per module/package: one argument opens one package, they do not cover each other):

```text
--add-opens=java.base/jdk.internal.misc=ALL-UNNAMED
--add-opens=java.base/java.util.zip=ALL-UNNAMED
```

The distribution `bin/kunpeng` script and the jib image already include them; without them startup fails with `InaccessibleObjectException`.

## 4. Port Model

### 4.1 Default Ports

Ports listened on after a default single-machine startup (broker + embedded gateway):

| Port | Purpose | Protocol | Config | Value |
|------|---------|----------|--------|-------|
| 2024 | HTTP (REST / OpenAPI) | HTTP (TCP) | `server.port` | fixed default |
| 9600 | Actuator (health, metrics, Prometheus) | HTTP (TCP) | `management.server.port` | fixed default |
| 24108 | gRPC client access (gateway) | gRPC (HTTP/2, TCP) | `kunpeng.gateway.network.port` (embedded gateway: `kunpeng.broker.gateway.network.port`) | derived: start-port − port-offset |
| 24109 | Cluster membership (SWIM) | Aeron (reliable UDP) | `kunpeng.cluster.start-port` | anchor port |
| 24110 | Broker business (raft) traffic | Aeron (reliable UDP) | `kunpeng.broker.network.port` | derived: start-port + port-offset |

### 4.2 Port Derivation: Explicit Wins, Derived When Unset

Engine ports are derived from the anchor `kunpeng.cluster.start-port` (default 24109) with offset `kunpeng.cluster.port-offset` (default 1):

```text
membership (SWIM)   = start-port            (24109)
broker business     = start-port + offset   (24110)
gateway gRPC        = start-port − offset   (24108)   ← symmetric to the business port: one adds, one subtracts
```

- **Explicit wins**: once a plane has an explicit `port`, that plane stops deriving and uses the explicit value; the others keep deriving.
- **Derived when unset**: change only `start-port` (or `port-offset`) and all three engine ports shift together — no per-plane configuration needed.
- `port-offset` must be ≥ 1 (default 1, validated at startup); `start-port − port-offset` must be > 0 and `start-port + port-offset` < 65535.
- For multiple nodes on one machine, space `start-port` values by ≥ 10: each node occupies three ports, from `start-port − 1` to `start-port + 1`.

## 5. Startup Forms

The same distribution supports three forms, selected by `start.type` and the embedded gateway switch (`kunpeng.broker.gateway.enable`, default `true`):

| Form | How to start | Notes |
|------|--------------|-------|
| broker + gateway together (default) | no `start.type`, or `-Dstart.type=broker` | Single process, broker with embedded gateway: engine capabilities plus gRPC client access |
| broker only | `-Dstart.type=broker -Dkunpeng.broker.gateway.enable=false` | Disables the embedded gateway; pure engine/storage node, clients connect via standalone gateways |
| gateway only | `-Dstart.type=gateway` | Standalone access-layer process joining an existing broker cluster, forwarding client requests |

`start.type` accepts a command-line argument (`--start.type=gateway`), a system property (`-Dstart.type=gateway`), or an environment variable (`START_TYPE=gateway`), resolved by Spring precedence.

### 5.1 Default Form (broker + embedded gateway)

```bash
java --add-opens=java.base/jdk.internal.misc=ALL-UNNAMED --add-opens=java.base/java.util.zip=ALL-UNNAMED \
  -jar kunpeng-<version>.jar
```

### 5.2 Broker Only

```bash
java -Dstart.type=broker -Dkunpeng.broker.gateway.enable=false \
  --add-opens=java.base/jdk.internal.misc=ALL-UNNAMED --add-opens=java.base/java.util.zip=ALL-UNNAMED \
  -jar kunpeng-<version>.jar
```

### 5.3 Gateway Only

Requires an existing broker cluster; the gateway reuses `kunpeng.cluster.*` config (node-id, seed-nodes, start-port) to join. When co-located with brokers on one machine, just pick a distinct `start-port` — the gRPC port derives automatically as `start-port − offset`:

```bash
java -Dstart.type=gateway \
  -Dserver.port=2025 -Dmanagement.server.port=9601 \
  -Dkunpeng.cluster.node-id=gateway01 \
  -Dkunpeng.cluster.start-port=24219 \
  --add-opens=java.base/jdk.internal.misc=ALL-UNNAMED --add-opens=java.base/java.util.zip=ALL-UNNAMED \
  -jar kunpeng-<version>.jar
```

Above, the gateway's gRPC port derives to 24218 (24219 − 1) and its membership port is 24219; to pin the gRPC port, set `kunpeng.gateway.network.port` explicitly.

## 6. Partition Configuration & Cluster Bootstrap

### 6.1 Partition Configuration

The engine runs two raft planes:

| Plane | Config | Key settings | Default |
|-------|--------|--------------|---------|
| Business (process data partitions) | `kunpeng.broker.raft.partitioning.*` | `partitions-count` / `replication-factor` / `scheme` (`round-robin` / `fixed`) | 1 / 1 / `round-robin` |
| Management (cluster metadata raft) | `kunpeng.broker.manage.*` | `replication-factor` / `init-broker-id` | 1 / required, no default |

- `partitions-count` and both `replication-factor` values are validated ≥ 1 at startup; odd replication factors are recommended (even values only warn);
- Business partition IDs derive as 1..N from `partitions-count`; `scheme: fixed` requires a complete broker→partition mapping under `fixed`.

### 6.2 Bootstrap Flow of a Fresh System

1. On first startup, the node whose node-id equals `kunpeng.broker.manage.init-broker-id` becomes the initial node and creates the management raft partition (single member, itself as leader);
2. Once the management partition is ready, initialization records are written automatically: the management plane expands to `manage.replication-factor` replicas; the business plane generates placement for `partitions-count × replication-factor` partitions (round-robin or fixed);
3. The dispatch subsystem ships partition assignments to each broker, which start their raft partitions and event logs; the cluster becomes available once leader elections complete;
4. Further nodes join the existing cluster; scale-up/down and partition rebalancing reuse the same dispatch-plan chain.

### 6.3 init-broker-id Constraints

- Must be the **bare node-id of a real node** (e.g. `node01`) — no `broker@` prefix;
- **All nodes in the cluster must configure the same value**: it is consumed only by the initial node during bootstrap, but every node's config must carry it (blank fails startup);
- A wrong value (matching no node) produces no error — unmatched nodes wait forever for the initial node and the cluster never forms. Double-check before deploying.

## 7. Local Multi-Node Demo (4 Nodes)

node01's membership port is 24109, matching the default seed (`127.0.0.1:24109`); the other nodes join via the default seed. All nodes use the distribution default `init-broker-id: node01` (the initial bootstrap node) — no extra configuration needed. Each node only sets `node-id`, `start-port`, and HTTP/actuator ports — gRPC and business ports derive automatically:

| Node | node-id | start-port | gRPC (−1) | membership | business (+1) | HTTP | actuator |
|------|---------|-----------|-----------|------------|---------------|------|----------|
| node01 | node01 (default) | 24109 (default) | 24108 | 24109 | 24110 | 2024 (default) | 9600 (default) |
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

> The demo defaults to no replication (`replication-factor: 1`); for multi-node production deployments configure an odd replication factor (`kunpeng.broker.raft.partitioning.replication-factor`).

## 8. Docker

```bash
docker run -d --name kunpeng \
  -p 2024:2024 -p 24108:24108 -p 9600:9600 -p 24109-24110:24109-24110 \
  -v $PWD/data:/kunpeng/data -v $PWD/logs:/kunpeng/logs \
  -e TZ=Asia/Shanghai \
  kunpeng:<version>
```

## 9. Client Access

Clients connect via the gateway gRPC port (default `127.0.0.1:24108`). Java SDK example:

```java
KunpengClient client =
    KunpengClient.newClientBuilder()
        .grpcAddress(URI.create("http://127.0.0.1:24108"))
        .usePlaintext()
        .build();
```

The client has gateway discovery and load balancing built in: configure one gateway address and it discovers the cluster's other gateways and spreads load dynamically. Full SDK and Spring Boot starter documentation: [clients/client-java](../../clients/client-java) and [clients/spring-boot-starter-client](../../clients/spring-boot-starter-client).

## 10. Common Configuration

Configuration can be set via `config/application-*.yml`, `-D` system properties, or `--` arguments (Spring relaxed binding). The distribution `config/` directory ships full templates per form.

| Config | Default | Notes |
|--------|---------|-------|
| `start.type` | `broker` | Startup type: `broker` / `gateway` |
| `server.port` | `2024` | HTTP port (REST / OpenAPI) |
| `management.server.port` | `9600` | Actuator port |
| `kunpeng.cluster.node-id` | `node01` | Node ID, unique in the cluster |
| `kunpeng.cluster.cluster-name` | `kunpeng-cluster` | Cluster name |
| `kunpeng.cluster.seed-nodes` | `127.0.0.1:24109` | Seed node addresses (host:membership port) |
| `kunpeng.cluster.start-port` | `24109` | Anchor port: membership = start-port |
| `kunpeng.cluster.port-offset` | `1` (≥1) | Port offset: business +offset, gRPC −offset |
| `kunpeng.cluster.advance-host` | auto-detected | Advertised address |
| `kunpeng.broker.network.port` | derived: start-port + offset (24110) | Broker business (raft) port |
| `kunpeng.gateway.network.port` | derived: start-port − offset (24108) | Standalone gateway gRPC port |
| `kunpeng.broker.gateway.network.port` | derived: start-port − offset (24108) | Embedded gateway gRPC port |
| `kunpeng.broker.gateway.enable` | `true` | Enable the broker's embedded gateway |
| `kunpeng.broker.raft.partitioning.partitions-count` | `1` | Partition count |
| `kunpeng.broker.raft.partitioning.replication-factor` | `1` | Business partition replication factor (odd, for multi-node) |
| `kunpeng.broker.manage.init-broker-id` | none (required) | Bare node-id of the initial bootstrap node; same value on every node |
| `kunpeng.broker.manage.replication-factor` | `1` | Management raft replication factor (odd recommended) |
| `kunpeng.broker.threads.cpu-thread-count` / `io-thread-count` | `2` / `2` | Broker CPU / IO threads |

## 11. Data Directories, Logs, Shutdown

- Data directory: under `basedir` (current directory by default), data written to `data/`;
- Log files: `logs/kunpeng-<startup-type>.log`;
- Graceful shutdown: `spring.lifecycle.timeout-per-shutdown-phase` (default 30s) caps each shutdown phase;
- The `standalone` profile is active by default; dev/test profiles write data to a temporary directory removed at shutdown.

## Related Links

- [Distribution & assembly details](../../dist/README.md)
- [Java client SDK](../../clients/client-java)
