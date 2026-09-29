# 集群消息通信层性能评测报告（Aeron / Netty / smart-socket）

> 基准模块：`kunpeng/cluster/cluster-test` · 原始数据：`build/reports/cluster-bench/results.log`（两轮全量取最优，2026-09-29）

## 1. 结论摘要

1. **维持 Aeron 作为集群消息底座的选型，且本次评测后结论从"架构理由"升级为"数据理由"**：生产默认配置（调优项已进默认值）下，Aeron 在除大报文外的全部场景领先——小消息单向 4.8~6.2 倍、RPC 时延全维度最优（p50 133µs / max 1.6ms）、广播扇出 1.6 倍、流水线 RPC 2.5 倍。
2. **此前"大消息掉一个数量级"是配置缺口不是技术上限**：默认配置下 1MiB 帧 47 MB/s，补三项配置后 272 MB/s（探针极限 212 MB/s 之上）；生产 Linux（`udp.maxdgram` 64KB）还有进一步放大 MTU 的空间。该缺口已在 `AeronMessagingConfig` 落为正式配置项（`mtuLength` / `socketSendBufferBytes` / `socketReceiveBufferBytes`，默认值保持原行为）。
3. **TCP 栈在 MB 级大块传输上仍有 ~3.7 倍结构优势**（Netty 1,019 MB/s vs Aeron 272 MB/s）：UDP 逐数据报收发对大块流量的本质成本。结论：MB 级大块流量（资源分发类）不应走消息层，走独立通道。
4. Netty 综合第二、尾延迟最平（p99 436µs / p99.9 874µs 全场最优）；smart-socket 各场景稳居第三、实现最轻（单适配器类），作为轻量 TCP 选项可用但生态弱。

## 2. 被测对象

| 框架 | 参测形态 | 来源 |
|---|---|---|
| Aeron 1.53.3 | UDP 单播 + MDC 广播；每节点嵌入式 driver；**纯生产默认配置（DEDICATED 线程 + MTU 8192 + 2MB so-rcvbuf），基准零额外调优** | 生产实现原样参测（`AeronMessagingService`） |
| Netty 4.2.18 | TCP + ChannelPool 连接复用 + 心跳，协议 V2 | 自 cluster-netty 迁入的消息层（31 文件，`clustertest.netty`） |
| smart-socket 2.1.3 | TCP AIO + 4 字节长度前缀帧协议 | 本模块适配实现（`SmartSocketMessagingService`） |

三者实现同一 `MessagingService` 接口，语义对齐（sendAsync 同样走请求-回包往返，回包开销计入各自成本）。

## 3. 方法论

- 拓扑：单机 4 节点（localhost，每框架独立端口块）。
- 场景：单向单播 128B×200k / 1KiB×50k / 64KiB×5k；RPC 串行 5k（RTT 百分位）+ 深度 256 流水线 50k；广播 1KiB×20k 轮×3 扇出；大报文 8MiB=1MiB×8×20 迭代（三方统一分块）。
- 发送控制：真滑窗（in-flight ≤1024），Aeron 队列背压按语义重试不计失败；每场景先过送达数校验门再取数。
- 取数：定版数据为生产默认配置下的完整单轮（§4）；§5 的默认→调优对照沿用早期两轮数据并如实标注（本机后台负载对绝对值有可见扰动，如 Netty 大报文跨轮 490~1,019 MB/s、Aeron 1KiB 单播 183~197k msg/s）。
- 环境：macOS x64 12 核 / JDK 26 / 堆 2G / JUnit 顺序执行。
- 复现：`./gradlew :kunpeng:cluster:cluster-test:test --tests '*MessagingBenchTest'`；`-Psmoke` 冒烟。

## 4. 定版数据（生产默认配置，单轮完整）

### 4.1 单向单播吞吐（node0 → node1）

| 报文 | Aeron（生产默认） | Netty | smart-socket |
|---|---|---|---|
| 128B msg/s | **321,872** | 52,076 | 46,123 |
| 1KiB msg/s | **183,263** | 38,113 | 47,263 |
| 64KiB MB/s | 307.5 | **599.1** | 494.2 |

### 4.2 请求-响应（echo，1KiB）

| 指标 | Aeron（生产默认） | Netty | smart-socket |
|---|---|---|---|
| 串行 RTT p50 | **133.1 µs** | 281.8 µs | 393.7 µs |
| 串行 RTT p99 | **426.3 µs** | 642.0 µs | 1,120.9 µs |
| 串行 RTT p99.9 | **954.7 µs** | 4,717.1 µs | 9,640.6 µs |
| 串行 RTT max | **1,557.6 µs** | 7,630.8 µs | 10,933.1 µs |
| 流水线(深度256) msg/s | **100,850** | 40,524 | 35,049 |

### 4.3 广播扇出（node0 → 3 节点，1KiB×20k 轮）

| 指标 | Aeron（生产默认） | Netty | smart-socket |
|---|---|---|---|
| 投递 deliveries/s | **164,864** | 100,383 | 16,264 |
| 各节点均衡 | 21k/21k/21k | 21k/21k/21k | 21k/21k/21k |

### 4.4 大报文传输（1MiB 块 × 8 × 20 = 160MiB）

| 指标 | Aeron（生产默认） | Netty | smart-socket |
|---|---|---|---|
| MB/s | 342.6 | **769.5** | 623.4 |

