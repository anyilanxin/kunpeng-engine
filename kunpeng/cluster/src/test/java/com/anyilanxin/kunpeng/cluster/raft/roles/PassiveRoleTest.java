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
package com.anyilanxin.kunpeng.cluster.raft.roles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.anyilanxin.kunpeng.cluster.cluster.MemberId;
import com.anyilanxin.kunpeng.cluster.raft.impl.RaftContext;
import com.anyilanxin.kunpeng.cluster.raft.metrics.RaftReplicationMetrics;
import com.anyilanxin.kunpeng.cluster.raft.protocol.AppendRequest;
import com.anyilanxin.kunpeng.cluster.raft.protocol.AppendResponse;
import com.anyilanxin.kunpeng.cluster.raft.protocol.PersistedRaftRecord;
import com.anyilanxin.kunpeng.cluster.raft.protocol.ProtocolVersionHandler;
import com.anyilanxin.kunpeng.cluster.raft.protocol.ReplicatableJournalRecord;
import com.anyilanxin.kunpeng.cluster.raft.protocol.VersionedAppendRequest;
import com.anyilanxin.kunpeng.cluster.raft.storage.RaftStorage;
import com.anyilanxin.kunpeng.cluster.raft.storage.log.IndexedRaftLogEntry;
import com.anyilanxin.kunpeng.cluster.raft.storage.log.RaftLog;
import com.anyilanxin.kunpeng.cluster.raft.journal.CheckedJournalException;
import com.anyilanxin.kunpeng.cluster.raft.journal.JournalException;
import com.anyilanxin.kunpeng.cluster.raft.journal.JournalException.InvalidChecksum;
import com.anyilanxin.kunpeng.cluster.raft.snapshot.PersistableSnapshot;
import com.anyilanxin.kunpeng.cluster.raft.snapshot.RaftSnapshot;
import com.anyilanxin.kunpeng.cluster.raft.snapshot.ReceivableSnapshotStore;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.jupiter.api.AutoClose;
import org.junit.rules.Timeout;

public class PassiveRoleTest {

  @Rule public Timeout timeout = new Timeout(30, TimeUnit.SECONDS);
  private RaftLog log;
  private PassiveRole role;
  private RaftContext ctx;
  @AutoClose private final MeterRegistry meterRegistry = new SimpleMeterRegistry();

  @Before
  public void setup() throws IOException {
    ctx = mock(RaftContext.class);

    log = mock(RaftLog.class);
    when(log.flushesDirectly()).thenReturn(true);
    when(ctx.getLog()).thenReturn(log);

    final RaftSnapshot snapshot = mock(RaftSnapshot.class);
    when(snapshot.getIndex()).thenReturn(1L);
    when(snapshot.getTerm()).thenReturn(1L);

    final ReceivableSnapshotStore store = mock(ReceivableSnapshotStore.class);
    when(store.getLatestSnapshot()).thenReturn(Optional.of(snapshot));

    final RaftStorage storage = mock(RaftStorage.class);
    when(ctx.getStorage()).thenReturn(storage);
    when(ctx.getLog()).thenReturn(log);
    when(ctx.getPersistedSnapshotStore()).thenReturn(store);
    when(ctx.getTerm()).thenReturn(1L);
    when(ctx.getReplicationMetrics()).thenReturn(mock(RaftReplicationMetrics.class));
    when(ctx.getMeterRegistry()).thenReturn(meterRegistry);
    when(ctx.getName()).thenReturn("partition-1");

    role = new PassiveRole(ctx);
  }

  @Test
  public void shouldFailAppendWithIncorrectChecksum() {
    // given
    final var entries = List.of(new ReplicatableJournalRecord(1, 1, 12345, new byte[1]));
    final VersionedAppendRequest request =
        VersionedAppendRequest.builder()
            .withTerm(2)
            .withLeader(MemberId.anonymous())
            .withPrevLogTerm(0)
            .withPrevLogIndex(0)
            .withEntries(entries)
            .withCommitIndex(1)
            .build();

    when(log.append(any(ReplicatableJournalRecord.class)))
        .thenThrow(new JournalException.InvalidChecksum("expected"));

    // when
    final AppendResponse response =
        role.handleAppend(ProtocolVersionHandler.transform(request)).join();

    // then
    assertThat(response.succeeded()).isFalse();
  }

