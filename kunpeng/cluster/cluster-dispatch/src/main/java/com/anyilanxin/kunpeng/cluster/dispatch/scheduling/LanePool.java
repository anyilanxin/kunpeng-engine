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
package com.anyilanxin.kunpeng.cluster.dispatch.scheduling;

import com.anyilanxin.kunpeng.scheduler.ActorSchedulingService;
import com.anyilanxin.kunpeng.scheduler.ConcurrencyControl;
import com.anyilanxin.kunpeng.scheduler.future.ActorFuture;
import com.anyilanxin.kunpeng.scheduler.future.ActorFutureCollector;
import java.util.EnumMap;
import java.util.concurrent.atomic.AtomicReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 一组固定的 {@link LaneActor 车道 actor}，每个 {@link ExecutionLane} 一个。
 *
 * <p>池以车道自身的调度提示启动每条车道，并将启动与关闭聚合为单个 future。车道 actor 失败时以新实例
 * 原子替换并重新提交（对齐"调度基座随分区存亡"的自愈语义），避免引擎主体仍在运行而调度车道永久死亡 导致定时任务静默丢失；失败与恢复经 {@link LaneFailureListener}
 * 通知上层。
 */
public final class LanePool {

  private static final Logger LOG = LoggerFactory.getLogger(LanePool.class);

  private final EnumMap<ExecutionLane, AtomicReference<LaneActor>> actors =
      new EnumMap<>(ExecutionLane.class);
  private final ActorSchedulingService pool;
  private final TimerSchedulerFactory factory;
  private final int partitionId;
  private volatile LaneFailureListener failureListener;
  private volatile boolean shuttingDown;

  public LanePool(
      final ActorSchedulingService pool,
      final TimerSchedulerFactory factory,
      final int partitionId) {
    this.pool = pool;
    this.factory = factory;
    this.partitionId = partitionId;
    for (final ExecutionLane lane : ExecutionLane.values()) {
      actors.put(lane, new AtomicReference<>(createActor(lane)));
    }
  }

  /** 注册车道失败/恢复回调。 */
  public void setFailureListener(final LaneFailureListener listener) {
    failureListener = listener;
  }

  /** 返回给定车道当前的车道 actor。 */
  public LaneActor actorFor(final ExecutionLane lane) {
    return actors.get(lane).get();
  }

  /** 启动所有车道 actor，并聚合各提交 future。 */
  public ActorFuture<Void> launch(final ConcurrencyControl cc) {
    return actors.values().stream()
        .map(ref -> pool.submitActor(ref.get(), ref.get().lane().schedulingHints()))
        .collect(new ActorFutureCollector<>(cc))
        .thenApply(ignored -> null);
  }

  /** 关闭所有车道 actor，并聚合各关闭 future；关闭开始后不再重建失败车道。 */
  public ActorFuture<Void> shutdown(final ConcurrencyControl cc) {
    shuttingDown = true;
    return actors.values().stream()
        .map(AtomicReference::get)
        .map(LaneActor::closeAsync)
        .collect(new ActorFutureCollector<>(cc))
        .thenApply(ignored -> null);
  }

  private LaneActor createActor(final ExecutionLane lane) {
    return new LaneActor(lane, factory, partitionId, this::onLaneFailed);
  }

  // 失败 actor 的载体线程上调用; 必须非阻塞
  private void onLaneFailed(final LaneActor failed) {
    final ExecutionLane lane = failed.lane();
    LOG.error(
        "Lane actor failed and will be rebuilt; scheduled entries on it are lost and will "
            + "be re-triggered by the due-date rescan. [lane: {}]",
        failed.getName());
    final var listener = failureListener;
    if (listener != null) {
      listener.onLaneFailed(lane);
    }
    if (shuttingDown) {
      return;
    }
    final var ref = actors.get(lane);
    // 仅当当前仍是失败实例时替换, 避免覆盖更新的重建
    if (!ref.compareAndSet(failed, createActor(lane))) {
      return;
    }
    final LaneActor rebuilt = ref.get();
    pool.submitActor(rebuilt, lane.schedulingHints())
        .onComplete(
            (ignored, error) -> {
              if (error != null) {
                LOG.error("Failed to submit rebuilt lane actor. [lane: {}]", lane, error);
                return;
              }
              LOG.info("Lane actor rebuilt successfully. [lane: {}]", rebuilt.getName());
              final var currentListener = failureListener;
              if (currentListener != null) {
                currentListener.onLaneRecovered(lane);
              }
            });
  }
}
