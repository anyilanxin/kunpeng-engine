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
 * 内嵌 Media Driver 持有者：每个传输实例独占一个驱动（专属 aeron 目录与驱动线程）。
 *
 * <p>刻意不做进程级共享：同进程双实例（集群面/业务面）若共享驱动，两面的网络收发系统调用会耦合进同一条 Sender/Receiver
 * 线程——快照复制风暴期业务命令路径尾部劣化，恰好破坏双面拆分要隔离的争用对（基准实测共享线程族 在大报文下吞吐塌陷）。同 JVM 多节点（测试、基准）同样各自驱动互不干扰。
 *
 * <p>外置驱动模式不经过本类。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
final class AeronProcessDriverHolder {
  private static final Logger LOGGER = LoggerFactory.getLogger(AeronProcessDriverHolder.class);

  private MediaDriver driver;
  private String aeronDir;

  /** 为本传输实例启动专属驱动。 */
  static AeronProcessDriverHolder exclusive(
      final MessagingConfig messagingConfig, final AeronMessagingConfig config) {
    final AeronProcessDriverHolder holder = new AeronProcessDriverHolder();
    holder.launch(messagingConfig, config);
    return holder;
  }

  /** 释放：关闭本实例的驱动。 */
  void release() {
    driver.close();
    LOGGER.info("Aeron 驱动已关闭: dir={}", aeronDir);
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
