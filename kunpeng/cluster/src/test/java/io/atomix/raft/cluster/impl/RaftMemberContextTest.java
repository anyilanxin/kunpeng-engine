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
package io.atomix.raft.cluster.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.atomix.cluster.MemberId;
import io.atomix.raft.cluster.RaftMember.Type;
import io.atomix.raft.storage.log.IndexedRaftLogEntry;
import io.atomix.raft.storage.log.RaftLog;
import io.atomix.raft.storage.log.RaftLogReader;
import io.atomix.raft.snapshot.PersistedSnapshot;
import io.atomix.raft.snapshot.SnapshotChunkReader;
import java.nio.ByteBuffer;
import java.time.Instant;
import org.junit.jupiter.api.Test;

/** 跟随者复制上下文：字节级复制延迟（lag）统计、快照安装状态与追加成功标记。 */
final class RaftMemberContextTest {

  /** 构造一个未打开复制上下文的成员，返回其上下文对象。 */
  private static RaftMemberContext freshContext() {
    return new RaftMemberContext(
        new DefaultRaftMember(MemberId.from("m"), Type.ACTIVE, Instant.now()),
        mock(RaftClusterContext.class),
        1);
  }

  /** 打开复制上下文：日志 reader 返回 hasNext=false、seek 原样返回、给定剩余字节数。 */
  private static RaftMemberContext contextWithReader(final long bytesUntilEnd) {
    final var reader = mock(RaftLogReader.class);
    when(reader.hasNext()).thenReturn(false);
    when(reader.seek(anyLong())).thenAnswer(inv -> inv.getArgument(0));
    when(reader.bytesUntilEnd()).thenReturn(bytesUntilEnd);
    final var log = mock(RaftLog.class);
    when(log.openUncommittedReader()).thenReturn(reader);

    final var context = freshContext();
    context.openReplicationContext(log);
    return context;
  }

  @Test
  void logLagStartsAtZero() {
    assertThat(freshContext().getLogReplicationLag()).isZero();
  }

  @Test
  void appendedBytesAccumulateIntoLogLag() {
    final var context = freshContext();

    context.recordAppendedBytes(320);
    context.recordAppendedBytes(80);

    assertThat(context.getLogReplicationLag()).isEqualTo(400);
  }

  @Test
  void acknowledgingInFlightBatchDeductsItsBytes() {
    final var context = freshContext();
    context.recordAppendedBytes(600);
    final long watermark = context.recordInFlightAppend(240);

    context.acknowledgeInFlightAppends(watermark);

    assertThat(context.getLogReplicationLag()).isEqualTo(360);
  }

  @Test
  void sameWatermarkIsNeverDeductedTwice() {
    final var context = freshContext();
    context.recordAppendedBytes(600);
    final long watermark = context.recordInFlightAppend(240);
    context.acknowledgeInFlightAppends(watermark);

    context.acknowledgeInFlightAppends(watermark);

    assertThat(context.getLogReplicationLag()).isEqualTo(360);
  }

  @Test
  void acknowledgingNewerWatermarkCoversTimedOutOlderBatch() {
    final var context = freshContext();
    context.recordAppendedBytes(420);
    final long older = context.recordInFlightAppend(240);
    final long newer = context.recordInFlightAppend(180);

    context.acknowledgeInFlightAppends(newer);
    assertThat(context.getLogReplicationLag()).isZero();

    // 旧批次迟到到达时不再重复扣减
    context.acknowledgeInFlightAppends(older);
    assertThat(context.getLogReplicationLag()).isZero();
  }

  @Test
  void resetRecalibratesLogLagFromReader() {
    final var context = contextWithReader(2048L);
    context.recordAppendedBytes(64); // 制造漂移，验证 reset 后被校准覆盖

    context.reset(120);

    assertThat(context.getLogReplicationLag()).isEqualTo(2048L);
  }

  @Test
  void staleAppendCallbackFromBeforeResetIsIgnored() {
    final var context = contextWithReader(640L);
    context.recordAppendedBytes(200);
    final long watermark = context.recordInFlightAppend(200);
    context.reset(120);

    context.acknowledgeInFlightAppends(watermark);

    assertThat(context.getLogReplicationLag()).isEqualTo(640L);
  }

  @Test
  void snapshotInstallResetsLagWithoutMovingReplicationReader() {
    // 复制 reader 停在当前条目；快照安装后应由独立的 lag reader 重新计算
    final var replicationReader = mock(RaftLogReader.class);
    final var lagReader = mock(RaftLogReader.class);
    final var currentEntry = mock(IndexedRaftLogEntry.class);
    when(replicationReader.hasNext()).thenReturn(true);
    when(replicationReader.next()).thenReturn(currentEntry);
    when(lagReader.bytesUntilEnd()).thenReturn(96L);
    final var log = mock(RaftLog.class);
    when(log.openUncommittedReader()).thenReturn(replicationReader, lagReader);
    final var snapshot = mock(PersistedSnapshot.class);
    when(snapshot.getIndex()).thenReturn(14L);
    when(snapshot.getTotalSizeInBytes()).thenReturn(2_000L);

    final var context = freshContext();
    context.openReplicationContext(log);
    context.recordAppendedBytes(4_096);
    context.setNextSnapshotChunkId(ByteBuffer.allocate(4));
    final var positionBeforeInstall = context.getCurrentEntry();

    context.beginSnapshotInstall(log, snapshot);

    assertThat(context.getNextSnapshotIndex()).isEqualTo(14L);
    assertThat(context.getNextSnapshotChunk()).isNull();
    assertThat(context.getSnapshotReplicationLag()).isEqualTo(2_000L);
    assertThat(context.getLogReplicationLag()).isEqualTo(96L);
    assertThat(context.getCurrentEntry()).isSameAs(positionBeforeInstall);
    verify(lagReader).seek(15L);
    verify(lagReader).close();
    verify(replicationReader, never()).reset();
    verify(replicationReader, never()).seek(15L);
  }

