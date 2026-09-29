/*
 * Copyright © 2026 anyilanxin zxh(anyilanxin@aliyun.com)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.anyilanxin.kunpeng.cluster.cluster.messaging;

import com.anyilanxin.kunpeng.cluster.utils.config.Config;
import java.io.File;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import org.jspecify.annotations.Nullable;

/**
 * Aeron 传输层配置。
 *
 * <p>与 {@link MessagingConfig} 配合使用：后者决定绑定地址与端口（沿用原语义），本配置决定 Aeron 专属行为。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public class AeronMessagingConfig implements Config {

  /** 集群 RPC 流的 stream id（sendAsync/sendAndReceive/事件广播共用的逻辑流）。 */
  private int streamId = 1001;

  /** 不可靠单播流（UnicastService）的 stream id。 */
  private int unicastStreamId = 1002;

  /**
   * RPC 流每个对端发布通道的 term buffer 长度。
   *
   * <p>注意：aeron-all 1.48 的单条消息上限为 {@code min(term/8, 16MiB)}（较新源码为 term/2）。Raft 快照安装块 默认 4MiB，为覆盖「块
   * + 帧头」，默认 64MiB（上限 8MiB）。term 以稀疏文件映射，实际内存按需提交； 内存开销约为「对端数 × term 长度」，小集群或测试环境可调低。
   */
  private int termBufferLength = 64 * 1024 * 1024;

  /** 单播流每个对端发布通道的 term buffer 长度（单播载荷小，默认 4MiB → 单条上限 512KiB）。 */
  private int unicastTermBufferLength = 4 * 1024 * 1024;

  /**
   * 流控初始窗口字节数；-1 表示自动取 {@code min(RPC term/8, 16MiB)}（与单条消息上限对齐）。
   *
   * <p>关键约束：Aeron 驱动默认窗口仅 128KB。单条消息虽可越过窗口发送，但后续消息必须等接收方 SM 确认 推进窗口——4MiB 级的快照块会退化为逐块串行等待（实测
   * ~1-2MiB/s）。窗口对齐到单条上限后大块可流水线 传输。
   */
  private int initialWindowLength = -1;

  /** 是否内嵌启动 Media Driver（同一 JVM 内多个节点必须各自使用独立的 aeronDir）。 */
  private boolean embeddedDriver = true;

  /**
   * 内嵌驱动是否为进程级共享（引用计数）：集群面/业务面等多个传输实例复用同一驱动，省去每实例一套驱动线程与 CnC 共享内存。
   *
   * <p>默认 false（每实例独占驱动）——同 JVM 多节点场景（测试、基准）保持线程并行度不受损；生产装配（集群面+业务面双实例）显式开启。
   */
  private boolean sharedDriver = false;

  /** Aeron 目录；null 表示自动生成临时目录（内嵌模式）或使用默认目录（外部驱动模式）。 */
  private File aeronDir;

  /** 驱动线程模式：SHARED / SHARED_NETWORK / DEDICATED。 */
  private String driverThreadingMode = "DEDICATED";

  /**
   * Media Driver keepalive 超时（毫秒）；0 表示跟随 Aeron 缺省（10s）。
   *
   * <p>客户端 conductor 据此判定驱动存活：全进程停顿（调试器断点、长 GC、宿主机 CPU 饥饿）超过该值即判定驱动死亡并 终止客户端，进而触发 broker fail-fast
   * 关闭。生产保持缺省即可；开发/调试环境建议 30-60s，避免断点暂停引爆集群。 内嵌驱动模式该值经 cnc 对客户端自动生效；外置驱动模式仅作用于客户端侧，需与外部驱动自身配置对齐。
   */
  private long driverTimeoutMs = 10_000;

  /** sendAsync 的投递截止（发布通道迟迟未连接或持续背压时按连接失败完成 future）。 */
  private Duration connectTimeout = Duration.ofSeconds(5);

  /** 代理线程空闲策略：backoff / sleeping / yielding / busy-spin。 */
  private String idleStrategy = "backoff";

  /**
   * 发送/广播入队队列容量（两条队列各自的上限）。
   *
   * <p>入队队列是有界的多生产者→单消费者队列（任意业务线程提交、代理线程单点消费）。代理线程停滞时（如对端不可达、任务滞留重试至投递截止），无界队列会随提交速率无限增长直至内存耗尽；有界队列将该情况转化为显式背压——有完成回调的任务按异常完成，尽力而为任务告警丢弃。
   */
  private int sendQueueCapacity = 4096;

  /**
   * 单个 Aeron 数据报的最大长度（通道 URI 的 {@code mtu} 参数）。
   *
   * <p>超过单帧上限（term/8）的消息由 Aeron 按 MTU 切片传输：报文越大切片数越多，收发两端的逐数据报 固定成本线性增长——大报文吞吐对 MTU 高度敏感（基准探针实测
   * 1MiB 帧 MTU 4096→8192 提速 2.8 倍）。 默认 8192：在 macOS 发送上限（{@code udp.maxdgram}=9216）内跨平台可用，云 VPC
   * 普遍支持巨型帧； MTU 为发送侧属性，新旧节点可混布滚动升级。1500 MTU 以太网上 8KB 数据报会引入 IP 分片， 丢包敏感网络可显式调回 4096。
   */
  private int mtuLength = 8192;

  /**
   * 内核发送缓冲（通道 URI 的 {@code so-sndbuf} 参数，字节）；0 表示不设置、沿用内核默认。
   *
   * <p>刻意不设大默认值：该缓冲按 socket（每目的地一个发布通道）计，万级成员节点上大默认值 × 海量
   * 发布通道是不确定的内核内存上界，且探针实测发送缓冲对吞吐贡献最小（接收缓冲才是主项）——留给 部署侧按需显式调优。
   */
  private int socketSendBufferBytes = 0;

  /**
   * 内核接收缓冲（通道 URI 的 {@code so-rcvbuf} 参数，字节）；0 表示不设置、沿用内核默认。
   *
   * <p>默认 2MB：突发大报文时接收缓冲不足会丢数据报触发 Aeron NAK 重传，是吞吐长尾的来源之一； 接收 socket 每节点固定两个（消息流 +
   * 单向流），成本上界确定，可安全设默认。
   */
  private int socketReceiveBufferBytes = 2 * 1024 * 1024;

  /**
   * 是否启用应用层传输加密（AES-256-GCM 信封，见 {@code AeronFrameCrypto}）。
   *
   * <p>启用后发送侧在投递前整体加密帧、接收侧先解密再解析；线上载荷、共享内存 term buffer 与任何录制文件均为密 文。明文帧首字节为 0x01、密文信封为
   * 0xAE，可区分，支持部分节点先启用的滚动升级窗口。
   */
  private boolean cryptoEnabled = false;

  /** 密钥表：keyId → 256-bit 密钥（64 个十六进制字符）。接收侧兼容表中全部世代，用于「先分发、后切换」的滚动轮换。 */
  private final Map<Integer, String> cryptoKeys = new LinkedHashMap<>();

  /** 发送使用的密钥世代 keyId；-1 表示取密钥表中最大 keyId（新增更大 keyId 的密钥即完成切换）。 */
  private int sendCryptoKeyId = -1;

  /** 发送方标识（用于按发送方派生 AES 密钥）；null 时由 AtomixCluster 自动填充成员 ID， 仍未设置则回退为绑定地址。 */
  private @Nullable String cryptoSenderId;

  public int getStreamId() {
    return streamId;
  }

  public AeronMessagingConfig setStreamId(final int streamId) {
    this.streamId = streamId;
    return this;
  }

  public int getUnicastStreamId() {
    return unicastStreamId;
  }

  public AeronMessagingConfig setUnicastStreamId(final int unicastStreamId) {
    this.unicastStreamId = unicastStreamId;
    return this;
  }

  public int getTermBufferLength() {
    return termBufferLength;
  }

  public AeronMessagingConfig setTermBufferLength(final int termBufferLength) {
    this.termBufferLength = termBufferLength;
    return this;
  }

  public int getUnicastTermBufferLength() {
    return unicastTermBufferLength;
  }

  public int getMtuLength() {
    return mtuLength;
  }

  public AeronMessagingConfig setMtuLength(final int mtuLength) {
    this.mtuLength = mtuLength;
    return this;
  }

  public int getSocketSendBufferBytes() {
    return socketSendBufferBytes;
  }

  public AeronMessagingConfig setSocketSendBufferBytes(final int socketSendBufferBytes) {
    this.socketSendBufferBytes = socketSendBufferBytes;
    return this;
  }

  public int getSocketReceiveBufferBytes() {
    return socketReceiveBufferBytes;
  }

  public AeronMessagingConfig setSocketReceiveBufferBytes(final int socketReceiveBufferBytes) {
    this.socketReceiveBufferBytes = socketReceiveBufferBytes;
    return this;
  }

  public AeronMessagingConfig setUnicastTermBufferLength(final int unicastTermBufferLength) {
    this.unicastTermBufferLength = unicastTermBufferLength;
    return this;
  }

  public int getInitialWindowLength() {
    return initialWindowLength;
  }

  public AeronMessagingConfig setInitialWindowLength(final int initialWindowLength) {
    this.initialWindowLength = initialWindowLength;
    return this;
  }

  public boolean isEmbeddedDriver() {
    return embeddedDriver;
  }

  public AeronMessagingConfig setEmbeddedDriver(final boolean embeddedDriver) {
    this.embeddedDriver = embeddedDriver;
    return this;
  }

  public boolean isSharedDriver() {
    return sharedDriver;
  }

  public AeronMessagingConfig setSharedDriver(final boolean sharedDriver) {
    this.sharedDriver = sharedDriver;
    return this;
  }

  public File getAeronDir() {
    return aeronDir;
  }

  public AeronMessagingConfig setAeronDir(final File aeronDir) {
    this.aeronDir = aeronDir;
    return this;
  }

  public String getDriverThreadingMode() {
    return driverThreadingMode;
  }

  public AeronMessagingConfig setDriverThreadingMode(final String driverThreadingMode) {
    this.driverThreadingMode = driverThreadingMode;
    return this;
  }

  public long getDriverTimeoutMs() {
    return driverTimeoutMs;
  }

  public AeronMessagingConfig setDriverTimeoutMs(final long driverTimeoutMs) {
    this.driverTimeoutMs = driverTimeoutMs;
    return this;
  }

  public Duration getConnectTimeout() {
    return connectTimeout;
  }

  public AeronMessagingConfig setConnectTimeout(final Duration connectTimeout) {
    this.connectTimeout = connectTimeout;
    return this;
  }

  public String getIdleStrategy() {
    return idleStrategy;
  }

  public AeronMessagingConfig setIdleStrategy(final String idleStrategy) {
    this.idleStrategy = idleStrategy;
    return this;
  }

  public int getSendQueueCapacity() {
    return sendQueueCapacity;
  }

  public AeronMessagingConfig setSendQueueCapacity(final int sendQueueCapacity) {
    this.sendQueueCapacity = sendQueueCapacity;
    return this;
  }

  public boolean isCryptoEnabled() {
    return cryptoEnabled;
  }

  public AeronMessagingConfig setCryptoEnabled(final boolean cryptoEnabled) {
    this.cryptoEnabled = cryptoEnabled;
    return this;
  }

  public Map<Integer, String> getCryptoKeys() {
    return cryptoKeys;
  }

  public AeronMessagingConfig setCryptoKeys(final Map<Integer, String> cryptoKeys) {
    this.cryptoKeys.clear();
    this.cryptoKeys.putAll(cryptoKeys);
    return this;
  }

  public AeronMessagingConfig addCryptoKey(final int keyId, final String keyHex) {
    cryptoKeys.put(keyId, keyHex);
    return this;
  }

  public int getSendCryptoKeyId() {
    return sendCryptoKeyId;
  }

  public AeronMessagingConfig setSendCryptoKeyId(final int sendCryptoKeyId) {
    this.sendCryptoKeyId = sendCryptoKeyId;
    return this;
  }

  public @Nullable String getCryptoSenderId() {
    return cryptoSenderId;
  }

  public AeronMessagingConfig setCryptoSenderId(final String cryptoSenderId) {
    this.cryptoSenderId = cryptoSenderId;
    return this;
  }
}
