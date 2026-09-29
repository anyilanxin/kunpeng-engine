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
package com.anyilanxin.kunpeng.cluster.cluster.messaging.impl;

import com.anyilanxin.kunpeng.cluster.cluster.messaging.AeronMessagingConfig;
import com.anyilanxin.kunpeng.cluster.cluster.messaging.MessagingConfig;
import io.aeron.Aeron;
import io.aeron.driver.MediaDriver;
import io.aeron.driver.ThreadingMode;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 内嵌 Media Driver 持有者：支持进程级共享（引用计数）与独占两种模式。
 *
 * <p><b>共享模式</b>（{@link AeronMessagingConfig#isSharedDriver()}）：同进程的多个 {@link AeronTransport}
 * 实例（集群面/业务面各自独立端口与代理线程）复用同一个驱动与 aeron 目录，避免每实例重复一套驱动线程与 CnC 共享内存。驱动级参数（线程模式、流控窗口、socket 缓冲）以
 * <b>首个启动</b> 的实例配置为准——通常为集群面实例。进程内最后一个使用方释放时关闭驱动。
 *
 * <p><b>独占模式</b>（默认）：每个传输实例各自启动/关闭驱动——同 JVM 多节点（测试、基准）保持线程并行度，不受彼此影响。
 *
 * <p>外置驱动模式不经过本类。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
final class AeronProcessDriverHolder {
  private static final Logger LOGGER = LoggerFactory.getLogger(AeronProcessDriverHolder.class);

  /** 共享模式的进程内单例；类锁守护（各传输实例自身的 lifecycleLock 互不相同）。 */
  private static AeronProcessDriverHolder instance;

  private final boolean shared;
  private MediaDriver driver;
  private String aeronDir;
  private int refCount;

  private AeronProcessDriverHolder(final boolean shared) {
    this.shared = shared;
  }

  /** 共享模式：引用计数复用进程级单例驱动。 */
  static synchronized AeronProcessDriverHolder retain(
      final MessagingConfig messagingConfig, final AeronMessagingConfig config) {
    if (instance == null) {
      instance = new AeronProcessDriverHolder(true);
      instance.launch(messagingConfig, config);
      LOGGER.info(
          "进程级共享 Aeron 驱动已启动: dir={}, 线程模式={}", instance.aeronDir, config.getDriverThreadingMode());
    }
    instance.refCount++;
    return instance;
  }

  /** 独占模式：为本传输实例启动专属驱动。 */
  static AeronProcessDriverHolder exclusive(
      final MessagingConfig messagingConfig, final AeronMessagingConfig config) {
    final AeronProcessDriverHolder holder = new AeronProcessDriverHolder(false);
    holder.launch(messagingConfig, config);
    return holder;
  }

  /** 释放：共享模式减引用（归零关驱动，与 retain 同类锁），独占模式直接关闭自己的驱动。 */
  void release() {
    if (shared) {
      releaseShared(this);
      return;
    }
    driver.close();
    LOGGER.info("Aeron 驱动已关闭: dir={}", aeronDir);
  }

  private static synchronized void releaseShared(final AeronProcessDriverHolder holder) {
    if (holder != instance || holder.refCount <= 0) {
      return;
    }
    holder.refCount--;
    if (holder.refCount > 0) {
      return;
    }
    holder.driver.close();
    instance = null;
    LOGGER.info("进程级共享 Aeron 驱动已关闭(最后一个使用方释放): dir={}", holder.aeronDir);
  }

  String aeronDir() {
    return aeronDir;
  }

  private void launch(final MessagingConfig messagingConfig, final AeronMessagingConfig config) {
    final File configured = config.getAeronDir();
    if (configured != null) {
      aeronDir = configured.getAbsolutePath();
    } else {
      try {
        aeronDir = Files.createTempDirectory("aeron-kunpeng-").toAbsolutePath().toString();
      } catch (final IOException error) {
        throw new IllegalStateException("无法创建 Aeron 临时目录", error);
      }
    }
    // 流控窗口默认仅 128KB: 单条消息虽可越过窗口, 但后续消息须等接收方 SM 推进——4MiB 快照块会退化为
    // 逐块串行(实测 ~1-2MiB/s)。窗口对齐单条消息上限(term/8)后大块才能流水线传输;
    // 驱动校验要求 SO_RCVBUF ≥ 窗口, socket 缓冲不足时一并抬升。
    int windowLength =
        config.getInitialWindowLength() > 0
            ? config.getInitialWindowLength()
            : Math.min(config.getTermBufferLength() >> 3, 16 * 1024 * 1024);
    // 通道级接收缓冲(tuningParams 的 so-rcvbuf)被显式调小时, 驱动默认窗口必须随之收缩:
    // Aeron 校验 receiverWindowLength(缺省取驱动窗口)不得超过通道 so-rcvbuf, 否则订阅注册被拒
    if (config.getSocketReceiveBufferBytes() > 0) {
      windowLength = Math.min(windowLength, config.getSocketReceiveBufferBytes());
    }
    final MediaDriver.Context context =
        new MediaDriver.Context()
            .aeronDirectoryName(aeronDir)
            .threadingMode(ThreadingMode.valueOf(config.getDriverThreadingMode()))
            .termBufferSparseFile(true)
            .initialWindowLength(windowLength)
            .socketRcvbufLength(
                Math.max(Math.max(messagingConfig.getSocketReceiveBuffer(), 0), windowLength))
            .socketSndbufLength(
                Math.max(Math.max(messagingConfig.getSocketSendBuffer(), 0), windowLength))
            .dirDeleteOnStart(true)
            .dirDeleteOnShutdown(true);
    if (config.getDriverTimeoutMs() > 0) {
      context.driverTimeoutMs(config.getDriverTimeoutMs());
    }
    driver = MediaDriver.launch(context);
  }

  /** 外置驱动模式的 aeron 目录：显式配置优先，否则 Aeron 默认目录。 */
  static String externalDir(final AeronMessagingConfig config) {
    final File configured = config.getAeronDir();
    return configured != null ? configured.getAbsolutePath() : Aeron.Context.AERON_DIR_PROP_DEFAULT;
  }
}
