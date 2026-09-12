/*
 * Copyright © 2026 anyilanxin zxh (anyilanxin@aliyun.com)
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
package com.anyilanxin.kunpeng.cluster.raft.impl;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.anyilanxin.kunpeng.cluster.raft.metrics.RaftServiceMetrics;
import com.anyilanxin.kunpeng.cluster.raft.snapshot.InMemorySnapshot;
import com.anyilanxin.kunpeng.cluster.raft.snapshot.TestSnapshotStore;
import com.anyilanxin.kunpeng.cluster.raft.storage.log.RaftLog;
import com.anyilanxin.kunpeng.cluster.utils.concurrent.ThreadContext;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.AutoClose;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** 日志压缩器：按快照边界截断日志，并保留一段追赶余量。 */
final class LogCompactorTest {

  // 与上游测试不同的常量取值：边界 24、保留余量 8，期望截断到 16
  private static final long TRIM_BOUND = 24L;
  private static final int CATCHUP_RETENTION = 8;
  private static final long TRIM_TARGET_WITH_RETENTION = TRIM_BOUND - CATCHUP_RETENTION;

  private ThreadContext raftThread;
  private RaftLog raftLog;
  private LogCompactor compactor;
  @AutoClose private final MeterRegistry meterRegistry = new SimpleMeterRegistry();

  @BeforeEach
  void setUp() {
    raftThread = mock(ThreadContext.class);
    raftLog = mock(RaftLog.class);
    // 让提交到 Raft 线程的任务同步执行，简化测试
    doAnswer(
            invocation -> {
              invocation.getArgument(0, Runnable.class).run();
              return null;
            })
        .when(raftThread)
        .execute(any());
    compactor =
        new LogCompactor(raftThread, raftLog, CATCHUP_RETENTION, new RaftServiceMetrics("ct", meterRegistry));
  }

  @AfterEach
  void tearDown() {
    meterRegistry.close();
  }

  @Test
  void compactTrimsUpToBoundMinusRetention() {
    compactor.setCompactableIndex(TRIM_BOUND);

    compactor.compact();

    // 截断目标 = 可截断边界 - 为落后跟随者保留的余量
    verify(raftLog).deleteUntil(TRIM_TARGET_WITH_RETENTION);
  }

  @Test
  void compactIgnoringRetentionTrimsExactlyToBound() {
    compactor.setCompactableIndex(TRIM_BOUND);

    compactor.compactIgnoringReplicationThreshold();

    verify(raftLog).deleteUntil(TRIM_BOUND);
  }

  @Test
  void compactEnforcesRaftThreadAffinity() {
    doThrow(new IllegalStateException("仅允许 Raft 线程执行"))
        .when(raftThread)
        .checkThread();
    compactor.setCompactableIndex(TRIM_BOUND);

    assertThatThrownBy(compactor::compact).isInstanceOf(IllegalStateException.class);
    assertThatThrownBy(compactor::compactIgnoringReplicationThreshold)
        .isInstanceOf(IllegalStateException.class);
  }

  @Test
  void compactFromSnapshotsUsesOldestSnapshotAsBound() {
    final var store = new TestSnapshotStore(new AtomicReference<>());
    // 两个快照：保留较旧的那个（reserve），较新的直接提交；压缩边界取最低快照索引
    InMemorySnapshot.newPersistedSnapshot(0, 13L, 1, 60, store).reserve();
    InMemorySnapshot.newPersistedSnapshot(0, 41L, 1, 60, store);

    compactor.compactFromSnapshots(store);

    // 最低快照索引 13 再减去保留余量 8，得到截断目标 5
    verify(raftLog).deleteUntil(5L);
  }
}
