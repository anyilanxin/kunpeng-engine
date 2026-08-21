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

import io.atomix.raft.storage.log.entry.ApplicationEntry;
import io.atomix.raft.storage.log.entry.RaftLogEntry;
import io.atomix.raft.storage.log.entry.SerializedApplicationEntry;
import io.atomix.raft.journal.CheckedJournalException;
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

class RaftLogUncommittedReaderTest {

  private RaftLog raftlog;
  private RaftLogReader uncommittedReader;

  @AutoClose private final MeterRegistry meterRegistry = new SimpleMeterRegistry();

  private final ByteBuffer data =
      ByteBuffer.allocate(Integer.BYTES).order(ByteOrder.LITTLE_ENDIAN).putInt(0, 123456);

  @BeforeEach
  void setup(@TempDir final File directory) {
    raftlog =
        RaftLog.builder(meterRegistry)
            .withDirectory(directory)
            .withName("test")
            .withMetaStore(new InMemory())
            .build();
    uncommittedReader = raftlog.openUncommittedReader();
    data.order(ByteOrder.LITTLE_ENDIAN).putInt(123456);
  }

  @AfterEach
  void tearDown() {
    uncommittedReader.close();
    raftlog.close();
  }

  @Test
  void shouldReadUncommittedEntries() {
    // when
    appendEntries(2);

    // then
    assertThat(uncommittedReader.hasNext()).isTrue();
    assertThat(uncommittedReader.next().index()).isEqualTo(1);

    assertThat(uncommittedReader.hasNext()).isTrue();
    assertThat(uncommittedReader.next().index()).isEqualTo(2);

    assertThat(uncommittedReader.hasNext()).isFalse();
  }

  @Test
  void shouldSeekToIndex() {
    // given
    appendEntries(10);

    // when
    final var nextIndex = uncommittedReader.seek(5);

    // then
    assertThat(nextIndex).isEqualTo(5);
    assertThat(uncommittedReader.hasNext()).isTrue();
    assertThat(uncommittedReader.next().index()).isEqualTo(5);
  }

  @Test
  void shouldSeekToIndexAfterTruncate() throws CheckedJournalException {
    // given
    appendEntries(6);
    uncommittedReader.seek(5);

    // when
    raftlog.deleteAfter(3);
    final var nextIndex = uncommittedReader.seek(5);

    // then
    assertThat(nextIndex).isEqualTo(4);
    assertThat(uncommittedReader.hasNext()).isFalse();
  }

  @Test
  void shouldSeekToLast() {
    // given
    appendEntries(5);

    // when
    final var nextIndex = uncommittedReader.seekToLast();

    // then
    assertThat(nextIndex).isEqualTo(5);
    assertThat(uncommittedReader.hasNext()).isTrue();
    assertThat(uncommittedReader.next().index()).isEqualTo(5);
  }

  @Test
  void shouldSeekToAsqn() {
    // given
    appendEntries(10);

    // when
    final var nextIndex = uncommittedReader.seekToAsqn(5);

    // then
    assertThat(nextIndex).isEqualTo(5);
    assertThat(uncommittedReader.hasNext()).isTrue();
    assertThat(uncommittedReader.next().getPersistedRaftRecord().asqn()).isEqualTo(5);
  }

  @Test
  void shouldSeekToLastAsqn() {
    // given
    appendEntries(10);

    // when
    final var nextIndex = uncommittedReader.seekToAsqn(Long.MAX_VALUE);

    // then
    assertThat(nextIndex).isEqualTo(10);
    assertThat(uncommittedReader.hasNext()).isTrue();
    assertThat(uncommittedReader.next().getPersistedRaftRecord().asqn()).isEqualTo(10);
  }

  private void appendEntries(final int count) {
    for (int i = 0; i < count; i++) {
      final ApplicationEntry applicationEntry = new SerializedApplicationEntry(i + 1, i + 1, data);
      final RaftLogEntry entry = new RaftLogEntry(1, applicationEntry);
      raftlog.append(entry);
    }
  }
}
