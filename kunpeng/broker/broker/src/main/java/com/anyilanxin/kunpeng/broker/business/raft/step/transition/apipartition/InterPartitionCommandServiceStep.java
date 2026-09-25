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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.anyilanxin.kunpeng.broker.business.raft.step.transition.apipartition;

import com.anyilanxin.kunpeng.broker.business.raft.step.transition.BusinessTransitionContent;
import com.anyilanxin.kunpeng.broker.business.raft.step.transition.apipartition.impl.InterPartitionCommandReceiverActor;
import com.anyilanxin.kunpeng.broker.business.raft.step.transition.apipartition.impl.InterPartitionCommandSenderService;
import com.anyilanxin.kunpeng.cluster.business.step.transition.TransitionStep;
import com.anyilanxin.kunpeng.protocol.business.impl.record.DefaultRecordValueMapper;
import com.anyilanxin.kunpeng.scheduler.ConcurrencyControl;
import com.anyilanxin.kunpeng.scheduler.future.ActorFuture;

/**
 * 分区间命令收发服务的 transition 步骤：leader 分区安装接收器（消费其他分区发来的命令并写入本分区日志）与发送器（引擎跨分区命令出口， 存入 content 供引擎装配消费）；非
 * leader 角色关闭两者。
 *
 * <p>发送器的分区 leader 路由（{@code setCurrentLeader}）依赖拓扑层的分区监听注册 API， 待集群拓扑层提供后再接线。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public final class InterPartitionCommandServiceStep
    implements TransitionStep<BusinessTransitionContent> {

  @Override
  public String getName() {
    return "Inter Partition LogEventProcessor";
  }

  @Override
  public ActorFuture<Void> onLeader(
      final BusinessTransitionContent context, final long currentTerm) {
    final ConcurrencyControl concurrencyControl = context.getConcurrencyControl();
    final ActorFuture<Void> future = concurrencyControl.createFuture();
    concurrencyControl.run(
        () ->
            installReceiver(context)
                .onComplete(
                    (ignored, error) -> {
                      if (error != null) {
                        future.completeExceptionally(error);
                        return;
                      }
                      installSender(context)
                          .onComplete(
                              (ignored2, error2) -> {
                                if (error2 != null) {
                                  future.completeExceptionally(error2);
                                } else {
                                  future.complete(null);
                                }
                              });
                    }));
    return future;
  }

  @Override
  public ActorFuture<Void> onFollower(
      final BusinessTransitionContent context, final long currentTerm) {
    return close(context);
  }

  @Override
  public ActorFuture<Void> onInactive(
      final BusinessTransitionContent context, final long currentTerm) {
    return close(context);
  }

  private ActorFuture<Void> installReceiver(final BusinessTransitionContent context) {
    final var receiver =
        new InterPartitionCommandReceiverActor(
            context.getRaftPartitionId().id(),
            context.getCommunicationService(),
            context.getEventLog().newWriter(),
            DefaultRecordValueMapper.getInstance());
    return context
        .getSchedulingService()
        .submitActor(receiver)
        .thenApply(
            ignored -> {
              context.setPartitionCommandReceiver(receiver);
              return null;
            });
  }

  private ActorFuture<Void> installSender(final BusinessTransitionContent context) {
    final var sender =
        new InterPartitionCommandSenderService(
            context.getCommunicationService(), context.getRaftPartitionId().id());
    return context
        .getSchedulingService()
        .submitActor(sender)
        .thenApply(
            ignored -> {
              context.setPartitionCommandSender(sender);
              // 挂接分区拓扑通知：leader 变化即刷新 sender 路由（setCurrentLeader）
              final var notifier = context.getTopologyNotifier();
              if (notifier != null) {
                notifier.addListener(sender);
              }
              return null;
            });
  }

  private ActorFuture<Void> close(final BusinessTransitionContent context) {
    final ConcurrencyControl concurrencyControl = context.getConcurrencyControl();
    final ActorFuture<Void> future = concurrencyControl.createFuture();
    concurrencyControl.run(
        () -> {
          final var notifier = context.getTopologyNotifier();
          final var receiver = context.getPartitionCommandReceiver();
          final var sender = context.getPartitionCommandSender();
          if (notifier != null && sender != null) {
            notifier.removeListener(sender);
          }
          final ActorFuture<Void> receiverClosed =
              receiver == null
                  ? concurrencyControl.<Void>createCompletedFuture()
                  : receiver.closeAsync();
          final ActorFuture<Void> senderClosed =
              sender == null
                  ? concurrencyControl.<Void>createCompletedFuture()
                  : sender.closeAsync();
          receiverClosed.onComplete(
              (ignored, error) -> {
                context.setPartitionCommandReceiver(null);
                senderClosed.onComplete(
                    (ignored2, error2) -> {
                      context.setPartitionCommandSender(null);
                      if (error != null) {
                        future.completeExceptionally(error);
                      } else if (error2 != null) {
                        future.completeExceptionally(error2);
                      } else {
                        future.complete(null);
                      }
                    });
              });
        });
    return future;
  }
}
