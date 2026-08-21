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
package io.atomix.raft.roles;

import static org.assertj.core.api.Assertions.assertThat;

import io.atomix.raft.ControllableRaftContexts;
import io.atomix.raft.RaftServer.Role;
import io.atomix.test.util.RegressionTest;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Random;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;

class CandidateRoleTest {
  private ControllableRaftContexts raftContexts;

  @TempDir private Path raftDataDirectory;

  @BeforeEach
  public void before() throws Exception {
    raftContexts = new ControllableRaftContexts(3);
    raftContexts.setup(raftDataDirectory, new Random(1));
  }

  @AfterEach
  public void shutdown() throws IOException {
    raftContexts.shutdown();
  }

  @RegressionTest("https://github.com/camunda/camunda/issues/11665")
  void shouldTransitionToFollowerWhenElectionTimesOut() {
    // given
    final var chosenCandidate = 0; // chose any member as candidate
    // Timeout on chosen candidate so that it can start election before other members
    raftContexts.tickElectionTimeout(chosenCandidate);
    raftContexts.tickHeartbeatTimeout(chosenCandidate);

    // wait until chosen member becomes a candidate
    int steps = 100;
    while (!isCandidate(chosenCandidate)) {
      raftContexts.tickHeartbeatTimeout();
      raftContexts.processAllMessage();
      raftContexts.runUntilDone();
      if (steps-- < 0) {
        break;
      }
    }

    assertThat(isCandidate(chosenCandidate)).isTrue();

    // when

    // Allow enough time to run two rounds of vote request
    steps = 100;
    while (isCandidate(chosenCandidate)) {
      raftContexts.tickHeartbeatTimeout(chosenCandidate);
      // Other members do nothing so that the vote requests from the candidate can timeout
      if (steps-- < 0) {
        break;
      }
    }

    // then
    assertThat(raftContexts.getRaftContext(chosenCandidate).getRole()).isEqualTo(Role.FOLLOWER);
  }

  private boolean isCandidate(final int expectedCandidate) {
    return raftContexts.getRaftContext(expectedCandidate).getRole() == Role.CANDIDATE;
  }
}
