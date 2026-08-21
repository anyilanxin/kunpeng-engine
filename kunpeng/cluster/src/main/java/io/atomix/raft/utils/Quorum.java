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

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Quorum arithmetic and acknowledgement counting, following the model used by jraft's {@code
 * Configuration#quorum()} and {@code Ballot}: a quorum of an n-member voting set is {@code n / 2 +
 * 1}, and during a joint (two-phase) configuration a round is only granted when majorities of both
 * the old and the new member sets have acknowledged.
 */
public final class Quorum {

  private Quorum() {}

  /**
   * The number of acknowledgements needed to form a quorum in a voting set of the given size.
   *
   * @param votingSetSize the number of voting (ACTIVE) members, including the local member
   * @return the majority threshold, {@code votingSetSize / 2 + 1}
   */
  public static int majorityOf(final int votingSetSize) {
    return votingSetSize / 2 + 1;
  }

  /**
   * Finds the smallest value that a majority of the given values is at least at — the
   * acknowledgement-counting equivalent of "the value carried by a quorum". Every distinct value is
   * tried from the largest down, counting how many members reported a value at least as high; the
   * first value whose count reaches the majority threshold is the quorum value. Values are counted
   * per member exactly once, mirroring a ballot where each member grants at most one vote.
   *
   * @param values the values reported by the voting members (may include the local member's own
   *     value)
   * @return empty when there are no values, otherwise the majority-acknowledged value
   */
  public static <T extends Comparable<T>> Optional<T> majorityAcknowledgedValue(
      final Collection<T> values) {
    if (values.isEmpty()) {
      return Optional.empty();
    }

    final int threshold = majorityOf(values.size());
    final List<T> distinctDescending =
        values.stream().distinct().sorted(Comparator.reverseOrder()).toList();

    for (final T candidate : distinctDescending) {
      int acknowledgements = 0;
      for (final T value : values) {
        if (value.compareTo(candidate) >= 0) {
          acknowledgements++;
        }
      }
      if (acknowledgements >= threshold) {
        return Optional.of(candidate);
      }
    }

    // Unreachable: the smallest distinct value is backed by at least one member, and a set of one
    // already forms its own majority. Kept for safety.
    return Optional.empty();
  }
}
