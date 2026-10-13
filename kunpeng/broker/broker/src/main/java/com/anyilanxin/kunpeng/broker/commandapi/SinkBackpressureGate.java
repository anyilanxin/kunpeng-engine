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
package com.anyilanxin.kunpeng.broker.commandapi;

import static com.anyilanxin.kunpeng.broker.commandapi.CommandApiMetricsDoc.BACKPRESSURE_LAG;
import static com.anyilanxin.kunpeng.broker.commandapi.CommandApiMetricsDoc.BACKPRESSURE_REJECTED;
import static com.anyilanxin.kunpeng.broker.commandapi.CommandApiMetricsDoc.BACKPRESSURE_REJECTING;

import com.anyilanxin.kunpeng.broker.BrokerLoggers;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.concurrent.TimeUnit;
import java.util.function.LongSupplier;
import org.slf4j.Logger;

/**
 * sink 滞后背压闸门（每分区一个）：lag = 最后写入位 − 全部 sink 最低确认位，双阈值迟滞（越过 rejectBacklog 转拒绝、回落 resumeBacklog
 * 才重开，两阈值之间维持原状态防边界抖动）。
 *
 * <p>无 sink 接入则闸门不存在：sink 位置源未绑定（repository 未就绪/无 sink 配置）或 lowest 返回 −1 时恒放行——运行时判定， 与日志压缩门槛「−1 =
 * 无约束」同语义。只拦业务面 USER_COMMAND 入口；排空侧不受限，lag 单调下降后闸门必然重开。
 *
 * <p>线程模型：{@link #tryAdmit()} 与 {@link #bindSinkPositionSupplier(LongSupplier)} 只在 CommandApi actor
 * 线程调用（迟滞状态与 TTL 缓存无锁）；指标 gauge 的读取线程只碰 volatile 字段与两个线程安全的 supplier。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
final class SinkBackpressureGate {
  private static final Logger LOG = BrokerLoggers.CLUSTER_BUSINESS;

  /** lowestSinkPosition 读穿透缓存有效期：准入热路径避免每条命令都打 RocksDB */
  private static final long POSITION_CACHE_TTL_NANOS = TimeUnit.MILLISECONDS.toNanos(250);

  private final LongSupplier lastWrittenPosition;
  private final long rejectBacklog;
  private final long resumeBacklog;
  private final Counter rejectedCounter;

  /** sink 最低确认位 supplier；null = 信号源未绑定，闸门恒放行 */
  private volatile LongSupplier lowestSinkPosition;

  // 以下仅 CommandApi actor 线程读写（rejecting 例外：gauge 读取线程可见性要求 volatile）
  private volatile boolean rejecting;
  private long cachedLowestSinkPosition = -1;
  private long cacheExpiresAtNanos;

  SinkBackpressureGate(
      final int partitionId,
      final LongSupplier lastWrittenPosition,
      final long rejectBacklog,
      final long resumeBacklog,
      final MeterRegistry registry) {
    if (resumeBacklog >= rejectBacklog) {
      throw new IllegalStateException(
          String.format(
              "Invalid sink backpressure configuration: resumeBacklog (%d) must be less than"
                  + " rejectBacklog (%d)",
              resumeBacklog, rejectBacklog));
    }
    this.lastWrittenPosition = lastWrittenPosition;
    this.rejectBacklog = rejectBacklog;
    this.resumeBacklog = resumeBacklog;
    if (registry == null) {
      rejectedCounter = null;
      return;
    }
    final String partitionTag = String.valueOf(partitionId);
    rejectedCounter =
        Counter.builder(BACKPRESSURE_REJECTED.getName())
            .tags("partition", partitionTag)
            .description(BACKPRESSURE_REJECTED.getDescription())
            .register(registry);
    Gauge.builder(BACKPRESSURE_LAG.getName(), this, gate -> gate.currentLag())
        .tags("partition", partitionTag)
        .description(BACKPRESSURE_LAG.getDescription())
        .register(registry);
    Gauge.builder(BACKPRESSURE_REJECTING.getName(), this, gate -> gate.isRejecting() ? 1d : 0d)
        .tags("partition", partitionTag)
        .description(BACKPRESSURE_REJECTING.getDescription())
        .register(registry);
  }

  /** 绑定 sink 最低确认位 supplier（与日志压缩门槛同源，repository 就绪后由 transition 装配） */
  void bindSinkPositionSupplier(final LongSupplier supplier) {
    this.lowestSinkPosition = supplier;
  }

  /** 准入判定：true=放行。迟滞状态机在此翻转；每条被拒命令计数一次 */
  boolean tryAdmit() {
    final LongSupplier supplier = lowestSinkPosition;
    if (supplier == null) {
      return true;
    }
    final long lowest = lowestPosition(supplier);
    if (lowest < 0) {
      return true;
    }
    final long lag = lastWrittenPosition.getAsLong() - lowest;
    if (rejecting) {
      if (lag <= resumeBacklog) {
        rejecting = false;
        LOG.info(
            "Sink backpressure gate reopened: lag {} fell to resumeBacklog {}", lag, resumeBacklog);
      }
    } else if (lag >= rejectBacklog) {
      rejecting = true;
      LOG.warn(
          "Sink backpressure gate closed: lag {} reached rejectBacklog {}; USER_COMMANDs are"
              + " rejected until the backlog drains",
          lag,
          rejectBacklog);
    }
    if (rejecting && rejectedCounter != null) {
      rejectedCounter.increment();
    }
    return !rejecting;
  }

  boolean isRejecting() {
    return rejecting;
  }

  /** 当前 lag（指标路径直读 supplier，不走准入缓存）；信号源未绑定或无 sink 时为 0 */
  long currentLag() {
    final LongSupplier supplier = lowestSinkPosition;
    if (supplier == null) {
      return 0;
    }
    final long lowest = supplier.getAsLong();
    return lowest < 0 ? 0 : lastWrittenPosition.getAsLong() - lowest;
  }

  private long lowestPosition(final LongSupplier supplier) {
    final long now = System.nanoTime();
    if (now >= cacheExpiresAtNanos) {
      cachedLowestSinkPosition = supplier.getAsLong();
      cacheExpiresAtNanos = now + POSITION_CACHE_TTL_NANOS;
    }
    return cachedLowestSinkPosition;
  }
}
