# Aeron 传输与 wire 协议（cluster-messaging · 01）

集群面与业务面共用的传输底座细节。位置：`kunpeng/cluster/cluster/src/main/java/com/anyilanxin/kunpeng/cluster/cluster/messaging/`（`AeronTransport`、`AeronProcessDriverHolder`、`AeronFrameCodec`、`AeronMessagingConfig` 等）。

## 传输底座（AeronTransport）

- **单代理线程**驱动收发与周期 tick（超时扫描、空闲淘汰）；未投递帧排有界队列，代理线程统一组帧/加密后 offer 给 Aeron
- **发送路径**：任意线程 `submit`/`submitBroadcast` 入有界队列（容量 `sendQueueCapacity`，默认 4096）→ 代理线程组帧 → Aeron offer 成功后完成 future；队列满显式失败（背压），绝不无限堆积
- **广播用 MDC**（`control-mode=manual`）：组帧/加密一次、驱动扇出到全部目的地；内存从「目的地数 × term」降为单条 term
- **空闲淘汰**：单播发布通道与 MDC 广播目的地均 60s 未使用自动回收（成员下线后释放独占 term 缓冲）
- **同端口双逻辑流**：`streamId`（1001，RPC/事件）与 `unicastStreamId`（1002，不可靠单向）
- **自寻址 IPC 短路**（`localIpcEnabled`，默认开）：目的地=本节点绑定地址时发布通道短路为 `aeron:ipc?term-length=...`（共享内存直拷，免 UDP 内核回环两次网络栈穿越）；接收侧为同一代理线程上的独立 IPC 订阅 + **独立 FragmentAssembler**（IPC 与 UDP 会话号空间独立，共享重组缓冲会串包）；帧格式/加密/分发管线不变，miss（地址不一致）只损失优化不损语义；MDC 广播自送达不走该短路
- **驱动持有**（`AeronProcessDriverHolder`）：每传输实例独占驱动（专属 aeron 目录与驱动线程）；刻意不做进程级共享——共享会把双实例的网络收发系统调用耦合进同一条 Sender/Receiver 线程，快照复制风暴期业务命令路径尾部劣化，破坏双面拆分要隔离的争用对

## 帧布局（AeronFrameCodec，小端）

明文帧首字节 `0x01`，加密帧首字节 `0xAE`（见「加密与滚动升级」）。

### REQUEST（kind=0，请求）

| 字段 | 宽度 | 说明 |
|---|---|---|
| version | 1 | 协议版本 |
| kind | 1 | 帧类型 |
| correlationId | 8 | 请求-应答关联 id |
| senderHostLen + host | 2 + n | 发送方主机（回地址址用） |
| senderPort | 4 | 发送方端口 |
| subjectLen + subject | 4 + n | 路由键 |
| payload | … | 消息体 |

### REPLY（kind=1，应答）

version / kind / correlationId / status(1) / payload。status：`0` OK、`1` no-handler（无订阅者）、`2` handler-exception（处理方抛错）。

### UNICAST（kind=2，不可靠单向）

version / kind / senderHostLen(2) + host / senderPort(4) / subjectLen(4) + subject / payload。

## 通道 URI

```
aeron:udp?endpoint=host:port|mtu=8192|so-sndbuf=...|so-rcvbuf=...
```

mtu / so-sndbuf / so-rcvbuf 由 `AeronMessagingConfig` 注入，0 值项跳过（沿用内核与驱动默认）。

## AeronMessagingConfig 参数表

| 参数 | 默认 | 说明 |
|---|---|---|
| `streamId` | 1001 | RPC/事件流 id |
| `unicastStreamId` | 1002 | 不可靠单向流 id |
| `termBufferLength` | 64MiB | RPC 流 term 缓冲 |
| `unicastTermBufferLength` | 4MiB | 单向流 term 缓冲 |
| `initialWindowLength` | -1 | ≤0 时自动取 `min(term/8, 16MiB)`，再被 so-rcvbuf 钳制 |
| `embeddedDriver` | true | 内嵌 Media Driver；false = 外置驱动（目录用 Aeron 默认目录，显式配置优先） |
| `driverThreadingMode` | DEDICATED | 驱动线程模式 |
| `driverTimeoutMs` | 10_000 | 驱动心跳超时；调试器暂停进程超过即驱动自杀 |
| `connectTimeout` | 5s | 连接超时 |
| `sendQueueCapacity` | 4096 | 发送/广播队列容量（背压阈值） |
| `mtuLength` | 8192 | 通道 MTU |
| `socketReceiveBufferBytes` | 2MB | 通道 so-rcvbuf |
| `socketSendBufferBytes` | 0 | 通道 so-sndbuf（0=内核默认） |
| `aeronDir` | null | null 时每驱动独占 `aeron-kunpeng-*` 临时目录（dirDeleteOnStart/OnShutdown 均开、term 稀疏文件） |
| `cryptoEnabled` / `cryptoKeys` / `sendCryptoKeyId` / `cryptoSenderId` | false / - / - / 节点 ID | 帧加密（见下） |
| `localIpcEnabled` | true | 自寻址消息短路走 `aeron:ipc`（见传输底座；配置面 `kunpeng.cluster.local-ipc-enabled`，仅同实例自发自收生效，业务面显式关闭） |

业务面派生配置（`BusinessMessagingServiceStep.aeronConfigOf`）：继承流 id、队列容量与加密设置，term 钳制 32MiB（业务面无快照大块，与 `kunpeng.broker.network` 的 maxMessageSize 默认对齐）。

驱动级校验（`AeronProcessDriverHolder.launch`）：socketRcv/Sndbuf 强制 ≥ 窗口；通道 so-rcvbuf 显式调小时驱动窗口随之收缩（Aeron 校验 receiverWindowLength ≤ so-rcvbuf）。

## 加密与滚动升级

- `cryptoEnabled` 时每帧由 AES-256-GCM 整体加密封装，wire 首字节 `0xAE`；明文帧首字节 `0x01`
- 新旧节点混跑（滚动升级）按首字节互识别，两态共存安全
- 密钥按发送方派生：`cryptoSenderId` 默认取节点 ID（天然唯一），用户可配置覆盖；接收端按 `cryptoKeys` 密钥表解密

## 性能要点（基准结论，详见 docs/cluster-messaging-benchmark.md）

- **MTU 高度敏感**：消息按 MTU 切片逐数据报收发；mtu / so-rcvbuf 不可配时硬吃 Aeron 默认值，基准实测该缺口价值 6 倍吞吐
- **窗口对齐才能流水线**：驱动默认窗口仅 128KB 时，单条 4MiB 快照块可越过窗口但后续消息须等 SM 推进，退化为逐块串行（实测 ~1-2MiB/s）；窗口对齐 term/8 后大块才流水线传输
- **OS 层约束**：so-rcvbuf 受内核最大值（`net.core.rmem_max` 等）约束，容器/宿主机需同步抬升，否则配置值被静默钳到上限
