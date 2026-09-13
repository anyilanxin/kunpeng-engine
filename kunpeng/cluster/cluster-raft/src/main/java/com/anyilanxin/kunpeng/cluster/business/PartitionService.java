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
package com.anyilanxin.kunpeng.cluster.business;

import com.anyilanxin.kunpeng.cluster.cluster.MemberId;
import com.anyilanxin.kunpeng.cluster.cluster.PartitionId;
import com.anyilanxin.kunpeng.cluster.raft.cluster.RaftMember;
import com.anyilanxin.kunpeng.cluster.raft.partition.RaftPartition;
import com.anyilanxin.kunpeng.scheduler.future.ActorFuture;
import com.anyilanxin.kunpeng.scheduler.startup.StartupProcess;
import com.anyilanxin.kunpeng.utils.FileUtil;
import java.util.Collection;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import org.slf4j.Logger;

/**
 * @author zxuanhong
 * @since
 */
@SuppressWarnings("rawtypes")
public class PartitionService<CONTENT extends PartitionStartupContext> {
  private static final Logger LOGGER = ClusterRaftLoggers.CLUSTER_RAFT;
  private final AtomicBoolean started = new AtomicBoolean(false);
  private final CONTENT context;
  private final StartupProcess<CONTENT> startupProcess;

  protected PartitionService(final CONTENT context, final StartupProcess<CONTENT> startupProcess) {
    this.context = context;
    this.startupProcess = startupProcess;
  }

  public ActorFuture<PartitionService<CONTENT>> start() {
    final var concurrencyControl = context.getConcurrencyControl();
    final var result = concurrencyControl.<PartitionService<CONTENT>>createFuture();
    concurrencyControl.run(
        () -> {
          // 防重入：重复 start 会派出两条启动链跑同一个 context，失败链的 abort 会破坏成功链的共享状态
          if (!started.compareAndSet(false, true)) {
            result.completeExceptionally(
                new IllegalStateException("Partition service has already been started"));
            return;
          }
          final var start = startupProcess.startup(concurrencyControl, context);
          concurrencyControl.runOnCompletion(
              start,
              (_, error) -> {
                if (error != null) {
                  // 失败后释放标志，允许后续重新启动
                  started.set(false);
                  result.completeExceptionally(error);
                } else {
                  result.complete(this);
                }
              });
        });
    return result;
  }

  public ActorFuture<PartitionService<CONTENT>> stop() {
    final var concurrencyControl = context.getConcurrencyControl();
    final var result = concurrencyControl.<PartitionService<CONTENT>>createFuture();
    concurrencyControl.run(
        () -> {
          final var start = startupProcess.shutdown(concurrencyControl, context);
          concurrencyControl.runOnCompletion(
              start,
              (ok, error) -> {
                if (error != null) {
                  result.completeExceptionally(error);
                } else {
                  started.set(false);
                  result.complete(this);
                }
              });
        });
    return result;
  }

  /** 请求离开分区，成功后执行关闭流程并删除分区数据目录。 */
  public ActorFuture<PartitionService<CONTENT>> leave() {
    final var concurrencyControl = context.getConcurrencyControl();
    final var result = concurrencyControl.<PartitionService<CONTENT>>createFuture();
    concurrencyControl.run(
        () -> {
          final var raftPartition = raftPartition();
          if (raftPartition == null) {
            result.completeExceptionally(errorPartitionNotAvailable("leave"));
            return;
          }
          raftPartition
              .leave()
              .whenComplete(
                  (_, leaveError) ->
                      concurrencyControl.run(() -> onPartitionLeaveCompleted(leaveError, result)));
        });
    return result;
  }

  /** 请求离开分区，成功后执行关闭流程并删除分区数据目录。 */
  public ActorFuture<PartitionService<CONTENT>> stopAndDelete() {
    final var concurrencyControl = context.getConcurrencyControl();
    final var result = concurrencyControl.<PartitionService<CONTENT>>createFuture();
    concurrencyControl.run(
        () -> {
          final var raftPartition = raftPartition();
          if (raftPartition == null) {
            result.completeExceptionally(errorPartitionNotAvailable("leave"));
            return;
          }
          onPartitionLeaveCompleted(null, result);
        });
    return result;
  }

