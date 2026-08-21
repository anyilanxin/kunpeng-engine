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
package io.atomix.raft;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import io.atomix.raft.storage.log.IndexedRaftLogEntry;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class RaftAppendTest {

  @Rule @Parameter public RaftRule raftRule;

  @Parameters(name = "{index}: {0}")
  public static Object[][] raftConfigurations() {
    return new Object[][] {
      new Object[] {RaftRule.withBootstrappedNodes(2)},
      new Object[] {RaftRule.withBootstrappedNodes(3)},
      new Object[] {RaftRule.withBootstrappedNodes(4)},
      new Object[] {RaftRule.withBootstrappedNodes(5)}
    };
  }

  @Test
  public void shouldAppendEntryOnAllNodes() throws Throwable {
    // given

    // when
    final long commitIndex = raftRule.appendEntry();

    // then
    raftRule.awaitCommit(commitIndex);
    raftRule.awaitSameLogSizeOnAllNodes(commitIndex);
    final var memberLog = raftRule.getMemberLogs();

    final var logLength = memberLog.values().stream().map(List::size).findFirst().orElseThrow();
    assertThat(logLength).withFailMessage(memberLog.toString()).isEqualTo(2);

    assertMemberLogs(memberLog);
  }

  @Test
  public void shouldNotifyCommitListenerOnAllNodes() throws Throwable {
    // given
    final var raftCommitListener = mock(RaftCommitListener.class);
    raftRule.addCommitListener(raftCommitListener);

    // when
    final long commitIndex = raftRule.appendEntry();

    // then
    verify(raftCommitListener, timeout(1000L).times(raftRule.getNodes().size()))
        .onCommit(commitIndex);
  }

  @Test
  public void shouldNotifyCommittedEntryListenerOnLeaderOnly() throws Throwable {
    // given
    final var committedEntryListener = mock(RaftApplicationEntryCommittedPositionListener.class);
    raftRule.addCommittedEntryListener(committedEntryListener);

    // when
    raftRule.appendEntry(); // awaits commit

    // then
    verify(committedEntryListener, timeout(1000L).times(1)).onCommit(anyLong());
  }

  @Test
  public void shouldAppendEntriesOnAllNodes() throws Throwable {
    // given
    final var entryCount = 128;

    // when
    final var lastIndex = raftRule.appendEntries(entryCount);

    // then
    raftRule.awaitSameLogSizeOnAllNodes(lastIndex);
    final var memberLog = raftRule.getMemberLogs();

    final var maxIndex =
        memberLog.values().stream()
            .flatMap(Collection::stream)
            .map(IndexedRaftLogEntry::index)
            .max(Long::compareTo)
            .orElseThrow();
    assertThat(maxIndex).isEqualTo(lastIndex);
    assertThat(lastIndex).isEqualTo(entryCount + 1);
    assertMemberLogs(memberLog);
  }

  private void assertMemberLogs(final Map<String, List<IndexedRaftLogEntry>> memberLog) {
    final var firstMemberEntries = memberLog.get("1");
    final var members = memberLog.keySet();
    for (final var member : members) {
      if (!member.equals("1")) {
        final var otherEntries = memberLog.get(member);

        assertThat(otherEntries)
            .describedAs("Entry comparison 1 vs " + member)
            .containsExactly(firstMemberEntries.toArray(new IndexedRaftLogEntry[0]));
      }
    }
  }
}
