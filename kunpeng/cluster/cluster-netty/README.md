# kunpeng-cluster

`kunpeng:cluster:cluster`：SWIM 成员协议、成员服务、集群通信/事件服务、Raft
共识、journal、快照、领导权转移等功能完整保留，**通信底座为 Aeron 可靠 UDP**
（取代早期的 Netty TCP/UDP 实现，包名维持 `com.anyilanxin.kunpeng.cluster.*`，
下游模块导入零改动）。

## 目录结构（三段式）

```
cluster/src/main/java/com/anyilanxin/kunpeng/cluster/
├── cluster/   # AtomixCluster 装配、messaging(含 Aeron 实现)、discovery、SWIM protocol、leaderfound
├── raft/      # Raft 共识全量：roles、protocol、journal、storage、partition、rebalance
└── utils/     # Address、Version、config、concurrent 等
```

## 通信底座：Aeron 替换了什么

所有上层组件只依赖 `MessagingService` / `UnicastService` 接口，两个 Aeron 实现
（`cluster/messaging/impl/AeronMessagingService` / `AeronUnicastService`）通过共享的
`AeronTransport`（内嵌 Media Driver + Aeron 客户端 + 单代理线程）承载全部通信——SWIM 探测/同步、
Raft 协议报文、事件广播、点对点 RPC。

**Aeron 模型下冗余、已整体移除的原机制：**

| 原机制 | 去向 |
| --- | --- |
| ChannelPool 连接池 | 每对端一条 `ExclusivePublication`，无连接建立开销，掉线重连由驱动完成 |
| TCP 心跳（HeartbeatHandler） | Aeron 驱动的状态报文天然保活；成员活性由 SWIM 探测负责 |
| 协议版本协商（V1/V2 编解码器） | 帧内版本字节 + 帧型分发 |
| 连接抽象（Client/ServerConnection 等 15 类） | 单代理线程 + 发送队列 + correlationId 关联 |
| 事件广播逐订阅者单播（N 条通道 × N 次 offer） | **MDC 多目的地发布**（`control-mode=manual` + 动态 `asyncAddDestination`）：组帧/加密一次、驱动扇出，内存从「订阅者数 × term」降为单条 term，`fc=max` 流控避免慢订阅者阻塞广播；目的地按使用惰性管理，60s 未被广播指向自动移除 |

**语义对齐**（与 Netty 版一一对应）：`sendAsync` 完成即投递到对端驱动；`sendAndReceive` 超时抛
`TimeoutException`；对端无处理器抛 `NoRemoteHandler`；处理器失败抛 `RemoteHandlerFailure`；对端不可达抛
`ConnectException`。差异：Aeron 无不可靠模式，`reliable=false`/单播同样走可靠 UDP（语义增强）；
`MessagingConfig` 中 TLS/压缩等字段保留但当前不生效（传输加密见下方「传输加密」章节的应用层 AES-GCM 方案）。

## 编解码（Raft 协议报文全量 SBE 化）

- **传输帧**（`AeronFrameCodec`）：定长小端头 + 尾部变长数据布局（与 SBE 变长消息同构），Agrona `UnsafeBuffer`
  原地解析，载荷单次拷贝——帧头仅约 30 字节，引入独立 schema 无净收益，保持手写。
- **Raft 协议报文**（27 个模板，`raft-protocol-schema.xml` + `SbeRaftProtocolSerializer`）：**全量 SBE 替换
  原 Fory 命名空间**，解码 flyweight 直接包裹入参 byte[] 原地读取（Aeron 风格），枚举按名字映射、可空字段用
  哨兵值。基准（`SbeRaftProtocolBenchmarkTest`，5 轮取最优）：
  - `VersionedAppendRequest`（10×512B 条目，复制主力报文）：**1.3x~2.2x 快于 Fory**
  - `PollRequest`（小报文，选举期）：约 1.2x 快于 Fory（单轮均值噪声大，以多轮最优为准）
  - Raft journal/entry/snapshot 的既有 SBE 载荷原样保留。

## 使用

```java
ClusterConfig config = new ClusterConfig()
    .setClusterId("kunpeng")
    .setNodeConfig(new MemberConfig().setId("1").setAddress(Address.from("10.0.0.1:9000")))
    .setMessagingConfig(new MessagingConfig().setPort(9000))
    .setAeronConfig(new AeronMessagingConfig())       // Aeron 专属配置
    .setDiscoveryConfig(...)                          // 与原模块一致
    .setProtocolConfig(new SwimMembershipProtocolConfig());

AtomixCluster cluster = new AtomixCluster(config, Version.from("1.0.0"), meterRegistry);
cluster.start().join();   // SWIM/Raft/事件服务全部跑在 Aeron 上
```

