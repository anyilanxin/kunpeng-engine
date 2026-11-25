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
package com.anyilanxin.kunpeng.broker.business.raft.step.transition.bpmnengine;

import com.anyilanxin.kunpeng.broker.client.business.commandapi.CommandApiService;
import com.anyilanxin.kunpeng.cluster.raft.LeadershipTransferWriteBarrier;
import com.anyilanxin.kunpeng.engine.bpmn.EngineProcessService;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * leader 转移期间冻结业务分区写入的屏障：Raft 层的协调转移在转移开始时冻结、在老 leader 继续任职的收尾路径上解冻。
 *
 * <p>冻结分三步，顺序不能换：先让命令入口回绝新命令（网关收到 {@code NOT_LEADER}
 * 会退避后重新寻址再发，不会撞上后面的冻结），再冻结日志定序器并暂停内部触发，最后等引擎静止—— 到此，冻结之前收下的命令全部处理并应答完毕，日志头不再移动，Raft
 * 层才固定追赶目标。解冻反过来：先解冻定序器，再放开命令入口。
 *
 * <p>超出时限仍未静止就回滚，转移失败，老 leader 继续服务。转移成功时老 leader 直接退位、不会有人来解冻：命令入口的标记随退位清除，定序器的冻结随引擎关闭解除。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public final class EngineLeadershipTransferWriteBarrier implements LeadershipTransferWriteBarrier {
  private static final Logger LOG =
      LoggerFactory.getLogger(EngineLeadershipTransferWriteBarrier.class);

  private final int partitionId;
  private final CommandApiService commandApiService;
  private final EngineProcessService engineService;

  public EngineLeadershipTransferWriteBarrier(
      final int partitionId,
      final CommandApiService commandApiService,
      final EngineProcessService engineService) {
    this.partitionId = partitionId;
    this.commandApiService = commandApiService;
    this.engineService = engineService;
  }

  /** 完成值为命令入口关上的时刻：此后分区不再收下新的业务命令，Raft 层的暂停看门狗从这一刻起算。 */
  @Override
  public CompletableFuture<Long> freeze(final Duration timeout) {
    return commandApiService
        .markLeadershipTransferring(partitionId, true)
        .toCompletableFuture()
        .thenCompose(
            ignored -> {
              final long commandsRejectedSinceMs = System.currentTimeMillis();
              return engineService
                  .freezeForLeadershipTransfer()
                  .toCompletableFuture()
                  .thenApply(quiesced -> commandsRejectedSinceMs);
            })
        .orTimeout(timeout.toMillis(), TimeUnit.MILLISECONDS)
        .whenComplete(
            (ignored, error) -> {
              if (error != null) {
                LOG.warn(
                    "Partition {} writes could not be frozen for leadership transfer within {};"
                        + " resuming the partition",
                    partitionId,
                    timeout,
                    error);
                unfreeze();
              }
            });
  }

  @Override
  public CompletableFuture<Void> unfreeze() {
    // 先解冻定序器再放开命令入口：顺序反了，入口放进来的命令会撞上还冻着的定序器
    return engineService
        .unfreezeFromLeadershipTransfer()
        .toCompletableFuture()
        .thenCompose(
            ignored ->
                commandApiService
                    .markLeadershipTransferring(partitionId, false)
                    .toCompletableFuture());
  }
}
