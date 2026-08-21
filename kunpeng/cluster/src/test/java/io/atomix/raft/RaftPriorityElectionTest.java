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

import io.atomix.cluster.MemberId;
import io.atomix.raft.RaftRule.Configurator;
import io.atomix.raft.RaftServer.Builder;
import io.atomix.raft.partition.RaftElectionConfig;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class RaftPriorityElectionTest {

  @Rule @Parameter public RaftRule raftRule;

  @Parameters(name = "{index}: {0}")
  public static Object[][] raftConfigurations() {
    return new Object[][] {
      new Object[] {
        RaftRule.withBootstrappedNodes(
            3,
            new Configurator() {
              @Override
              public void configure(final MemberId id, final Builder builder) {
                builder.withElectionConfig(
                    RaftElectionConfig.ofPriorityElection(3, Integer.parseInt(id.id()) + 1));
              }
            })
      },
      new Object[] {
        RaftRule.withBootstrappedNodes(
            4,
            new Configurator() {
              @Override
              public void configure(final MemberId id, final Builder builder) {
                builder.withElectionConfig(
                    RaftElectionConfig.ofPriorityElection(4, Integer.parseInt(id.id()) + 1));
              }
            })
      },
      new Object[] {
        RaftRule.withBootstrappedNodes(
            5,
            new Configurator() {
              @Override
              public void configure(final MemberId id, final Builder builder) {
                builder.withElectionConfig(
                    RaftElectionConfig.ofPriorityElection(5, Integer.parseInt(id.id()) + 1));
              }
            })
      }
    };
  }

  // Note: Priority election is not deterministic hence we cannot deterministically test if leaders
  // are elected according to the priority. Instead here we only test that leader election succeeds.
  @Test
  public void shouldElectNewLeadersWhenLeaderUnavailable() throws Throwable {
    // given
    final int failureTolerance = (raftRule.getMemberIds().size() - 1) / 2;

    for (int i = 0; i < failureTolerance; i++) {
      raftRule.appendEntries(1);
      // when
      raftRule.shutdownLeader();

      // then
      raftRule.awaitNewLeader();
    }
  }
}