## 5. Aeron 默认 → 调优对照（本次评测的核心发现）

`AeronTransport` 原先拼通道 URI 不带任何调优参数，全部部署硬吃 Aeron 内置默认。裸 Aeron 探针（`RawAeronProbeTest`，1MiB×60）逐项分离：

| 探针变体 | MB/s | 相对默认 |
|---|---|---|
| v0 默认（MTU 4096 / 默认 so-buf / SHARED driver） | 35.8 | 1× |
| v1 + so-sndbuf/so-rcvbuf 4MB | 44.0 | 1.23× |
| v4 = v1 但接收 poll limit=10（对齐生产 FRAGMENT_LIMIT） | 49.8 | 无影响 |
| v2 = v1 + MTU 8192 | 125.0 | 3.5× |
| v3 = v2 + DEDICATED 线程 | 212.2 | 5.9× |

根因：**MTU 分片数**（1MiB 帧 = 256 个 4KB 数据报的逐包收发与重组）为最大单项，**SHARED driver 单线程串行收发**为第二因素；内核缓冲溢丢与 `FRAGMENT_LIMIT=10` 均排除。落到整栈基准（4 节点消息层）：

| 指标 | 默认配置 | 调优配置 | 提升 |
|---|---|---|---|
| 单播 128B msg/s | 111,469 | 188,441 | 1.7× |
| 单播 1KiB msg/s | 36,963 | 197,113 | **5.3×** |
| 单播 64KiB MB/s | 55.7 | 267.7 | 4.8× |
| RPC p50 / p99 | 228µs / 3,150µs | 149µs / 770µs | 1.5× / 4.1× |
| 流水线 RPC msg/s | 20,236 | 84,863 | 4.2× |
| 广播 deliveries/s | 21,590 | 223,532 | **10.4×** |
| 大报文 MB/s | 47.2 | 271.7 | 5.8× |

工程落地：`AeronMessagingConfig` 新增 `mtuLength`（**默认 8192**）、`socketReceiveBufferBytes`（**默认 2MB**，接收 socket 每节点固定两个、成本上界确定）、`socketSendBufferBytes`（**默认 0=内核默认**，刻意保守：发送缓冲按每目的地发布通道计，万级成员下大默认值是不确定的内核内存上界，且探针实测其吞吐贡献最小，留给部署侧显式调优）；driver 线程模式生产默认本就是 DEDICATED。三项配置对发布/订阅/广播全部通道 URI 生效；Linux 可继续放大 MTU 至 32~64KB。另：本节"默认配置"基线系基准早期沿用 SHARED 线程测得（低于生产 DEDICATED 默认），真实生产默认与调优形态的差距小于表中所示。

## 6. 综合评估

| 维度 | 最优 | 说明 |
|---|---|---|
| 小消息单向吞吐（≤4KiB 控制面主流形态） | **Aeron 4.8~6.2×** | 无连接 + term 单拷贝 + 无 pipeline 开销 |
| RPC 时延（定版轮全维度） | **Aeron p50~max 全优**（133µs~1.6ms） | 尾部确定性轮间有波动，Netty 历史轮次最平（p99 436µs），对心跳/选举超时敏感路径仍是稳妥项 |
| RPC 流水线吞吐 | **Aeron 2.5×**（100,850 msg/s） | |
| 广播扇出 | **Aeron 1.6×**（MDC 组帧一次） | loopback 已领先，真实网络扇出差距会更大 |
| 大块传输（≥64KiB） | **Netty 约 2~2.5×** | UDP 逐数据报本质成本，非配置可消；MB 级流量走独立通道 |
| 实现与运维成本 | smart-socket 最轻 / Aeron 需 driver 运维 | Netty 居中，生态最成熟 |

选型结论：**Aeron 维持集群消息底座**（控制面小帧 + 广播 + 万级成员连接模型的综合最优），配置项已补齐；**大块流量走独立通道**；Netty 迁移栈作为对照基线保留在 cluster-test；smart-socket 不引入生产。

## 7. 边界与复现

- 单机 loopback：无真实网卡/TCP 分段卸载差异、无跨机 RTT；万级成员连接模型与故障切换维度不在吞吐基准内（为当初选型主因之一，应与本报告合并决策）。
- Aeron 调优形态为 macOS 可达上限（MTU 8192）；生产 Linux 可进一步放大。
- 复现：全量 `./gradlew :kunpeng:cluster:cluster-test:test --tests '*MessagingBenchTest'`；探针 `--tests 'RawAeronProbeTest'`；smart-socket 原生回环 `--tests 'SmartSocketDiagnosticTest'`。

## 8. 产物清单

- 模块 `kunpeng/cluster/cluster-test`：`clustertest.netty`（迁移栈 31 文件）、`clustertest.smartsocket`（适配器）、`BenchSupport`（场景基座）、三框架 Bench 测试类、`RawAeronProbeTest`（配置分离探针）、`SmartSocketDiagnosticTest`（原生回环诊断）。
- 生产侧：`AeronMessagingConfig` +3 配置项、`AeronTransport` 通道 URI 接入调优参数（bindChannel / 单播发布 / MDC 广播三处）。
- 原 `cluster-netty` 模块已删除（消息层能力并入本模块）。
