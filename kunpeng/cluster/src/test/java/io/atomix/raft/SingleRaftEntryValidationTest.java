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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.atomix.raft.storage.log.entry.ApplicationEntry;
import io.atomix.raft.storage.log.entry.InitialEntry;
import io.atomix.raft.storage.log.entry.SerializedApplicationEntry;
import io.atomix.raft.zeebe.EntryValidator;
import io.atomix.raft.zeebe.EntryValidator.ValidationResult;
import io.atomix.test.util.TestUtil;
import java.util.Collection;
import java.util.function.BiFunction;
import org.junit.Rule;
import org.junit.Test;

public class SingleRaftEntryValidationTest {

  private final TestEntryValidator entryValidator = new TestEntryValidator();

  @Rule
  public RaftRule raftRule = RaftRule.withBootstrappedNodes(1).setEntryValidator(entryValidator);

  @Test
  public void shouldFailAppendOnInvalidEntry() {
    // given
    entryValidator.validation = (last, current) -> ValidationResult.failure("invalid");

    // when - then expect
    assertThatThrownBy(() -> raftRule.appendEntry()).hasMessageContaining("invalid");
  }

  @Test
  public void shouldNotAppendInvalidEntryToLog() throws Exception {
    // given
    entryValidator.validation = (last, current) -> ValidationResult.failure("invalid");

    // when
    assertThatThrownBy(() -> raftRule.appendEntry()).hasMessageContaining("invalid");
    entryValidator.validation = (last, current) -> ValidationResult.ok();
    TestUtil.waitUntil(() -> getEntryTypeCount(InitialEntry.class) == 2);
    raftRule.appendEntry();

    // then
    assertThat(getEntryTypeCount(SerializedApplicationEntry.class)).isOne();
  }

  private int getEntryTypeCount(final Class type) {
    return (int)
        raftRule.getMemberLogs().values().stream()
            .flatMap(Collection::stream)
            .filter(e -> e.entry().getClass().isAssignableFrom(type))
            .count();
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
