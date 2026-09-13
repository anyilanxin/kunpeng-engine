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
package com.anyilanxin.kunpeng.cluster.business.step;

import com.anyilanxin.kunpeng.cluster.business.PartitionStartupContext;
import com.anyilanxin.kunpeng.scheduler.future.ActorFuture;
import com.anyilanxin.kunpeng.scheduler.startup.StartupStep;

/**
 * 分区启动流程中的加入（join）步骤：创建 Raft 分区并加入已存在的集群，关闭时释放分区。
 *
 * @author zxuanhong
 */
@SuppressWarnings("rawtypes")
public abstract class RaftJoinStep<CONTENT extends PartitionStartupContext>
    implements StartupStep<CONTENT> {

  @Override
  public ActorFuture<CONTENT> startup(final CONTENT context) {
    final var result = context.getConcurrencyControl().<CONTENT>createFuture();
    final var partition =
        context
            .getRaftPartitionFactory()
            .createRaftPartition(
                context.getPartitionMetadata(),
                context.getSnapshotProvider(),
                context.getEntryValidator(),
                context.getMeterRegistry());
    partition
        .join()
        .whenComplete(
            (_, throwable) -> {
              if (throwable == null) {
                context.setRaftPartition(partition);
                result.complete(context);
              } else {
                result.completeExceptionally(throwable);
              }
            });

    return result;
  }

  @Override
  public ActorFuture<CONTENT> shutdown(final CONTENT context) {
    final var result = context.getConcurrencyControl().<CONTENT>createFuture();
    final var raftPartition = context.getRaftPartition();
    if (raftPartition == null) {
      result.complete(context);
      return result;
    }
    raftPartition
        .close()
        .whenComplete(
            (ignored, throwable) -> {
              if (throwable == null) {
                context.setRaftPartition(null);
                result.complete(context);
              } else {
                result.completeExceptionally(throwable);
              }
            });
    return result;
  }
}