  @Test
  void appendAcksFromBeforeSnapshotInstallAreDiscarded() {
    final var replicationReader = mock(RaftLogReader.class);
    final var lagReader = mock(RaftLogReader.class);
    when(lagReader.bytesUntilEnd()).thenReturn(96L);
    final var log = mock(RaftLog.class);
    when(log.openUncommittedReader()).thenReturn(replicationReader, lagReader);
    final var snapshot = mock(PersistedSnapshot.class);
    when(snapshot.getIndex()).thenReturn(14L);
    when(snapshot.getTotalSizeInBytes()).thenReturn(2_000L);

    final var context = freshContext();
    context.openReplicationContext(log);
    final long staleWatermark = context.recordInFlightAppend(40L);

    context.beginSnapshotInstall(log, snapshot);
    context.acknowledgeInFlightAppends(staleWatermark);

    assertThat(context.getLogReplicationLag()).isEqualTo(96L);
  }

  @Test
  void totalReplicationLagSumsLogAndSnapshotParts() {
    final var context = freshContext();

    context.recordAppendedBytes(640);
    context.setSnapshotReplicationLag(360);

    assertThat(context.getReplicationLagBytes()).isEqualTo(1000);
  }

  @Test
  void resetStateClearsLogAndSnapshotCounters() {
    final var context = freshContext();
    context.recordAppendedBytes(2_048);
    context.setSnapshotReplicationLag(4_096);

    context.resetState(mock(RaftLog.class));

    assertThat(context.getLogReplicationLag()).isZero();
    assertThat(context.getSnapshotReplicationLag()).isZero();
    assertThat(context.getReplicationLagBytes()).isZero();
  }

  @Test
  void snapshotLagStartsAtZero() {
    final var context = freshContext();

    assertThat(context.getSnapshotReplicationLag()).isZero();
    assertThat(context.getReplicationLagBytes()).isZero();
  }

  @Test
  void snapshotLagIsSetAndDecremented() {
    final var context = freshContext();

    context.setSnapshotReplicationLag(2_000);
    context.subtractSnapshotReplicationLag(600);

    assertThat(context.getSnapshotReplicationLag()).isEqualTo(1_400);
    assertThat(context.getReplicationLagBytes()).isEqualTo(1_400);
  }

  @Test
  void snapshotLagNeverGoesNegative() {
    final var context = freshContext();
    context.setSnapshotReplicationLag(200);

    context.subtractSnapshotReplicationLag(900);

    assertThat(context.getSnapshotReplicationLag()).isZero();
    assertThat(context.getReplicationLagBytes()).isZero();
  }

  @Test
  void inFlightSnapshotChunkBytesAreRemembered() {
    final var context = freshContext();

    context.setSnapshotChunkBytesInFlight(1_024);

    assertThat(context.getSnapshotChunkBytesInFlight()).isEqualTo(1_024);
  }

  @Test
  void resetStateClearsSnapshotTransferState() {
    final var context = freshContext();
    context.setSnapshotReplicationLag(4_096);
    context.setSnapshotChunkBytesInFlight(1_024);

    context.resetState(mock(RaftLog.class));

    assertThat(context.getSnapshotReplicationLag()).isZero();
    assertThat(context.getSnapshotChunkBytesInFlight()).isZero();
    assertThat(context.getReplicationLagBytes()).isZero();
  }

  @Test
  void closeShutsTheSnapshotChunkReader() {
    final var context = freshContext();
    final var chunkReader = mock(SnapshotChunkReader.class);
    context.setSnapshotChunkReader(chunkReader);

    context.close();

    verify(chunkReader).close();
  }

  @Test
  void appendNotYetAckedInitially() {
    assertThat(freshContext().hasAckedAppend()).isFalse();
  }

  @Test
  void successfulAppendMarksAcked() {
    final var context = freshContext();

    context.appendSucceeded();

    assertThat(context.hasAckedAppend()).isTrue();
  }

  @Test
  void failedAppendClearsAckedMark() {
    final var context = freshContext();
    context.appendSucceeded();

    context.appendFailed();

    assertThat(context.hasAckedAppend()).isFalse();
  }

  @Test
  void reopeningReplicationContextClearsAckedMark() {
    final var log = mock(RaftLog.class);
    when(log.openUncommittedReader()).thenReturn(mock(RaftLogReader.class));
    final var context = freshContext();
    context.appendSucceeded();

    context.openReplicationContext(log);

    assertThat(context.hasAckedAppend()).isFalse();
  }
}
