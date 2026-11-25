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
package com.anyilanxin.kunpeng.broker.business.raft.step.transition.apipartition;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.anyilanxin.kunpeng.broker.business.raft.step.transition.BusinessTransitionContent;
import com.anyilanxin.kunpeng.broker.business.raft.step.transition.apipartition.impl.InterPartitionCommandReceiverActor;
import com.anyilanxin.kunpeng.broker.business.raft.step.transition.apipartition.impl.InterPartitionCommandSenderService;
import com.anyilanxin.kunpeng.scheduler.ConcurrencyControl;
import com.anyilanxin.kunpeng.scheduler.future.ActorFuture;
import com.anyilanxin.kunpeng.scheduler.future.CompletableActorFuture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * 分区间命令收发服务在分区退位时的关闭单测。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
class InterPartitionCommandServiceStepTest {
  private final InterPartitionCommandServiceStep step = new InterPartitionCommandServiceStep();
  private final BusinessTransitionContent context = mock(BusinessTransitionContent.class);
  private final ConcurrencyControl concurrencyControl = mock(ConcurrencyControl.class);
  private final ActorFuture<Void> transitionFuture = new CompletableActorFuture<>();

  @BeforeEach
  void setUp() {
    // 被测逻辑全部跑在分区 actor 任务里：让任务就地执行
    doAnswer(
            invocation -> {
              invocation.<Runnable>getArgument(0).run();
              return null;
            })
        .when(concurrencyControl)
        .run(any(Runnable.class));
    when(context.getConcurrencyControl()).thenReturn(concurrencyControl);
    when(concurrencyControl.<Void>createFuture()).thenReturn(transitionFuture);
  }

  @Test
  void shouldCloseTheSenderAndTheReceiverWhenSteppingDown() {
    final var sender = mock(InterPartitionCommandSenderService.class);
    final var receiver = mock(InterPartitionCommandReceiverActor.class);
    final var senderClosed = new CompletableActorFuture<Void>();
    final var receiverClosed = new CompletableActorFuture<Void>();
    when(context.getPartitionCommandSender()).thenReturn(sender);
    when(context.getPartitionCommandReceiver()).thenReturn(receiver);
    when(sender.closeAsync()).thenReturn(senderClosed);
    when(receiver.closeAsync()).thenReturn(receiverClosed);

    final ActorFuture<Void> closing = step.onFollower(context, 2L);

    // 发送器也在关闭之列：不关的话它会带着未完成交付的重发跨任期存活
    verify(sender).closeAsync();
    verify(receiver).closeAsync();
    assertThat(closing.isDone()).isFalse();
    senderClosed.complete(null);
    assertThat(closing.isDone()).isFalse();
    receiverClosed.complete(null);
    assertThat(closing.isDone()).isTrue();
  }

  @Test
  void shouldCompleteWhenNeitherWasInstalled() {
    final ActorFuture<Void> closing = step.onInactive(context, 2L);

    assertThat(closing.isDone()).isTrue();
  }
}