  @Test
  public void shouldFlushAfterAppendRequest() throws CheckedJournalException {
    // given
    final var entries =
        List.of(
            new ReplicatableJournalRecord(1, 1, 1, new byte[1]),
            new ReplicatableJournalRecord(1, 2, 1, new byte[1]));
    final VersionedAppendRequest request =
        VersionedAppendRequest.builder()
            .withTerm(1)
            .withLeader(MemberId.anonymous())
            .withPrevLogTerm(0)
            .withPrevLogIndex(0)
            .withEntries(entries)
            .withCommitIndex(2)
            .build();

    when(log.append(any(ReplicatableJournalRecord.class)))
        .thenReturn(mock(IndexedRaftLogEntry.class))
        .thenReturn(mock(IndexedRaftLogEntry.class));

    // when
    final AppendResponse response =
        role.handleAppend(ProtocolVersionHandler.transform(request)).join();

    // then
    verify(log, times(1)).flush();
    assertThat(response.lastLogIndex()).isEqualTo(2);
  }

  @Test
  public void shouldFlushAfterPartiallyAppendedRequest() throws CheckedJournalException {
    // given
    final var entries =
        List.of(
            new ReplicatableJournalRecord(1, 1, 1, new byte[1]),
            new ReplicatableJournalRecord(1, 2, 1, new byte[1]));
    final VersionedAppendRequest request =
        VersionedAppendRequest.builder()
            .withTerm(1)
            .withLeader(MemberId.anonymous())
            .withPrevLogTerm(0)
            .withPrevLogIndex(0)
            .withEntries(entries)
            .withCommitIndex(2)
            .build();

    when(log.append(any(ReplicatableJournalRecord.class)))
        .thenReturn(mock(IndexedRaftLogEntry.class))
        .thenThrow(new InvalidChecksum.InvalidChecksum("expected"));

    // when
    final AppendResponse response =
        role.handleAppend(ProtocolVersionHandler.transform(request)).join();

    // then
    verify(log, times(1)).flush();
    assertThat(response.lastLogIndex()).isOne();
  }

  @Test
  public void shouldNotFlushIfNoEntryIsAppended() throws CheckedJournalException {
    // given
    final var entries = List.of(new ReplicatableJournalRecord(1, 1, 1, new byte[1]));
    final VersionedAppendRequest request =
        VersionedAppendRequest.builder()
            .withTerm(1)
            .withLeader(MemberId.anonymous())
            .withPrevLogTerm(0)
            .withPrevLogIndex(0)
            .withEntries(entries)
            .withCommitIndex(2)
            .build();

    when(log.append(any(ReplicatableJournalRecord.class)))
        .thenThrow(new InvalidChecksum.InvalidChecksum("expected"));

    // when
    final AppendResponse response =
        role.handleAppend(ProtocolVersionHandler.transform(request)).join();

    // then
    verify(log, never()).flush();
    assertThat(response.lastLogIndex()).isZero();
  }

  @Test
  public void shouldFlushEventWithFailure() throws CheckedJournalException {
    // given
    final var entries =
        List.of(
            new ReplicatableJournalRecord(1, 1, 1, new byte[1]),
            new ReplicatableJournalRecord(1, 2, 1, new byte[1]),
            new ReplicatableJournalRecord(1, 3, 1, new byte[1]));
    final VersionedAppendRequest request =
        VersionedAppendRequest.builder()
            .withTerm(1)
            .withLeader(MemberId.anonymous())
            .withPrevLogTerm(0)
            .withPrevLogIndex(0)
            .withEntries(entries)
            .withCommitIndex(3)
            .build();

    when(log.append(any(ReplicatableJournalRecord.class)))
        .thenReturn(mock(IndexedRaftLogEntry.class))
        .thenReturn(mock(IndexedRaftLogEntry.class))
        .thenThrow(new InvalidChecksum("expected"));
    when(ctx.getLog()).thenReturn(log);

    // when
    role.handleAppend(ProtocolVersionHandler.transform(request)).join();

    // then
    verify(log, times(1)).flush();
  }