  private void onPartitionLeaveCompleted(
      final Throwable leaveError, final ActorFuture<PartitionService<CONTENT>> result) {
    if (leaveError != null) {
      result.completeExceptionally(leaveError);
      return;
    } else {
      started.set(false);
    }
    shutdownAndDelete(result);
  }

  /** 执行关闭流程并删除分区数据目录，删除失败仅告警（数据残留待人工清理） */
  private void shutdownAndDelete(final ActorFuture<PartitionService<CONTENT>> result) {
    final var concurrencyControl = context.getConcurrencyControl();
    final var partitionDirectory = context.getPartitionDirectory();
    concurrencyControl.runOnCompletion(
        startupProcess.shutdown(concurrencyControl, context),
        (_, shutdownError) -> {
          if (shutdownError != null) {
            result.completeExceptionally(shutdownError);
            return;
          }
          try {
            LOGGER.info("---正在删除分区-----{}", partitionDirectory);
            FileUtil.deleteTreeIfExists(partitionDirectory);
          } catch (final Exception e) {
            LOGGER.warn(
                "Failed to delete partition directory {}. Data will remain until manually removed.",
                partitionDirectory,
                e);
          }
          result.complete(this);
        });
  }

  public ActorFuture<Void> reconfigurePriority(final int newPriority) {
    final var concurrencyControl = context.getConcurrencyControl();
    final var result = concurrencyControl.<Void>createFuture();
    concurrencyControl.run(
        () -> {
          final var raftPartition = raftPartition();
          if (raftPartition == null) {
            result.completeExceptionally(errorPartitionNotAvailable("reconfigure priority of"));
            return;
          }
          raftPartition
              .getServer()
              .reconfigurePriority(newPriority)
              .whenComplete(
                  (_, configureError) -> {
                    if (configureError != null) {
                      result.completeExceptionally(configureError);
                    } else {
                      result.complete(null);
                    }
                  });
        });

    return result;
  }

  public ActorFuture<Void> dataMerge(final PartitionId targetPartitionId) {
    final var concurrencyControl = context.getConcurrencyControl();
    final ActorFuture<Void> future = concurrencyControl.createFuture();
    future.complete(null);
    return future;
  }

  public ActorFuture<Void> forceReconfigure(final Collection<MemberId> members) {
    final var concurrencyControl = context.getConcurrencyControl();
    final var result = concurrencyControl.<Void>createFuture();
    concurrencyControl.run(
        () -> {
          final var raftPartition = raftPartition();
          if (raftPartition == null) {
            result.completeExceptionally(errorPartitionNotAvailable("force reconfigure"));
            return;
          }
          // 这里假定所有成员均为 ACTIVE 类型，因为目前尚不支持 PASSIVE 成员。
          final var membersWithType =
              members.stream().collect(Collectors.toMap(m -> m, m -> RaftMember.Type.ACTIVE));
          raftPartition
              .getServer()
              .forceReconfigure(membersWithType)
              .whenComplete(
                  (_, configureError) -> {
                    if (configureError != null) {
                      result.completeExceptionally(configureError);
                    } else {
                      result.complete(null);
                    }
                  });
        });

    return result;
  }

  private IllegalStateException errorPartitionNotAvailable(final String operation) {
    return new IllegalStateException(
        String.format(
            "Expected to %s partition %s, but raft partition is not available",
            operation, context.getPartitionMetadata().id().id()));
  }

  public RaftPartition raftPartition() {
    return context.getRaftPartition();
  }

  public int id() {
    return context.getPartitionMetadata().id().id();
  }

  public CONTENT context() {
    return context;
  }

  public boolean isStarted() {
    return started.get();
  }

  @Override
  public boolean equals(final Object o) {
    if (!(o instanceof final PartitionService<?> that)) {
      return false;
    }
    return Objects.equals(context.getPartitionMetadata(), that.context.getPartitionMetadata());
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(context.getPartitionMetadata());
  }
}
