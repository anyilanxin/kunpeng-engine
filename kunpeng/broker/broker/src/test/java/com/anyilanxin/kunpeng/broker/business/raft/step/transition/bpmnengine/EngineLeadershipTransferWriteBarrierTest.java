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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.anyilanxin.kunpeng.broker.client.business.commandapi.CommandApiService;
import com.anyilanxin.kunpeng.engine.bpmn.EngineProcessService;
import com.anyilanxin.kunpeng.scheduler.future.CompletableActorFuture;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.mockito.InOrder;

/**
 * leader 转移写屏障单测：冻结与解冻的先后顺序、超时回滚、引擎拒绝冻结时回滚。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
@Timeout(30)
class EngineLeadershipTransferWriteBarrierTest {
  private static final int PARTITION_ID = 1;
  private static final Duration TIMEOUT = Duration.ofSeconds(5);

  private final CommandApiService commandApiService = mock(CommandApiService.class);
  private final EngineProcessService engineService = mock(EngineProcessService.class);
  private final EngineLeadershipTransferWriteBarrier barrier =
      new EngineLeadershipTransferWriteBarrier(PARTITION_ID, commandApiService, engineService);

  @BeforeEach
  void setUp() {
    when(commandApiService.markLeadershipTransferring(anyInt(), anyBoolean()))
        .thenReturn(CompletableActorFuture.completed());
    when(engineService.freezeForLeadershipTransfer())
        .thenReturn(CompletableActorFuture.completed());
    when(engineService.unfreezeFromLeadershipTransfer())
        .thenReturn(CompletableActorFuture.completed());
  }

  @Test
  void shouldCloseTheCommandEntryBeforeFreezingTheEngineAndReportThatInstant()
      throws Exception {
    final long before = System.currentTimeMillis();

    final long commandsRejectedSinceMs = barrier.freeze(TIMEOUT).get(10, TimeUnit.SECONDS);

    // 完成值是命令入口关上的时刻：不早于冻结发起，不晚于引擎静止
    assertThat(commandsRejectedSinceMs).isBetween(before, System.currentTimeMillis());
    final InOrder inOrder = inOrder(commandApiService, engineService);
    inOrder.verify(commandApiService).markLeadershipTransferring(PARTITION_ID, true);
    inOrder.verify(engineService).freezeForLeadershipTransfer();
  }

  @Test
  void shouldReopenTheCommandEntryOnlyAfterTheEngineIsUnfrozen() throws Exception {
    barrier.unfreeze().get(10, TimeUnit.SECONDS);

    final InOrder inOrder = inOrder(commandApiService, engineService);
    inOrder.verify(engineService).unfreezeFromLeadershipTransfer();
    inOrder.verify(commandApiService).markLeadershipTransferring(PARTITION_ID, false);
  }

  @Test
  void shouldResumeThePartitionWhenTheEngineDoesNotQuiesceInTime() {
    // 引擎一直在处理，静止不了
    when(engineService.freezeForLeadershipTransfer())
        .thenReturn(new CompletableActorFuture<>());

    assertThatThrownBy(() -> barrier.freeze(Duration.ofMillis(300)).get(10, TimeUnit.SECONDS))
        .hasCauseInstanceOf(TimeoutException.class);

    final InOrder inOrder = inOrder(commandApiService, engineService);
    inOrder.verify(engineService).unfreezeFromLeadershipTransfer();
    inOrder.verify(commandApiService).markLeadershipTransferring(PARTITION_ID, false);
  }

  @Test
  void shouldResumeThePartitionWhenTheEngineRefusesToFreeze() {
    // 例如数据手术冻结正在进行：两种冻结互斥
    when(engineService.freezeForLeadershipTransfer())
        .thenReturn(
            CompletableActorFuture.<Void>completedExceptionally(
                new IllegalStateException("already frozen for DATA_SURGERY")));

    assertThatThrownBy(() -> barrier.freeze(TIMEOUT).get(10, TimeUnit.SECONDS))
        .cause()
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("already frozen for DATA_SURGERY");

    // 入口已经关上，必须重新放开
    verify(commandApiService).markLeadershipTransferring(PARTITION_ID, false);
  }

  @Test
  void shouldNotFreezeTheEngineWhenTheCommandEntryCannotBeMarked() {
    when(commandApiService.markLeadershipTransferring(anyInt(), anyBoolean()))
        .thenReturn(
            CompletableActorFuture.<Void>completedExceptionally(
                new IllegalStateException("command API closed")));

    assertThatThrownBy(() -> barrier.freeze(TIMEOUT).get(10, TimeUnit.SECONDS))
        .hasCauseInstanceOf(IllegalStateException.class);

    verify(engineService, never()).freezeForLeadershipTransfer();
  }
}