  @Test
  public void shouldAppendOldVersion() throws CheckedJournalException {
    // given
    final var entries = List.of(new PersistedRaftRecord(1, 1, 1, 1, new byte[1]));
    final var request = new AppendRequest(2, "a", 0, 0, entries, 1);

    when(log.append(any(PersistedRaftRecord.class))).thenReturn(mock(IndexedRaftLogEntry.class));

    // when
    final AppendResponse response =
        role.handleAppend(ProtocolVersionHandler.transform(request)).join();

    // then
    assertThat(response.succeeded()).isTrue();
  }

  @Test
  public void shouldCompleteFutureWithErrorIfAppendFails() throws CheckedJournalException {
    // given
    final var entries = List.of(new PersistedRaftRecord(1, 1, 1, 1, new byte[1]));
    final var request = new AppendRequest(2, "a", 0, 0, entries, 1);
    when(log.append(any(PersistedRaftRecord.class))).thenThrow(new IllegalStateException("error"));

    // when
    final var result =
        role.handleAppend(ProtocolVersionHandler.transform(request)).toCompletableFuture().join();
    // then
    assertThat(result.succeeded()).isFalse();
  }

  @Test
  public void shouldNotAbortPendingSnapshotOnEmptyAppend() throws Exception {
    // given - a pending snapshot is in progress
    final PersistableSnapshot receivedSnapshot = mock(PersistableSnapshot.class);
    setPendingSnapshot(receivedSnapshot);

    // an empty append request (heartbeat)
    final VersionedAppendRequest request =
        VersionedAppendRequest.builder()
            .withTerm(1)
            .withLeader(MemberId.anonymous())
            .withPrevLogTerm(0)
            .withPrevLogIndex(0)
            .withEntries(List.of())
            .withCommitIndex(0)
            .build();

    // when
    role.handleAppend(ProtocolVersionHandler.transform(request)).join();

    // then - the pending snapshot should not be aborted
    verify(receivedSnapshot, never()).abort();
    assertThat(getPendingSnapshot()).as("pending snapshot should still be present").isNotNull();
  }

  @Test
  public void shouldAbortPendingSnapshotOnNonEmptyAppend() throws Exception {
    // given - a pending snapshot is in progress
    final PersistableSnapshot receivedSnapshot = mock(PersistableSnapshot.class);
    setPendingSnapshot(receivedSnapshot);

    // an append request with entries
    final var entries = List.of(new ReplicatableJournalRecord(1, 1, 1, new byte[1]));
    final VersionedAppendRequest request =
        VersionedAppendRequest.builder()
            .withTerm(1)
            .withLeader(MemberId.anonymous())
            .withPrevLogTerm(0)
            .withPrevLogIndex(0)
            .withEntries(entries)
            .withCommitIndex(1)
            .build();

    when(log.append(any(ReplicatableJournalRecord.class)))
        .thenReturn(mock(IndexedRaftLogEntry.class));

    // when
    role.handleAppend(ProtocolVersionHandler.transform(request)).join();

    // then - the pending snapshot should be aborted
    verify(receivedSnapshot).abort();
    assertThat(getPendingSnapshot()).as("pending snapshot should be cleared").isNull();
  }

  private void setPendingSnapshot(final PersistableSnapshot snapshot) throws Exception {
    final var field = PassiveRole.class.getDeclaredField("pendingSnapshot");
    field.setAccessible(true);
    field.set(role, snapshot);
  }

  private Object getPendingSnapshot() throws Exception {
    final var field = PassiveRole.class.getDeclaredField("pendingSnapshot");
    field.setAccessible(true);
    return field.get(role);
  }
}