## Aeron 配置（`AeronMessagingConfig`）

| 配置 | 默认 | 说明 |
| --- | --- | --- |
| `streamId` / `unicastStreamId` | 1001 / 1002 | RPC 与单播流的 stream id，同一 UDP endpoint 上双流复用 |
| `termBufferLength` | 64MiB | RPC 流发布通道 term。**aeron-all 1.48 单条上限 = min(term/8, 16MiB)**，64MiB → 8MiB，覆盖 Raft 快照 4MiB 安装块；term 为稀疏文件映射，按需占内存；测试建议调低 |
| `unicastTermBufferLength` | 4MiB | 单播流 term（单条上限 512KiB） |
| `initialWindowLength` | -1(自动) | 流控初始窗口；自动取 `min(RPC term/8, 16MiB)`，与单条消息上限对齐。**驱动默认仅 128KB**——单条消息虽可越过窗口，但后续消息须等接收方 SM 推进，4MiB 快照块会退化为逐块串行（实测 ~1-2MiB/s）；对齐后 4MiB 块流水线可达百 MiB/s 级（socket 缓冲不足时自动抬升到窗口大小） |
| `embeddedDriver` | true | 内嵌 Media Driver；同 JVM 多节点必须各自独立 `aeronDir` |
| `aeronDir` | 自动临时目录 | 外部驱动模式指向已有目录 |
| `driverThreadingMode` | DEDICATED | SHARED / SHARED_NETWORK / DEDICATED（测试建议 SHARED） |
| `connectTimeout` | 5s | 发布通道建立/背压的投递截止，超时按 `ConnectException` 完成 |
| `idleStrategy` | backoff | 代理线程空闲策略：backoff / sleeping / yielding / busy-spin |

地址映射：成员 `Address(host:port)` 直接映射 `aeron:udp?endpoint=host:port`，端口沿用 `MessagingConfig`，
无需重新规划网络。

## SWIM 成员元数据拆分（基础信息全量推 + 元数据按版本拉取）

原 SWIM 的 gossip/sync/probe 报文携带**全量成员记录（含整个 properties 表）**，每轮全网 O(N²) 重复传播，百节点级即达每轮几十 KB、万节点级 MB 级。本模块拆为三层：

- **基础信息全量传播**（id/address/state/incarnation——SWIM 故障检测与冲突仲裁的根基）+ **内联属性白名单**（`inlinePropertyNames`，默认 `event-service-topics-subscribed`——事件广播寻址必需，随行直传）+ **`metadataVersion` 版本号**（拥有者每次元数据变更 +1，随基础记录传播）；
- **惰性拉取**：接收方比对「缓存版本 < 线上版本」即触发（版本号即"是否需要拉取"的标签，对每个接收方各自计算、丢失自愈）；向 **owner 优先、提供记录的对端兜底** 拉取全量元数据（任何节点都可应答，版本号仲裁新鲜度——分区场景下可经中转对端获取）；随机抖动 + 在途去重规避成员加入时的拉取风暴；
- **反熵兜底**（`metadataAntiEntropyInterval`，默认 5 分钟）：定期重查版本落后的成员重新拉取，免疫丢失的变化通知。

新成员加入 = 出现基础记录 → 自动拉取；元数据变化 = 同记录版本升高 → 自动拉取；成员移除 = 记录消失 + incarnation 仲裁（SWIM 原机制）——线上无需显式事件类型字段，本地 API 层照旧翻译为 `MEMBER_ADDED/METADATA_CHANGED/MEMBER_REMOVED`。100 节点每轮 gossip 体积约降 5-7 倍，万节点约降 10-20 倍（渐进复杂度仍 O(N)，十万级需再上 digest 差量同步）。

## 传输加密（应用层 AES-256-GCM）

Aeron 开源版（含 1.53.x）**没有数据面加密**——`io.aeron.security` 只是认证，ATS（Aeron Transport Security）
实现不在开源仓库。本模块在自有收发咽喉处整体加解密帧（`AeronFrameCrypto`），对上层服务完全透明：

