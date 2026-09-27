/*
 * Copyright © 2026 anyilanxin zxh(anyilanxin@aliyun.com)
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
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.anyilanxin.kunpeng.sink.rdbms;

import static org.assertj.core.api.Assertions.assertThat;

import com.anyilanxin.kunpeng.protocol.business.ValueType;
import com.anyilanxin.kunpeng.protocol.business.record.RecordType;
import com.anyilanxin.kunpeng.protocol.business.record.command.processinstance.ProcessInstanceLifeCycle;
import org.junit.jupiter.api.Test;

class RdbmsRecordMatcherTest {

  private final RdbmsRecordMatcher matcher = new RdbmsRecordMatcher();

  @Test
  void shouldAcceptOnlyEvents() {
    assertThat(matcher.acceptsRecordType(RecordType.EVENT)).isTrue();
    assertThat(matcher.acceptsRecordType(RecordType.COMMAND)).isFalse();
    assertThat(matcher.acceptsRecordType(RecordType.COMMAND_REJECTION)).isFalse();
  }

  @Test
  void shouldAcceptOnlyHandledValueTypes() {
    assertThat(matcher.acceptsValueType(ValueType.PROCESS_INSTANCE)).isTrue();
    assertThat(matcher.acceptsValueType(ValueType.VARIABLE)).isTrue();
    assertThat(matcher.acceptsValueType(ValueType.DEPLOYMENT)).isFalse();
  }

  @Test
  void shouldIgnoreUnknownLifecycle() {
    assertThat(matcher.acceptsIntent(ProcessInstanceLifeCycle.LISTENER_CREATE)).isTrue();
  }
}
