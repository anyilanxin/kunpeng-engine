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
package com.anyilanxin.kunpeng.cluster.raft;

import static org.assertj.core.api.Assertions.assertThat;

import com.anyilanxin.kunpeng.cluster.raft.storage.log.entry.ApplicationEntry;
import com.anyilanxin.kunpeng.cluster.raft.zeebe.EntryValidator;
import com.anyilanxin.kunpeng.cluster.raft.zeebe.EntryValidator.ValidationResult;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;
import org.junit.Rule;
import org.junit.Test;

public final class EntryValidationTest {

  private final TestEntryValidator entryValidator = new TestEntryValidator();

  @Rule
  public RaftRule raftRule = RaftRule.withBootstrappedNodes(3).setEntryValidator(entryValidator);

  @Test
  public void shouldValidateEntryWithLastAfterFailOver() throws Exception {
    // given
    entryValidator.validation =
        (last, current) -> {
          assertThat(last).isNull();
          return ValidationResult.ok();
        };
    raftRule.appendEntry();

    final CountDownLatch latch = new CountDownLatch(1);
    entryValidator.validation =
        (last, current) -> {
          assertThat(last).isNotNull();
          assertThat(last.lowestPosition()).isEqualTo(1);
          assertThat(last.highestPosition()).isEqualTo(11);
          latch.countDown();

          return ValidationResult.ok();
        };
    raftRule.shutdownLeader();
    raftRule.awaitNewLeader();

    // when
    raftRule.appendEntry();

    // then
    assertThat(latch.await(10, TimeUnit.SECONDS)).isTrue();
  }

  private static final class TestEntryValidator implements EntryValidator {
    BiFunction<ApplicationEntry, ApplicationEntry, ValidationResult> validation;

    @Override
    public ValidationResult validateEntry(
        final ApplicationEntry lastEntry, final ApplicationEntry entry) {
      return validation.apply(lastEntry, entry);
    }
  }
}