- **信封**：`[0xAE][senderIdLen][senderId][IV 12B = keyId(4)+counter(8)][密文 = 帧 || GCM tag(16)]`，每帧
  增约 50 字节；明文帧首字节恒为 0x01，与 magic 可区分，支持「部分节点先启用」的滚动升级窗口；
- **密钥体系**：集群共享 PSK（`keyId → 256-bit hex`），实际 AES 密钥由 HKDF-SHA256 按 senderId 派生，各
  成员 IV 空间互不重叠（计数器随机起点单调递增）；**keyId 即轮换世代**——接收侧兼容全部配置世代，发送侧
  默认取最大 keyId（也可 `sendCryptoKeyId` 显式钉住），「先分发新密钥、后切换」两步完成滚动轮换；
- **覆盖面**：RPC、SWIM gossip 单播、Raft 报文、快照分块全部走密文；加密发生在 `publication.offer()` 之前，
  因此共享内存 term buffer 与任何录制/落盘文件同样是密文（驱动级 UDP 加密反而做不到）；
- **失败语义**：认证失败/未知世代/信封畸形 → 丢弃该帧并限流记日志，不影响代理线程与其他帧；
- **开销实测**（本机 JDK 26 / x86_64，加+解一对、缓冲复用）：512B ≈ 1.3µs、4KiB ≈ 3.1µs（对比 SBE 编码
  ~1.6µs 与毫秒级 RTT 可忽略）；4MiB 快照块 ≈ 4.8ms/侧（1Gbps 线路传输本身 ~34ms）。

```java
config.getAeronConfig()
    .setCryptoEnabled(true)
    .addCryptoKey(1, "0123456789abcdef...64 位十六进制");  // 256-bit PSK
// senderId 默认取成员 ID（未设置时为随机 UUID），可用 setCryptoSenderId 覆盖
```

注意事项：共享 PSK 提供机密性与对外完整性，**不提供成员间互相认证**（持有 PSK 者可伪造任意 senderId）；
需要成员认证/前向保密时，升级为每成员密钥或证书协商（可参考 Aeron `Authenticator` 的 challenge/response
模式做握手）。线上仍可见 Aeron 协议元数据（帧类型/流 id/长度轮廓），对固定端口的自建集群通常可接受。

## 测试

移植原模块全部测试（Netty 专属的传输内部测试除外），另增 Aeron 传输语义测试：

- `AeronMessagingServiceTest`——请求-回复、单向、`NoRemoteHandler`、`RemoteHandlerFailure`、超时、
  不可达对端、1.5MiB 大报文分片往返（16MiB term）；
- `AeronMessagingServiceBroadcastTest`——MDC 多目的地扇出：一次群发到达全部目的地（明文/加密）、
  单地址集合回退；
- `TenNodeAeronBenchTest`——N 节点(`-Dbench.nodes`)通信能力基准（RPC 延迟分位、单向/
  单播吞吐、事件广播扇出、大报文、大文件流式传输），输出统一 `[bench10n]` 行（替代旧 Netty
  模块的同名基准）；3 节点回环实测：RPC p50
  87µs / p99 468µs（Netty 377/1842µs）、单播 100% 送达（Netty 丢 ~79%）、广播扇出 24µs/ev（Netty 66µs）、
  4MiB 块大文件 75~128 MiB/s（Netty 206，回环 TCP 优势，真实 1Gbps 网络上 Aeron 已打满带宽）；
- `AeronFrameCryptoTest` / `AeronMessagingServiceCryptoTest`——加密往返、防篡改、无密钥对端不可读、
  密钥轮换窗口（旧世代分发期可用、新世代对滞后节点不可读）；
- `AeronUnicastServiceTest`——单播双向投递；
- `AeronAtomixClusterSimpleTest`——三节点 SWIM 收敛 + 主题事件广播全链路 Aeron（含开启加密的完整重放场景）；
- SWIM 元数据拆分由 `SwimProtocolTest` 验证：非白名单属性经拉取通道传播（含 owner 分区时经中转对端
  兜底拉取的场景）、白名单属性随行内联传播（E2E 事件广播即依赖此路径）；
- 原有套件（`AtomixClusterSimpleTest`、SWIM、`RaftGroupTest`、journal、快照、领导权转移等）在 Aeron
  底座上运行；randomized 属性测试默认排除（`-Prandomized` 显式开启），与原模块一致。
