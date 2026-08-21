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
package io.atomix.raft.utils;

import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import io.atomix.cluster.MemberId;
import java.util.function.Consumer;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** 投票 quorum：多数派判定、去重、未知成员忽略，以及失败后的不可恢复性。 */
final class VoteQuorumTest {

  /** 构造一个以 “n1/n2/n3” 为选民集合的 quorum，回调用 mock 记录判定结果。 */
  private static SimpleVoteQuorum simpleQuorum(final Consumer<Boolean> onDecided) {
    return new SimpleVoteQuorum(
        onDecided,
        java.util.Set.of(MemberId.from("n1"), MemberId.from("n2"), MemberId.from("n3")));
  }

  @SuppressWarnings("unchecked")
  private static Consumer<Boolean> recorder() {
    return mock(Consumer.class);
  }

  @Nested
  final class SingleConfigurationQuorum {

    @Test
    void oneAckOfThreeIsNotEnough() {
      final var callback = recorder();
      final var quorum = simpleQuorum(callback);

      quorum.succeed(MemberId.from("n1"));

      verify(callback, never()).accept(anyBoolean());
    }

    @Test
    void twoAcksOfThreeReachQuorum() {
      final var callback = recorder();
      final var quorum = simpleQuorum(callback);

      quorum.succeed(MemberId.from("n1"));
      quorum.succeed(MemberId.from("n2"));

      verify(callback, times(1)).accept(true);
    }

    @Test
    void repeatedAcksFromSameVoterAreCountedOnce() {
      final var callback = recorder();
      final var quorum = simpleQuorum(callback);

      quorum.succeed(MemberId.from("n2"));
      quorum.succeed(MemberId.from("n2"));

      verify(callback, never()).accept(anyBoolean());
    }

    @Test
    void acksFromVotersOutsideTheConfigurationAreDropped() {
      final var callback = recorder();
      final var quorum = simpleQuorum(callback);

      quorum.succeed(MemberId.from("n9"));
      quorum.succeed(MemberId.from("n8"));

      verify(callback, never()).accept(anyBoolean());
    }

    @Test
    void decisionIsEmittedExactlyOnce() {
      final var callback = recorder();
      final var quorum = simpleQuorum(callback);

      quorum.succeed(MemberId.from("n1"));
      quorum.succeed(MemberId.from("n3"));
      quorum.succeed(MemberId.from("n2"));

      verify(callback, times(1)).accept(true);
    }

    @Test
    void majorityOfFailuresDecidesNegatively() {
      final var callback = recorder();
      final var quorum = simpleQuorum(callback);

      quorum.fail(MemberId.from("n1"));
      quorum.fail(MemberId.from("n3"));

      verify(callback, times(1)).accept(false);
    }

    @Test
    void subsequentSuccessCannotOverturnFailure() {
      final var callback = recorder();
      final var quorum = simpleQuorum(callback);

      quorum.fail(MemberId.from("n1"));
      quorum.fail(MemberId.from("n3"));
      quorum.succeed(MemberId.from("n1"));
      quorum.succeed(MemberId.from("n2"));

      verify(callback, times(1)).accept(false);
      verify(callback, never()).accept(true);
    }
  }

  @Nested
  final class JointConsensusQuorum {

    // 旧配置 n1/n2/n4，新配置 n1/n2/n3：两份配置都需要各自的多数派
    private static JointConsensusVoteQuorum jointQuorum(final Consumer<Boolean> onDecided) {
      return new JointConsensusVoteQuorum(
          onDecided,
          java.util.Set.of(MemberId.from("n1"), MemberId.from("n2"), MemberId.from("n4")),
          java.util.Set.of(MemberId.from("n1"), MemberId.from("n2"), MemberId.from("n3")));
    }

    @Test
    void sharedVotersSatisfyBothSidesAtOnce() {
      final var callback = recorder();
      final var quorum = jointQuorum(callback);

      quorum.succeed(MemberId.from("n1"));
      quorum.succeed(MemberId.from("n2"));

      verify(callback, times(1)).accept(true);
    }

    @Test
    void disjointSidesEachNeedTheirOwnMajority() {
      final var callback = recorder();
      final var quorum =
          new JointConsensusVoteQuorum(
              callback,
              java.util.Set.of(MemberId.from("n1"), MemberId.from("n2"), MemberId.from("n3")),
              java.util.Set.of(MemberId.from("n4"), MemberId.from("n5"), MemberId.from("n6")));

      quorum.succeed(MemberId.from("n1"));
      quorum.succeed(MemberId.from("n2"));
      quorum.succeed(MemberId.from("n4"));
      quorum.succeed(MemberId.from("n5"));

      verify(callback, times(1)).accept(true);
    }

    @Test
    void oldSideMajorityAloneIsInsufficient() {
      final var callback = recorder();
      final var quorum = jointQuorum(callback);

      quorum.succeed(MemberId.from("n1"));
      quorum.succeed(MemberId.from("n4"));

      verify(callback, never()).accept(anyBoolean());
    }

    @Test
    void newSideMajorityAloneIsInsufficient() {
      final var callback = recorder();
      final var quorum = jointQuorum(callback);

      quorum.succeed(MemberId.from("n1"));
      quorum.succeed(MemberId.from("n3"));

      verify(callback, never()).accept(anyBoolean());
    }

    @Test
    void failuresOnBothSidesDecideNegatively() {
      final var callback = recorder();
      final var quorum = jointQuorum(callback);

      quorum.fail(MemberId.from("n1"));
      quorum.fail(MemberId.from("n4"));

      verify(callback, times(1)).accept(false);
    }

    @Test
    void lateSuccessesCannotOverturnFailure() {
      final var callback = recorder();
      final var quorum = jointQuorum(callback);

      quorum.fail(MemberId.from("n1"));
      quorum.fail(MemberId.from("n4"));
      quorum.succeed(MemberId.from("n1"));
      quorum.succeed(MemberId.from("n2"));
      quorum.succeed(MemberId.from("n3"));

      verify(callback, times(1)).accept(false);
      verify(callback, never()).accept(true);
    }
  }
}
