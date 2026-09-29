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
package com.anyilanxin.kunpeng.broker.business.raft.step;

import com.anyilanxin.kunpeng.broker.business.raft.BusinessPartitionStartupContext;
import com.anyilanxin.kunpeng.cluster.business.step.RaftBootstrapStep;
import com.anyilanxin.kunpeng.cluster.cluster.MemberId;
import com.anyilanxin.kunpeng.cluster.cluster.PartitionId;
import com.anyilanxin.kunpeng.cluster.raft.partition.RaftPartition;
import com.anyilanxin.kunpeng.scheduler.future.ActorFuture;
import java.util.concurrent.CompletableFuture;

/**
 * 业务分区启动流程中的引导（bootstrap）步骤，负责在初始成员上创建并引导业务 Raft 分组。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public final class BusinessRaftBootstrapStep
    extends RaftBootstrapStep<BusinessPartitionStartupContext> {
  private final boolean isBootstrapSnapshot;
  private final PartitionId sourcePartitionId;
  private final MemberId memberId;

  public BusinessRaftBootstrapStep(
      final boolean isBootstrapSnapshot,
      final PartitionId sourcePartitionId,
      final MemberId memberId) {
    this.isBootstrapSnapshot = isBootstrapSnapshot;
    this.sourcePartitionId = sourcePartitionId;
    this.memberId = memberId;
  }

  @Override
  public ActorFuture<BusinessPartitionStartupContext> startup(
      final BusinessPartitionStartupContext context) {
    final var result =
        context.getConcurrencyControl().<BusinessPartitionStartupContext>createFuture();
    final var partition =
        context
            .getRaftPartitionFactory()
            .createRaftPartition(
                context.getPartitionMetadata(),
                context.getSnapshotProvider(),
                context.getEntryValidator(),
                context.getMeterRegistry(),
                context.getTransferSnapshotProvider());
    final CompletableFuture<RaftPartition> bootstrap;
    if (isBootstrapSnapshot) {
      bootstrap = partition.bootstrap(sourcePartitionId, memberId);
    } else {
      bootstrap = partition.bootstrap();
    }
    bootstrap.whenComplete(
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
  public String getName() {
    return "Business Bootstrapped Raft Partition";
  }
}
