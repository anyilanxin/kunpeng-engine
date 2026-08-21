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
package io.atomix.raft.storage.log;

import static org.assertj.core.api.Assertions.assertThat;

import io.atomix.raft.storage.log.entry.RaftLogEntry;
import io.atomix.raft.storage.log.entry.SerializedApplicationEntry;
import io.atomix.raft.journal.JournalMetaStore.InMemory;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.io.File;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.AutoClose;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class RaftLogCommittedReaderTest {

  private RaftLog raftlog;
  private RaftLogReader committedReader;

  @AutoClose private final MeterRegistry meterRegistry = new SimpleMeterRegistry();

  private final ByteBuffer data =
      ByteBuffer.allocate(Integer.BYTES).order(ByteOrder.LITTLE_ENDIAN).putInt(123456);

  @BeforeEach
  void setup(@TempDir final File directory) {
    raftlog =
        RaftLog.builder(meterRegistry)
            .withDirectory(directory)
            .withName("test")
            .withMetaStore(new InMemory())
            .build();
    committedReader = raftlog.openCommittedReader();
  }

  @AfterEach
  void tearDown() {
    committedReader.close();
    raftlog.close();
  }

  @Test
  void shouldReadOnlyCommittedEntries() {
    // given
    appendEntries(3);

    // when
    raftlog.setCommitIndex(2);

    // then
    assertThat(committedReader.hasNext()).isTrue();
    assertThat(committedReader.next().index()).isEqualTo(1);

    assertThat(committedReader.hasNext()).isTrue();
    assertThat(committedReader.next().index()).isEqualTo(2);

    assertThat(committedReader.hasNext()).isFalse();
  }

  @Test
  void shouldSeekToIndex() {
    // given
    appendEntries(10);
    raftlog.setCommitIndex(10);

    // when
    final var nextIndex = committedReader.seek(5);

    // then
    assertThat(nextIndex).isEqualTo(5);
    assertThat(committedReader.hasNext()).isTrue();
    assertThat(committedReader.next().index()).isEqualTo(5);
  }

  @Test
  void shouldNotSeekBeyondCommittedIndex() {
    // given
    appendEntries(10);
    raftlog.setCommitIndex(4);

    // when
    final var nextIndex = committedReader.seek(5);

    // then
    assertThat(nextIndex).isEqualTo(5);
    assertThat(committedReader.hasNext()).isFalse();
  }

  @Test
  void shouldSeekToLastCommittedIndex() {
    // given
    appendEntries(10);
    raftlog.setCommitIndex(5);

    // when
    final var nextIndex = committedReader.seekToLast();

    // then
    assertThat(nextIndex).isEqualTo(5);
    assertThat(committedReader.hasNext()).isTrue();
    assertThat(committedReader.next().index()).isEqualTo(5);
  }

  @Test
  void shouldSeekToAsqnWithInCommittedIndex() {
    // given
    appendEntries(10);
    raftlog.setCommitIndex(4);

    // when
    final var nextIndex = committedReader.seekToAsqn(5);

    // then
    assertThat(nextIndex).isEqualTo(4);
    assertThat(committedReader.hasNext()).isTrue();
    assertThat(committedReader.next().getPersistedRaftRecord().asqn()).isEqualTo(4);
  }

  @Test
  void shouldSeekToLastAsqnWithInCommittedIndex() {
    // given
    appendEntries(10);
    raftlog.setCommitIndex(4);

    // when
    final var nextIndex = committedReader.seekToAsqn(Long.MAX_VALUE);

    // then
    assertThat(nextIndex).isEqualTo(4);
    assertThat(committedReader.hasNext()).isTrue();
    assertThat(committedReader.next().getPersistedRaftRecord().asqn()).isEqualTo(4);
  }

  private void appendEntries(final int count) {
    for (int i = 0; i < count; i++) {
      final var applicationEntry = new SerializedApplicationEntry(i + 1, i + 1, data);
      final var entry = new RaftLogEntry(1, applicationEntry);
      raftlog.append(entry);
    }
  }
}
